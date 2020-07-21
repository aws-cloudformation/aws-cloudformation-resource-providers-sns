package software.amazon.sns.topic;

import com.google.common.collect.Sets;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.*;

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

  protected ProgressEvent<ResourceModel, CallbackContext> modifyTags(AmazonWebServicesClientProxy proxy, ProxyClient<SnsClient> proxyClient, ResourceModel model, Set<Tag> existingTags, ProgressEvent<ResourceModel, CallbackContext> progress, Logger logger) {
    final Set<Tag> currentTags = new HashSet<>(Optional.ofNullable(model.getTags()).orElse(Collections.emptySet()));
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
}
