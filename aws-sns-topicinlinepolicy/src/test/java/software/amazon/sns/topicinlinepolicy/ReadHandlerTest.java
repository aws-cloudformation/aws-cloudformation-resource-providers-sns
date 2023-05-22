package software.amazon.sns.topicinlinepolicy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.ProxyClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<SnsClient> proxyClient;

    @Mock
    SnsClient snsClient;

    ReadHandler handler;

    private Map<String, String> attributesWithWrongPolicy = getDefaultTestMap();

    @BeforeEach
    public void setup() {
        handler = new ReadHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        snsClient = mock(SnsClient.class);
        proxyClient = MOCK_PROXY(proxy, snsClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("Policy", testPolicDocument("123456789012", "arn:aws:sns:us-east-1:123456789012:sns-topic-name"));
        attributes.put("TopicArn", "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenReturn(GetTopicAttributesResponse.builder()
                        .attributes(attributes)
                        .build());
        final ResourceModel model = ResourceModel.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .policyDocument(convertStringToObject(testPolicDocument("123456789012", "arn:aws:sns:us-east-1:123456789012:sns-topic-name")))
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isZero();
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Failure_TopicExisted() {
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenReturn(GetTopicAttributesResponse.builder()
                        .attributes(attributesWithWrongPolicy)
                        .build());

        String topic = "arn:aws:sns:us-east-1:123456789012:sns-topic-name";

        Map<String, Object> policyDocument = getSNSPolicy();

        final ResourceModel model = ResourceModel.builder()
                .topicArn(topic)
                .policyDocument(policyDocument)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void handleRequest_Failure_Empty_Topics() {
        String topic = "";

        Map<String, Object> policyDocument = getSNSPolicy();

        final ResourceModel model = ResourceModel.builder()
                .topicArn(topic)
                .policyDocument(policyDocument)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertThat(response).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(response.getMessage()).isEqualTo(EMPTY_POLICY_AND_TOPICARN_ERROR_MESSAGE);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);

    }

    @Test
    public void handleRequest_Failure_Null_ResourceModel() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(null)
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertThat(response).isNotNull();
        assertThat(response.getErrorCode()).isEqualByComparingTo(HandlerErrorCode.InvalidRequest);
        assertThat(response.getMessage()).isEqualTo(EMPTY_POLICY_AND_TOPICARN_ERROR_MESSAGE);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);

    }

}
