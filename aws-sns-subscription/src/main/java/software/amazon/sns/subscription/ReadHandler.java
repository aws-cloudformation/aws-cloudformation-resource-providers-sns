package software.amazon.sns.subscription;

// TODO: replace all usage of SdkClient with your service client type, e.g; YourServiceAsyncClient
// import software.amazon.awssdk.services.yourservice.YourServiceAsyncClient;

import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import java.util.Map;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<SnsClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        // should this be previous state TODO check!!!
        final ResourceModel model = request.getDesiredResourceState();

        return proxy.initiate("AWS-SNS-Subscription::Read", proxyClient, request.getDesiredResourceState(), callbackContext)

            .translateToServiceRequest(Translator::translateToReadRequest)

            .makeServiceCall((getSubscriptionAttributesRequest, client) -> {
                GetSubscriptionAttributesResponse getSubscriptionAttributesResponse= null;
                try {

                    if (!checkTopicExists(model.getTopicArn(), proxyClient, logger))
                        throw new CfnNotFoundException(new Exception(String.format("topic %s not found!", model.getTopicArn())));

                    getSubscriptionAttributesResponse = proxyClient.injectCredentialsAndInvokeV2(getSubscriptionAttributesRequest, proxyClient.client()::getSubscriptionAttributes);
                

                } catch (final AwsServiceException e) { // ResourceNotFoundException

                    throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e); 
                }

                return getSubscriptionAttributesResponse;
            })
            .done(getSubscriptionAttributesResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(getSubscriptionAttributesResponse)));
    }

}
