package software.amazon.sns.topic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest;
import software.amazon.awssdk.services.sns.model.GetDataProtectionPolicyRequest;
import software.amazon.awssdk.services.sns.model.GetDataProtectionPolicyResponse;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesResponse;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicRequest;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicResponse;
import software.amazon.awssdk.services.sns.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.sns.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.sns.model.ListTopicsRequest;
import software.amazon.awssdk.services.sns.model.ListTopicsResponse;
import software.amazon.awssdk.services.sns.model.PutDataProtectionPolicyRequest;
import software.amazon.awssdk.services.sns.model.SetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.TagResourceRequest;
import software.amazon.awssdk.services.sns.model.UnsubscribeRequest;
import software.amazon.awssdk.services.sns.model.UntagResourceRequest;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a centralized placeholder for
 * - api request construction
 * - object translation to/from aws sdk
 * - resource model construction for read/list handlers
 */

public class Translator {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static CreateTopicRequest translateToCreateTopicRequest(final ResourceModel model, Map<String, String> desiredResourceTags) {

        final Set<Tag> tags = convertResourceTagsToSet(desiredResourceTags);
        return CreateTopicRequest.builder()
                .name(model.getTopicName())
                .attributes(translateTopicAttributesToMap(model))
                .tags(translateTagsToSdk(tags))
                .dataProtectionPolicy(getDataProtectionPolicyAsString(model))
                .build();
    }

    static CreateTopicRequest translateToCreateTopicRequest(final ResourceModel model) {
        return CreateTopicRequest.builder()
                .name(model.getTopicName())
                .attributes(translateTopicAttributesToMap(model))
                .dataProtectionPolicy(getDataProtectionPolicyAsString(model))
                .build();
    }

    static PutDataProtectionPolicyRequest translatePutDataProtectionPolicyRequest(final ResourceModel model) {
        return PutDataProtectionPolicyRequest.builder()
                .dataProtectionPolicy(getDataProtectionPolicyAsString(model))
                .resourceArn(model.getTopicArn())
                .build();
    }

    static GetDataProtectionPolicyRequest getDataProtectionPolicyRequest(final String topicArn) {
        return GetDataProtectionPolicyRequest.builder()
                .resourceArn(topicArn)
                .build();
    }

    static Set<software.amazon.awssdk.services.sns.model.Tag> translateTagsToSdk(Set<Tag> tags) {
        return streamOfOrEmpty(tags)
                .map(tag -> software.amazon.awssdk.services.sns.model.Tag.builder().key(tag.getKey()).value(tag.getValue()).build())
                .collect(Collectors.toSet());
    }

    static DeleteTopicRequest translateToDeleteTopic(final ResourceModel model) {
        return DeleteTopicRequest.builder()
                .topicArn(model.getTopicArn())
                .build();
    }

    static GetTopicAttributesRequest translateToGetTopicAttributes(final ResourceModel model) {
        return GetTopicAttributesRequest.builder()
                .topicArn(model.getTopicArn())
                .build();
    }

    static ListTopicsRequest translateToListTopicRequest(final String nextToken) {
        return ListTopicsRequest.builder()
                .nextToken(nextToken)
                .build();
    }

    static GetTopicAttributesRequest translateToGetTopicAttributes(String awsPartition, String region, String accountId, String topicName) {
        String topicArn = String.format("arn:%s:sns:%s:%s:%s", awsPartition, region, accountId, topicName);
        return GetTopicAttributesRequest.builder()
                .topicArn(topicArn)
                .build();
    }

    static List<ResourceModel> translateFromListTopicRequest(final ListTopicsResponse listTopicsResponse) {
        return streamOfOrEmpty(listTopicsResponse.topics())
                .map(topic -> ResourceModel.builder()
                        .topicArn(topic.topicArn())
                        .build())
                .collect(Collectors.toList());
    }

