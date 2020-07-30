package software.amazon.sns.topicpolicy;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Lists;

import software.amazon.awssdk.services.sns.model.SetTopicAttributesRequest;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

/**
 * This class is a centralized placeholder for - api request construction - object translation to/from aws sdk -
 * resource model construction for read/list handlers
 */

public class Translator {

    static SetTopicAttributesRequest translateToCreateRequest(final String topicArn, String topicPolicy) {
        return SetTopicAttributesRequest.builder()
                .attributeName(TopicAttribute.Policy.name())
                .attributeValue(topicPolicy)
                .topicArn(topicArn)
                .build();
    }

    /**
     * Translates resource object from sdk into a resource model
     *
     * @return model resource model
     */
    static ResourceModel translateFromReadResponse() {
        return ResourceModel.builder()
                .build();
    }

    /**
     * Request to delete a resource
     *
     * @param model
     *            resource model
     * @return awsRequest the aws service request to delete a resource
     */
    static SetTopicAttributesRequest translateToDeleteRequest(final ResourceHandlerRequest<ResourceModel> request,
            final String topicArn) {
        return SetTopicAttributesRequest.builder()
                .attributeName(TopicAttribute.Policy.name())
                .attributeValue(getDefaultPolicy(request, topicArn))
                .topicArn(topicArn)
                .build();
    }

    /**
     * Translates resource objects from sdk into a resource model (primary identifier only)
     *
     * @param awsResponse
     *            the aws service describe resource response
     * @return list of resource models
     */
    static List<ResourceModel> translateFromListRequest() {
        return streamOfOrEmpty(Lists.newArrayList())
                .map(resource -> ResourceModel.builder()
                        // include only primary identifier
                        .build())
                .collect(Collectors.toList());
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }

    /*
     * Unfortunately, SNS requires a policy for a topic, so we must reset to the default policy for now. The original
     * default policy, created by the SNS service (not CloudFormation) when the topic was created, specified an
     * uppercase SNS: asin spite of the public AWS documentation always using a lowercase sns:. So this method uses SNS:
     * as the service does. The Ruby integration tests will fail if the code below uses sns: instead.
     */
    static String getDefaultPolicy(final ResourceHandlerRequest<ResourceModel> request, String topicArn) {
        String accountId = request.getAwsAccountId();
        StringBuilder sb = new StringBuilder()
                .append("{")
                .append("    \"Version\": \"2008-10-17\",")
                .append("    \"Id\": \"__default_policy_ID\",")
                .append("    \"Statement\": [")
                .append("      {")
                .append("        \"Effect\": \"Allow\",")
                .append("        \"Sid\": \"__default_statement_ID\",")
                .append("        \"Principal\": {")
                .append("          \"AWS\": \"*\"")
                .append("        },")
                .append("        \"Action\": [")
                .append("          \"SNS:GetTopicAttributes\",")
                .append("          \"SNS:SetTopicAttributes\",")
                .append("          \"SNS:AddPermission\",")
                .append("          \"SNS:RemovePermission\",")
                .append("          \"SNS:DeleteTopic\",")
                .append("          \"SNS:Subscribe\",")
                .append("          \"SNS:ListSubscriptionsByTopic\",")
                .append("          \"SNS:Publish\",")
                .append("          \"SNS:Receive\"")
                .append("        ],")
                .append("        \"Resource\": \"").append(topicArn).append("\",")
                .append("        \"Condition\": {")
                .append("          \"StringEquals\": {")
                .append("            \"AWS:SourceOwner\": \"").append(accountId).append("\"")
                .append("          }")
                .append("        }")
                .append("      }")
                .append("    ]")
                .append("}");
        return sb.toString();
    }

}
