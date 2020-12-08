package software.amazon.sns.topicpolicy;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

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

        return ProgressEvent.failed(null,
                        null,
                        HandlerErrorCode.InvalidRequest,
                        "Read operation is not supported.") ;
    }

}
