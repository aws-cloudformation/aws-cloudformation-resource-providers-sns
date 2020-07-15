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

        return proxy.initiate("AWS-SNS-Subscription::Delete", proxyClient, model, callbackContext)
                        .translateToServiceRequest(Translator::translateToDeleteRequest)
                        .makeServiceCall(this::deleteSubscription)
                        .stabilize(this::stabilizeOnDelete)
                        .done(awsResponse -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                            .status(OperationStatus.SUCCESS)
                            .build());
    }

    private UnsubscribeResponse deleteSubscription(
        final UnsubscribeRequest unsubscribeRequest,
        final ProxyClient<SnsClient> proxyClient) {

        UnsubscribeResponse unsubscribeResponse = null;
        String token = null;
        
        // TODO figure out what exception is thrown when subscription pending = true TODO

        // UnsubscribeRequest unsubscribeRequest = UnsubscribeRequest.builder()
        //                                         .subscriptionArn(getSubscriptionArnForTopic(subscribeRequest, proxyClient, token))
        //                                         .build();
        
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
    // private Boolean subscriptionCanBeDeleted(final UnsubscribeRequest unsubscribeRequest, final ProxyClient<SnsClient> proxyClient) {

    //     GetSubscriptionAttributesRequest getSubscriptionAttributesRequest = GetSubscriptionAttributesRequest.builder()
    //                                                                         .subscriptionArn(unsubscribeRequest.subscriptionArn()).build();

    //     GetSubscriptionAttributesResponse getSubscriptionAttributesResponse = proxyClient.client().getSubscriptionAttributes(getSubscriptionAttributesRequest);
    //     Map<String, String> attributes = getSubscriptionAttributesResponse.attributes();
    //     if (attributes.get("PendingConfirmation").equals("false"))
    //         return true;
    //     return false;
    // }
    // private String getSubscriptionArnForTopic(final SubscribeRequest subscribeRequest,
    // final ProxyClient<SnsClient> proxyClient, String token) {
    //     ListSubscriptionsByTopicResponse listSubscriptionsByTopicResponse = proxyClient.client().listSubscriptionsByTopic(ListSubscriptionsByTopicRequest
    //     .builder()
    //     .topicArn(subscribeRequest.topicArn()).build());

    //     if (listSubscriptionsByTopicResponse.hasSubscriptions()) {
    //         for (Subscription subscription : listSubscriptionsByTopicResponse.subscriptions()) {
    //             if ((subscription.protocol().compareTo(subscribeRequest.protocol()) == 0)
    //                 && (subscription.endpoint().compareTo(subscribeRequest.endpoint()) == 0)) {
    //                     return subscription.subscriptionArn();
    //                 }
    //         }
    //     } 
        
    //     token = listSubscriptionsByTopicResponse.nextToken();

    //     if (token == null) {
    //         return token;
    //     }

    //     return getSubscriptionArnForTopic(subscribeRequest, proxyClient, token);
    // }

}
