package software.amazon.sns.topic;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.awssdk.services.sns.model.Subscription;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.*;

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
                    .makeServiceCall((getTopicAttributesRequest, client) -> proxy.injectCredentialsAndInvokeV2(getTopicAttributesRequest, client.client()::getTopicAttributes))
                    .handleError((awsRequest, exception, client, resourceModel, context) -> {
                        if (exception instanceof NotFoundException)
                            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getId(), exception);
                        throw exception;
                    })
                    .progress()
            )
            .then(progress ->
                proxy.initiate("AWS-SNS-Topic::Unsubscribe", proxyClient, model, callbackContext)
                    .translateToServiceRequest(Translator::translateToListSubscriptionByTopic)
                    .makeServiceCall((listSubscriptionsByTopicRequest, client) -> {
                        ListSubscriptionsByTopicResponse response = proxy.injectCredentialsAndInvokeV2(listSubscriptionsByTopicRequest, client.client()::listSubscriptionsByTopic);
                        List<String> arnList = Translator.streamOfOrEmpty(response.subscriptions())
                                .map(Subscription::subscriptionArn)
                                .collect(Collectors.toList());
                        callbackContext.setSubscriptionArnToUnsubscribe(arnList);
                        return response;
                    })
                    .progress()

            )
            .then(progress -> removeSubscription(proxy, proxyClient, progress, logger))
            .then(progress ->
                proxy.initiate("AWS-SNS-Topic::Delete", proxyClient, model, callbackContext)
                    .translateToServiceRequest(Translator::translateToDeleteTopic)
                    .makeServiceCall((deleteTopicRequest, client) -> proxy.injectCredentialsAndInvokeV2(deleteTopicRequest, client.client()::deleteTopic))
                    .done(awsResponse -> {
                        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                                .status(OperationStatus.SUCCESS)
                                .resourceModel(model)
                                .callbackContext(callbackContext)
                                .build();
                    })
            );
    }
}
