package software.amazon.sns.topicpolicy;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SetTopicAttributesResponse;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

import java.util.List;

public class DeleteHandler extends BaseHandlerStd {
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
            return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.InvalidRequest, "ResourceModel is required");
        }
        else if(CollectionUtils.isNullOrEmpty(resourceModel.getTopics())){
            return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.InvalidRequest, "Topic is required");
        }

        logger.log(String.format("[StackId: %s, ClientRequestToken: %s] Calling Delete TopicPolicy", request.getStackId(), request.getClientRequestToken()));

        return ProgressEvent.progress(resourceModel, callbackContext)
                .then(progress -> Delete(proxy, proxyClient, request, progress, logger))
                .then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }

    protected ProgressEvent<ResourceModel, CallbackContext> Delete(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<SnsClient> proxyClient,
            final ResourceHandlerRequest<ResourceModel> request,
            ProgressEvent<ResourceModel, CallbackContext> progress,
            final Logger logger) {
        final ResourceModel model = request.getDesiredResourceState();
        final CallbackContext callbackContext = progress.getCallbackContext();
        List<String> topics = model.getTopics();
        for (final String topicArn : topics) {
            String policy = getDefaultPolicy(request,topicArn);
            final ProgressEvent<ResourceModel, CallbackContext> progressEvent = proxy
                    .initiate("AWS-SNS-TopicPolicy::Delete", proxyClient, model, callbackContext)
                    .translateToServiceRequest((resourceModel) -> Translator.translateToRequest(topicArn, policy))
                    .makeServiceCall((awsRequest, client) -> {
                        SetTopicAttributesResponse response = proxyClient.injectCredentialsAndInvokeV2(awsRequest, client.client()::setTopicAttributes);
                        logger.log(String.format("Resource Deleted in StackId: %s", request.getStackId()));
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