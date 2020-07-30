package software.amazon.sns.topicpolicy;

import java.util.List;

import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<SnsClient> proxyClient,
            final Logger logger) {

        super.logger = logger;
        super.handler = "AWS-SNS-TopicPolicy::Update";

        final ResourceModel model = request.getDesiredResourceState();
        logger.log(String.format("In UpdateHandler : handleRequest ") + model.getPrimaryIdentifier());
        // primary id must be set up
        if (StringUtils.isNullOrEmpty(model.getId())) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, "cannot update a resource which does not exist.");
        }
        // Check if invalid request
        if (CollectionUtils.isNullOrEmpty(model.getPolicyDocument())
                || CollectionUtils.isNullOrEmpty(model.getTopics()))
        {
            throw new CfnInvalidRequestException("Invalid Update Request.");
        }

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> initCallbackContext(proxy, proxyClient, request, callbackContext, progress))
                .then(progress -> doCreate(proxy, proxyClient, request, progress))
                .then(progress -> doDelete(proxy, proxyClient, request, progress))
                .then(progress -> ProgressEvent.success(model, callbackContext));
    }

    /**
     * Invocation of initCallbackContext initializes the context & stores topics in it.
     *
     * @param proxy
     *            {@link AmazonWebServicesClientProxy} to initiate proxy chain
     * @param client
     *            the aws service client {@link ProxyClient<SnsClient>} to make the call
     * @param request
     *            {@link ResourceHandlerRequest<ResourceModel>}
     * @param callbackContext
     *            {@link CallbackContext}
     * @param progress
     *            {@link ProgressEvent<ResourceModel, CallbackContext>} to place hold the current progress data
     * @return {@link ProgressEvent<ResourceModel, CallbackContext>}
     */

    private ProgressEvent<ResourceModel, CallbackContext> initCallbackContext(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<SnsClient> proxyClient,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            ProgressEvent<ResourceModel, CallbackContext> progress) {
        final ResourceModel model = request.getDesiredResourceState();
        final ResourceModel previousState = request.getPreviousResourceState();

        final CallbackContext currentContext = callbackContext == null ? CallbackContext
                .builder()
                .build() : callbackContext;
        try {
            final String policyDocument = MAPPER.writeValueAsString(model.getPolicyDocument());
            // store policy document in the callback context
            currentContext.setPolicyDocument(policyDocument);
            // store new topics in the callback context
            List<String> newTopics = model.getTopics();
            currentContext.setTopics(newTopics);
            // store previous topics in the callback context
            List<String> previousTopics = previousState.getTopics();
            // extract the topics that needs to be deleted.
            previousTopics.removeAll(newTopics);
            currentContext.setPreviousTopics(previousTopics);

        } catch (JsonProcessingException e) {
            throw new CfnInvalidRequestException(e);
        }
        logger.log(String.format("In initCallbackContext, PrimaryIdentifier : %s", model.getPrimaryIdentifier()));
        return ProgressEvent.progress(model, currentContext);
    }

    /**
     * Invocation of doDelete calls setTopicAttributes to delete topic-policy of a given topic ARN.
     *
     * @param proxy
     *            {@link AmazonWebServicesClientProxy} to initiate proxy chain
     * @param client
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

        final CallbackContext callbackContext = progress.getCallbackContext();
        List<String> previousTopics = callbackContext.getPreviousTopics();
        return deleteResourceHandler(proxy, proxyClient, request, progress, previousTopics);
    }

}
