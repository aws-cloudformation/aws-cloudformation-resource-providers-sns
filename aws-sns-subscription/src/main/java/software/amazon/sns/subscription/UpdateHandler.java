package software.amazon.sns.subscription;


import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotUpdatableException;

public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<SnsClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel currentModel = request.getDesiredResourceState();
        final ResourceModel previousModel = request.getPreviousResourceState();

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(process -> proxy.initiate("AWS-SNS-Subscription::Check-Subscription-Exists", proxyClient, currentModel, callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((getSubscriptionAttributesRequest, client) -> {
                    GetSubscriptionAttributesResponse getSubscriptionAttributesResponse = null;
                 
                    if (!checkTopicExists(currentModel.getTopicArn(), proxyClient, logger))
                        throw new CfnNotFoundException(ResourceModel.TYPE_NAME,  String.format("topic %s not found!", currentModel.getTopicArn()));
                    
                    if (!currentModel.getProtocol().equals(previousModel.getProtocol()))
                        throw new CfnNotUpdatableException(ResourceModel.TYPE_NAME, String.format("Protocol for subscription %s cannot be updated!", currentModel.getSubscriptionArn()));
                
                    if (!currentModel.getTopicArn().equals(previousModel.getTopicArn()))
                        throw new CfnNotUpdatableException(ResourceModel.TYPE_NAME, String.format("TopicArn for subscription %s cannot be updated!", currentModel.getSubscriptionArn()));
                    
                    getSubscriptionAttributesResponse = proxyClient.injectCredentialsAndInvokeV2(getSubscriptionAttributesRequest, proxyClient.client()::getSubscriptionAttributes);
                    if (!getSubscriptionAttributesResponse.hasAttributes())
                        throw new CfnNotFoundException(ResourceModel.TYPE_NAME, String.format("subscription %s not found!", currentModel.getSubscriptionArn()));

                    return getSubscriptionAttributesResponse;
                })
                .stabilize(this::stabilizeSnsSubscription)
                .progress())
             .then(progress -> modifyPolicy(proxy, proxyClient, currentModel.getFilterPolicy(), currentModel, SubscriptionAttribute.FilterPolicy, previousModel.getFilterPolicy(), progress, logger))
             .then(progress -> modifyPolicy(proxy, proxyClient, currentModel.getDeliveryPolicy(), currentModel, SubscriptionAttribute.DeliveryPolicy,previousModel.getDeliveryPolicy(), progress, logger))
             .then(progress -> modifyPolicy(proxy, proxyClient, currentModel.getRedrivePolicy(), currentModel, SubscriptionAttribute.RedrivePolicy,previousModel.getRedrivePolicy(), progress, logger))
             .then(progress -> modifyRawMessageDelivery(proxy, proxyClient, currentModel.getRawMessageDelivery(), currentModel, SubscriptionAttribute.RawMessageDelivery,previousModel.getRawMessageDelivery(), progress, logger))
             .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}
