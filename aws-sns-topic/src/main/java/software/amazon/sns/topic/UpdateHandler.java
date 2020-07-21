package software.amazon.sns.topic;

import com.google.common.collect.Sets;
import org.codehaus.plexus.util.StringUtils;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicResponse;
import software.amazon.awssdk.services.sns.model.NotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.*;
import java.util.stream.Collectors;

public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<SnsClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        ResourceModel model = request.getDesiredResourceState();
        ResourceModel previousModel = request.getPreviousResourceState();

        Set<Subscription> desiredSubscription = new HashSet<>(Optional.ofNullable(model.getSubscription()).orElse(Collections.emptySet()));
        Set<Subscription> previousSubscription = new HashSet<>(Optional.ofNullable(previousModel.getSubscription()).orElse(Collections.emptySet()));
        Set<Subscription> toSubscribe = Sets.difference(desiredSubscription, previousSubscription);
        Set<Subscription> toUnsubscribe = Sets.difference(previousSubscription, desiredSubscription);

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress ->
                proxy.initiate("AWS-SNS-Topic::Update::PreExistanceCheck", proxyClient, model, callbackContext)
                    .translateToServiceRequest(Translator::translateToGetTopicAttributes)
                    .makeServiceCall((getTopicAttributesRequest, client) -> proxy.injectCredentialsAndInvokeV2(getTopicAttributesRequest, client.client()::getTopicAttributes))
                    .handleError((awsRequest, exception, client, resourceModel, context) -> {
                         if (exception instanceof NotFoundException)
                             throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getId(), exception);
                         throw exception;
                    })
                    .progress()
            )
            .then(progress -> {
                if(!StringUtils.equals(model.getDisplayName(), previousModel.getDisplayName())) {
                    return proxy.initiate("AWS-SNS-Topic::Update::DisplayName", proxyClient, model, callbackContext)
                            .translateToServiceRequest(m -> Translator.translateToSetAttributesRequest(m.getId(), TopicAttributes.DISPLAY_NAME, m.getDisplayName()))
                            .makeServiceCall((setTopicAttributesRequest, client) -> proxy.injectCredentialsAndInvokeV2(setTopicAttributesRequest, client.client()::setTopicAttributes))
                            .progress();
                }
                return progress;
            })
            .then(progress -> {
                if(!StringUtils.equals(model.getKmsMasterKeyId(), previousModel.getKmsMasterKeyId())) {
                    return proxy.initiate("AWS-SNS-Topic::Update::KMSKeyId", proxyClient, model, callbackContext)
                            .translateToServiceRequest(m -> Translator.translateToSetAttributesRequest(m.getId(), TopicAttributes.KMS_MASTER_KEY_ID, m.getKmsMasterKeyId()))
                            .makeServiceCall((setTopicAttributesRequest, client) -> proxy.injectCredentialsAndInvokeV2(setTopicAttributesRequest, client.client()::setTopicAttributes))
                            .progress();
                }
                return progress;
            })
            .then(progress -> addSubscription(proxy, proxyClient, progress, toSubscribe, logger))
            .then(progress ->
                proxy.initiate("AWS-SNS-Topic::Update::ListSubscriptionArn", proxyClient, model, callbackContext)
                    .translateToServiceRequest(Translator::translateToListSubscriptionByTopic)
                    .makeServiceCall((listSubscriptionsByTopicRequest, client) -> {
                        ListSubscriptionsByTopicResponse response = proxy.injectCredentialsAndInvokeV2(listSubscriptionsByTopicRequest, client.client()::listSubscriptionsByTopic);
                        List<String> unsubscriptionArnList = getUnsubscriptionArnList(response.subscriptions(), toUnsubscribe);
                        callbackContext.setSubscriptionArnToUnsubscribe(unsubscriptionArnList);
                        return response;
                    })
                    .progress()
            )
            .then(progress -> removeSubscription(proxy, proxyClient, progress, logger))
            .then(progress -> modifyTags(proxy, proxyClient, model, previousModel.getTags(), progress, logger))
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private List<String> getUnsubscriptionArnList(List<software.amazon.awssdk.services.sns.model.Subscription> subscriptions, Set<Subscription> unsubscribes) {
        Map<String, String> subscriptionArnMap = Translator.streamOfOrEmpty(subscriptions)
                .collect(Collectors.toMap(subscription -> getEndpointProtocolString(subscription.endpoint(), subscription.protocol()), subscription -> subscription.subscriptionArn()));

        return Translator.streamOfOrEmpty(unsubscribes).map(u -> subscriptionArnMap.get(getEndpointProtocolString(u.getEndpoint(), u.getProtocol()))).collect(Collectors.toList());
    }

    private String getEndpointProtocolString(String endpoint, String protocol) {
        return String.format("[%s][%s]", endpoint, protocol);
    }
}
