package software.amazon.sns.subscription;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.awssdk.services.sns.SnsClient;


public class ListHandler extends BaseHandlerStd {
    private Logger logger;

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<SnsClient> proxyClient,
        final Logger logger) {

        this.logger = logger;
        ResourceModel resourceModel = request.getDesiredResourceState();

        if (resourceModel == null) {
            return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.InvalidRequest, "List request is invalid");
        }

        logger.log(String.format("[StackId: %s, ClientRequestToken: %s] Calling List SNS Subscription", request.getStackId(),
                request.getClientRequestToken()));

        return proxy.initiate("AWS-SNS-Subscription::List", proxyClient, resourceModel, callbackContext)
                .translateToServiceRequest(m -> Translator.translateToListSubscriptionsRequest(request.getNextToken()))
                .makeServiceCall((getSubscriptionsRequest, client) -> client.injectCredentialsAndInvokeV2(getSubscriptionsRequest, client.client()::listSubscriptions))
                .handleError((awsRequest, exception, client, model, context) -> handleError(awsRequest, exception, client, model, context))
                .done((getSubscriptionsRequest, getSubscriptionsResponse, client, resourceModel1, context) ->
                        ProgressEvent.<ResourceModel, CallbackContext>builder()
                                .resourceModels(Translator.translateFromListRequest(getSubscriptionsResponse))
                                .status(OperationStatus.SUCCESS)
                                .nextToken(getSubscriptionsResponse.nextToken())
                                .build());
    }
}
