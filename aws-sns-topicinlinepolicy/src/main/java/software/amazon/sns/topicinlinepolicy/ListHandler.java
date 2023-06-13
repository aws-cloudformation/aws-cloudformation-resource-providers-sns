package software.amazon.sns.topicinlinepolicy;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.awssdk.services.sns.SnsClient;

public class ListHandler extends BaseHandlerStd {

    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<SnsClient> proxyClient,
            final Logger logger) {
        ResourceModel resourceModel = request.getDesiredResourceState();
        logger.log(String.format("StackId: %s, ClientRequestToken: %s] Calling List TopicInlinePolicy", request.getStackId(), request.getClientRequestToken()));
        return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.InvalidRequest,
                "Unsupported operation LIST for resource AWS::SNS::TopicInlinePolicy.");
    }

}
