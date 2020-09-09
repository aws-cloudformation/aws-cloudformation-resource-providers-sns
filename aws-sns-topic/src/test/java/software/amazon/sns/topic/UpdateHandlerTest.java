package software.amazon.sns.topic;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<SnsClient> proxyClient;

    @Mock
    SnsClient client;

    private UpdateHandler handler;

    @BeforeEach
    public void setup() {
        handler = new UpdateHandler();
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
                .id("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .displayName("topic-display-name")
                .build();
        final ResourceModel previousModel = ResourceModel.builder()
                .id("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributes.DISPLAY_NAME, "topic-display-name");
        attributes.put(TopicAttributes.TOPIC_ARN, "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
                .attributes(attributes)
                .build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
        final SetTopicAttributesResponse setTopicAttributesResponse = SetTopicAttributesResponse.builder().build();
        when(proxyClient.client().setTopicAttributes(any(SetTopicAttributesRequest.class))).thenReturn(setTopicAttributesResponse);
        final ListSubscriptionsByTopicResponse listSubscriptionsByTopicResponse = ListSubscriptionsByTopicResponse.builder().build();
        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenReturn(listSubscriptionsByTopicResponse);
        final ListTagsForResourceResponse listTagsForStreamResponse = ListTagsForResourceResponse.builder().build();
        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(listTagsForStreamResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).previousResourceState(previousModel).build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).setTopicAttributes(any(SetTopicAttributesRequest.class));
        verify(proxyClient.client(), times(2)).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client(), times(2)).listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class));
    }

    @Test
    public void handleRequest_SimpleSuccess_UpdateKmsKeyId() {
        final ResourceModel model = ResourceModel.builder()
                .id("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .kmsMasterKeyId("dummy-key-id-2")
                .build();
        final ResourceModel previousModel = ResourceModel.builder()
                .id("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .kmsMasterKeyId("dummy-key-id-1")
                .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributes.KMS_MASTER_KEY_ID, "dummy-key-id-2");
        attributes.put(TopicAttributes.TOPIC_ARN, "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
                .attributes(attributes)
                .build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
        final SetTopicAttributesResponse setTopicAttributesResponse = SetTopicAttributesResponse.builder().build();
        when(proxyClient.client().setTopicAttributes(any(SetTopicAttributesRequest.class))).thenReturn(setTopicAttributesResponse);
        final ListSubscriptionsByTopicResponse listSubscriptionsByTopicResponse = ListSubscriptionsByTopicResponse.builder().build();
        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenReturn(listSubscriptionsByTopicResponse);
        final ListTagsForResourceResponse listTagsForStreamResponse = ListTagsForResourceResponse.builder().build();
        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(listTagsForStreamResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).previousResourceState(previousModel).build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).setTopicAttributes(any(SetTopicAttributesRequest.class));
        verify(proxyClient.client(), times(2)).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client(), times(2)).listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class));
    }

    @Test
    public void handleRequest_NotFound() {
        final ResourceModel model = ResourceModel.builder()
                .id("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();
        final ResourceModel previousModel = ResourceModel.builder().build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenThrow(NotFoundException.class);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).previousResourceState(previousModel).build();
        assertThrows(CfnNotFoundException.class, () -> {handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);});

        verify(proxyClient.client()).getTopicAttributes(any(GetTopicAttributesRequest.class));
    }
}
