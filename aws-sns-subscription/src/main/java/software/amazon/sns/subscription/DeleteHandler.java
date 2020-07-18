package software.amazon.sns.subscription;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.*;
import java.util.Map;

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
                    .then(process -> proxy.initiate("AWS-SNS-Subscription::Check-Subscription-Exists", proxyClient, model, callbackContext)
                        .translateToServiceRequest(Translator::translateToReadRequest)
                        .makeServiceCall((getSubscriptionAttributesRequest, client) -> {
                            GetSubscriptionAttributesResponse getSubscriptionAttributesResponse = null;

                            // note handle subscription pending
                            try {
                                if (!checkTopicExists(model.getTopicArn(), proxyClient, logger))
                                    throw new CfnNotFoundException(new Exception(String.format("topic %s not found!", model.getTopicArn())));

                                if (!checkSubscriptionExists(model.getSubscriptionArn(), proxyClient, logger))
                                    throw new CfnNotFoundException(new Exception(String.format("subscription %s not found!", model.getSubscriptionArn())));

                                if (!checkSubscriptionNotPending(model.getSubscriptionArn(), proxyClient, logger))
                                    throw new CfnNotFoundException(new Exception(String.format("subscription %s cannot be deleted if pending confirmation", model.getSubscriptionArn())));
                      
                            } catch (final NotFoundException e) {
                                throw new CfnNotFoundException(e);
                            }
                    
                            return getSubscriptionAttributesResponse;
                        })
                        .progress())
                    .then(process -> proxy.initiate("AWS-SNS-Subscription::Unsubscribe", proxyClient, model, callbackContext)
                        .translateToServiceRequest(Translator::translateToDeleteRequest)           
                        .makeServiceCall(this::deleteSubscription)
                        .stabilize(this::stabilizeOnDelete)
                        .done(awsResponse -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                            .status(OperationStatus.SUCCESS)
                            .build()));
    }

    private UnsubscribeResponse deleteSubscription(
        final UnsubscribeRequest unsubscribeRequest,
        final ProxyClient<SnsClient> proxyClient) {

        UnsubscribeResponse unsubscribeResponse = null;
        String token = null;
        
        // TODO figure out what exception is thrown when subscription pending = true TODO

        try {
            logger.log(String.format("delete subscription for topic arn: %s", unsubscribeRequest.subscriptionArn()));
            unsubscribeResponse = proxyClient.injectCredentialsAndInvokeV2(unsubscribeRequest, proxyClient.client()::unsubscribe);

        } catch (final NotFoundException e) {
            throw new CfnNotFoundException(e);
        }

        logger.log(String.format("%s successfully delete.", ResourceModel.IDENTIFIER_KEY_SUBSCRIPTIONARN));
        return unsubscribeResponse;
    }

    private boolean stabilizeOnDelete(
        final UnsubscribeRequest unsbscribeRequest,
        final UnsubscribeResponse unsubscribeResponse,
        final ProxyClient<SnsClient> proxyClient,
        final ResourceModel model,
        final CallbackContext callbackContext) {

        try {
            proxyClient.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(model), proxyClient.client()::getSubscriptionAttributes);
        } catch (final ResourceNotFoundException e) {
            return true;
        }
        return false;
    }
  

}
