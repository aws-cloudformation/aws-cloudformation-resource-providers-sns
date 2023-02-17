package software.amazon.sns.topic;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.cloudformation.exceptions.*;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.delay.Constant;

import java.time.Duration;
import java.util.*;


public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

    public static final int TOPIC_NAME_MAX_LENGTH = 256;
    public static final String FIFO_TOPIC_EXTENSION = ".fifo";

    //For stabilization backoff, set the timeout to 2 mins and duration as 15 second for initial strategy
    public static final Constant BACKOFF_STRATEGY = Constant.of().timeout(Duration.ofMinutes(2L)).delay(Duration.ofSeconds(15L)).build();
    private static final int DELAY_TIME_MILLI_SECS = 8000;

    @Override
    public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {
        return handleRequest(
                proxy,
                request,
                callbackContext != null ? callbackContext : new CallbackContext(),
                proxy.newProxy(ClientBuilder::getClient),
                logger
        );
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<SnsClient> proxyClient,
            final Logger logger);

    protected boolean checkIfTopicAlreadyExist(
            final ResourceHandlerRequest<ResourceModel> request,
            final ProxyClient<SnsClient> proxyClient,
            final String topicName,
            final Logger logger
    ) {
        String awsPartition = request.getAwsPartition();
        String region = request.getRegion();
        String accountId = request.getAwsAccountId();

        logger.log("Parsing Request INFO - Aws Paritiion: " + awsPartition + " - Region: " + region + " - Valid Account Id: " + !StringUtils.isEmpty(accountId));

        try {
            proxyClient.injectCredentialsAndInvokeV2(Translator.translateToGetTopicAttributes(awsPartition, region, accountId, topicName), proxyClient.client()::getTopicAttributes);
            return true;
        } catch (NotFoundException e) {
            logger.log(topicName + " does not exist; creating the resource.");
            return false;
        } catch (AuthorizationErrorException e) {
            throw new CfnAccessDeniedException(e);
        } catch (InternalError e) {
            throw new CfnInternalFailureException(e);
        } catch (InvalidParameterException e) {
            throw new CfnInvalidRequestException(e);
        } catch (InvalidSecurityException e) {
            throw new CfnInvalidCredentialsException(e);
        } catch (Exception e) {
            throw new CfnInternalFailureException(e);
        }
    }

    protected void createSubscriptions(
            final ProxyClient<SnsClient> proxyClient,
            final List<Subscription> subscriptions,
            final ResourceModel model,
            final Logger logger
    ) {
        if (subscriptions == null) {
            return;
        }
        // Sleep before and after calling Subscribe because SNS has inconsistency issue, then Subscribe
        // may fail. In workflow implementation, we have an initial wait time as 10 seconds.
        try {
            Thread.sleep(DELAY_TIME_MILLI_SECS);
        } catch (InterruptedException e) {
            throw new CfnGeneralServiceException(e);
        }
        for (final Subscription subscription : subscriptions) {
            SubscribeRequest subscribeRequest = Translator.translateToSubscribeRequest(model, subscription);
            proxyClient.injectCredentialsAndInvokeV2(subscribeRequest, proxyClient.client()::subscribe);
        }
    }

    protected ProgressEvent<ResourceModel, CallbackContext> addSubscription(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<SnsClient> client,
            ProgressEvent<ResourceModel, CallbackContext> progress,
            List<Subscription> subscriptions,
            Logger logger
    ) {
        final ResourceModel model = progress.getResourceModel();
        final CallbackContext callbackContext = progress.getCallbackContext();

        if (subscriptions == null) {
            return ProgressEvent.progress(model, callbackContext);
        }
        // Sleep before and after calling Subscribe because SNS has inconsistency issue, then Subscribe
        // may fail. In workflow implementation, we have an initial wait time as 10 seconds.
        try {
            Thread.sleep(DELAY_TIME_MILLI_SECS);
        } catch (InterruptedException e) {
            throw new CfnGeneralServiceException(e);
        }
        for (final Subscription subscription : subscriptions) {
            final ProgressEvent<ResourceModel, CallbackContext> progressEvent = proxy
                    .initiate("AWS-SNS-Topic::Subscribe-" + subscription.hashCode(), client, model, callbackContext)
                    .translateToServiceRequest(model1 -> Translator.translateToSubscribeRequest(model1, subscription))
                    .makeServiceCall((subscriptionRequest, proxyClient) -> proxy.injectCredentialsAndInvokeV2(subscriptionRequest, proxyClient.client()::subscribe))
                    .success();

            if (!progressEvent.isSuccess()) {
                return progressEvent;
            }
        }
        return ProgressEvent.progress(model, callbackContext);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> removeSubscription(AmazonWebServicesClientProxy proxy, ProxyClient<SnsClient> client, ProgressEvent<ResourceModel, CallbackContext> progress, Logger logger) {
        final ResourceModel model = progress.getResourceModel();
        final CallbackContext callbackContext = progress.getCallbackContext();
        final List<String> unsubscribeArnList = callbackContext.getSubscriptionArnToUnsubscribe();

        if (unsubscribeArnList == null) {
            return ProgressEvent.progress(model, callbackContext);
        }

        for (final String subscriptionArn : unsubscribeArnList) {
            if (null != subscriptionArn && !"PendingConfirmation".equals(subscriptionArn)) {
                final ProgressEvent<ResourceModel, CallbackContext> progressEvent = proxy
                        .initiate("AWS-SNS-Topic::Unsubscribe-" + subscriptionArn.hashCode(), client, model, callbackContext)
                        .translateToServiceRequest(model1 -> Translator.translateToUnsubscribe(subscriptionArn))
                        .makeServiceCall((subscriptionRequest, proxyClient) -> proxy.injectCredentialsAndInvokeV2(subscriptionRequest, proxyClient.client()::unsubscribe))
                        .success();

                if (!progressEvent.isSuccess()) {
                    return progressEvent;
                }
            }
        }
        return ProgressEvent.progress(model, callbackContext);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> modifyTags(AmazonWebServicesClientProxy proxy, ProxyClient<SnsClient> proxyClient, ResourceModel model, Set<Tag> currentTags, Set<Tag> existingTags, ProgressEvent<ResourceModel, CallbackContext> progress, Logger logger) {
        final Set<Tag> tagsToRemove = Sets.difference(existingTags, currentTags);
        final Set<Tag> tagsToAdd = Sets.difference(currentTags, existingTags);

        if (tagsToRemove.size() > 0) {
            invokeUnTagResource(proxyClient, model.getTopicArn(), tagsToRemove, logger);
        }
        if (tagsToAdd.size() > 0) {
            invokeTagResource(proxyClient, model.getTopicArn(), tagsToAdd, logger);
        }
        return progress;
    }

    protected GetTopicAttributesResponse getTopicAttributes(
            final GetTopicAttributesRequest getTopicAttributesRequest,
            final ProxyClient<SnsClient> proxyClient) {
        GetTopicAttributesResponse getTopicAttributesResponse;
        try {
            getTopicAttributesResponse = proxyClient.injectCredentialsAndInvokeV2(getTopicAttributesRequest, proxyClient.client()::getTopicAttributes);
        } catch (final NotFoundException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, getTopicAttributesRequest.topicArn(), e);
        } catch (final InvalidParameterException e) {
            throw new CfnInvalidRequestException(e);
        } catch (final SnsException e) {
            throw translateServiceExceptionToFailure(e);
        } catch (final SdkException e) {
            throw new CfnInternalFailureException(e);
        }
        return getTopicAttributesResponse;
    }

    protected ListTagsForResourceResponse invokeListTagsForResource(final ProxyClient<SnsClient> proxyClient, final String topicArn, final Logger logger) {
        try {
            return proxyClient.injectCredentialsAndInvokeV2(Translator.listTagsForResourceRequest(topicArn), proxyClient.client()::listTagsForResource);
        } catch (AuthorizationErrorException e) {
            // This is a short term fix for Fn::GetAtt backwards compatibility
            logger.log(String.format("AccessDenied error: %s for topic: %s", e.getMessage(), topicArn));
            return ListTagsForResourceResponse.builder().build();
        } catch (SnsException e) {
            throw translateServiceExceptionToFailure(e);
        }
    }

    protected ListSubscriptionsByTopicResponse invokeListSubscriptionsByTopic(final ProxyClient<SnsClient> proxyClient, final ResourceModel resourceModel, final Logger logger) {
        try {
            return proxyClient.injectCredentialsAndInvokeV2(Translator.translateToListSubscriptionByTopic(resourceModel), proxyClient.client()::listSubscriptionsByTopic);
        } catch (AuthorizationErrorException e) {
            // This is a short term fix for Fn::GetAtt backwards compatibility
            logger.log(String.format("AccessDenied error: %s for topic: %s", e.getMessage(), resourceModel.getTopicArn()));
            return ListSubscriptionsByTopicResponse.builder().build();
        } catch (SnsException e) {
            throw translateServiceExceptionToFailure(e);
        }
    }

    protected GetDataProtectionPolicyResponse invokeGetDataProtectionPolicy(final ProxyClient<SnsClient> proxyClient, final String topicArn, final Logger logger) {
        try {
            return proxyClient.injectCredentialsAndInvokeV2(Translator.getDataProtectionPolicyRequest(topicArn), proxyClient.client()::getDataProtectionPolicy);
        } catch (AuthorizationErrorException e) {
            // This is for backwards compatibility so that we don't break existing SNS CF customers.
            logger.log(String.format("AccessDenied error: %s for topic: %s", e.getMessage(), topicArn));
            return GetDataProtectionPolicyResponse.builder().build();
        } catch (SnsException e) {
            // This is for backwards compatibility so that we don't break customers in regions where MessageDataProtection is not launched.
            if (400 == e.statusCode() && e.awsErrorDetails() != null && "InvalidAction".equals(e.awsErrorDetails().errorCode())) {
                logger.log(String.format("MessageDataProtection not allowlisted error: %s for topic: %s", e.getMessage(), topicArn));
                return GetDataProtectionPolicyResponse.builder().build();
            }
            throw translateServiceExceptionToFailure(e);
        }
    }

    private void invokeUnTagResource(final ProxyClient<SnsClient> proxyClient, final String topicArn, final Set<Tag> tagsToRemove, final Logger logger) {
        try {
            proxyClient.injectCredentialsAndInvokeV2(Translator.translateToUntagRequest(topicArn, tagsToRemove), proxyClient.client()::untagResource);
        } catch (AuthorizationErrorException e) {
            logger.log(String.format("AccessDenied error: %s for topic: %s", e.getMessage(), topicArn));
            throw new CfnAccessDeniedException(e);
        } catch (SnsException e) {
            throw translateServiceExceptionToFailure(e);
        }
    }

    private void invokeTagResource(final ProxyClient<SnsClient> proxyClient, final String topicArn, final Set<Tag> tagsToAdd, final Logger logger) {
        try {
            proxyClient.injectCredentialsAndInvokeV2(Translator.translateToTagRequest(topicArn, tagsToAdd), proxyClient.client()::tagResource);
        } catch (AuthorizationErrorException e) {
            logger.log(String.format("AccessDenied error: %s for topic: %s", e.getMessage(), topicArn));
            throw new CfnAccessDeniedException(e);
        } catch (SnsException e) {
            throw translateServiceExceptionToFailure(e);
        }
    }

    private BaseHandlerException translateServiceExceptionToFailure(final SnsException ex) {
        if (ex instanceof ThrottledException || ex.isThrottlingException()) {
            return new CfnThrottlingException(ex); // CFN can retry on throttling error.
        }
        return new CfnGeneralServiceException(ex);
    }

}
