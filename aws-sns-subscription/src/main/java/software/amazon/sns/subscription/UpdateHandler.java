package software.amazon.sns.subscription;


import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidCredentialsException;
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
            .then(progress -> checkTopicExists(proxy, proxyClient, currentModel, progress, logger))   
            .then(progress -> checkSubscriptionExists(proxy, proxyClient, previousModel, progress, logger))
            .then(process -> proxy.initiate("AWS-SNS-Subscription::Check-Subscription-Exists", proxyClient, currentModel, callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((getSubscriptionAttributesRequest, client) -> {
                    final GetSubscriptionAttributesResponse getSubscriptionAttributesResponse;
                 
                    if (!currentModel.getProtocol().equals(previousModel.getProtocol()))
                        throw new CfnNotUpdatableException(ResourceModel.TYPE_NAME, String.format("Protocol for subscription %s cannot be updated!", currentModel.getSubscriptionArn()));
                
                    if (!currentModel.getTopicArn().equals(previousModel.getTopicArn()))
                        throw new CfnNotUpdatableException(ResourceModel.TYPE_NAME, String.format("TopicArn for subscription %s cannot be updated!", currentModel.getSubscriptionArn()));
                    
                    try {
                        getSubscriptionAttributesResponse = proxyClient.injectCredentialsAndInvokeV2(getSubscriptionAttributesRequest, proxyClient.client()::getSubscriptionAttributes);
                
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
