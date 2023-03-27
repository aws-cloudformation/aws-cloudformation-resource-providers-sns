package software.amazon.sns.topicpolicy;

import software.amazon.awssdk.services.sns.model.SetTopicAttributesRequest;

/**
 * This class is a centralized placeholder for - api request construction - object translation to/from aws sdk -
 * resource model construction for read/list handlers
 */

public class Translator {

    static SetTopicAttributesRequest translateToRequest(final String topicArn, String topicPolicy) {
        return SetTopicAttributesRequest.builder()
                .attributeName(TopicAttribute.Policy.name())
                .attributeValue(topicPolicy)
                .topicArn(topicArn)
                .build();
    }
}
