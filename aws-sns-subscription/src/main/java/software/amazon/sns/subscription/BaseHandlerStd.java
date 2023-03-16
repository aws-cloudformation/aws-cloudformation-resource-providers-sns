package software.amazon.sns.subscription;

import software.amazon.awssdk.services.sns.model.GetSubscriptionAttributesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidCredentialsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SnsRequest;
import software.amazon.awssdk.regions.Region;

import java.util.Objects;


public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {
    private static String PENDING_CONFIRMATION_ERROR = "Invalid Arn \"%s\". Please verify that the subscription is confirmed before trying to update attributes";
    public static String AUTHORIZATION_ERROR= "AuthorizationError";
    public static String FILTER_POLICY_LIMIT_EXCEEDED  = "FilterPolicyLimitExceeded";
    public static String INTERNAL_ERROR = "InternalError";
    public static String INVALID_PARAMETER = "InvalidParameter";
    public static String INVALID_SECURITY = "InvalidSecurity";
    public static String NOT_FOUND = "NotFound";
    public static String SUBSCRIPTION_LIMIT_EXCEEDED = "SubscriptionLimitExceeded";

    protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<SnsClient> proxyClient,
            final Logger logger);

    @Override
    public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        return handleRequest(
                proxy,
                request,
                callbackContext != null ? callbackContext : new CallbackContext(),
                proxy.newProxy(() -> {return (request.getDesiredResourceState().getRegion() != null) ?
                        ClientBuilder.getClient(Region.of(request.getDesiredResourceState().getRegion())) :
                        ClientBuilder.getClient();}),
                logger
        );
    }

    protected ProgressEvent<ResourceModel, CallbackContext> checkSubscriptionExistsAndSetSubscriptionArn(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<SnsClient> proxyClient,
            ResourceHandlerRequest<ResourceModel> request,
            ProgressEvent<ResourceModel, CallbackContext> progress,
            final CallbackContext callbackContext,
            boolean successUponWaitingForConfirmation,
            Logger logger) {

        ResourceModel resourceModel = request.getDesiredResourceState();
        logger.log(String.format("checking if subscription exists in Topic ARN %s ", resourceModel.getTopicArn()));

        return proxy.initiate("AWS-SNS-Subscription::CheckSubscriptionExists", proxyClient, resourceModel, progress.getCallbackContext())
                .translateToServiceRequest(m -> Translator.translateToReadRequest(resourceModel))
                .makeServiceCall((readRequest, client) -> {
                    GetSubscriptionAttributesResponse getSubscriptionAttributesResponse = client.injectCredentialsAndInvokeV2(readRequest, client.client()::getSubscriptionAttributes);
                    return getSubscriptionAttributesResponse;
                })
                .handleError((awsRequest, exception, client, model, context) -> handleError(awsRequest, exception, client, model, context))
                .done(getSubscriptionAttributesResponse -> {
                    //If ARN is PendingConfirmation, it should fail for update and succeed for delete
                    if (getSubscriptionAttributesResponse.hasAttributes() && Objects.equals(getSubscriptionAttributesResponse.attributes().get(Definitions.pendingConfirmation), Definitions.subscriptionNotPending)) {
                        if (successUponWaitingForConfirmation) {
                            return ProgressEvent.success(request.getDesiredResourceState(), callbackContext, String.format(PENDING_CONFIRMATION_ERROR, resourceModel.getSubscriptionArn()));
                        } else {
                            //Fail the update
                            return ProgressEvent.failed(request.getDesiredResourceState(), callbackContext, HandlerErrorCode.NotFound, String.format(PENDING_CONFIRMATION_ERROR, resourceModel.getSubscriptionArn()));
                        }
                    //If ARN is valid, we can proceed with delete/update
                    } else {
                        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext);
                    }
                });
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleError(
            final SnsRequest request,
            final Exception e,
            final ProxyClient<SnsClient> proxyClient,
            final ResourceModel resourceModel,
            final CallbackContext callbackContext) {

        BaseHandlerException ex = null;

        if (AUTHORIZATION_ERROR.equals(getErrorCode(e))) {
            ex = new CfnAccessDeniedException(e);
        } else if(FILTER_POLICY_LIMIT_EXCEEDED.equals(getErrorCode(e))){
            ex = new CfnServiceLimitExceededException(e);
        } else if(INTERNAL_ERROR.equals(getErrorCode(e))){
            ex = new CfnInternalFailureException(e);
        } else if (INVALID_PARAMETER.equals(getErrorCode(e))) {
            ex = new CfnInvalidRequestException(e);
        } else if (INVALID_SECURITY.equals(getErrorCode(e))){
            ex = new CfnInvalidCredentialsException(e);
        } else if (NOT_FOUND.equals(getErrorCode(e))){
            ex = new CfnNotFoundException(e);
        } else if (SUBSCRIPTION_LIMIT_EXCEEDED.equals(getErrorCode(e))){
            ex = new CfnServiceLimitExceededException(e);
        } else {
            ex = new CfnGeneralServiceException(e);
        }
        return ProgressEvent.failed(resourceModel, callbackContext, ex.getErrorCode(), ex.getMessage());
    }

    protected static String getErrorCode(Exception e) {
        if (e instanceof AwsServiceException) {
            return ((AwsServiceException) e).awsErrorDetails().errorCode();
        }
        return e.getMessage();
    }
}
