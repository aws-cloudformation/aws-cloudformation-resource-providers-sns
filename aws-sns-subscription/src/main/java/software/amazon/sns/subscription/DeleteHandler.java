package software.amazon.sns.subscription;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;


public class DeleteHandler extends BaseHandlerStd {
    final private int EVENTUAL_CONSISTENCY_DELAY_SECONDS = 60;
    final private String ARN_SEPERATOR =":";
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<SnsClient> proxyClient,
        final Logger logger) {

        this.logger = logger;
        ResourceModel resourceModel = request.getDesiredResourceState();
        final String subscriptionArn = resourceModel.getSubscriptionArn();

        //Note that although we check the existence of Subscription ARN, it does not necessarily mean it's a valid one
        //Subscription could be PendingConfirmation
        if (resourceModel == null || com.amazonaws.util.StringUtils.isNullOrEmpty(subscriptionArn)) {
            return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.InvalidRequest, "Subscription ARN is required");
        }

        final String topicArn = subscriptionArn.substring(0, subscriptionArn.lastIndexOf(ARN_SEPERATOR));
        request.getDesiredResourceState().setTopicArn(topicArn);

        logger.log(String.format("[StackId: %s, ClientRequestToken: %s] Calling Delete SNS Subscription", request.getStackId(),
                request.getClientRequestToken()));

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> checkSubscriptionExistsAndSetSubscriptionArn(proxy, proxyClient, request, progress, callbackContext, true, logger))
                .then(progress -> proxy.initiate("AWS-SNS-Subscription::Delete", proxyClient, resourceModel, callbackContext)
                                    .translateToServiceRequest(Translator::translateToDeleteRequest)
                                    .makeServiceCall((unsubscribeRequest, client) -> client.injectCredentialsAndInvokeV2(unsubscribeRequest, proxyClient.client()::unsubscribe))
                                    .handleError((awsRequest, exception, client, model, context) -> handleError(awsRequest, exception, client, model, context))
                                    .progress())
                .then(progress -> {
                    if (progress.getCallbackContext().isPropagationDelay()) {
                        logger.log("Propagation delay completed");
                        return ProgressEvent.progress(progress.getResourceModel(), progress.getCallbackContext());
                    }
                    progress.getCallbackContext().setPropagationDelay(true);
                    callbackContext.setItFirstTime(false);
                    logger.log("Setting propagation delay");
                    return ProgressEvent.defaultInProgressHandler(progress.getCallbackContext(),
                            EVENTUAL_CONSISTENCY_DELAY_SECONDS, progress.getResourceModel());
                })
                .then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));
    }
}
