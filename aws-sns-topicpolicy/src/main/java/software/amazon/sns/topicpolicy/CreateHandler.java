package software.amazon.sns.topicpolicy;


import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SetTopicAttributesResponse;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

import java.util.HashSet;
import java.util.Set;

public class CreateHandler extends BaseHandlerStd {
    private software.amazon.cloudformation.proxy.Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<SnsClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        ResourceModel resourceModel = request.getDesiredResourceState();

        if (resourceModel == null) {
            return ProgressEvent.failed(resourceModel, callbackContext, null, "Property PolicyDocument cannot be empty");
        }
        else if (CollectionUtils.isNullOrEmpty(resourceModel.getTopics())) {
            return ProgressEvent.failed(resourceModel, callbackContext, null, "Value of property Topics must be of type List of String");
        }
        else if (CollectionUtils.isNullOrEmpty(resourceModel.getPolicyDocument())) {
            return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.InvalidRequest, "Invalid parameter: Policy Error: null");
        }

        logger.log(String.format("[StackId: %s, ClientRequestToken: %s] Calling Create SNS TopicPolicy", request.getStackId(), request.getClientRequestToken()));

        return ProgressEvent.progress(resourceModel, callbackContext)
                .then(progress -> Create(proxy, proxyClient, request, progress, logger))
                .then(progress -> ProgressEvent.success(resourceModel, callbackContext));
    }

    protected ProgressEvent<ResourceModel, CallbackContext> Create(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<SnsClient> proxyClient,
            final ResourceHandlerRequest<ResourceModel> request,
            ProgressEvent<ResourceModel, CallbackContext> progress,
            final Logger logger) {
        final ResourceModel model = request.getDesiredResourceState();
        final CallbackContext callbackContext = progress.getCallbackContext();
        final String policy = getPolicyDocument(request);
        final Set<String> topics = new HashSet<>(model.getTopics());
        for (final String topicArn : topics) {
            final ProgressEvent<ResourceModel, CallbackContext> progressEvent = proxy
                    .initiate("AWS-SNS-TopicPolicy::Create", proxyClient, model, callbackContext)
                    .translateToServiceRequest((resourceModel) -> Translator.translateToRequest(topicArn, policy))
                    .makeServiceCall((awsRequest, client) -> {
                        SetTopicAttributesResponse response = proxyClient.injectCredentialsAndInvokeV2(awsRequest, client.client()::setTopicAttributes);
                        logger.log(String.format("Resource Created in StackId: %s", request.getStackId()));
                        model.setId(topicArn);
                        return response;
                    })
                    .handleError((awsRequest, exception, client, rModel, context) -> handleError(awsRequest, exception, client, rModel, context))
                    .success();
            if (!progressEvent.isSuccess()) {
                return progressEvent;
            }
        }
        return ProgressEvent.progress(model, callbackContext);
    }

}
