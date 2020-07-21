package software.amazon.sns.topic;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.sns.model.*;

import java.util.*;
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

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  static AwsRequest translateToReadRequest(final ResourceModel model) {
    final AwsRequest awsRequest = null;
    // TODO: construct a request
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L20-L24
    return awsRequest;
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param awsResponse the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final AwsResponse awsResponse) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L58-L73
    return ResourceModel.builder()
        //.someProperty(response.property())
        .build();
  }

  static DeleteTopicRequest translateToDeleteTopic(final ResourceModel model) {
    return DeleteTopicRequest.builder()
            .topicArn(model.getId())
            .build();
  }

  /**
   * Request to update properties of a previously created resource
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  static AwsRequest translateToFirstUpdateRequest(final ResourceModel model) {
    final AwsRequest awsRequest = null;
    // TODO: construct a request
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L45-L50
    return awsRequest;
  }

  /**
   * Request to update some other properties that could not be provisioned through first update request
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  static AwsRequest translateToSecondUpdateRequest(final ResourceModel model) {
    final AwsRequest awsRequest = null;
    // TODO: construct a request
    return awsRequest;
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
