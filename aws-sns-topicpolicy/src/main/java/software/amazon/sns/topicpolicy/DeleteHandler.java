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
    private Logger logger;
    private final String DELETE_HANDLER = "AWS-SNS-TopicPolicy::Delete";

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<SnsClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();
        // primary id must be set up
        if (StringUtils.isNullOrEmpty(model.getId())
                || CollectionUtils.isNullOrEmpty(model.getTopics()))
        {
            throw new CfnInvalidRequestException("Invalid delete request, must contain Primary Id.");
        }

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> doDelete(proxy, proxyClient, request, progress))
                .then(progress -> ProgressEvent.<ResourceModel, CallbackContext> builder()
                        .status(OperationStatus.SUCCESS)
                        .build());
    }

    private ProgressEvent<ResourceModel, CallbackContext> doDelete(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<SnsClient> proxyClient,
            final ResourceHandlerRequest<ResourceModel> request,
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        List<String> topics = request.getDesiredResourceState().getTopics();
        return handleDelete(proxy, proxyClient, request, progress, topics, ACTION_DELETED, DELETE_HANDLER , logger);
    }

}
