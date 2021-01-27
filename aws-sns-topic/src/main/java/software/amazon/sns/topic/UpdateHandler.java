package software.amazon.sns.topic;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

        final ResourceModel model = request.getDesiredResourceState();
        final ResourceModel previousModel = request.getPreviousResourceState();
        final Map<String, String> desiredResourceTags = request.getDesiredResourceTags();

        Set<Subscription> desiredSubscription = new HashSet<>(Optional.ofNullable(model.getSubscription()).orElse(Collections.emptySet()));
        Set<Subscription> previousSubscription = new HashSet<>(Optional.ofNullable(previousModel.getSubscription()).orElse(Collections.emptySet()));
        Set<Subscription> toSubscribe = Sets.difference(desiredSubscription, previousSubscription);
        Set<Subscription> toUnsubscribe = Sets.difference(previousSubscription, desiredSubscription);

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-SNS-Topic::Update::PreExistanceCheck", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToGetTopicAttributes)
                                .makeServiceCall(this::getTopicAttributes)
                                .progress()
                )
                .then(progress -> {
                    if(!StringUtils.equals(model.getDisplayName(), previousModel.getDisplayName())) {
                        return proxy.initiate("AWS-SNS-Topic::Update::DisplayName", proxyClient, model, callbackContext)
                                .translateToServiceRequest(m -> Translator.translateToSetAttributesRequest(m.getTopicArn(), TopicAttributeName.DISPLAY_NAME, m.getDisplayName()))
                                .makeServiceCall((setTopicAttributesRequest, client) -> proxy.injectCredentialsAndInvokeV2(setTopicAttributesRequest, client.client()::setTopicAttributes))
                                .progress();
                    }
                    return progress;
                })
                .then(progress -> {
                    if(!StringUtils.equals(model.getKmsMasterKeyId(), previousModel.getKmsMasterKeyId())) {
                        return proxy.initiate("AWS-SNS-Topic::Update::KMSKeyId", proxyClient, model, callbackContext)
                                .translateToServiceRequest(m -> Translator.translateToSetAttributesRequest(m.getTopicArn(), TopicAttributeName.KMS_MASTER_KEY_ID, m.getKmsMasterKeyId()))
                                .makeServiceCall((setTopicAttributesRequest, client) -> proxy.injectCredentialsAndInvokeV2(setTopicAttributesRequest, client.client()::setTopicAttributes))
                                .progress();
                    }
                    return progress;
                })
                .then(progress -> {
                    String previousVal = previousModel.getContentBasedDeduplication() != null ? previousModel.getContentBasedDeduplication().toString() : null;
                    String desiredVal =  model.getContentBasedDeduplication() != null ? model.getContentBasedDeduplication().toString() : null;
                    if (!StringUtils.equals(previousVal, desiredVal)) {
                        return proxy.initiate("AWS-SNS-Topic::Update::ContentBasedDeduplication", proxyClient, model, callbackContext)
                                .translateToServiceRequest(m -> Translator.translateToSetAttributesRequest(m.getTopicArn(), TopicAttributeName.CONTENT_BASED_DEDUPLICATION, desiredVal))
                                .makeServiceCall((setTopicAttributesRequest, client) -> proxy.injectCredentialsAndInvokeV2(setTopicAttributesRequest, client.client()::setTopicAttributes))
                                .progress();
                    }
                    return progress;
                })
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
                .then(progress -> addSubscription(proxy, proxyClient, progress, toSubscribe, logger))
                .then(progress -> modifyTags(proxy, proxyClient, model, desiredResourceTags, previousModel.getTags(), progress, logger))
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
