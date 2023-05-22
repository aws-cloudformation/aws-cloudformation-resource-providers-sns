package software.amazon.sns.topicinlinepolicy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesResponse;
import software.amazon.awssdk.services.sns.model.SetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is a centralized placeholder for - api request construction - object translation to/from aws sdk -
 * resource model construction for read/list handlers
 */

public class Translator {

    static SetTopicAttributesRequest translateToSetRequest(final String topicArn, String topicPolicy) {
        return SetTopicAttributesRequest.builder()
                .attributeName(TopicAttribute.Policy.name())
                .attributeValue(topicPolicy)
                .topicArn(topicArn)
                .build();
    }

    static GetTopicAttributesRequest translateToGetRequest(final String topicArn) {
        return GetTopicAttributesRequest.builder()
                .topicArn(topicArn)
                .build();
    }

    static ResourceModel translateFromGetRequest(GetTopicAttributesResponse getTopicAttributesResponse, String TopicArn) {
        Map<String, String> attributes = getTopicAttributesResponse.attributes();
        return ResourceModel.builder()
                .policyDocument(convertStringToObject(attributes.get("Policy")))
                .topicArn(attributes.get("TopicArn"))
                .build();

    }

    protected static Map<String, Object> convertStringToObject(String policyDocument) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> object = null;
        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
        };
        try {
            if (policyDocument != null) {
                object = mapper.readValue(URLDecoder.decode(policyDocument, StandardCharsets.UTF_8.toString()), typeRef);
            }
        } catch (IOException e) {
            throw new CfnInvalidRequestException(e);
        }
        return object;
    }



}
