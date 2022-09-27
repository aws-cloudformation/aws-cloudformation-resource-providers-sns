package software.amazon.sns.topic;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.AuthorizationErrorException;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicResponse;
import software.amazon.awssdk.services.sns.model.PutDataProtectionPolicyRequest;
import software.amazon.awssdk.services.sns.model.ThrottledException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
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
import java.util.ArrayList;
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

        Set<Tag> previousResourceTags = Translator.convertResourceTagsToSet(request.getPreviousResourceTags());
        Set<Tag> desiredResourceTags = Translator.convertResourceTagsToSet(request.getDesiredResourceTags());

        Set<Subscription> desiredSubscription = new HashSet<>(Optional.ofNullable(model.getSubscription()).orElse(Collections.emptyList()));
        Set<Subscription> previousSubscription = new HashSet<>(Optional.ofNullable(previousModel.getSubscription()).orElse(Collections.emptyList()));
        Set<Subscription> toSubscribe = Sets.difference(desiredSubscription, previousSubscription);
        Set<Subscription> toUnsubscribe = Sets.difference(previousSubscription, desiredSubscription);

        String previousDataProtectionPolicy = Translator.getDataProtectionPolicyAsString(previousModel);
        String desiredDataProtectionPolicy = Translator.getDataProtectionPolicyAsString(model);

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
                    if(!StringUtils.equals(model.getSignatureVersion(), previousModel.getSignatureVersion())) {
                        return proxy.initiate("AWS-SNS-Topic::Update::SignatureVersion", proxyClient, model, callbackContext)
                                .translateToServiceRequest(m -> Translator.translateToSetAttributesRequest(m.getTopicArn(), TopicAttributeName.SIGNATURE_VERSION, m.getSignatureVersion()))
                                .makeServiceCall((setTopicAttributesRequest, client) -> proxy.injectCredentialsAndInvokeV2(setTopicAttributesRequest, client.client()::setTopicAttributes))
                                .progress();
                    }
                    return progress;
                })
                .then(progress -> {
                    String previousVal = previousModel.getContentBasedDeduplication() != null ? previousModel.getContentBasedDeduplication().toString() : "false";
                    String desiredVal =  model.getContentBasedDeduplication() != null ? model.getContentBasedDeduplication().toString() : "false";
                    if (!StringUtils.equals(previousVal, desiredVal)) {
                        return proxy.initiate("AWS-SNS-Topic::Update::ContentBasedDeduplication", proxyClient, model, callbackContext)
                                .translateToServiceRequest(m -> Translator.translateToSetAttributesRequest(m.getTopicArn(), TopicAttributeName.CONTENT_BASED_DEDUPLICATION, desiredVal))
                                .makeServiceCall((setTopicAttributesRequest, client) -> proxy.injectCredentialsAndInvokeV2(setTopicAttributesRequest, client.client()::setTopicAttributes))
                                .progress();
                    }
                    return progress;
                })
                .then(progress -> {
                    if (!StringUtils.equals(previousDataProtectionPolicy, desiredDataProtectionPolicy)) {
                        return proxy.initiate("AWS-SNS-Topic::Update::DataProtectionPolicy", proxyClient, model, callbackContext)
                                .translateToServiceRequest(m -> Translator.translatePutDataProtectionPolicyRequest(model))
                                .makeServiceCall((putDataProtectionPolicyRequest, client) -> proxy.injectCredentialsAndInvokeV2(putDataProtectionPolicyRequest, client.client()::putDataProtectionPolicy))
                                .handleError(this::handlePutDataProtectionPolicyError)
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
                .then(progress -> addSubscription(proxy, proxyClient, progress, new ArrayList<>(toSubscribe), logger, false))
                .then(progress -> modifyTags(proxy, proxyClient, model, desiredResourceTags, previousResourceTags, progress, logger))
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

    private ProgressEvent<ResourceModel, CallbackContext> handlePutDataProtectionPolicyError(
            final PutDataProtectionPolicyRequest request,
            final Exception ex,
            final ProxyClient<SnsClient> proxyClient,
            final ResourceModel model,
            final CallbackContext context) {
        if (ex instanceof SdkException) {
            return translateSdkExceptionToFailure((SdkException) ex);
        }
        return ProgressEvent.defaultFailureHandler(ex, HandlerErrorCode.GeneralServiceException);
    }

    private ProgressEvent<ResourceModel, CallbackContext> translateSdkExceptionToFailure(final SdkException ex) {
        if (ex instanceof AwsServiceException) {
            return translateServiceExceptionToFailure((AwsServiceException) ex);
        }
        return ProgressEvent.defaultFailureHandler(ex, HandlerErrorCode.GeneralServiceException);
    }

    private ProgressEvent<ResourceModel, CallbackContext> translateServiceExceptionToFailure(final AwsServiceException ex) {
        if (ex instanceof AuthorizationErrorException) {
            return ProgressEvent.defaultFailureHandler(ex, HandlerErrorCode.AccessDenied);
        } else if (ex instanceof ThrottledException) {
            return ProgressEvent.defaultFailureHandler(ex, HandlerErrorCode.Throttling);
        } else if (ex instanceof InvalidParameterException) {
            return ProgressEvent.defaultFailureHandler(ex, HandlerErrorCode.InvalidRequest);
        }
        return ProgressEvent.defaultFailureHandler(ex, HandlerErrorCode.ServiceInternalError);
    }

}
