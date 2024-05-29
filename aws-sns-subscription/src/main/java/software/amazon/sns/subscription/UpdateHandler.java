package software.amazon.sns.subscription;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.cloudformation.exceptions.CfnNotUpdatableException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

import java.util.Map;
import java.util.Objects;


public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<SnsClient> proxyClient,
        final Logger logger) {

        this.logger = logger;
        ResourceModel resourceModel = request.getDesiredResourceState();
        final String subscriptionArn = resourceModel.getSubscriptionArn();

        //Note that although we check the existence of Subscription ARN, it does not necessarily mean it's a valid one
        //Subscription could be PendingConfirmation
        if (resourceModel == null || com.amazonaws.util.StringUtils.isNullOrEmpty(subscriptionArn)) {
            return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.InvalidRequest, "Subscription ARN is required");
        }

        logger.log(String.format("[StackId: %s, ClientRequestToken: %s] Calling Update SNS Subscription", request.getStackId(),
                request.getClientRequestToken()));

        final ResourceModel currentModel = request.getDesiredResourceState();
        final ResourceModel previousModel = request.getPreviousResourceState();

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress -> checkSubscriptionExistsAndSetSubscriptionArn(proxy, proxyClient, request, progress, callbackContext, false, logger))
            .then(progress -> validateRegionUpdate(previousModel, currentModel, request, progress))
            .then(progress -> modifyString(proxy, proxyClient, currentModel.getFilterPolicyScope(), currentModel, SubscriptionAttribute.FilterPolicyScope, previousModel.getFilterPolicyScope(), progress, logger))
            .then(progress -> modifyPolicy(proxy, proxyClient, currentModel.getFilterPolicy(), currentModel, SubscriptionAttribute.FilterPolicy, previousModel.getFilterPolicy(), progress, logger))
            .then(progress -> modifyPolicy(proxy, proxyClient, currentModel.getDeliveryPolicy(), currentModel, SubscriptionAttribute.DeliveryPolicy,previousModel.getDeliveryPolicy(), progress, logger))
            .then(progress -> modifyPolicy(proxy, proxyClient, currentModel.getRedrivePolicy(), currentModel, SubscriptionAttribute.RedrivePolicy,previousModel.getRedrivePolicy(), progress, logger))
            .then(progress -> modifyBoolean(proxy, proxyClient, currentModel.getRawMessageDelivery(), currentModel, SubscriptionAttribute.RawMessageDelivery,previousModel.getRawMessageDelivery(), progress, logger))
            .then(progress -> modifyString(proxy, proxyClient, currentModel.getSubscriptionRoleArn(), currentModel, SubscriptionAttribute.SubscriptionRoleArn, previousModel.getSubscriptionRoleArn(), progress, logger))
            .then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));
    }

    private ProgressEvent<ResourceModel, CallbackContext> validateRegionUpdate(ResourceModel previousModel, ResourceModel currentModel, ResourceHandlerRequest<ResourceModel> request, ProgressEvent<ResourceModel, CallbackContext> progress) {
        String prevRegion = previousModel.getRegion();
        String curRegion = currentModel.getRegion();
        final String currentRequestRegion = request.getRegion();

        if (!StringUtils.equals(
                prevRegion == null ? currentRequestRegion : prevRegion,
                curRegion == null ? currentRequestRegion : curRegion)) {
            throw new CfnNotUpdatableException(ResourceModel.TYPE_NAME, currentModel.getRegion());
        }

        return progress;
    }

    private ProgressEvent<ResourceModel, CallbackContext> modifyString(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<SnsClient> proxyClient,
            String desiredString,
            ResourceModel currentModel,
            SubscriptionAttribute subscriptionAttribute,
            String previousString,
            ProgressEvent<ResourceModel, CallbackContext> progress,
            Logger logger) {

        if (StringUtils.equals(desiredString, previousString)) {
            return progress;
        }

        return proxy.initiate("AWS-SNS-Subscription::Update-"+subscriptionAttribute.name(), proxyClient, currentModel, progress.getCallbackContext())
                .translateToServiceRequest((resouceModel) -> Translator.translateToUpdateRequest(subscriptionAttribute, resouceModel, previousString, desiredString))
                .makeServiceCall((setSubscriptionAttributesRequest, client) -> proxyClient.injectCredentialsAndInvokeV2(setSubscriptionAttributesRequest, proxyClient.client()::setSubscriptionAttributes))
                .handleError((awsRequest, exception, client, resouceModel, context) -> handleError(awsRequest, exception, client, resouceModel, context))
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> modifyBoolean(
        AmazonWebServicesClientProxy proxy,
        ProxyClient<SnsClient> proxyClient,
        Boolean rawBoolean,
        ResourceModel model,
        SubscriptionAttribute subscriptionAttribute,
        Boolean previousBoolean,
        ProgressEvent<ResourceModel, CallbackContext> progress,
        Logger logger) {

        final Boolean desiredRawMessageDelivery = rawBoolean != null ? rawBoolean : Boolean.FALSE;
        if (previousBoolean == null || desiredRawMessageDelivery.equals(previousBoolean)) {
            return progress;
        }

        return proxy.initiate("AWS-SNS-Subscription::Update-"+subscriptionAttribute.name(), proxyClient, model, progress.getCallbackContext())
                .translateToServiceRequest((resouceModel) -> Translator.translateToUpdateRequest(subscriptionAttribute, resouceModel, previousBoolean, desiredRawMessageDelivery))
                .makeServiceCall((setSubscriptionAttributesRequest, client) -> proxyClient.injectCredentialsAndInvokeV2(setSubscriptionAttributesRequest, proxyClient.client()::setSubscriptionAttributes))
                .handleError((awsRequest, exception, client, resouceModel, context) -> handleError(awsRequest, exception, client, resouceModel, context))
                .progress();
    }


    protected ProgressEvent<ResourceModel, CallbackContext> modifyPolicy(
        AmazonWebServicesClientProxy proxy,
        ProxyClient<SnsClient> proxyClient,
        Map<String, Object> desiredPolicy,
        ResourceModel model,
        SubscriptionAttribute subscriptionAttribute,
        Map<String, Object> previousPolicy,
        ProgressEvent<ResourceModel, CallbackContext> progress,
        Logger logger) {

        if (Objects.equals(desiredPolicy, previousPolicy)) {
            return progress;
        }

        return proxy.initiate("AWS-SNS-Subscription::Update-"+subscriptionAttribute.name(), proxyClient, model, progress.getCallbackContext())
                .translateToServiceRequest((resouceModel) -> Translator.translateToUpdateRequest(subscriptionAttribute, resouceModel, previousPolicy, desiredPolicy))
                .makeServiceCall((setSubscriptionAttributesRequest, client) -> proxyClient.injectCredentialsAndInvokeV2(setSubscriptionAttributesRequest, proxyClient.client()::setSubscriptionAttributes))
                .handleError((awsRequest, exception, client, resouceModel, context) -> handleError(awsRequest, exception, client, resouceModel, context))
                .progress();
    }
}
