package software.amazon.sns.topicpolicy;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

public class ListHandler extends BaseHandler<CallbackContext> {

    /**
     * Dummy implementation returning default success.
     *
     * @param proxy
     *            {@link AmazonWebServicesClientProxy} to initiate proxy chain
     * @param request
     *            {@link ResourceHandlerRequest<ResourceModel>}
     * @param callbackContext
     *            {@link CallbackContext}
     * @param logger
     *            {@link Logger}
     * @return {@link ProgressEvent<ResourceModel, CallbackContext>}
     */

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        return ProgressEvent.<ResourceModel, CallbackContext> builder()
                .resourceModels(Translator.translateFromListRequest())
                .nextToken(null)
                .status(OperationStatus.FAILED)
                .errorCode(HandlerErrorCode.InvalidRequest)
                .message("List operation is not supported.")
                .build();
    }
}
