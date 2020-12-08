package software.amazon.sns.topicpolicy;

import com.amazonaws.util.StringUtils;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

public class CreateHandler extends BaseHandlerStd {

    private final int MAX_LENGTH_SNS_TOPICPOLICY_ID = 256;
    private final String CREATE_HANDLER = "AWS-SNS-TopicPolicy::Create";

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<SnsClient> proxyClient,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        // Check if invalid request
        if (CollectionUtils.isNullOrEmpty(model.getPolicyDocument())
                || CollectionUtils.isNullOrEmpty(model.getTopics()))
        {
            throw new CfnInvalidRequestException(
                    String.format("Invalid create request, topics & policy document cannot be null or empty : %s)",
                            model.toString()));
        }
        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> initCallbackContextAndPrimaryIdentifier(proxy, proxyClient, request, callbackContext,
                        progress))
                .then(progress -> doCreate(proxy, proxyClient, request, progress, ACTION_CREATED, CREATE_HANDLER, logger ))
                .then(progress -> ProgressEvent.success(model, callbackContext));
    }

    /**
     * Invocation of initCallbackContextAndPrimaryIdentifier generates primary identifier, initializes the context &
     * stores topics in it.
     *
     * @param request
     *            {@link ResourceHandlerRequest}
     * @param callbackContext
     *            {@link CallbackContext}
     * @return {@link ProgressEvent}
     */

    private ProgressEvent<ResourceModel, CallbackContext> initCallbackContextAndPrimaryIdentifier(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<SnsClient> proxyClient,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            ProgressEvent<ResourceModel, CallbackContext> progress) {

        final CallbackContext currentContext = callbackContext == null ? CallbackContext
                .builder()
                .build() : callbackContext;
        final ResourceModel model = request.getDesiredResourceState();
        // setting up primary id if not provided
        if (StringUtils.isNullOrEmpty(model.getId())) {
            final String Id = IdentifierUtils.generateResourceIdentifier(
                    request.getLogicalResourceIdentifier() == null ? "SnsTopicPolicy"
                            : request.getLogicalResourceIdentifier(),
                    request.getClientRequestToken(),
                    MAX_LENGTH_SNS_TOPICPOLICY_ID);
            model.setId(Id.toLowerCase());
        }
        return ProgressEvent.progress(model, currentContext);
    }
}
