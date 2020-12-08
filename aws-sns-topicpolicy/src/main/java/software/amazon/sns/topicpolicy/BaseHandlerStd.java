package software.amazon.sns.topicpolicy;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.AuthorizationErrorException;
import software.amazon.awssdk.services.sns.model.InternalErrorException;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.InvalidSecurityException;
import software.amazon.awssdk.services.sns.model.NotFoundException;
import software.amazon.awssdk.services.sns.model.SetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.SetTopicAttributesResponse;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidCredentialsException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

// The functionality that could be shared across Create/Read/Update/Delete/List Handlers

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

    private  final ObjectMapper MAPPER = new ObjectMapper();
    protected static final String ACTION_CREATED = "created";
    protected static final String ACTION_DELETED = "deleted";

    @Override
    public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {
        return handleRequest(
                proxy,
                request,
                callbackContext != null ? callbackContext : new CallbackContext(),
                proxy.newProxy(ClientBuilder::getClient),
                logger);
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<SnsClient> proxyClient,
            final Logger logger);

    /**
     * Invocation of doCreate calls setTopicAttributes to set topic-policy on a given topic ARN.
     *
     * @param proxy
     *            {@link AmazonWebServicesClientProxy} to initiate proxy chain
     * @param proxyClient
     *            the aws service client {@link ProxyClient} to make the call
     * @param request
     *            {@link ResourceHandlerRequest}
     * @param progress
     *            {@link ProgressEvent} to place hold the current progress data
     * @param action
     *             Action Performed by the Resource Handler (CREATE / UPDATE)
     * @param handler
     *            The handler invoking doCreate
     * @param logger
     *             {@link Logger}
     * @return {@link ProgressEvent}
     */
    protected ProgressEvent<ResourceModel, CallbackContext> doCreate(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<SnsClient> proxyClient,
            final ResourceHandlerRequest<ResourceModel> request,
            ProgressEvent<ResourceModel, CallbackContext> progress,
            final String action,
            final String handler,
            final Logger logger) {
        final ResourceModel model = request.getDesiredResourceState();
        final CallbackContext callbackContext = progress.getCallbackContext();
        final String policy = getPolicyDocument(request);
        List<String> topics = model.getTopics();
        for (final String topicArn : topics) {
            final ProgressEvent<ResourceModel, CallbackContext> progressEvent = proxy
                    .initiate(handler + topicArn.hashCode(), proxyClient, model, callbackContext)
                    .translateToServiceRequest((resourceModel) -> Translator.translateToRequest(topicArn, policy))
                    .makeServiceCall(
                            (topicRequest, snsClient) -> invokeSetTopicAttributes(topicRequest, snsClient, action, logger))
                    .success();
            if (!progressEvent.isSuccess()) {
                return progressEvent;
            }
        }
        return ProgressEvent.progress(model, callbackContext);
    }

    /**
     * Invocation of handleDelete calls setTopicAttributes to delete a topic-policy of a given topic ARN.
     *
     * @param proxy
     *            {@link AmazonWebServicesClientProxy} to initiate proxy chain
     * @param proxyClient
     *            the aws service client {@link ProxyClient} to make the call
     * @param request
     *            {@link ResourceHandlerRequest}
     * @param progress
     *            {@link ProgressEvent} to place hold the current progress data
     * @param topics
     *             Topics to be deleted by the resource handler
     * @param action
     *             Action performed by the resource hanlder: DELETE
     * @param handler
     *            The handler invoking handleDelete
     * @param logger
     *             {@link Logger}
     * @return {@link ProgressEvent}
     */

    protected ProgressEvent<ResourceModel, CallbackContext> handleDelete(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<SnsClient> proxyClient,
            final ResourceHandlerRequest<ResourceModel> request,
            final ProgressEvent<ResourceModel, CallbackContext> progress,
            final List<String> topics,
            final String action,
            final String handler,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final CallbackContext callbackContext = progress.getCallbackContext();
        for (final String topicArn : topics) {
            final String defaultPolicy = Translator.getDefaultPolicy(request, topicArn);
            final ProgressEvent<ResourceModel, CallbackContext> progressEvent = proxy
                    .initiate(handler + topicArn.hashCode(), proxyClient, model, callbackContext)
                    .translateToServiceRequest((resouceModel) -> Translator.translateToRequest(topicArn, defaultPolicy))
                    .makeServiceCall(
                            (topicRequest, snsClient) -> invokeSetTopicAttributes(topicRequest, snsClient, action, logger))
                    .success();
            if (!progressEvent.isSuccess()) {
                return progressEvent;
            }
        }
        return ProgressEvent.progress(model, callbackContext);
    }

    /**
     * Invocation of invokeSetTopicAttributes set's the topic-policy of a given topic ARN.
     *
     * @param setTopicAttributesRequest
     *            {@link SetTopicAttributesRequest}
     * @param proxyClient
     *            the aws service client {@link ProxyClient} to make the call
     * @param action
     *             Action performed by the resource hanlder
     * @param logger
     *             {@link Logger}
     * @return {@link SetTopicAttributesResponse}
     */

    protected SetTopicAttributesResponse invokeSetTopicAttributes(
            final SetTopicAttributesRequest setTopicAttributesRequest,
            final ProxyClient<SnsClient> proxyClient,
            final String action,
            final Logger logger) {

        SetTopicAttributesResponse setTopicAttributesResponse = null;
        try {
            setTopicAttributesResponse = proxyClient.injectCredentialsAndInvokeV2(setTopicAttributesRequest,
                    proxyClient.client()::setTopicAttributes);
        } catch (NotFoundException e) {
            throw new CfnNotFoundException(e);
        } catch (final InvalidParameterException e) {
            throw new CfnInvalidRequestException(e);
        } catch (final InternalErrorException ex) {
            throw new CfnServiceInternalErrorException(ex);
        } catch (final AuthorizationErrorException ex) {
            throw new CfnAccessDeniedException(ex);
        } catch (final InvalidSecurityException e) {
            throw new CfnInvalidCredentialsException(e);
        }
        logger.log(String.format("%s successfully %s.", ResourceModel.TYPE_NAME, action));
        return setTopicAttributesResponse;
    }

    /**
     * Invocation of getPolicyDocument returns the policy document .
     *
     * @param request
     *            {@link ResourceHandlerRequest<ResourceModel>}
     * @return Returns policy document
     */
    private String getPolicyDocument(final ResourceHandlerRequest<ResourceModel> request) {
        try {
            return MAPPER.writeValueAsString(request.getDesiredResourceState().getPolicyDocument());
        } catch (JsonProcessingException e) {
            throw new CfnInvalidRequestException(e);
        }
    }

}
