package software.amazon.sns.topicpolicy;

import java.util.List;

import com.amazonaws.util.StringUtils;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<SnsClient> proxyClient,
            final Logger logger) {

        super.logger = logger;
        super.handler = "AWS-SNS-TopicPolicy::Delete";

        final ResourceModel model = request.getDesiredResourceState();
        logger.log(String.format("In DeleteHandler : handleRequest ") + model.getPrimaryIdentifier());
        // primary id must be set up
        if (StringUtils.isNullOrEmpty(model.getId())
                || CollectionUtils.isNullOrEmpty(model.getTopics()))
        {
            throw new CfnInvalidRequestException("Invalid delete request, must contain Primary Id.");
        }

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> initCallbackContext(request, callbackContext, progress))
                .then(progress -> doDelete(proxy, proxyClient, request, progress))
                .then(progress -> ProgressEvent.<ResourceModel, CallbackContext> builder()
                        .status(OperationStatus.SUCCESS)
                        .build());
    }

    /**
     * Invocation of initCallbackContext initializes the context & stores topics in it.
     *
     * @param request
     *            {@link ResourceHandlerRequest<ResourceModel>}
     * @param callbackContext
     *            {@link CallbackContext}
     * @param progress
     *            {@link ProgressEvent<ResourceModel, CallbackContext>} to place hold the current progress data
     * @return {@link ProgressEvent<ResourceModel, CallbackContext>}
     */

    private ProgressEvent<ResourceModel, CallbackContext> initCallbackContext(
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        final ResourceModel model = request.getDesiredResourceState();
        final CallbackContext currentContext = callbackContext == null ? CallbackContext
                .builder()
                .build() : callbackContext;
        // store topics in the callback context
        currentContext.setTopics(model.getTopics());
        return ProgressEvent.progress(model, currentContext);
    }

    private ProgressEvent<ResourceModel, CallbackContext> doDelete(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<SnsClient> proxyClient,
            final ResourceHandlerRequest<ResourceModel> request,
            final ProgressEvent<ResourceModel, CallbackContext> progress) {

        final CallbackContext callbackContext = progress.getCallbackContext();
        List<String> topics = callbackContext.getTopics();
        return deleteResourceHandler(proxy, proxyClient, request, progress, topics);
    }

}
