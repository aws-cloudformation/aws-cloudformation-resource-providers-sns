package software.amazon.sns.topic;

import com.google.common.collect.Maps;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

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
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .displayName("topic-display-name")
                .build();
        final ResourceModel previousModel = ResourceModel.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributeName.DISPLAY_NAME.toString(), "topic-display-name");
        attributes.put(TopicAttributeName.TOPIC_ARN.toString(), "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        setupUpdateHandlerMocks(attributes);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).previousResourceState(previousModel).build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        validateResponseSuccess(response);
        verify(proxyClient.client()).setTopicAttributes(any(SetTopicAttributesRequest.class));
        verify(proxyClient.client(), times(2)).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client(), times(2)).listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class));
    }

    @Test
    public void handleRequest_SimpleSuccess_UpdateKmsKeyId() {
        final ResourceModel model = ResourceModel.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .kmsMasterKeyId("dummy-key-id-2")
                .build();
        final ResourceModel previousModel = ResourceModel.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .kmsMasterKeyId("dummy-key-id-1")
                .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributeName.KMS_MASTER_KEY_ID.toString(), "dummy-key-id-2");
        attributes.put(TopicAttributeName.TOPIC_ARN.toString(), "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        setupUpdateHandlerMocks(attributes);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).previousResourceState(previousModel).build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        validateResponseSuccess(response);
        verify(proxyClient.client()).setTopicAttributes(any(SetTopicAttributesRequest.class));
        verify(proxyClient.client(), times(2)).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client(), times(2)).listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class));
    }

    @Test
    public void handleRequest_NotFound() {
        final ResourceModel model = ResourceModel.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();
        final ResourceModel previousModel = ResourceModel.builder().build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenThrow(NotFoundException.class);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).previousResourceState(previousModel).build();
        assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(proxyClient.client()).getTopicAttributes(any(GetTopicAttributesRequest.class));
    }

    @Test
    public void handleRequest_SimpleSuccess_tags() {
        final Map<String, String> tags = Maps.newHashMap();
        tags.put("key1", "value1");
        tags.put("key3", "value3");

        final Map<String, String> oldTags = Maps.newHashMap();
        oldTags.put("key1", "value1");
        oldTags.put("key2", "value2");

        final ResourceModel model = ResourceModel.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();
        final ResourceModel previousModel = ResourceModel.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributeName.TOPIC_ARN.toString(), "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
                .attributes(attributes)
                .build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
        final TagResourceResponse tagResourceResponse = TagResourceResponse.builder().build();
        when(proxyClient.client().tagResource(any(TagResourceRequest.class))).thenReturn(tagResourceResponse);
        final UntagResourceResponse untagResourceResponse = UntagResourceResponse.builder().build();
        when(proxyClient.client().untagResource(any(UntagResourceRequest.class))).thenReturn(untagResourceResponse);
        readHandlerMocks();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .desiredResourceTags(tags)
                .previousResourceTags(oldTags)
                .previousResourceState(previousModel)
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        validateResponseSuccess(response);
        verify(proxyClient.client()).tagResource(any(TagResourceRequest.class));
        verify(proxyClient.client()).untagResource(any(UntagResourceRequest.class));
        verify(proxyClient.client(), times(2)).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client(), times(2)).listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class));
    }

    @Test
    public void handleRequest_SimpleSuccess_ContentBasedDeduplication() {

        String fifoTopicArn = "arn:aws:sns:us-east-1:123456789012:sns-topic-name.fifo";

        final ResourceModel model = ResourceModel.builder()
                .topicArn(fifoTopicArn)
                .fifoTopic(true)
                .contentBasedDeduplication(true)
                .build();
        final ResourceModel previousModel = ResourceModel.builder()
                .topicArn(fifoTopicArn)
                .fifoTopic(true)
                .contentBasedDeduplication(false)
                .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributeName.CONTENT_BASED_DEDUPLICATION.toString(), "false");
        attributes.put(TopicAttributeName.TOPIC_ARN.toString(), fifoTopicArn);

        setupUpdateHandlerMocks(attributes);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).previousResourceState(previousModel).build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        validateResponseSuccess(response);
        verify(proxyClient.client()).setTopicAttributes(any(SetTopicAttributesRequest.class));
        verify(proxyClient.client(), times(2)).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client(), times(2)).listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class));
    }

    @Test
    public void handleRequest_SwallowTagResourceAuthorizationException() {
        final Map<String, String> tags = Maps.newHashMap();
        tags.put("key1", "value1");
        tags.put("key3", "value3");

        final ResourceModel model = ResourceModel.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();
        final ResourceModel previousModel = ResourceModel.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributeName.TOPIC_ARN.toString(), "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
                .attributes(attributes)
                .build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
        when(proxyClient.client().tagResource(any(TagResourceRequest.class))).thenThrow(AuthorizationErrorException.builder().message("Tagging Access Denied").build());
        readHandlerMocks();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .desiredResourceTags(tags)
                .previousResourceState(previousModel)
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        validateResponseSuccess(response);
        verify(proxyClient.client()).tagResource(any(TagResourceRequest.class));
        verify(proxyClient.client(), times(2)).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client(), times(2)).listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class));
    }

    @Test
    public void handleRequest_SwallowUntagResourceAuthorizationException() {
        final Map<String, String> oldTags = new HashMap<>();
        oldTags.put("key1", "value1");
        oldTags.put("key2", "value2");

        final ResourceModel model = ResourceModel.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();
        final ResourceModel previousModel = ResourceModel.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributeName.TOPIC_ARN.toString(), "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
                .attributes(attributes)
                .build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
        when(proxyClient.client().untagResource(any(UntagResourceRequest.class))).thenThrow(AuthorizationErrorException.builder().message("Untagging Access Denied").build());
        readHandlerMocks();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .previousResourceState(previousModel)
                .previousResourceTags(oldTags)
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        validateResponseSuccess(response);
        verify(proxyClient.client()).untagResource(any(UntagResourceRequest.class));
        verify(proxyClient.client(), times(2)).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client(), times(2)).listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class));
    }

    @Test
    public void handleRequest_TagResourcException() {
        final Map<String, String> tags = Maps.newHashMap();
        tags.put("key1", "value1");
        tags.put("key3", "value3");

        final ResourceModel model = ResourceModel.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();
        final ResourceModel previousModel = ResourceModel.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributeName.TOPIC_ARN.toString(), "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
                .attributes(attributes)
                .build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
        when(proxyClient.client().tagResource(any(TagResourceRequest.class))).thenThrow(ConcurrentAccessException.builder().message("Tagging Concurrent Access").build());
        final ListSubscriptionsByTopicResponse listSubscriptionsByTopicResponse = ListSubscriptionsByTopicResponse.builder().build();
        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenReturn(listSubscriptionsByTopicResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .desiredResourceTags(tags)
                .previousResourceState(previousModel)
                .build();

        assertThrows(CfnGeneralServiceException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_UntagResourceException() {
        final Map<String, String> oldTags = new HashMap<>();
        oldTags.put("key1", "value1");
        oldTags.put("key2", "value2");

        final ResourceModel model = ResourceModel.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();
        final ResourceModel previousModel = ResourceModel.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributeName.TOPIC_ARN.toString(), "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
                .attributes(attributes)
                .build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
        when(proxyClient.client().untagResource(any(UntagResourceRequest.class))).thenThrow(ConcurrentAccessException.builder().message("Tagging Concurrent Access").build());
        final ListSubscriptionsByTopicResponse listSubscriptionsByTopicResponse = ListSubscriptionsByTopicResponse.builder().build();
        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenReturn(listSubscriptionsByTopicResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .previousResourceState(previousModel)
                .previousResourceTags(oldTags)
                .build();
        assertThrows(CfnGeneralServiceException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    private void validateResponseSuccess(final ProgressEvent<ResourceModel, CallbackContext> response) {
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    private void setupUpdateHandlerMocks(Map<String, String> attributes) {
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
                .attributes(attributes)
                .build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
        final SetTopicAttributesResponse setTopicAttributesResponse = SetTopicAttributesResponse.builder().build();
        when(proxyClient.client().setTopicAttributes(any(SetTopicAttributesRequest.class))).thenReturn(setTopicAttributesResponse);
        readHandlerMocks();
    }

    private void readHandlerMocks() {
        final ListSubscriptionsByTopicResponse listSubscriptionsByTopicResponse = ListSubscriptionsByTopicResponse.builder().build();
        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenReturn(listSubscriptionsByTopicResponse);
        final ListTagsForResourceResponse listTagsForStreamResponse = ListTagsForResourceResponse.builder().build();
        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(listTagsForStreamResponse);
    }
}
