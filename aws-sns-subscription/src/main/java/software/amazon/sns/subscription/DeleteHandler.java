package software.amazon.sns.subscription;

// TODO: replace all usage of SdkClient with your service client type, e.g; YourServiceAsyncClient
// import software.amazon.awssdk.services.yourservice.YourServiceAsyncClient;

import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

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
                .then(progress ->
                        proxy.initiate("AWS-SNS-Subscription::Delete", proxyClient, model, callbackContext)
                        .translateToServiceRequest(Translator::translateToDeleteRequest)
                        .makeServiceCall(this::deleteSubscription)
                        .progress());
    }

    private UnsubscribeResponse deleteSubscription(
        final UnsubscribeRequest unsubscribeRequest,
        final ProxyClient<SnsClient> proxyClient) {

        UnsubscribeResponse unsubscribeResponse = null;

        // UnsubscribeRequest a = new Un
        // proxyClient.client().uns
        //     // try {
        logger.log(String.format("delete subscription for arn: %s", unsubscribeRequest.subscriptionArn()));

        System.out.println("arn " + unsubscribeRequest.subscriptionArn());
        unsubscribeResponse = proxyClient.injectCredentialsAndInvokeV2(unsubscribeRequest, proxyClient.client()::unsubscribe);
        //     // }

        logger.log(String.format("%s successfully delete.", ResourceModel.IDENTIFIER_KEY_SUBSCRIPTIONARN));
        return unsubscribeResponse;
    }

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
