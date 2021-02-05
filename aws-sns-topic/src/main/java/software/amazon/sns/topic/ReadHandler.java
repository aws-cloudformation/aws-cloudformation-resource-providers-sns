package software.amazon.sns.topic;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicResponse;
import software.amazon.awssdk.services.sns.model.ListTagsForResourceResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<SnsClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-SNS-Topic::Read", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToGetTopicAttributes)
                                .makeServiceCall(this::getTopicAttributes)
                                .done((getTopicAttributesRequest, getTopicAttributesResponse, sdkProxyClient, resourceModel, context) -> {
                                    final ListTagsForResourceResponse listTagsForResourceResponse = invokeListTagsForResource(sdkProxyClient, resourceModel.getTopicArn(), logger);
                                    final ListSubscriptionsByTopicResponse listSubscriptionsByTopicResponse = invokeListSubscriptionsByTopic(sdkProxyClient, resourceModel, logger);
                                    return ProgressEvent.success(Translator.translateFromGetTopicAttributes(getTopicAttributesResponse, listSubscriptionsByTopicResponse, listTagsForResourceResponse), callbackContext);
                                }));
    }
}
