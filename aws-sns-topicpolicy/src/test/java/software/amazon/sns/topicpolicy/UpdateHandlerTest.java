package software.amazon.sns.topicpolicy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SetTopicAttributesRequest;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.awssdk.services.sns.model.NotFoundException;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.InternalErrorException;
import software.amazon.awssdk.services.sns.model.AuthorizationErrorException;
import software.amazon.awssdk.services.sns.model.InvalidSecurityException;
import software.amazon.awssdk.services.sns.model.ThrottledException;
import software.amazon.awssdk.services.sns.model.ConcurrentAccessException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<SnsClient> proxyClient;

    @Mock
    SnsClient snsClient;

    UpdateHandler handler;

    @BeforeEach
    public void setup() {
        handler = new UpdateHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        snsClient = mock(SnsClient.class);
        proxyClient = MOCK_PROXY(proxy, snsClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {

        // New Topics
        Map<String, Object> policyDocument = getSNSPolicy();
        final List<String> new_topics = new ArrayList<>();
        new_topics.add("arn:aws:sns:us-east-1:123456789:my-topic3");
        new_topics.add("arn:aws:sns:us-east-1:123456789:my-topic4");
        final ResourceModel desiredResourceState = ResourceModel.builder()
                .id("aws-sns-topic-policy-id")
                .topics(new_topics)
                .policyDocument(policyDocument)
                .build();

        // Old Topics
        final List<String> old_topics = new ArrayList<>();
        old_topics.add("arn:aws:sns:us-east-1:123456789:my-topic1");
        old_topics.add("arn:aws:sns:us-east-1:123456789:my-topic2");
        final ResourceModel previousResourceState = ResourceModel.builder()
                .topics(old_topics)
                .policyDocument(policyDocument)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(desiredResourceState)
                .previousResourceState(previousResourceState)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Same_Topic_Success() {

        // New Topics
        Map<String, Object> policyDocument = getSNSPolicy();
        final List<String> new_topics = new ArrayList<>();
        new_topics.add("arn:aws:sns:us-east-1:123456789:my-topic1");
        final ResourceModel desiredResourceState = ResourceModel.builder()
                .id("aws-sns-topic-policy-id")
                .topics(new_topics)
                .policyDocument(policyDocument)
                .build();

        // Old Topics
        final List<String> old_topics = new ArrayList<>();
        old_topics.add("arn:aws:sns:us-east-1:123456789:my-topic1");
        final ResourceModel previousResourceState = ResourceModel.builder()
                .topics(old_topics)
                .policyDocument(policyDocument)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(desiredResourceState)
                .previousResourceState(previousResourceState)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Different_Topic_Success() {

        // New Topics
        Map<String, Object> policyDocument = getSNSPolicy();
        final List<String> new_topics = new ArrayList<>();
        new_topics.add("arn:aws:sns:us-east-1:123456789:my-topic1");
        final ResourceModel desiredResourceState = ResourceModel.builder()
                .id("aws-sns-topic-policy-id")
                .topics(new_topics)
                .policyDocument(policyDocument)
                .build();

        // Old Topics
        final List<String> old_topics = new ArrayList<>();
        old_topics.add("arn:aws:sns:us-east-1:123456789:my-topic1");
        old_topics.add("arn:aws:sns:us-east-1:123456789:my-topic2");
        final ResourceModel previousResourceState = ResourceModel.builder()
                .topics(old_topics)
                .policyDocument(policyDocument)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(desiredResourceState)
                .previousResourceState(previousResourceState)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Failure_PolicyNullOrEmpty() {
        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic110");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic220");
        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-InternalErrorException")
                .topics(topics)
                .policyDocument(new HashMap<>())
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertThat(response).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(response.getMessage()).isEqualTo("Invalid parameter: Policy Error: null");
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
    }

    @Test
    public void handleRequest_Failure_TopicsNullOrEmpty() {
        Map<String, Object> policyDocument = getSNSPolicy();
        final List<String> topics = new ArrayList<>();
        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-InternalErrorException")
                .topics(topics)
                .policyDocument(policyDocument)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertThat(response).isNotNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getMessage()).isEqualTo("Value of property Topics must be of type List of String");
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
    }

    @Test
    public void handleRequest_Failure_PolicyDocument_JsonProcessingException() {
        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic110");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic220");

        Map<String, Object> policyDocument = new HashMap<>();
        policyDocument.put(null, "");
        policyDocument.put("event", "[\"order_placed\"]");

        final ResourceModel model = ResourceModel.builder()
                .id("handleRequest_Failure_JsonProcessingException")
                .topics(topics)
                .policyDocument(policyDocument)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

    }

    @Test
    public void handleRequest_Failure_NotFoundException() {

        Map<String, Object> policyDocument = getSNSPolicy();

        // New Topics
        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic110");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic220");

        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-cfnnotfound")
                .topics(topics)
                .policyDocument(policyDocument)
                .build();

        // Previous Topics
        final List<String> previousTopics = new ArrayList<>();
        previousTopics.add("arn:aws:sns:us-east-1:123456789:my-topic1");
        previousTopics.add("arn:aws:sns:us-east-1:123456789:my-topic2");

        final ResourceModel previousState = ResourceModel.builder()
                .topics(previousTopics)
                .policyDocument(policyDocument)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .previousResourceState(previousState)
                .build();

        when(proxyClient.client().setTopicAttributes(any(SetTopicAttributesRequest.class)))
                .thenThrow(NotFoundException.class);
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void handleRequest_Failure_InvalidParameterException() {

        Map<String, Object> policyDocument = getSNSPolicy();

        // New Topics
        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic110");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic220");

        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-cfnnotfound")
                .topics(topics)
                .policyDocument(policyDocument)
                .build();

        // Previous Topics
        final List<String> previousTopics = new ArrayList<>();
        previousTopics.add("arn:aws:sns:us-east-1:123456789:my-topic1");
        previousTopics.add("arn:aws:sns:us-east-1:123456789:my-topic2");

        final ResourceModel previousState = ResourceModel.builder()
                .topics(previousTopics)
                .policyDocument(policyDocument)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .previousResourceState(previousState)
                .build();

        when(proxyClient.client().setTopicAttributes(any(SetTopicAttributesRequest.class)))
                .thenThrow(InvalidParameterException.class);
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
    }

    @Test
    public void handleRequest_Failure_InternalErrorException() {

        Map<String, Object> policyDocument = getSNSPolicy();

        // New Topics
        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic110");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic220");

        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-cfnnotfound")
                .topics(topics)
                .policyDocument(policyDocument)
                .build();

        // Previous Topics
        final List<String> previousTopics = new ArrayList<>();
        previousTopics.add("arn:aws:sns:us-east-1:123456789:my-topic1");
        previousTopics.add("arn:aws:sns:us-east-1:123456789:my-topic2");

        final ResourceModel previousState = ResourceModel.builder()
                .topics(previousTopics)
                .policyDocument(policyDocument)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .previousResourceState(previousState)
                .build();

        when(proxyClient.client().setTopicAttributes(any(SetTopicAttributesRequest.class)))
                .thenThrow(InternalErrorException.class);
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceInternalError);
    }

    @Test
    public void handleRequest_Failure_AuthorizationErrorException() {

        Map<String, Object> policyDocument = getSNSPolicy();

        // New Topics
        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic110");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic220");

        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-cfnnotfound")
                .topics(topics)
                .policyDocument(policyDocument)
                .build();

        // Previous Topics
        final List<String> previousTopics = new ArrayList<>();
        previousTopics.add("arn:aws:sns:us-east-1:123456789:my-topic1");
        previousTopics.add("arn:aws:sns:us-east-1:123456789:my-topic2");

        final ResourceModel previousState = ResourceModel.builder()
                .topics(previousTopics)
                .policyDocument(policyDocument)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .previousResourceState(previousState)
                .build();

        when(proxyClient.client().setTopicAttributes(any(SetTopicAttributesRequest.class)))
                .thenThrow(AuthorizationErrorException.class);
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);

    }

    @Test
    public void handleRequest_Failure_InvalidSecurityException() {

        Map<String, Object> policyDocument = getSNSPolicy();

        // New Topics
        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic110");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic220");

        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-cfnnotfound")
                .topics(topics)
                .policyDocument(policyDocument)
                .build();

        // Previous Topics
        final List<String> previousTopics = new ArrayList<>();
        previousTopics.add("arn:aws:sns:us-east-1:123456789:my-topic1");
        previousTopics.add("arn:aws:sns:us-east-1:123456789:my-topic2");

        final ResourceModel previousState = ResourceModel.builder()
                .topics(previousTopics)
                .policyDocument(policyDocument)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .previousResourceState(previousState)
                .build();

        when(proxyClient.client().setTopicAttributes(any(SetTopicAttributesRequest.class)))
                .thenThrow(InvalidSecurityException.class);
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidCredentials);
    }

    @Test
    public void handleRequest_Failure_ThrottledException() {

        Map<String, Object> policyDocument = getSNSPolicy();

        // New Topics
        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic110");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic220");

        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-cfnnotfound")
                .topics(topics)
                .policyDocument(policyDocument)
                .build();

        // Previous Topics
        final List<String> previousTopics = new ArrayList<>();
        previousTopics.add("arn:aws:sns:us-east-1:123456789:my-topic1");
        previousTopics.add("arn:aws:sns:us-east-1:123456789:my-topic2");

        final ResourceModel previousState = ResourceModel.builder()
                .topics(previousTopics)
                .policyDocument(policyDocument)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .previousResourceState(previousState)
                .build();

        when(proxyClient.client().setTopicAttributes(any(SetTopicAttributesRequest.class)))
                .thenThrow(ThrottledException.class);
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.Throttling);
    }

    @Test
    public void handleRequest_Failure_Null_ResourceModel() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(null)
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertThat(response).isNotNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getMessage()).isEqualTo("Property PolicyDocument cannot be empty");
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);

    }

    @Test
    public void handleRequest_Failure_GeneralException() {
        Map<String, Object> policyDocument = getSNSPolicy();

        // New Topics
        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic110");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic220");

        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-CfnInvalidCredentialsException")
                .topics(topics)
                .policyDocument(policyDocument)
                .build();

        // Previous Topics
        final List<String> previousTopics = new ArrayList<>();
        previousTopics.add("arn:aws:sns:us-east-1:123456789:my-topic1");
        previousTopics.add("arn:aws:sns:us-east-1:123456789:my-topic2");

        final ResourceModel previousState = ResourceModel.builder()
                .topics(previousTopics)
                .policyDocument(policyDocument)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .previousResourceState(previousState)
                .build();

        when(proxyClient.client().setTopicAttributes(any(SetTopicAttributesRequest.class)))
                .thenThrow(ConcurrentAccessException.class);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }

}
