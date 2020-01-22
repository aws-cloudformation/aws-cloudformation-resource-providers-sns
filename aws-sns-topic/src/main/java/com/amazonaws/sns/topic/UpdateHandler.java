package com.amazonaws.sns.topic;

import com.amazonaws.cloudformation.exceptions.CfnGeneralServiceException;
import com.amazonaws.cloudformation.exceptions.CfnInvalidRequestException;
import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicRequest;
import software.amazon.awssdk.services.sns.model.SetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.SnsException;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;
import software.amazon.awssdk.services.sns.model.Subscription;
import software.amazon.awssdk.services.sns.model.UnsubscribeRequest;
import software.amazon.awssdk.services.sns.paginators.ListSubscriptionsByTopicIterable;

import java.util.List;
import java.util.stream.Collectors;

public class UpdateHandler extends BaseHandler<CallbackContext> {

    private AmazonWebServicesClientProxy proxy;
    private Logger logger;

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {
        final CallbackContext context = callbackContext == null ? CallbackContext.builder().build() : callbackContext;

        this.proxy = proxy;
        this.logger = logger;

        if (!context.isUpdateStarted()) {
            // ensure requested object exists before attempting update
            new ReadHandler().handleRequest(proxy, request, context, logger);

            updateTopic(request.getDesiredResourceState(), request.getPreviousResourceState());

            context.setUpdateStarted(true);
        }

        if (!context.isUpdateStabilized()) {

            final List<Subscription> currentSubscriptions = getTopicSubscriptions(request.getDesiredResourceState());

            context.setSubscriptionsToAdd(
                Translator.getSubscriptionArnsToAdd(request.getDesiredResourceState(), request.getPreviousResourceState())
            );
            context.setSubscriptionsToDelete(
                Translator.getSubscriptionArnsToDelete(request.getDesiredResourceState(), currentSubscriptions)
            );

            context.setUpdateStabilized(true);
        }


        while (!context.getSubscriptionsToAdd().isEmpty()) {
            addSubscriptions(request.getDesiredResourceState(), context);
        }

        while (!context.getSubscriptionsToDelete().isEmpty()) {
            deleteSubscriptions(context);
        }

        return new ReadHandler().handleRequest(proxy, request, context, logger);
    }

    private void updateTopic(
        final ResourceModel desiredModel,
        final ResourceModel previousModel) {

        final List<SetTopicAttributesRequest> requests = Translator.translateToUpdateRequests(desiredModel, previousModel);

        requests.forEach(this::setTopicAttribute);
    }

    private void setTopicAttribute(final SetTopicAttributesRequest request) {
        try {
            proxy.injectCredentialsAndInvokeV2(request,
                ClientBuilder.getClient()::setTopicAttributes);
            logger.log(String.format("%s [%s] successfully updated.",
                ResourceModel.TYPE_NAME, request.attributeName()));
        } catch (final InvalidParameterException e) {
            throw new CfnInvalidRequestException(e.getMessage());
        } catch (final SnsException e) {
            throw new CfnGeneralServiceException("SetTopicAttribute: " + e.getMessage());
        }
    }

    private List<Subscription> getTopicSubscriptions(final ResourceModel model) {
        try {
            ListSubscriptionsByTopicIterable iterable;
            final ListSubscriptionsByTopicRequest request = ListSubscriptionsByTopicRequest.builder()
                .topicArn(model.getArn())
                .build();
            iterable = proxy.injectCredentialsAndInvokeIterableV2(request,
                    ClientBuilder.getClient()::listSubscriptionsByTopicPaginator);
            return iterable.subscriptions()
                .stream()
                .collect(Collectors.toList());
        } catch (final InvalidParameterException e) {
            throw new CfnInvalidRequestException(e.getMessage());
        } catch (final SnsException e) {
            throw new CfnGeneralServiceException("SetTopicAttribute: " + e.getMessage());
        }
    }

    void addSubscriptions(final ResourceModel model,
                          final CallbackContext callbackContext) {
        for (com.amazonaws.sns.topic.Subscription subscription : callbackContext.getSubscriptionsToAdd()) {
            final SubscribeRequest request = SubscribeRequest.builder()
                .topicArn(model.getArn())
                .endpoint(subscription.getEndpoint())
                .protocol(subscription.getProtocol())
                .build();
            addSubscription(request);
            callbackContext.getSubscriptionsToAdd().remove(subscription);
        }
    }

    private void addSubscription(final SubscribeRequest request) {
        try {
            SubscribeResponse response = proxy.injectCredentialsAndInvokeV2(request,
                ClientBuilder.getClient()::subscribe);
            logger.log(String.format("%s successfully subscribed to %s",
                response.subscriptionArn(), request.topicArn()));
        } catch (final InvalidParameterException e) {
            throw new CfnInvalidRequestException(e.getMessage());
        } catch (final SnsException e) {
            throw new CfnGeneralServiceException("Subscribe: " + e.getMessage());
        }
    }

    void deleteSubscriptions(final CallbackContext callbackContext) {
        for (String subscriptionArn : callbackContext.getSubscriptionsToDelete()) {
            final UnsubscribeRequest request = UnsubscribeRequest.builder()
                .subscriptionArn(subscriptionArn)
                .build();
            deleteSubscription(request);
            callbackContext.getSubscriptionsToDelete().remove(subscriptionArn);
        }
    }

    private void deleteSubscription(final UnsubscribeRequest request) {
        try {
            proxy.injectCredentialsAndInvokeV2(request,
                ClientBuilder.getClient()::unsubscribe);
            logger.log(String.format("%s successfully unsubscribed",
                request.subscriptionArn()));
        } catch (final InvalidParameterException e) {
            throw new CfnInvalidRequestException(e.getMessage());
        } catch (final SnsException e) {
            throw new CfnGeneralServiceException("Unsubscribe: " + e.getMessage());
        }
    }
}
