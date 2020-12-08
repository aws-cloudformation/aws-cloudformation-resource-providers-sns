package software.amazon.sns.subscription;


import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.GetSubscriptionAttributesRequest;
import software.amazon.cloudformation.proxy.*;


public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<SnsClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();
        logger.log("subscription arn: " + model.getSubscriptionArn());

        return ProgressEvent.progress(model, callbackContext)
            .then(progress -> checkTopicExists(proxy, proxyClient, model, progress, logger))
            .then(progress -> preliminaryGetSubscriptionCheck(Translator.translateToReadRequest(model), proxyClient, progress))
            .then(progress ->
                    proxy.initiate("AWS-SNS-Subscription::Read", proxyClient, model, callbackContext)
                        .translateToServiceRequest(Translator::translateToReadRequest)
                        .makeServiceCall((getSubscriptionAttributesRequest, client) -> readSubscriptionAttributes(getSubscriptionAttributesRequest, proxyClient))
            .done(getSubscriptionAttributesResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(getSubscriptionAttributesResponse))));
    }

    private ProgressEvent<ResourceModel, CallbackContext> preliminaryGetSubscriptionCheck(
            GetSubscriptionAttributesRequest getSubscriptionAttributesRequest,
            ProxyClient<SnsClient> proxyClient,
            ProgressEvent<ResourceModel, CallbackContext> progress)  {
        readSubscriptionAttributes(getSubscriptionAttributesRequest, proxyClient);
        return progress;
    }
}
