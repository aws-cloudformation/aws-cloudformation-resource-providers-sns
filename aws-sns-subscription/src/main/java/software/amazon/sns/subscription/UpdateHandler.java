package software.amazon.sns.subscription;


import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.cloudformation.exceptions.*;
import software.amazon.cloudformation.proxy.*;

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

        final ResourceModel currentModel = request.getDesiredResourceState();
        final ResourceModel previousModel = request.getPreviousResourceState();

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress -> checkTopicExists(proxy, proxyClient, currentModel, progress, logger))
            .then(progress -> checkSubscriptionExists(proxy, proxyClient, previousModel, progress, logger))
            .then(progress -> validateCreateOnlyProperties(previousModel, currentModel, progress))
            .then(progress -> modifyPolicy(proxy, proxyClient, currentModel.getFilterPolicy(), currentModel, SubscriptionAttribute.FilterPolicy, previousModel.getFilterPolicy(), progress, logger))
            .then(progress -> modifyPolicy(proxy, proxyClient, currentModel.getDeliveryPolicy(), currentModel, SubscriptionAttribute.DeliveryPolicy,previousModel.getDeliveryPolicy(), progress, logger))
            .then(progress -> modifyPolicy(proxy, proxyClient, currentModel.getRedrivePolicy(), currentModel, SubscriptionAttribute.RedrivePolicy,previousModel.getRedrivePolicy(), progress, logger))
            .then(progress -> modifyRawMessageDelivery(proxy, proxyClient, currentModel.getRawMessageDelivery(), currentModel, SubscriptionAttribute.RawMessageDelivery,previousModel.getRawMessageDelivery(), progress, logger))
            .then(progress -> modifySubscriptionRoleArn(proxy, proxyClient, currentModel.getSubscriptionRoleArn(), currentModel, SubscriptionAttribute.SubscriptionRoleArn, previousModel.getSubscriptionRoleArn(), progress, logger))
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private ProgressEvent<ResourceModel, CallbackContext> modifySubscriptionRoleArn(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<SnsClient> proxyClient,
            String desiredSubscriptionRoleArn,
            ResourceModel currentModel,
            SubscriptionAttribute subscriptionAttribute,
            String previousSubscriptionRoleArn,
            ProgressEvent<ResourceModel, CallbackContext> progress,
            Logger logger) {

        if (StringUtils.equals(desiredSubscriptionRoleArn, previousSubscriptionRoleArn)) {
            return progress;
        }

        return proxy.initiate("AWS-SNS-Subscription::SubscriptionRoleArn", proxyClient, currentModel, progress.getCallbackContext())
                .translateToServiceRequest((resouceModel) -> Translator.translateToUpdateRequest(subscriptionAttribute, resouceModel, previousSubscriptionRoleArn, desiredSubscriptionRoleArn))
                .makeServiceCall(this::updateSubscription)
                .stabilize(this::stabilizeSnsSubscription)
                .progress();
    }

    private ProgressEvent<ResourceModel, CallbackContext> validateCreateOnlyProperties(ResourceModel previousModel, ResourceModel currentModel, ProgressEvent<ResourceModel, CallbackContext> progress) {

        String prevEndpoint = previousModel.getEndpoint();
        String curEndpoint = currentModel.getEndpoint();

        String prevProtocol = previousModel.getProtocol();
        String curProtocol = currentModel.getProtocol();

        String prevTopicArn = previousModel.getTopicArn();
        String curTopicArn = currentModel.getTopicArn();

        if (!arePropertiesEqual(prevEndpoint, curEndpoint)) {
            throw new CfnNotUpdatableException(ResourceModel.TYPE_NAME, currentModel.getEndpoint());
        } else if (!arePropertiesEqual(prevProtocol, curProtocol)) {
            throw new CfnNotUpdatableException(ResourceModel.TYPE_NAME, currentModel.getProtocol());
        } else if (!arePropertiesEqual(prevTopicArn, curTopicArn)) {
            throw new CfnNotUpdatableException(ResourceModel.TYPE_NAME, currentModel.getTopicArn());
        }

        return progress;
    }

    private boolean arePropertiesEqual(String value1, String value2) {
        return StringUtils.equals(value1, value2);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> modifyRawMessageDelivery(
        AmazonWebServicesClientProxy proxy,
        ProxyClient<SnsClient> proxyClient,
        Boolean rawMessageDelivery,
        ResourceModel model,
        SubscriptionAttribute subscriptionAttribute,
        Boolean previousRawMessageDelivery,
        ProgressEvent<ResourceModel, CallbackContext> progress,
        Logger logger) {

        final Boolean desiredRawMessageDelivery = rawMessageDelivery != null ? rawMessageDelivery : Boolean.FALSE;
        if (previousRawMessageDelivery == null || desiredRawMessageDelivery.equals(previousRawMessageDelivery)) {
            return progress;
        }

        return proxy.initiate("AWS-SNS-Subscription::RawMessageDelivery", proxyClient, model, progress.getCallbackContext())
                .translateToServiceRequest((resouceModel) -> Translator.translateToUpdateRequest(subscriptionAttribute, resouceModel, previousRawMessageDelivery, desiredRawMessageDelivery))
                .makeServiceCall(this::updateSubscription)
                .stabilize(this::stabilizeSnsSubscription)
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

        if (Objects.equals(desiredPolicy,previousPolicy)) {
            return progress;
        }

        return proxy.initiate("AWS-SNS-Subscription::"+subscriptionAttribute.name(), proxyClient, model, progress.getCallbackContext())
                .translateToServiceRequest((resouceModel) -> Translator.translateToUpdateRequest(subscriptionAttribute, resouceModel, previousPolicy, desiredPolicy))
                .makeServiceCall(this::updateSubscription)
                .stabilize(this::stabilizeSnsSubscription)
                .progress();

      }

    private SetSubscriptionAttributesResponse updateSubscription(
        final SetSubscriptionAttributesRequest setSubscriptionAttributesRequest,
        final ProxyClient<SnsClient> proxyClient)  {


        final SetSubscriptionAttributesResponse setSubscriptionAttributesResponse;
        try {
            setSubscriptionAttributesResponse = proxyClient.injectCredentialsAndInvokeV2(setSubscriptionAttributesRequest, proxyClient.client()::setSubscriptionAttributes);

        } catch (final SubscriptionLimitExceededException e) {
            throw new CfnServiceLimitExceededException(e);
        } catch (final FilterPolicyLimitExceededException e) {
            throw new CfnServiceLimitExceededException(e);
        } catch (final InvalidParameterException e) {
            throw new CfnInvalidRequestException(e);
        } catch (final InternalErrorException e) {
            throw new CfnInternalFailureException(e);
        } catch (final NotFoundException e) {
            throw new CfnNotFoundException(e);
        } catch (final AuthorizationErrorException e) {
            throw new CfnAccessDeniedException(e);
        } catch (final InvalidSecurityException e) {
            throw new CfnInvalidCredentialsException(e);
        } catch (final Exception e) {
            throw new CfnInternalFailureException(e);
        }

        logger.log(String.format("Subscription Arn %s is updated successfully", setSubscriptionAttributesRequest.subscriptionArn()));
        return setSubscriptionAttributesResponse;
    }
}
