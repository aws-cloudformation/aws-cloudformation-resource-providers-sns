package software.amazon.sns.topic;

import java.time.Duration;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
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
    final GetDataProtectionPolicyResponse getDataProtectionPolicyResponse = GetDataProtectionPolicyResponse.builder().build();

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
        final List<Subscription> subscriptions = new ArrayList<>();
        Subscription.builder().endpoint("abc@xyz.com").protocol("email").build();
        final List<Tag> tags = new ArrayList<>();
        tags.add(Tag.builder().key("key1").value("value1").build());
        Map<String, Object> policy = new HashMap<>();
        policy.put("key", "val");

        final ResourceModel model = ResourceModel.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .topicName("sns-topic-name")
                .displayName("topic-display-name")
                .subscription(subscriptions)
                .signatureVersion("2")
                .tags(tags)
                .dataProtectionPolicy(policy)
                .build();


        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributeName.DISPLAY_NAME.toString(), "topic-display-name");
        attributes.put(TopicAttributeName.TOPIC_ARN.toString(), "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        attributes.put(TopicAttributeName.SIGNATURE_VERSION.toString(), "2");

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
        when(proxyClient.client().getDataProtectionPolicy(any(GetDataProtectionPolicyRequest.class)))
                .thenReturn(GetDataProtectionPolicyResponse.builder().dataProtectionPolicy(toJsonSafe(policy)).build());

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
        verify(proxyClient.client()).getDataProtectionPolicy(any(GetDataProtectionPolicyRequest.class));
    }

    @Test
    public void handleRequest_attributesForDriftDetection() {
        final List<Subscription> subscriptions = new ArrayList<>();
        Subscription.builder().endpoint("abc@xyz.com").protocol("email").build();
        final List<Tag> tags = new ArrayList<>();
        tags.add(Tag.builder().key("key1").value("value1").build());

        final String topicArn = "arn:aws:sns:us-east-1:123456789012:sns-topic-name.fifo";
        final String topicName = "sns-topic-name.fifo";
        final String topicDisplayName = "topic-display-name";
        final String signatureVersion = "2";

        final ResourceModel model = ResourceModel.builder()
                .topicArn(topicArn)
                .topicName(topicName)
                .displayName(topicDisplayName)
                .subscription(subscriptions)
                .signatureVersion(signatureVersion)
                .tags(tags)
                .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributeName.DISPLAY_NAME.toString(), topicDisplayName);
        attributes.put(TopicAttributeName.TOPIC_ARN.toString(), topicArn);
        attributes.put(TopicAttributeName.FIFO_TOPIC.toString(), "true");
        attributes.put(TopicAttributeName.SIGNATURE_VERSION.toString(), signatureVersion);
        attributes.put(TopicAttributeName.CONTENT_BASED_DEDUPLICATION.toString(), "true");
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
                .attributes(attributes)
                .build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenReturn(ListSubscriptionsByTopicResponse.builder().build());
        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(ListTagsForResourceResponse.builder().build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModel().getTopicArn()).isEqualTo(topicArn);
        assertThat(response.getResourceModel().getFifoTopic()).isEqualTo(true);
        assertThat(response.getResourceModel().getContentBasedDeduplication()).isEqualTo(true);

        verify(proxyClient.client()).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client()).listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class));
        verify(proxyClient.client()).listTagsForResource(any(ListTagsForResourceRequest.class));
        verify(proxyClient.client(), never()).getDataProtectionPolicy(any(GetDataProtectionPolicyRequest.class));
    }

    @Test
    public void handleRequest_NotFound() {
        final ResourceModel model = ResourceModel.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenThrow(NotFoundException.class);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();
        assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(proxyClient.client()).getTopicAttributes(any(GetTopicAttributesRequest.class));
    }

    @Test
    public void handleRequest_SwallowTagAuthorizationErrorException() {

        final ResourceModel model = ResourceModel.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributeName.DISPLAY_NAME.toString(), "topic-display-name");
        attributes.put(TopicAttributeName.TOPIC_ARN.toString(), "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
                .attributes(attributes)
                .build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenReturn(ListSubscriptionsByTopicResponse.builder().build());
        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenThrow(AuthorizationErrorException.class);
        when(proxyClient.client().getDataProtectionPolicy(any(GetDataProtectionPolicyRequest.class))).thenReturn(getDataProtectionPolicyResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_TagResourceServiceException() {

        final ResourceModel model = ResourceModel.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributeName.DISPLAY_NAME.toString(), "topic-display-name");
        attributes.put(TopicAttributeName.TOPIC_ARN.toString(), "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
                .attributes(attributes)
                .build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenThrow(ConcurrentAccessException.builder().message("Concurrent Access").build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();
        assertThrows(CfnGeneralServiceException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
        verify(proxyClient.client()).getTopicAttributes(any(GetTopicAttributesRequest.class));
    }

    @Test
    public void handleRequest_SwallowListSubscriptionAuthorizationErrorException() {

        final ResourceModel model = ResourceModel.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributeName.DISPLAY_NAME.toString(), "topic-display-name");
        attributes.put(TopicAttributeName.TOPIC_ARN.toString(), "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
                .attributes(attributes)
                .build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenThrow(AuthorizationErrorException.class);
        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(ListTagsForResourceResponse.builder().build());
        when(proxyClient.client().getDataProtectionPolicy(any(GetDataProtectionPolicyRequest.class))).thenReturn(getDataProtectionPolicyResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_ListSubscriptionServiceException() {

        final ResourceModel model = ResourceModel.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributeName.DISPLAY_NAME.toString(), "topic-display-name");
        attributes.put(TopicAttributeName.TOPIC_ARN.toString(), "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
                .attributes(attributes)
                .build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenThrow(InternalErrorException.builder().message("Internal Error").build());
        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(ListTagsForResourceResponse.builder().build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();
        assertThrows(CfnGeneralServiceException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_SwallowGetDataProtectionPolicyAuthorizationErrorException() {
        final ResourceModel model = ResourceModel.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributeName.DISPLAY_NAME.toString(), "topic-display-name");
        attributes.put(TopicAttributeName.TOPIC_ARN.toString(), "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
                .attributes(attributes)
                .build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenReturn(ListSubscriptionsByTopicResponse.builder().build());
        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(ListTagsForResourceResponse.builder().build());
        when(proxyClient.client().getDataProtectionPolicy(any(GetDataProtectionPolicyRequest.class))).thenThrow(AuthorizationErrorException.class);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_GetDataProtectionPolicyException() {
        final ResourceModel model = ResourceModel.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributeName.DISPLAY_NAME.toString(), "topic-display-name");
        attributes.put(TopicAttributeName.TOPIC_ARN.toString(), "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
                .attributes(attributes)
                .build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenReturn(ListSubscriptionsByTopicResponse.builder().build());
        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(ListTagsForResourceResponse.builder().build());
        when(proxyClient.client().getDataProtectionPolicy(any(GetDataProtectionPolicyRequest.class))).thenThrow(InternalErrorException.class);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();
        assertThrows(CfnGeneralServiceException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_GetDataProtectionPolicyThrottleException() {
        final ResourceModel model = ResourceModel.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributeName.DISPLAY_NAME.toString(), "topic-display-name");
        attributes.put(TopicAttributeName.TOPIC_ARN.toString(), "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
                .attributes(attributes)
                .build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenReturn(ListSubscriptionsByTopicResponse.builder().build());
        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(ListTagsForResourceResponse.builder().build());
        when(proxyClient.client().getDataProtectionPolicy(any(GetDataProtectionPolicyRequest.class))).thenThrow(ThrottledException.class);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();
        assertThrows(CfnThrottlingException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }
}
