package com.amazonaws.sns.topic;

import com.amazonaws.cloudformation.exceptions.CfnGeneralServiceException;
import com.amazonaws.cloudformation.exceptions.CfnInvalidRequestException;
import com.amazonaws.cloudformation.exceptions.CfnNotFoundException;
import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.OperationStatus;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest;
import software.amazon.awssdk.services.sns.model.DeleteTopicResponse;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.SnsException;

public class DeleteHandler extends BaseHandler<CallbackContext> {

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

        if (!context.isDeleteStarted()) {
            // ensure requested object exists before attempting delete
            new ReadHandler().handleRequest(proxy, request, context, logger);

            deleteTopic(request.getDesiredResourceState(), context);
        }

        try {
            request.getDesiredResourceState().setArn(context.getTopicArn());
            new ReadHandler().handleRequest(proxy, request, context, logger);
        } catch (CfnNotFoundException e) {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .status(OperationStatus.SUCCESS)
                .build();
        }

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .callbackContext(context)
            .callbackDelaySeconds(5)
            .status(OperationStatus.IN_PROGRESS)
            .resourceModel(request.getDesiredResourceState())
            .build();
    }

    private DeleteTopicResponse deleteTopic(
        final ResourceModel model,
        final CallbackContext callbackContext) {

        final DeleteTopicRequest request = Translator.translateToDeleteRequest(model);

        final DeleteTopicResponse response;

        try {
            response = proxy.injectCredentialsAndInvokeV2(request,
                ClientBuilder.getClient()::deleteTopic);
            logger.log(String.format("%s [%s] successfully deleted.",
                ResourceModel.TYPE_NAME, model.getTopicName()));
        } catch (final InvalidParameterException e) {
            throw new CfnInvalidRequestException(e.getMessage());
        } catch (final SnsException e) {
            throw new CfnGeneralServiceException("DeleteTopic: " + e.getMessage());
        }

        callbackContext.setDeleteStarted(true);
        callbackContext.setTopicArn(model.getArn());

        return response;
    }
}
