package software.amazon.sns.topicinlinepolicy;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import static software.amazon.sns.topicinlinepolicy.BaseHandlerStd.EMPTY_TOPICARN_ERROR_MESSAGE;

public class AbstractTestBase {
    protected static final Credentials MOCK_CREDENTIALS;
    protected static final LoggerProxy logger;

    static {
        MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
        logger = new LoggerProxy();
    }

    static ProxyClient<SnsClient> MOCK_PROXY(
            final AmazonWebServicesClientProxy proxy,
            final SnsClient snsClient) {
        return new ProxyClient<SnsClient>() {
            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseT injectCredentialsAndInvokeV2(
                    RequestT request, Function<RequestT, ResponseT> requestFunction) {
                return proxy.injectCredentialsAndInvokeV2(request, requestFunction);
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> CompletableFuture<ResponseT> injectCredentialsAndInvokeV2Async(
                    RequestT request, Function<RequestT, CompletableFuture<ResponseT>> requestFunction) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse, IterableT extends SdkIterable<ResponseT>> IterableT injectCredentialsAndInvokeIterableV2(
                    RequestT request, Function<RequestT, IterableT> requestFunction) {
                return proxy.injectCredentialsAndInvokeIterableV2(request, requestFunction);
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseInputStream<ResponseT> injectCredentialsAndInvokeV2InputStream(
                    RequestT requestT, Function<RequestT, ResponseInputStream<ResponseT>> function) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseBytes<ResponseT> injectCredentialsAndInvokeV2Bytes(
                    RequestT requestT, Function<RequestT, ResponseBytes<ResponseT>> function) {
                throw new UnsupportedOperationException();
            }

            @Override
            public SnsClient client() {
                return snsClient;
            }
        };
    }

    public Map<String, Object> getSNSPolicy() {
        final String key = "SNSTopicSPolicy";
        final String accountId = "123456789";
        Map<String, Object> policy = new HashMap<String, Object>();

        policy.put(key, policDocument(accountId, "*"));
        return policy;
    }

    public String policDocument(String accountId, String topicArn) {
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
                .append("          \"SNS:Publish\"")
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

    public String testPolicDocument(String accountId, String topicArn) {
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
                .append("          \"SNS:RemovePermission\"")
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

    public Map<String, String> getDefaultTestMap(){
        Map<String, String> attributes = new HashMap<>();
        attributes.put("Policy", policDocument(null, "arn:aws:sns:us-east-1:123456789012:sns-topic-name"));
        attributes.put("TopicArn", "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        return attributes;
    }

    public Map<String, String> getTestMap(){
        Map<String, String> attributes = new HashMap<>();
        attributes.put("Policy", testPolicDocument(null, "arn:aws:sns:us-east-1:123456789012:sns-topic-name"));
        attributes.put("TopicArn", "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        return attributes;
    }

}
