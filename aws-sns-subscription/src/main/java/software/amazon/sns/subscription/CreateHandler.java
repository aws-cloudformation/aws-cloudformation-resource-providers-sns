package software.amazon.sns.subscription;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Objects;

public class CreateHandler extends BaseHandlerStd {

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<SnsClient> proxyClient,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.progress(model, callbackContext)

            // ensure that a matching Subscription does not already exist, as the downstream API will not enforce this check
            .then(progress -> checkForPreCreateResourceExistence(proxy, request, progress))

            // create/stabilize progress chain
            .then(progress ->
                proxy.initiate("AWS-SNS-Subscription::Create", proxyClient, model, callbackContext)
                    .translate(Translator::translateToCreateRequest)
                    .call((r, c) -> c.injectCredentialsAndInvokeV2(r,  ClientBuilder.getClient()::subscribe))
                    .handleError()
                    .progress())

            // describe call/chain to return the resource model
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private ProgressEvent<ResourceModel, CallbackContext> checkForPreCreateResourceExistence(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final ProgressEvent<ResourceModel, CallbackContext> progressEvent) {
        final ResourceModel model = progressEvent.getResourceModel();
        final CallbackContext callbackContext = progressEvent.getCallbackContext();
        try {
            new ReadHandler().handleRequest(proxy, request, callbackContext, logger);
            throw new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, Objects.toString(model.getPrimaryIdentifier()));
        } catch (CfnNotFoundException e) {
            logger.log(model.getPrimaryIdentifier() + " does not exist; creating the resource.");
            return ProgressEvent.progress(model, callbackContext);
        }
    }
}
