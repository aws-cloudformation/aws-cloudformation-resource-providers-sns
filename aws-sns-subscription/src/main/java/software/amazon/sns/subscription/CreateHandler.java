package software.amazon.sns.subscription;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.SubscriptionLimitExceededException;
import software.amazon.awssdk.services.sns.model.FilterPolicyLimitExceededException;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.InternalErrorException;
import software.amazon.awssdk.services.sns.model.NotFoundException;
import software.amazon.awssdk.services.sns.model.AuthorizationErrorException;
import software.amazon.awssdk.services.sns.model.InvalidSecurityException;
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

public class CreateHandler extends BaseHandlerStd {
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
                    .then(progress ->
                        proxy.initiate("AWS-SNS-Subscription::Create", proxyClient, model, callbackContext)
                        .translateToServiceRequest(Translator::translateToCreateRequest)
                        .makeServiceCall((subscribeRequest, client) -> createSubscription(subscribeRequest, client, model))
                        .progress())
                    .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));

    }

    private SubscribeResponse createSubscription(
        final SubscribeRequest subscribeRequest,
        final ProxyClient<SnsClient> proxyClient,
        final ResourceModel model)  {

        final SubscribeResponse subscribeResponse;

        // exception thrown if topic not found
        retrieveTopicAttributes(Translator.translateToCheckTopicRequest(model), proxyClient);

        try {
            subscribeResponse = proxyClient.injectCredentialsAndInvokeV2(subscribeRequest, proxyClient.client()::subscribe);
            model.setSubscriptionArn(subscribeResponse.subscriptionArn());
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
        }  catch (final Exception e) {
            throw new CfnInternalFailureException(e);
        }

        logger.log(String.format("%s successfully created.", ResourceModel.TYPE_NAME));
        return subscribeResponse;
    }

}
