package com.amazonaws.sns.topic;

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesResponse;
import software.amazon.awssdk.services.sns.model.NotFoundException;

import java.util.Objects;

import static com.amazonaws.sns.topic.Translator.ATTRIBUTE_NAME_TOPIC_ARN;

public class ReadHandler extends BaseHandler<CallbackContext> {

    private AmazonWebServicesClientProxy proxy;
    private ResourceHandlerRequest<ResourceModel> request;
    private Logger logger;

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        this.proxy = proxy;
        this.request = request;
        this.logger = logger;

        return fetchTopicAndAssertExists();
    }

    private ProgressEvent<ResourceModel, CallbackContext> fetchTopicAndAssertExists() {
        final ResourceModel model = request.getDesiredResourceState();

        if (model == null ||
            (model.getTopicName() == null && model.getArn() == null)) {
            throwNotFoundException(model);
        }

        GetTopicAttributesResponse response = null;
        try {
            response = proxy.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(model),
                ClientBuilder.getClient()::getTopicAttributes);
        } catch (final NotFoundException e) {
            throwNotFoundException(model);
        }

        if (response.attributes() == null |
            !response.attributes().containsKey(ATTRIBUTE_NAME_TOPIC_ARN)) {
            throwNotFoundException(model);
        }

        final ResourceModel modelFromReadResult = Translator.translateForRead(response);
        if (modelFromReadResult.getTopicName() == null ||
            modelFromReadResult.getArn() == null) {
            throwNotFoundException(model);
        }

        return ProgressEvent.defaultSuccessHandler(modelFromReadResult);
    }

    private void throwNotFoundException(final ResourceModel model) {
        final ResourceModel nullSafeModel = model == null ? ResourceModel.builder().build() : model;
        throw new com.amazonaws.cloudformation.exceptions.ResourceNotFoundException(ResourceModel.TYPE_NAME,
            Objects.toString(nullSafeModel.getPrimaryIdentifier()));
    }
}
