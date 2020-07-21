package software.amazon.sns.topic;

import org.junit.jupiter.api.AfterEach;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ListTopicsRequest;
import software.amazon.awssdk.services.sns.model.ListTopicsResponse;
import software.amazon.awssdk.services.sns.model.Topic;
import software.amazon.cloudformation.proxy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static software.amazon.sns.topic.AbstractTestBase.*;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<SnsClient> proxyClient;

    @Mock
    SnsClient client;

    private ListHandler handler;

    @BeforeEach
    public void setup() {
        handler = new ListHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        client = mock(SnsClient.class);
        proxyClient = MOCK_PROXY(proxy, client);
    }

    @AfterEach
    public void postExecute() {
        verifyNoMoreInteractions(client);
    }

    @Test
    public void handleRequest_SimpleSuccess() {

        final ListTopicsResponse listTopicsResponse = ListTopicsResponse.builder()
                .topics(Topic.builder().topicArn("topic-arn-1").build(), Topic.builder().topicArn("topic-arn-2").build())
                .nextToken(null)
                .build();
        when(proxyClient.client().listTopics(any(ListTopicsRequest.class))).thenReturn(listTopicsResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).listTopics(any(ListTopicsRequest.class));
    }

    @Test
    public void handleRequest_HasNextToken() {

        final ListTopicsResponse listTopicsResponse = ListTopicsResponse.builder()
                .topics(Topic.builder().topicArn("topic-arn-1").build(), Topic.builder().topicArn("topic-arn-2").build())
                .nextToken("next-token")
                .build();
        when(proxyClient.client().listTopics(any(ListTopicsRequest.class))).thenReturn(listTopicsResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getNextToken()).isNotNull();

        verify(proxyClient.client()).listTopics(any(ListTopicsRequest.class));
    }
}
