package software.amazon.sns.topic;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<SnsClient> proxyClient;

    @Mock
    SnsClient client;

    private CreateHandler handler;

    @BeforeEach
    public void setup() {
        handler = new CreateHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        client = mock(SnsClient.class);
        proxyClient = MOCK_PROXY(proxy, client);
    }

    @AfterEach
    public void postExecute() {
        verify(client, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(client);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceModel model = ResourceModel.builder()
                .topicName("sns-topic-name")
                .build();

        final ListTopicsResponse listTopicsResponse = ListTopicsResponse.builder().build();
        when(proxyClient.client().listTopics(any(ListTopicsRequest.class))).thenReturn(listTopicsResponse);
        final CreateTopicResponse createTopicResponse = CreateTopicResponse.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();
        when(proxyClient.client().createTopic(any(CreateTopicRequest.class))).thenReturn(createTopicResponse);
        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributes.TOPIC_ARN, "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
                .attributes(attributes)
                .build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
        final ListSubscriptionsByTopicResponse listSubscriptionsByTopicResponse = ListSubscriptionsByTopicResponse.builder().build();
        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenReturn(listSubscriptionsByTopicResponse);
        final ListTagsForResourceResponse listTagsForStreamResponse = ListTagsForResourceResponse.builder().build();
        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(listTagsForStreamResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).listTopics(any(ListTopicsRequest.class));
        verify(proxyClient.client()).createTopic(any(CreateTopicRequest.class));
        verify(proxyClient.client()).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client()).listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class));
        verify(proxyClient.client()).listTagsForResource(any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_Failure_AlreadyExists() {
        final ResourceModel model = ResourceModel.builder()
                .topicName("sns-topic-name")
                .build();

        final ListTopicsResponse listTopicsResponse = ListTopicsResponse.builder()
                .topics(Topic.builder().topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name").build())
                .build();
        when(proxyClient.client().listTopics(any(ListTopicsRequest.class))).thenReturn(listTopicsResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isNotNull();

        verify(proxyClient.client()).listTopics(any(ListTopicsRequest.class));
    }
}
