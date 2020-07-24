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
        
        return proxy.initiate("AWS-SNS-Subscription::Read", proxyClient, request.getDesiredResourceState(), callbackContext)
            .translateToServiceRequest(Translator::translateToReadRequest)
            .makeServiceCall((getSubscriptionAttributesRequest, client) -> {
                
                GetSubscriptionAttributesResponse getSubscriptionAttributesResponse= null;
                if (!checkTopicExists(model.getTopicArn(), proxyClient, logger))
                    throw new CfnNotFoundException(new Exception(String.format("topic %s not found!", model.getTopicArn())));

                getSubscriptionAttributesResponse = proxyClient.injectCredentialsAndInvokeV2(getSubscriptionAttributesRequest, proxyClient.client()::getSubscriptionAttributes);
                if (!getSubscriptionAttributesResponse.hasAttributes()) {
                    throw new CfnNotFoundException(new Exception(String.format("subscription %s not found!", model.getSubscriptionArn())));
                }

                return getSubscriptionAttributesResponse;
            })
            .done(getSubscriptionAttributesResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(getSubscriptionAttributesResponse)));
    }

}
