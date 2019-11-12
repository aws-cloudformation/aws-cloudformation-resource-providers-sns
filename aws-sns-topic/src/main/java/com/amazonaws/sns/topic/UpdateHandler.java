package com.amazonaws.sns.topic;

import com.amazonaws.cloudformation.exceptions.CfnGeneralServiceException;
import com.amazonaws.cloudformation.exceptions.CfnInvalidRequestException;
import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.SetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.SnsException;

import java.util.List;

public class UpdateHandler extends BaseHandler<CallbackContext> {

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

        // ensure requested object exists before attempting update
        new ReadHandler().handleRequest(proxy, request, context, logger);

        updateTopic(request.getDesiredResourceState(), request.getPreviousResourceState());

        return new ReadHandler().handleRequest(proxy, request, context, logger);
    }

    private void updateTopic(
        final ResourceModel desiredModel,
        final ResourceModel previousModel) {

        final List<SetTopicAttributesRequest> requests = Translator.translateToUpdateRequests(desiredModel, previousModel);

        requests.forEach(this::setTopicAttribute);
    }

    private void setTopicAttribute(final SetTopicAttributesRequest request) {
        try {
            proxy.injectCredentialsAndInvokeV2(request,
                ClientBuilder.getClient()::setTopicAttributes);
            logger.log(String.format("%s [%s] successfully updated.",
                ResourceModel.TYPE_NAME, request.attributeName()));
        } catch (final InvalidParameterException e) {
            throw new CfnInvalidRequestException(e.getMessage());
        } catch (final SnsException e) {
            throw new CfnGeneralServiceException("SetTopicAttribute: " + e.getMessage());
        }
    }
}
