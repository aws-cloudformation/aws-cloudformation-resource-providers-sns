package software.amazon.sns.topicpolicy;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.cloudformation.proxy.*;

public class ReadHandler extends BaseHandlerStd {

    /**
     * Dummy implementation returning default success.
     *
     * @param proxy
     *            {@link AmazonWebServicesClientProxy} to initiate proxy chain
     * @param request
     *            {@link ResourceHandlerRequest}
     * @param callbackContext
     *            {@link CallbackContext}
     * @param proxyClient
     *            the aws service client {@link ProxyClient} to make the call
     * @param logger
     *            {@link Logger}
     * @return {@link ProgressEvent}
     */

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<SnsClient> proxyClient,
            final Logger logger) {

        ResourceModel resourceModel = request.getDesiredResourceState();
        return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.InvalidRequest,
                "Read operation is not supported.");
    }

}
