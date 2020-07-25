package software.amazon.sns.subscription;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;

import java.util.List;

public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<SnsClient> proxyClient,
        final Logger logger) {

        checkTopicExists(Translator.translateToCheckTopicRequest(request.getDesiredResourceState()), proxyClient);
        final ListSubscriptionsByTopicRequest listSubscriptionsByTopicRequest = 
                                                    ListSubscriptionsByTopicRequest.builder()
                                                    .nextToken(request.getNextToken())
                                                    .topicArn(request.getDesiredResourceState().getTopicArn())
                                                    .build();

        final ListSubscriptionsByTopicResponse listSubscriptionsByTopicResponse = proxy.injectCredentialsAndInvokeV2(listSubscriptionsByTopicRequest, proxyClient.client()::listSubscriptionsByTopic);

        final List<ResourceModel> models = Translator.translateFromListRequest(listSubscriptionsByTopicResponse);

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .nextToken(listSubscriptionsByTopicResponse.nextToken())
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
