package software.amazon.sns.topic;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;
    private static final int DESTROY_WAIT_MS = 45000;

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
                                    // Recreate the resource with same name, then subscribe may result in same subscription ARN
                                    // Have to wait long as a workaround
                                    // TODO: Need to check MDS code on our cache.
                                    try {
                                        Thread.sleep(DESTROY_WAIT_MS);
                                    } catch (InterruptedException e) {
                                        throw new CfnGeneralServiceException(e);
                                    }
                                    return ProgressEvent.<ResourceModel, CallbackContext>builder()
                                            .status(OperationStatus.SUCCESS)
                                            .build();
                                })
                );
    }
}
