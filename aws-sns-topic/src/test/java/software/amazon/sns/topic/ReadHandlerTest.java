package software.amazon.sns.topic;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotUpdatableException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<SnsClient> proxyClient;

    @Mock
    SnsClient client;

    private ReadHandler handler;

    @BeforeEach
    public void setup() {
        handler = new ReadHandler();
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
        final Set<Subscription> subscriptions = new HashSet<>();
        Subscription.builder().endpoint("abc@xyz.com").protocol("email").build();
        final Set<Tag> tags = new HashSet<>();
        tags.add(Tag.builder().key("key1").value("value1").build());

        final ResourceModel model = ResourceModel.builder()
                .id("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .topicName("sns-topic-name")
                .displayName("topic-display-name")
                .subscription(subscriptions)
                .tags(tags)
                .build();


        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributes.DISPLAY_NAME, "topic-display-name");
        attributes.put(TopicAttributes.TOPIC_ARN, "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
                .attributes(attributes)
                .build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
        final ListSubscriptionsByTopicResponse listSubscriptionsByTopicResponse = ListSubscriptionsByTopicResponse.builder()
                .subscriptions(software.amazon.awssdk.services.sns.model.Subscription.builder()
                        .subscriptionArn("subs-arn")
                        .endpoint("endpoint")
                        .protocol("email")
                        .build()
                )
                .build();
        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenReturn(listSubscriptionsByTopicResponse);
        final ListTagsForResourceResponse listTagsForStreamResponse = ListTagsForResourceResponse.builder()
                .tags(software.amazon.awssdk.services.sns.model.Tag.builder().key("key1").value("value1").build())
                .build();
        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(listTagsForStreamResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client()).listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class));
        verify(proxyClient.client()).listTagsForResource(any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_NotFound() {
        final ResourceModel model = ResourceModel.builder()
                .id("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenThrow(NotFoundException.class);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();
        assertThrows(CfnNotFoundException.class, () -> {handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);});

        verify(proxyClient.client()).getTopicAttributes(any(GetTopicAttributesRequest.class));
    }
}
