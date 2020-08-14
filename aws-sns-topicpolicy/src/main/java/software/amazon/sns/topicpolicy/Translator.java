package software.amazon.sns.topicpolicy;

import software.amazon.awssdk.services.sns.model.SetTopicAttributesRequest;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

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
