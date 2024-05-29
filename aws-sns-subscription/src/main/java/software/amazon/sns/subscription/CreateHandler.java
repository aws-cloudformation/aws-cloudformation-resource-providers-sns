package software.amazon.sns.subscription;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;


public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<SnsClient> proxyClient,
        final Logger logger) {

        this.logger = logger;
        ResourceModel resourceModel = request.getDesiredResourceState();

        if (resourceModel == null || StringUtils.isNullOrEmpty(resourceModel.getProtocol())) {
            return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.InvalidRequest, "Protocol is required");
        } else if (StringUtils.isNullOrEmpty(resourceModel.getTopicArn())) {
            return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.InvalidRequest, "Topic ARN is required");
        }

        logger.log(String.format("[StackId: %s, ClientRequestToken: %s] Calling Create SNS Subscription", request.getStackId(),
                request.getClientRequestToken()));

        return proxy.initiate("AWS-SNS-Subscription::Create", proxyClient, resourceModel, callbackContext)
                    .translateToServiceRequest(Translator::translateToCreateRequest)
                    .makeServiceCall((subscribeRequest, client) -> client.injectCredentialsAndInvokeV2(subscribeRequest, client.client()::subscribe))
                    .handleError((awsRequest, exception, client, model, context) -> handleError(awsRequest, exception, client, model, context))
                    .done((subscribeRequest, subscribeResponse, client, model, callbackContext1) -> {
                        model.setSubscriptionArn(subscribeResponse.subscriptionArn());
                        return ProgressEvent.success(model, callbackContext1);
                    });
    }
}
