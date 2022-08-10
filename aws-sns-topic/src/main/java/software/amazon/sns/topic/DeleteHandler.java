package software.amazon.sns.topic;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicResponse;
import software.amazon.awssdk.services.sns.model.Subscription;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;
import java.util.stream.Collectors;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<SnsClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-SNS-Topic::Delete::PreDeletionCheck", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToGetTopicAttributes)
                                .makeServiceCall(this::getTopicAttributes)
                                .progress()
                )
                .then(progress ->
                        proxy.initiate("AWS-SNS-Topic::Delete", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToDeleteTopic)
                                .makeServiceCall((deleteTopicRequest, client) -> proxy.injectCredentialsAndInvokeV2(deleteTopicRequest, client.client()::deleteTopic))
                                .done(awsResponse -> {
                                    return ProgressEvent.<ResourceModel, CallbackContext>builder()
                                            .status(OperationStatus.SUCCESS)
                                            .build();
                                })
                );
    }
}
