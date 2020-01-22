package com.amazonaws.sns.topic;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import lombok.NonNull;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesResponse;
import software.amazon.awssdk.services.sns.model.ListTopicsRequest;
import software.amazon.awssdk.services.sns.model.ListTopicsResponse;
import software.amazon.awssdk.services.sns.model.SetTopicAttributesRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class Translator {

    static final String ATTRIBUTE_NAME_DISPLAY_NAME = "DisplayName";
    static final String ATTRIBUTE_NAME_KMS_MASTER_KEY_ID = "KmsMasterKeyId";
    static final String ATTRIBUTE_NAME_OWNER = "Owner";
    static final String ATTRIBUTE_NAME_SUBSCRIPTIONS_CONFIRMED = "SubscriptionsConfirmed";
    static final String ATTRIBUTE_NAME_SUBSCRIPTIONS_DELETED = "SubscriptionsDeleted";
    static final String ATTRIBUTE_NAME_SUBSCRIPTIONS_PENDING = "SubscriptionsPending";
    static final String ATTRIBUTE_NAME_TOPIC_ARN = "TopicArn";

    private Translator() {
    }

    static CreateTopicRequest translateToCreateRequest(final ResourceModel model) {
        return CreateTopicRequest.builder()
            .name(model.getTopicName())
            .attributes(translateAttributesForCreate(model))
            .build();
    }

    private static Map<String, String> translateAttributesForCreate(final ResourceModel model) {
        final Map<String, String> attributes = new HashMap<>();
        attributes.put(ATTRIBUTE_NAME_DISPLAY_NAME, model.getDisplayName());
        attributes.put(ATTRIBUTE_NAME_KMS_MASTER_KEY_ID, model.getKmsMasterKeyId());
        return attributes;
    }

    static GetTopicAttributesRequest translateToReadRequest(final ResourceModel model) {
        return GetTopicAttributesRequest.builder()
            .topicArn(model.getArn())
            .build();
    }

    static List<SetTopicAttributesRequest> translateToUpdateRequests(final ResourceModel desiredModel,
                                                                     final ResourceModel previousModel) {
        final List<SetTopicAttributesRequest> requests = new ArrayList<>();

        if (!desiredModel.getDisplayName().equals(previousModel.getDisplayName())) {
            requests.add(SetTopicAttributesRequest.builder()
                .topicArn(desiredModel.getArn())
                .attributeName(ATTRIBUTE_NAME_DISPLAY_NAME)
                .attributeValue(desiredModel.getDisplayName())
                .build());
        }

        if (!desiredModel.getKmsMasterKeyId().equals(previousModel.getKmsMasterKeyId())) {
            requests.add(SetTopicAttributesRequest.builder()
                .topicArn(desiredModel.getArn())
                .attributeName(ATTRIBUTE_NAME_KMS_MASTER_KEY_ID)
                .attributeValue(desiredModel.getKmsMasterKeyId())
                .build());
        }

        return requests;
    }

    static List<Subscription> getSubscriptionArnsToAdd(final ResourceModel desiredModel,
                                                       final ResourceModel previousModel) {

        final List<Subscription> requests = new ArrayList<>();

        List<Subscription> desiredSubscriptions = new ArrayList<>();
        if (desiredModel != null && !desiredModel.getSubscription().isEmpty()) {
            desiredSubscriptions.addAll(desiredModel.getSubscription());
        }

        List<Subscription> previousSubscriptions = new ArrayList<>();
        if (previousModel != null && !previousModel.getSubscription().isEmpty()) {
            previousSubscriptions.addAll(previousModel.getSubscription());
        }

        desiredSubscriptions.stream()
            .filter(s -> !previousSubscriptions.contains(s))
            .forEach(requests::add);

        return requests;
    }

    static List<String> getSubscriptionArnsToDelete(final ResourceModel desiredModel,
                                                    final List<software.amazon.awssdk.services.sns.model.Subscription> previousSubscriptions) {
        List<Subscription> desiredSubscriptions = new ArrayList<>();
        if (desiredModel != null && !desiredModel.getSubscription().isEmpty()) {
            desiredSubscriptions.addAll(desiredModel.getSubscription());
        }

        List<software.amazon.awssdk.services.sns.model.Subscription> previousSubscriptionsCopy = new ArrayList<>(previousSubscriptions);

        for (software.amazon.awssdk.services.sns.model.Subscription previousSubscription : previousSubscriptions) {
            for (Subscription s : desiredSubscriptions) {
                if (s.getEndpoint().equals(previousSubscription.endpoint()) &&
                    s.getProtocol().equals(previousSubscription.protocol())) {
                    previousSubscriptionsCopy.remove(previousSubscription);
                    break;
                }
            }
        }

        return previousSubscriptionsCopy.stream()
            .map(software.amazon.awssdk.services.sns.model.Subscription::subscriptionArn)
            .collect(Collectors.toList());
    }

    static DeleteTopicRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteTopicRequest.builder()
            .topicArn(model.getArn())
            .build();
    }

    static ListTopicsRequest translateToListRequest(final String nextToken) {
        return ListTopicsRequest.builder()
            .nextToken(nextToken)
            .build();
    }

    static ResourceModel translateForRead(final GetTopicAttributesResponse response) {
        final ResourceModel model = ResourceModel.builder()
            .displayName(response.attributes().getOrDefault(ATTRIBUTE_NAME_DISPLAY_NAME, null))
            .kmsMasterKeyId(response.attributes().getOrDefault(ATTRIBUTE_NAME_KMS_MASTER_KEY_ID, null))
            .owner(response.attributes().getOrDefault(ATTRIBUTE_NAME_OWNER, null))
            .arn(response.attributes().getOrDefault(ATTRIBUTE_NAME_TOPIC_ARN, null))
            .build();

        model.setSubscriptionsConfirmed(response.attributes().containsKey(ATTRIBUTE_NAME_SUBSCRIPTIONS_CONFIRMED)
            ? Integer.parseInt(response.attributes().get(ATTRIBUTE_NAME_SUBSCRIPTIONS_CONFIRMED))
            : 0);
        model.setSubscriptionsDeleted(response.attributes().containsKey(ATTRIBUTE_NAME_SUBSCRIPTIONS_DELETED)
            ? Integer.parseInt(response.attributes().get(ATTRIBUTE_NAME_SUBSCRIPTIONS_DELETED))
            : 0);
        model.setSubscriptionsPending(response.attributes().containsKey(ATTRIBUTE_NAME_SUBSCRIPTIONS_PENDING)
            ? Integer.parseInt(response.attributes().get(ATTRIBUTE_NAME_SUBSCRIPTIONS_PENDING))
            : 0);

        model.setTopicName(extractTopicName(model.getArn()));

        return model;
    }

    static List<ResourceModel> translateForList(final ListTopicsResponse response) {
        return streamOfOrEmpty(response.topics())
            .map(topic -> ResourceModel.builder()
                .arn(topic.topicArn())
                .build())
            .collect(Collectors.toList());
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
            .map(Collection::stream)
            .orElseGet(Stream::empty);
    }

    /**
     * construct an ARN according to public docs at
     * https://docs.aws.amazon.com/IAM/latest/UserGuide/list_amazonsns.html#amazonsns-resources-for-iam-policies
     */
    static String constructTopicArn(
        @NonNull final String awsAccountId,
        @NonNull final String awsRegion,
        @NonNull final String topicName) {

        return String.format("arn:%s:sns:%s:%s:%s",
            Region.getRegion(Regions.fromName(awsRegion)).getPartition(),
            awsRegion,
            awsAccountId,
            topicName);
    }

    private static String extractTopicName(@NonNull final String topicArn) {
        return topicArn.substring(topicArn.lastIndexOf(':') + 1);
    }
}
