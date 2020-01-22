package com.amazonaws.sns.topic;

import com.amazonaws.cloudformation.exceptions.CfnGeneralServiceException;
import com.amazonaws.cloudformation.exceptions.CfnInvalidRequestException;
import com.amazonaws.cloudformation.exceptions.CfnNotFoundException;
import com.amazonaws.cloudformation.exceptions.ResourceAlreadyExistsException;
import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.OperationStatus;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;
import com.amazonaws.cloudformation.resource.IdentifierUtils;
import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.SnsException;

import java.util.Objects;

import static com.amazonaws.sns.topic.Translator.constructTopicArn;

public class CreateHandler extends BaseHandler<CallbackContext> {
    private static final String DEFAULT_TOPIC_NAME_PREFIX = "Topic";
    private static final int MAX_LENGTH_TOPIC_NAME = 256;

    private AmazonWebServicesClientProxy proxy;
    private Logger logger;

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {
        final CallbackContext context = callbackContext == null ? CallbackContext.builder().build() : callbackContext;

        this.proxy = proxy;
        this.logger = logger;
        final ResourceModel model = prepareResourceModel(request);

        if (!context.isCreateStarted()) {
            try {
                request.getDesiredResourceState().setArn(model.getArn());
                new ReadHandler().handleRequest(proxy, request, context, logger);
                throw new ResourceAlreadyExistsException(ResourceModel.TYPE_NAME,
                    Objects.toString(model.getPrimaryIdentifier()));
            } catch (CfnNotFoundException e) {
                logger.log(request.getDesiredResourceState().getPrimaryIdentifier() +
                    " does not exist; creating the resource.");
            }

            createTopic(model, context);

            context.setSubscriptionsToAdd(model.getSubscription());
        }

        if (!context.isCreateStabilized()) {
            try {
                request.getDesiredResourceState().setArn(context.getTopicArn());
                context.setCreateStabilized(true);
            } catch (CfnNotFoundException e) {
                logger.log(request.getDesiredResourceState().getPrimaryIdentifier() +
                    " does not exist; retrying stabilization.");
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .callbackContext(context)
                    .callbackDelaySeconds(5)
                    .status(OperationStatus.IN_PROGRESS)
                    .resourceModel(request.getDesiredResourceState())
                    .build();
            }
        }

        while (!context.getSubscriptionsToAdd().isEmpty()) {
            new UpdateHandler().addSubscriptions(model, context);
        }

        return new ReadHandler().handleRequest(proxy, request, context, logger);
    }

    private CreateTopicResponse createTopic(
        final ResourceModel model,
        final CallbackContext callbackContext) {

        final CreateTopicRequest request = Translator.translateToCreateRequest(model);

        final CreateTopicResponse response;

        try {
            response = proxy.injectCredentialsAndInvokeV2(request,
                ClientBuilder.getClient()::createTopic);
            logger.log(String.format("%s [%s] successfully created.",
                ResourceModel.TYPE_NAME, response.topicArn()));
        } catch (final InvalidParameterException e) {
            throw new CfnInvalidRequestException(e.getMessage());
        } catch (final SnsException e) {
            throw new CfnGeneralServiceException("CreateTopic: " + e.getMessage());
        }

        callbackContext.setCreateStarted(true);
        callbackContext.setTopicArn(response.topicArn());

        return response;
    }

    /**
     * Since the resource model itself can be null, as well as any of its attributes,
     * we need to prepare a model to safely operate on. This includes:
     *
     * 1. Setting an empty logical resource ID if it is null. Each real world request should
     *    have a logical ID, but we don't want the topic name generation to depend on it.
     * 2. Generating a topic name if one is not given. This is a createOnly property,
     *    but we generate one if one is not provided.
     * 3. Generates a Topic ARN from the specified Topic Name
     */
    private ResourceModel prepareResourceModel(final ResourceHandlerRequest<ResourceModel> request) {
        if (request.getDesiredResourceState() == null) {
            request.setDesiredResourceState(new ResourceModel());
        }

        final ResourceModel model = request.getDesiredResourceState();
        final String identifierPrefix = request.getLogicalResourceIdentifier() == null ?
            DEFAULT_TOPIC_NAME_PREFIX :
            request.getLogicalResourceIdentifier();

        if (StringUtils.isNullOrEmpty(model.getTopicName())) {
            model.setTopicName(
                IdentifierUtils.generateResourceIdentifier(
                    identifierPrefix,
                    request.getClientRequestToken(),
                    MAX_LENGTH_TOPIC_NAME
                )
            );
        }

        model.setArn(constructTopicArn(
            request.getAwsAccountId(),
            request.getRegion(),
            model.getTopicName()));

        return model;
    }
}
