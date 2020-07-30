package software.amazon.sns.topicpolicy;

import java.util.Random;

import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final int MAX_LENGTH_SNS_TOPICPOLICY_ID = 256;

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<SnsClient> proxyClient,
            final Logger logger) {

        super.logger = logger;
        super.handler = "AWS-SNS-TopicPolicy::Create";
        final ResourceModel model = request.getDesiredResourceState();
        logger.log(String.format("In CreateHandler : handleRequest " + model.getPolicyDocument()));

        // Check if invalid request
        if (CollectionUtils.isNullOrEmpty(model.getPolicyDocument())
                || CollectionUtils.isNullOrEmpty(model.getTopics()))
        {
            throw new CfnInvalidRequestException("Invalid Create Request.");
        }

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> initCallbackContextAndPrimaryIdentifier(proxy, proxyClient, request, callbackContext,
                        progress))
                .then(progress -> doCreate(proxy, proxyClient, request, progress))
                .then(progress -> ProgressEvent.success(model, callbackContext));
    }

    /**
     * Invocation of initCallbackContextAndPrimaryIdentifier generates primary identifier, initializes the context &
     * stores topics in it.
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
        try {
            final String policyDocument = MAPPER.writeValueAsString(model.getPolicyDocument());
            // store policy document in the callback context
            currentContext.setPolicyDocument(policyDocument);
            // store topics in the callback context
            currentContext.setTopics(model.getTopics());
            // setting up primary id if not provided
            if (StringUtils.isNullOrEmpty(model.getId())) {
                String TOPICPOLICY_ID_PREFIX = String.valueOf(new Random().nextInt(Integer.MAX_VALUE));
                final String Id = IdentifierUtils.generateResourceIdentifier(
                        request.getLogicalResourceIdentifier() == null ? TOPICPOLICY_ID_PREFIX
                                : request.getLogicalResourceIdentifier(),
                        request.getClientRequestToken(),
                        MAX_LENGTH_SNS_TOPICPOLICY_ID);
                model.setId(Id.toLowerCase());
            }
        } catch (JsonProcessingException e) {
            throw new CfnInvalidRequestException(e);
        }
        logger.log(String.format("In initCallbackContextAndPrimaryIdentifier, PrimaryIdentifier : %s",
                model.getPrimaryIdentifier()));
        return ProgressEvent.progress(model, currentContext);
    }
}
