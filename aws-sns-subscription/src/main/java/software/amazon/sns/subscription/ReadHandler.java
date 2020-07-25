package software.amazon.sns.subscription;


import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.awssdk.services.sns.model.*;

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
             .then(progress -> 
                 proxy.initiate("AWS-SNS-Subscription::Read", proxyClient, model, callbackContext)
                 .translateToServiceRequest(Translator::translateToReadRequest)
                 .makeServiceCall((getSubscriptionAttributesRequest, client) -> {
                    return this.checkSubscriptionExists(getSubscriptionAttributesRequest, proxyClient);
                })           
            .done(getSubscriptionAttributesResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(getSubscriptionAttributesResponse))));
    }

}
