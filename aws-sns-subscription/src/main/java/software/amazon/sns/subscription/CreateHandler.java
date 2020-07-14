package software.amazon.sns.subscription;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidCredentialsException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.proxy.*;

public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    // handle issue with multiple region clients todo!!

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<SnsClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        System.out.println("!!!!!");
        final ResourceModel model = request.getDesiredResourceState();
        r
        return ProgressEvent.progress(model, callbackContext)
                    .then(progress -> 
                        proxy.initiate("AWS-SNS-Subscription::Create", proxyClient, model, callbackContext)
                        .translateToServiceRequest(Translator::translateToCreateRequest)
                        .makeServiceCall(this::createSubscription)
                        .stabilize(this::updateResourceModel)
                        .progress());
           
    }

    // is this the right place to update the subscription arn??
    private Boolean updateResourceModel(
        final SubscribeRequest subscribeRequest,
        final SubscribeResponse subscribeResponse,
        final ProxyClient<SnsClient> proxyClient,
        final ResourceModel model,
        final CallbackContext callbackContext) {
            model.setSubscriptionArn(subscribeResponse.subscriptionArn());
            System.out.println("here !!!!!!!!!!!!! " + model.getSubscriptionArn());

            if (model.getSubscriptionArn() != null && model.getSubscriptionArn().length() > 0) {
                System.out.println("true!!");
                return true;
            }
            
            return false;
        }

    private SubscribeResponse createSubscription(
        final SubscribeRequest subscribeRequest,
        final ProxyClient<SnsClient> proxyClient) {

        SubscribeResponse subscribeResponse = null;
        try {
            subscribeResponse = proxyClient.injectCredentialsAndInvokeV2(subscribeRequest, proxyClient.client()::subscribe);
       
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

        logger.log(String.format("%s successfully created.", ResourceModel.TYPE_NAME));
        return subscribeResponse;
    }

}
