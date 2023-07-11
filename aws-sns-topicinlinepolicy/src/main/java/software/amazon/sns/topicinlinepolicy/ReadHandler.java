package software.amazon.sns.topicinlinepolicy;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesResponse;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.HandlerErrorCode;


public class ReadHandler extends BaseHandlerStd {
    private Logger Log;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<SnsClient> proxyClient,
            final Logger logger) {
        this.Log = logger;


        final ResourceModel model = request.getDesiredResourceState();

        if (model == null || StringUtils.isNullOrEmpty(model.getTopicArn())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, EMPTY_POLICY_AND_TOPICARN_ERROR_MESSAGE );
        }

        Log.log(String.format("StackId: %s, ClientRequestToken: %s] Calling Read TopicInlinePolicy", request.getStackId(), request.getClientRequestToken()));
        String TopicArn = model.getTopicArn();
        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> {
                    if (!doesTopicPolicyExist(proxyClient, request, model)) {
                        return ProgressEvent.failed(null, callbackContext, HandlerErrorCode.NotFound, noSuchPolicyErrorMessage(model));
                    }
                    return progress;
                })
                .then(progress -> proxy.initiate("AWS-SNS-TopicInlinePolicy::Read", proxyClient, model, callbackContext)
                        .translateToServiceRequest((resourceModel) -> Translator.translateToGetRequest(TopicArn))
                        .makeServiceCall((awsRequest, client) -> {
                            GetTopicAttributesResponse response = proxyClient.injectCredentialsAndInvokeV2(awsRequest, client.client()::getTopicAttributes);
                            Log.log(String.format("Resource Read in StackId: %s", request.getStackId()));
                            return response;
                        })
                        .handleError((awsRequest, exception, client, rModel, context) -> handleError(awsRequest, exception, client, rModel, context))
                        .done(getPolicyResponse ->  ProgressEvent.defaultSuccessHandler(Translator.translateFromGetRequest(getPolicyResponse, TopicArn))));
    }

}
