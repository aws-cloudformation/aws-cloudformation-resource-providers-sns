package software.amazon.sns.subscription;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidCredentialsException;
import software.amazon.cloudformation.proxy.*;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<SnsClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.progress(model, callbackContext)
                    .then(progress -> checkTopicExists(proxy, proxyClient, model, progress, logger))
                    .then(progress -> checkSubscriptionExists(proxy, proxyClient, model, progress, logger))
                    .then(process -> proxy.initiate("AWS-SNS-Subscription::Check-Subscription-Not-Pending", proxyClient, model, callbackContext)
                        .translateToServiceRequest(Translator::translateToReadRequest)
                        .makeServiceCall((getSubscriptionAttributesRequest, client) -> {
                            if (!checkSubscriptionNotPending(model.getSubscriptionArn(), proxyClient, logger))
                                throw new CfnInvalidRequestException(new Exception(String.format("subscription %s cannot be deleted if pending confirmation", model.getSubscriptionArn())));

                            return true;
                        })
                        .stabilize(this::stabilizeSnsSubscription)
                        .progress())
                    .then(process -> proxy.initiate("AWS-SNS-Subscription::Unsubscribe", proxyClient, model, callbackContext)
                        .translateToServiceRequest(Translator::translateToDeleteRequest)           
                        .makeServiceCall(this::deleteSubscription)
                        .stabilize(this::stabilizeSnsSubscription)
                        .done(awsResponse -> {return ProgressEvent.<ResourceModel, CallbackContext>builder()
                            .status(OperationStatus.SUCCESS)
                            .build(); }));
    }

    private Boolean deleteSubscription(
        final UnsubscribeRequest unsubscribeRequest,
        final ProxyClient<SnsClient> proxyClient) {

        final UnsubscribeResponse unsubscribeResponse;
        
        try {
            logger.log(String.format("Deleting subscription for subscription arn: %s", unsubscribeRequest.subscriptionArn()));
            unsubscribeResponse = proxyClient.injectCredentialsAndInvokeV2(unsubscribeRequest, proxyClient.client()::unsubscribe);

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
        } 

   
        logger.log(String.format("%s successfully deleted.", ResourceModel.IDENTIFIER_KEY_SUBSCRIPTIONARN));
        return true;
    }  

}
