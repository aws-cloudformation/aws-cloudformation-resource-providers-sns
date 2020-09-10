package software.amazon.sns.topic;

import com.google.common.collect.Sets;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesResponse;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.NotFoundException;
import software.amazon.awssdk.services.sns.model.SnsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

  public static final int TOPIC_NAME_MAX_LENGTH = 256;

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
      logger
    );
  }

  protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
    final AmazonWebServicesClientProxy proxy,
    final ResourceHandlerRequest<ResourceModel> request,
    final CallbackContext callbackContext,
    final ProxyClient<SnsClient> proxyClient,
    final Logger logger);

  protected ProgressEvent<ResourceModel, CallbackContext> addSubscription(AmazonWebServicesClientProxy proxy, ProxyClient<SnsClient> client, ProgressEvent<ResourceModel, CallbackContext> progress, Set<Subscription> subscriptions, Logger logger) {
    final ResourceModel model = progress.getResourceModel();
    final CallbackContext callbackContext = progress.getCallbackContext();

    if(subscriptions == null) {
      return ProgressEvent.progress(model, callbackContext);
    }
    for(final Subscription subscription : subscriptions) {
      final ProgressEvent<ResourceModel, CallbackContext> progressEvent = proxy
              .initiate("AWS-SNS-Topic::Subscribe-" + subscription.hashCode(), client, model, callbackContext)
              .translateToServiceRequest(model1 -> Translator.traslateToSubscribeRequest(model1, subscription))
              .makeServiceCall((subscriptionRequest, proxyClient) -> proxy.injectCredentialsAndInvokeV2(subscriptionRequest, proxyClient.client()::subscribe))
              .success();

      if (!progressEvent.isSuccess()) {
        return progressEvent;
      }
    }
    return ProgressEvent.progress(model, callbackContext);
  }

  protected ProgressEvent<ResourceModel, CallbackContext> removeSubscription(AmazonWebServicesClientProxy proxy, ProxyClient<SnsClient> client, ProgressEvent<ResourceModel, CallbackContext> progress, Logger logger) {
    final ResourceModel model = progress.getResourceModel();
    final CallbackContext callbackContext = progress.getCallbackContext();
    final List<String> unsubscribeArnList = callbackContext.getSubscriptionArnToUnsubscribe();

    if(unsubscribeArnList == null) {
      return ProgressEvent.progress(model, callbackContext);
    }

    for(final String subscriptionArn : unsubscribeArnList) {
      if(!"PendingConfirmation".equals(subscriptionArn)) {
        final ProgressEvent<ResourceModel, CallbackContext> progressEvent = proxy
                .initiate("AWS-SNS-Topic::Unsubscribe-" + subscriptionArn.hashCode(), client, model, callbackContext)
                .translateToServiceRequest(model1 -> Translator.translateToUnsubscribe(subscriptionArn))
                .makeServiceCall((subscriptionRequest, proxyClient) -> proxy.injectCredentialsAndInvokeV2(subscriptionRequest, proxyClient.client()::unsubscribe))
                .success();

        if (!progressEvent.isSuccess()) {
          return progressEvent;
        }
      }
    }
    return ProgressEvent.progress(model, callbackContext);
  }

  protected ProgressEvent<ResourceModel, CallbackContext> modifyTags(AmazonWebServicesClientProxy proxy, ProxyClient<SnsClient> proxyClient, ResourceModel model, Map<String, String> desiredResourceTags, Set<Tag> existingTags, ProgressEvent<ResourceModel, CallbackContext> progress, Logger logger) {
    final Set<Tag> currentTags = Translator.convertResourceTagsToSet(desiredResourceTags);
    final Set<Tag> previousTags = new HashSet<>(Optional.ofNullable(existingTags).orElse(Collections.emptySet()));
    final Set<Tag> tagsToRemove = Sets.difference(previousTags, currentTags);
    final Set<Tag> tagsToAdd = Sets.difference(currentTags, previousTags);

    if (tagsToRemove.size() > 0) {
      proxyClient.injectCredentialsAndInvokeV2(Translator.translateToUntagRequest(model.getId(), tagsToRemove), proxyClient.client()::untagResource);
    }
    if (tagsToAdd.size() > 0) {
      proxyClient.injectCredentialsAndInvokeV2(Translator.translateToTagRequest(model.getId(), tagsToAdd), proxyClient.client()::tagResource);
    }
    return progress;
  }

  protected GetTopicAttributesResponse getTopicAttributes(
          final GetTopicAttributesRequest getTopicAttributesRequest,
          final ProxyClient<SnsClient> proxyClient) {
    GetTopicAttributesResponse getTopicAttributesResponse;
    try {
      getTopicAttributesResponse = proxyClient.injectCredentialsAndInvokeV2(getTopicAttributesRequest, proxyClient.client()::getTopicAttributes);
    } catch (final NotFoundException e) {
      throw new CfnNotFoundException(ResourceModel.TYPE_NAME, getTopicAttributesRequest.topicArn(), e);
    } catch (final InvalidParameterException e) {
      throw new CfnInvalidRequestException(e);
    } catch (final SnsException e) {
      throw new CfnGeneralServiceException(e);
    } catch (final SdkException e) {
      throw new CfnInternalFailureException(e);
    }
    return getTopicAttributesResponse;
  }
}
