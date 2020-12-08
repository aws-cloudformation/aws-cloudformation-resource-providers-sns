package software.amazon.sns.topicpolicy;

import java.util.List;

import com.amazonaws.util.StringUtils;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;
    private final String UPDATE_HANDLER = "AWS-SNS-TopicPolicy::Update";

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
        if (StringUtils.isNullOrEmpty(model.getId())) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, "Cannot update a resource which does not exist.");
        }
        // Check if invalid request
        if (CollectionUtils.isNullOrEmpty(model.getPolicyDocument())
                || CollectionUtils.isNullOrEmpty(model.getTopics()))
        {
            throw new CfnInvalidRequestException(
                    String.format("Invalid create request, policy document & topics cannot be null or empty : %s)",
                            model.toString()));
        }

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> doCreate(proxy, proxyClient, request, progress, ACTION_CREATED, UPDATE_HANDLER, logger))
                .then(progress -> doDelete(proxy, proxyClient, request, progress))
                .then(progress -> ProgressEvent.success(model, callbackContext));
    }

    /**
     * Invocation of doDelete calls setTopicAttributes to delete topic-policy of a given topic ARN.
     *
     * @param proxy
     *            {@link AmazonWebServicesClientProxy} to initiate proxy chain
     * @param proxyClient
     *            the aws service client {@link ProxyClient<SnsClient>} to make the call
     * @param request
     *            {@link ResourceHandlerRequest<ResourceModel>}
     * @param progress
     *            {@link ProgressEvent<ResourceModel, CallbackContext>} to place hold the current progress data
     * @return {@link ProgressEvent<ResourceModel, CallbackContext>}
     */

    private ProgressEvent<ResourceModel, CallbackContext> doDelete(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<SnsClient> proxyClient,
            final ResourceHandlerRequest<ResourceModel> request,
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        final ResourceModel model = request.getDesiredResourceState();
        final ResourceModel previousState = request.getPreviousResourceState();
        // new topics
        List<String> newTopics = model.getTopics();
        // previous topics
        List<String> previousTopics = previousState.getTopics();
        // extract the topics that needs to be deleted.
        previousTopics.removeAll(newTopics);
        return handleDelete(proxy, proxyClient, request, progress, previousTopics, ACTION_DELETED, UPDATE_HANDLER, logger);
    }

}
