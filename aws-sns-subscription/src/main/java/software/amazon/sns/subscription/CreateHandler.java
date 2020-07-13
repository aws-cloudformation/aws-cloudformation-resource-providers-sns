package software.amazon.sns.subscription;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;


public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<SnsClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        System.out.println("!!!!!");
        final ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> 
                    proxy.initiate("AWS-SNS-Subscription::Create", proxyClient, model, callbackContext)
                    .translateToServiceRequest(Translator::translateToCreateRequest)
                    .makeServiceCall(this::createSubscription)
               //     .stabilize(this::stabilizeSubscription)
                    .progress());

        // return ProgressEvent.progress(model, callbackContext)
        // .then(progress ->
        //         proxy.initiate("AWS-Kinesis-Stream::Create", proxyClient, model, callbackContext)
        //                 .translateToServiceRequest(Translator::translateToCreateStreamRequest)
        //                 .makeServiceCall(this::createStream)
        //                 .stabilize(this::stabilizeKinesisStream)
        //                 .progress())
        // .then(progress -> modifyStreamEncryption(proxy, proxyClient, model, null, progress, logger))
        // .then(progress -> modifyRetentionPeriodHours(proxy, proxyClient, model, DEFAULT_RETENTION_PERIOD_HOURS, progress, logger))
        // .then(progress -> modifyTags(proxy, proxyClient, model, null, progress, logger))
        // .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));

    }

    private SubscribeResponse createSubscription(
        final SubscribeRequest subscribeRequest,
        final ProxyClient<SnsClient> proxyClient) {

        SubscribeResponse subscribeResponse = null;
        
            // try {
        subscribeResponse = proxyClient.injectCredentialsAndInvokeV2(subscribeRequest, proxyClient.client()::subscribe);
            // }

        logger.log(String.format("%s successfully created.", ResourceModel.TYPE_NAME));
        return subscribeResponse;
    }
    

    // private CreateStreamResponse createStream(
    //     final CreateStreamRequest createStreamRequest,
    //     final ProxyClient<KinesisClient> proxyClient) {
    // CreateStreamResponse createStreamResponse = null;
    // try {
    //     createStreamResponse = proxyClient.injectCredentialsAndInvokeV2(createStreamRequest, proxyClient.client()::createStream);
    // } catch (final ResourceInUseException e) {
    //     throw new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, createStreamRequest.streamName(), e);
    // } catch (final InvalidArgumentException e) {
    //     throw new CfnInvalidRequestException(e);
    // } catch (final LimitExceededException e) {
    //     throw new CfnServiceLimitExceededException(ResourceModel.TYPE_NAME, e.getMessage(), e);
    // }

}