    static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }

    static ResourceModel translateFromGetTopicAttributes(GetTopicAttributesResponse getTopicAttributesResponse,
                                                         ListSubscriptionsByTopicResponse listSubscriptionsByTopicResponse,
                                                         ListTagsForResourceResponse listTagsForResourceResponse,
                                                         GetDataProtectionPolicyResponse getDataProtectionPolicyResponse) {
        Map<String, String> attributes = getTopicAttributesResponse.attributes();

        List<Subscription> subscriptions = streamOfOrEmpty(listSubscriptionsByTopicResponse.subscriptions())
            .map(subscription -> Subscription.builder()
                    .endpoint(subscription.endpoint())
                    .protocol(subscription.protocol())
                    .build())
            .collect(Collectors.toList());


        return ResourceModel.builder()
                .topicArn(attributes.get(TopicAttributeName.TOPIC_ARN.toString()))
                .topicName(getTopicNameFromArn(attributes.get(TopicAttributeName.TOPIC_ARN.toString())))
                .displayName(nullIfEmpty(attributes.get(TopicAttributeName.DISPLAY_NAME.toString())))
                .kmsMasterKeyId(nullIfEmpty(attributes.get(TopicAttributeName.KMS_MASTER_KEY_ID.toString())))
                .signatureVersion(nullIfEmpty(attributes.get(TopicAttributeName.SIGNATURE_VERSION.toString())))
                .fifoTopic(nullIfEmptyBoolean(attributes.get(TopicAttributeName.FIFO_TOPIC.toString())))
                .contentBasedDeduplication(nullIfEmptyBoolean(attributes.get(TopicAttributeName.CONTENT_BASED_DEDUPLICATION.toString())))
                .dataProtectionPolicy(getDataProtectionPolicyResponse == null ? null : convertToJson(getDataProtectionPolicyResponse.dataProtectionPolicy()))
                .subscription(nullIfEmpty(subscriptions))
                .tags(nullIfEmpty(translateTagsFromSdk(listTagsForResourceResponse.tags())))
                .build();
    }

  static List<Tag> translateTagsFromSdk(List<software.amazon.awssdk.services.sns.model.Tag> tags) {
    return streamOfOrEmpty(tags)
            .map(tag -> Tag.builder().key(tag.key()).value(tag.value()).build())
            .collect(Collectors.toList());
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
                .topicArn(model.getTopicArn())
                .build();
    }

    public static UnsubscribeRequest translateToUnsubscribe(String subscriptionArn) {
        return UnsubscribeRequest.builder()
                .subscriptionArn(subscriptionArn)
                .build();
    }

    static SubscribeRequest translateToSubscribeRequest(ResourceModel model, Subscription subscription) {
        return SubscribeRequest.builder()
                .topicArn(model.getTopicArn())
                .protocol(subscription.getProtocol())
                .endpoint(subscription.getEndpoint())
                .build();
    }

    static SetTopicAttributesRequest translateToSetAttributesRequest(String id, TopicAttributeName attributName, String attributeValue) {
        return SetTopicAttributesRequest.builder()
                .topicArn(id)
                .attributeName(attributName.toString())
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

  static <T> List<T> nullIfEmpty(List<T> list) {
    return list != null && list.isEmpty() ? null : Objects.requireNonNull(list);
  }

    static String nullIfEmpty(String s) {
        return StringUtils.isEmpty(s) ? null : s;
    }

    static Boolean nullIfEmptyBoolean(String s) {
        return StringUtils.isEmpty(s) ? null : Boolean.valueOf(s);
    }

    static Set<Tag> convertResourceTagsToSet(Map<String, String> resourceTags) {
        Set<Tag> tags = Sets.newHashSet();
        if (resourceTags != null) {
            resourceTags.forEach((key, value) -> tags.add(Tag.builder().key(key).value(value).build()));
        }
        return tags;
    }

    static String getDataProtectionPolicyAsString(final ResourceModel model) {
        if (CollectionUtils.isNullOrEmpty(model.getDataProtectionPolicy())) {
            return "";
        }
        try {
            return MAPPER.writeValueAsString(model.getDataProtectionPolicy());
        } catch (JsonProcessingException e) {
            throw new CfnInvalidRequestException(e);
        }
    }

    private static Map<String,Object> convertToJson(String jsonString) {
        Map<String, Object> obj = null;

        if (StringUtils.isNotBlank(jsonString)) {
            try {
                obj = MAPPER.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                throw new CfnInvalidRequestException(e);
            }
        }
        return obj;
    }

    private static <V> void putIfNotNull(Map<String, String> attributeMap, String key, V value) {
        if (value != null) {
            attributeMap.put(key, value.toString());
        }
    }

    private static Map<String, String> translateTopicAttributesToMap(ResourceModel model) {
        Map<String, String> attributes = new HashMap<>();

        putIfNotNull(attributes, TopicAttributeName.DISPLAY_NAME.toString(), model.getDisplayName());
        putIfNotNull(attributes, TopicAttributeName.KMS_MASTER_KEY_ID.toString(), model.getKmsMasterKeyId());
        putIfNotNull(attributes, TopicAttributeName.SIGNATURE_VERSION.toString(), model.getSignatureVersion());
        putIfNotNull(attributes, TopicAttributeName.FIFO_TOPIC.toString(), model.getFifoTopic());
        putIfNotNull(attributes, TopicAttributeName.CONTENT_BASED_DEDUPLICATION.toString(), model.getContentBasedDeduplication());
        return attributes;
    }
}
