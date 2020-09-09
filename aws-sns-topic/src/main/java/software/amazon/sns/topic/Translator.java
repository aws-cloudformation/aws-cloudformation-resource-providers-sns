package software.amazon.sns.topic;

import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesResponse;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicRequest;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicResponse;
import software.amazon.awssdk.services.sns.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.sns.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.sns.model.ListTopicsRequest;
import software.amazon.awssdk.services.sns.model.ListTopicsResponse;
import software.amazon.awssdk.services.sns.model.SetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.TagResourceRequest;
import software.amazon.awssdk.services.sns.model.UnsubscribeRequest;
import software.amazon.awssdk.services.sns.model.UntagResourceRequest;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Translator {

  static CreateTopicRequest translateToCreateTopicRequest(final ResourceModel model) {
    Map<String, String> attributes = new HashMap<>();
    attributes.put(TopicAttributes.DISPLAY_NAME, model.getDisplayName());
    attributes.put(TopicAttributes.KMS_MASTER_KEY_ID, model.getKmsMasterKeyId());

    return CreateTopicRequest.builder()
            .name(model.getTopicName())
            .attributes(attributes)
            .tags(translateTagsToSdk(model.getTags()))
            .build();
  }

  static Set<software.amazon.awssdk.services.sns.model.Tag> translateTagsToSdk(Set<Tag> tags) {
    return streamOfOrEmpty(tags)
            .map(tag -> software.amazon.awssdk.services.sns.model.Tag.builder().key(tag.getKey()).value(tag.getValue()).build())
            .collect(Collectors.toSet());
  }

  static DeleteTopicRequest translateToDeleteTopic(final ResourceModel model) {
    return DeleteTopicRequest.builder()
            .topicArn(model.getId())
            .build();
  }

  static GetTopicAttributesRequest translateToGetTopicAttributes(final ResourceModel model) {
    return GetTopicAttributesRequest.builder()
            .topicArn(model.getId())
            .build();
  }

  static ListTopicsRequest translateToListTopicRequest(final String nextToken) {
    return ListTopicsRequest.builder()
            .nextToken(nextToken)
            .build();
  }

  static List<ResourceModel> translateFromListTopicRequest(final ListTopicsResponse listTopicsResponse) {
    return streamOfOrEmpty(listTopicsResponse.topics())
        .map(topic -> ResourceModel.builder()
            .id(topic.topicArn())
            .build())
        .collect(Collectors.toList());
  }

  static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
        .map(Collection::stream)
        .orElseGet(Stream::empty);
  }

  static ResourceModel translateFromGetTopicAttributes(GetTopicAttributesResponse getTopicAttributesResponse, ListSubscriptionsByTopicResponse listSubscriptionsByTopicResponse, ListTagsForResourceResponse listTagsForResourceResponse) {
    Map<String, String> attributes = getTopicAttributesResponse.attributes();

    Set<Subscription> subscriptions = streamOfOrEmpty(listSubscriptionsByTopicResponse.subscriptions())
            .map(subscription -> Subscription.builder()
                    .endpoint(subscription.endpoint())
                    .protocol(subscription.protocol())
                    .build())
            .collect(Collectors.toSet());


    return ResourceModel.builder()
            .id(attributes.get(TopicAttributes.TOPIC_ARN))
            .topicName(getTopicNameFromArn(attributes.get(TopicAttributes.TOPIC_ARN)))
            .displayName(attributes.get(TopicAttributes.DISPLAY_NAME))
            .kmsMasterKeyId(attributes.get(TopicAttributes.KMS_MASTER_KEY_ID))
            .subscription(subscriptions)
            .tags(translateTagsFromSdk(listTagsForResourceResponse.tags()))
            .build();
  }

  static Set<Tag> translateTagsFromSdk(List<software.amazon.awssdk.services.sns.model.Tag> tags) {
    return streamOfOrEmpty(tags)
            .map(tag -> Tag.builder().key(tag.key()).value(tag.value()).build())
            .collect(Collectors.toSet());
  }

  private static String getTopicNameFromArn(String arn) {
    String[] splitWords = arn.split(":");
    return splitWords[splitWords.length - 1];
  }

  static ListTagsForResourceRequest listTagsForResourceRequest(String id) {
    return ListTagsForResourceRequest.builder()
            .resourceArn(id)
            .build();
  }

  static ListSubscriptionsByTopicRequest translateToListSubscriptionByTopic(ResourceModel model) {
    return ListSubscriptionsByTopicRequest.builder()
            .topicArn(model.getId())
            .build();
  }

  public static UnsubscribeRequest translateToUnsubscribe(String subscriptionArn) {
    return UnsubscribeRequest.builder()
            .subscriptionArn(subscriptionArn)
            .build();
  }

  static SubscribeRequest traslateToSubscribeRequest(ResourceModel model, Subscription subscription) {
    return SubscribeRequest.builder()
            .topicArn(model.getId())
            .protocol(subscription.getProtocol())
            .endpoint(subscription.getEndpoint())
            .build();
  }

  static SetTopicAttributesRequest translateToSetAttributesRequest(String id, String attributName, String attributeValue) {
    return SetTopicAttributesRequest.builder()
            .topicArn(id)
            .attributeName(attributName)
            .attributeValue(attributeValue)
            .build();
  }

  static TagResourceRequest translateToTagRequest(String topicArn, Set<Tag> tags) {
    return TagResourceRequest.builder()
            .resourceArn(topicArn)
            .tags(translateTagsToSdk(tags))
            .build();
  }

  static UntagResourceRequest translateToUntagRequest(String topicArn, Set<Tag> tags) {
    Set<String> tagKeys = streamOfOrEmpty(tags).map(Tag::getKey).collect(Collectors.toSet());

    return UntagResourceRequest.builder()
            .resourceArn(topicArn)
            .tagKeys(tagKeys)
            .build();
  }
}
