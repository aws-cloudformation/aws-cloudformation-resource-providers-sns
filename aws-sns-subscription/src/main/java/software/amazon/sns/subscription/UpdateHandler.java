package software.amazon.sns.subscription;


import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;


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
                        throw new CfnNotFoundException(new Exception(String.format("topic %s not found!", currentModel.getTopicArn())));
                    
                    getSubscriptionAttributesResponse = proxyClient.injectCredentialsAndInvokeV2(getSubscriptionAttributesRequest, proxyClient.client()::getSubscriptionAttributes);
                    if (!getSubscriptionAttributesResponse.hasAttributes())
                        throw new CfnNotFoundException(new Exception(String.format("subscription %s not found!", currentModel.getSubscriptionArn())));

                    return getSubscriptionAttributesResponse;
                })
                .progress())
             .then(progress -> modifyPolicy(proxy, proxyClient, currentModel.getFilterPolicy(), currentModel, SubscriptionAttribute.FilterPolicy, previousModel.getFilterPolicy(), progress, logger))
             .then(progress -> modifyPolicy(proxy, proxyClient, currentModel.getDeliveryPolicy(), currentModel, SubscriptionAttribute.DeliveryPolicy,previousModel.getDeliveryPolicy(), progress, logger))
             .then(progress -> modifyPolicy(proxy, proxyClient, currentModel.getRedrivePolicy(), currentModel, SubscriptionAttribute.RedrivePolicy,previousModel.getRedrivePolicy(), progress, logger))
             .then(progress -> modifyRawMessageDelivery(proxy, proxyClient, currentModel.getRawMessageDelivery(), currentModel, SubscriptionAttribute.RawMessageDelivery,previousModel.getRawMessageDelivery(), progress, logger))
             .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}
