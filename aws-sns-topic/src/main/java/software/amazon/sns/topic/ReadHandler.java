package software.amazon.sns.topic;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.GetDataProtectionPolicyResponse;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicResponse;
import software.amazon.awssdk.services.sns.model.ListTagsForResourceResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Map;

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
                                    GetDataProtectionPolicyResponse getDataProtectionPolicyResponse = null;
                                    if (!isFifoTopic(getTopicAttributesResponse.attributes())) { // only standard topic supports data protection policy
                                        getDataProtectionPolicyResponse = invokeGetDataProtectionPolicy(sdkProxyClient, resourceModel.getTopicArn(), logger);
                                    }
                                    return ProgressEvent.success(Translator.translateFromGetTopicAttributes(getTopicAttributesResponse, listSubscriptionsByTopicResponse, listTagsForResourceResponse, getDataProtectionPolicyResponse), callbackContext);
                                }));
    }

    private boolean isFifoTopic(Map<String, String> topicAttributes) {
        return null != topicAttributes
                && topicAttributes.containsKey("FifoTopic")
                && Boolean.parseBoolean(topicAttributes.get("FifoTopic"));
    }
}
