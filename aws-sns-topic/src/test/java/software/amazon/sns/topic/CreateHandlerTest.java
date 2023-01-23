package software.amazon.sns.topic;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.cloudformation.exceptions.*;
import software.amazon.cloudformation.proxy.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @AfterEach()
    public void postExecute(org.junit.jupiter.api.TestInfo testInfo) {
        if(!testInfo.getTags().contains("skipSdkInteraction")) {
            verify(client, atMost(2)).serviceName();
            verifyNoMoreInteractions(client);
        }
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final List<Subscription> subscriptions = new ArrayList<>();
        Subscription subscription = Subscription.builder().endpoint("abc@xyz.com").protocol("email").build();
        subscriptions.add(subscription);
        final ResourceModel model = ResourceModel.builder()
                .subscription(subscriptions)
                .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributeName.TOPIC_ARN.toString(), "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
                .attributes(attributes)
                .build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenThrow(NotFoundException.builder().message("no topic found").build())
                .thenReturn(getTopicAttributesResponse);

        final CreateTopicResponse createTopicResponse = CreateTopicResponse.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();
        when(proxyClient.client().createTopic(any(CreateTopicRequest.class))).thenReturn(createTopicResponse);

        final ListTagsForResourceResponse listTagsForStreamResponse = ListTagsForResourceResponse.builder().build();
        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(listTagsForStreamResponse);

        final ListSubscriptionsByTopicResponse listSubscriptionsByTopicResponse = ListSubscriptionsByTopicResponse.builder()
                .subscriptions(software.amazon.awssdk.services.sns.model.Subscription.builder()
                        .subscriptionArn("subs-arn")
                        .endpoint("endpoint")
                        .protocol("email")
                        .build()
                )
                .build();
        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenReturn(listSubscriptionsByTopicResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier("SnsTopic")
                .clientRequestToken("dummy-token")
                .region("us-east-1")
                .awsAccountId("1234567890")
                .stackId("stackid")
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        validateResponseSuccess(response);

        verify(proxyClient.client()).createTopic(any(CreateTopicRequest.class));
        verify(proxyClient.client(), times(2)).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client(), times(2)).listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class));
        verify(proxyClient.client()).listTagsForResource(any(ListTagsForResourceRequest.class));
        verify(proxyClient.client()).getDataProtectionPolicy(any(GetDataProtectionPolicyRequest.class));
        verify(proxyClient.client()).subscribe(any(SubscribeRequest.class));
    }

    @Test
    public void handleRequest_SimpleSuccess_FifoTopic() {
        String fifoTopicName = "sns-topic-name.fifo";
        String fifoTopicArn = "arn:aws:sns:us-east-1:123456789012:" + fifoTopicName;

        final ResourceModel model = ResourceModel.builder()
                .fifoTopic(true)
                .contentBasedDeduplication(true)
                .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributeName.TOPIC_ARN.toString(), fifoTopicArn);
        attributes.put(TopicAttributeName.FIFO_TOPIC.toString(), "true");
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
                .attributes(attributes)
                .build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenThrow(NotFoundException.builder().message("no topic found").build())
                .thenReturn(getTopicAttributesResponse);

        final CreateTopicResponse createTopicResponse = CreateTopicResponse.builder()
                .topicArn(fifoTopicArn)
                .build();
        when(proxyClient.client().createTopic(any(CreateTopicRequest.class))).thenReturn(createTopicResponse);
        readHandlerMocks();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier("SnsTopic")
                .clientRequestToken("dummy-token")
                .region("us-east-1")
                .awsAccountId("1234567890")
                .stackId("stackid")
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        validateResponseSuccess(response);
        assertThat(response.getResourceModel().getTopicName()).isEqualTo(fifoTopicName);
        assertThat(response.getResourceModel().getTopicArn()).isEqualTo(fifoTopicArn);
        verify(proxyClient.client()).createTopic(any(CreateTopicRequest.class));
        verify(proxyClient.client(), times(2)).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client()).listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class));
        verify(proxyClient.client()).listTagsForResource(any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_SimpleSuccess_WithAttributes() {
        final ResourceModel model = ResourceModel.builder()
                .displayName("sns-topic")
                .kmsMasterKeyId("dummy-kms-key-id")
                .signatureVersion("2")
                .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributeName.TOPIC_ARN.toString(), "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        attributes.put(TopicAttributeName.SIGNATURE_VERSION.toString(), "2");
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
                .attributes(attributes)
                .build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenThrow(NotFoundException.builder().message("no topic found").build())
                .thenReturn(getTopicAttributesResponse);

        final CreateTopicResponse createTopicResponse = CreateTopicResponse.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();
        when(proxyClient.client().createTopic(any(CreateTopicRequest.class))).thenReturn(createTopicResponse);
        readHandlerMocks();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).logicalResourceIdentifier("SnsTopic").clientRequestToken("dummy-token").stackId("stackid").build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        validateResponseSuccess(response);
        verify(proxyClient.client()).createTopic(any(CreateTopicRequest.class));
        verify(proxyClient.client(), times(2)).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client()).listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class));
        verify(proxyClient.client()).listTagsForResource(any(ListTagsForResourceRequest.class));
        verify(proxyClient.client()).getDataProtectionPolicy(any(GetDataProtectionPolicyRequest.class));
    }

    @Test
    public void handleRequest_Failure_AlreadyExists() {
        final ResourceModel model = ResourceModel.builder()
                .topicName("sns-topic-name")
                .build();

        final Map<String, String> attributes = new HashMap<>();
        attributes.put("TopicArn", "arn:aws:sns:us-east-1:123456789012:sns-topic-name");

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
                .attributes(attributes)
                .build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getErrorCode()).isNotNull();

        verify(proxyClient.client(), times(1)).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client(), never()).createTopic(any(CreateTopicRequest.class));
    }

    @Test
    public void handleRequest_getTopicAttributeAuthorizationErrorException() {
        final ResourceModel model = ResourceModel.builder()
                .topicName("sns-topic-name")
                .build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenThrow(AuthorizationErrorException.class);
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();

        assertThrows(CfnAccessDeniedException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_getTopicAttributeInternalError() {
        final ResourceModel model = ResourceModel.builder()
                .topicName("sns-topic-name")
                .build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenThrow(InternalError.class);
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();
        assertThrows(CfnInternalFailureException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_getTopicAttributeInvalidParameterException() {
        final ResourceModel model = ResourceModel.builder()
                .topicName("sns-topic-name")
                .build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenThrow(InvalidParameterException.class);
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();
        assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_getTopicAttributeInvalidSecurityException() {
        final ResourceModel model = ResourceModel.builder()
                .topicName("sns-topic-name")
                .build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenThrow(InvalidSecurityException.class);
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();
        assertThrows(CfnInvalidCredentialsException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_getTopicAttributeRuntimeException() {
        final ResourceModel model = ResourceModel.builder()
                .topicName("sns-topic-name")
                .build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenThrow(RuntimeException.class);
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();
        assertThrows(CfnInternalFailureException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_CreateWithTagAuthorizationError() {
        final ResourceModel model = ResourceModel.builder()
                .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributeName.TOPIC_ARN.toString(), "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
                .attributes(attributes)
                .build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenThrow(NotFoundException.builder().message("no topic found").build())
                .thenReturn(getTopicAttributesResponse);

        when(proxyClient.client().createTopic(any(CreateTopicRequest.class))).thenThrow(AuthorizationErrorException.builder().message("Tagging Access Denied").build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceTags(ImmutableMap.of("KeyName", "Value"))
                .desiredResourceState(model)
                .logicalResourceIdentifier("SnsTopic")
                .clientRequestToken("dummy-token")
                .region("us-east-1")
                .awsAccountId("1234567890")
                .stackId("stackid")
                .build();

        assertThrows(CfnAccessDeniedException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_CreateTopicWithTagException() {
        final ResourceModel model = ResourceModel.builder()
                .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributeName.TOPIC_ARN.toString(), "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
                .attributes(attributes)
                .build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenThrow(NotFoundException.builder().message("no topic found").build())
                .thenReturn(getTopicAttributesResponse);

        final CreateTopicResponse createTopicResponse = CreateTopicResponse.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();
        when(proxyClient.client().createTopic(any(CreateTopicRequest.class))).thenThrow(InternalErrorException.class).thenReturn(createTopicResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceTags(ImmutableMap.of("KeyName", "Value"))
                .desiredResourceState(model)
                .logicalResourceIdentifier("SnsTopic")
                .clientRequestToken("dummy-token")
                .region("us-east-1")
                .awsAccountId("1234567890")
                .stackId("stackid")
                .build();

        assertThrows(CfnGeneralServiceException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
        verify(proxyClient.client()).createTopic(any(CreateTopicRequest.class));
    }

    @Test
    @org.junit.jupiter.api.Tag("skipSdkInteraction")
    public void handleRequest_Failure_InvalidRequest_Id() {
        final ResourceModel model = ResourceModel.builder()
                .topicName("sns-topic-name")
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    @Test
    public void handleRequest_SimpleSuccess_WithDataProtectionPolicy() {
        Map<String, Object> stewardPolicy = new HashMap<>();
        stewardPolicy.put("key", "test");
        final ResourceModel model = ResourceModel.builder()
                .dataProtectionPolicy(stewardPolicy)
                .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(TopicAttributeName.TOPIC_ARN.toString(), "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
                .attributes(attributes)
                .build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenThrow(NotFoundException.builder().message("no topic found").build())
                .thenReturn(getTopicAttributesResponse);

        final CreateTopicResponse createTopicResponse = CreateTopicResponse.builder()
                .topicArn("arn:aws:sns:us-east-1:123456789012:sns-topic-name")
                .build();
        when(proxyClient.client()
                .createTopic(argThat((ArgumentMatcher<CreateTopicRequest>) req -> req.dataProtectionPolicy().equals(toJsonSafe(stewardPolicy)))))
                .thenReturn(createTopicResponse);
        readHandlerMocks();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier("SnsTopic")
                .clientRequestToken("dummy-token")
                .region("us-east-1")
                .awsAccountId("1234567890")
                .stackId("stackid")
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        validateResponseSuccess(response);
        verify(proxyClient.client()).createTopic(any(CreateTopicRequest.class));
        verify(proxyClient.client(), times(2)).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client()).listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class));
        verify(proxyClient.client()).listTagsForResource(any(ListTagsForResourceRequest.class));
        verify(proxyClient.client()).getDataProtectionPolicy(any(GetDataProtectionPolicyRequest.class));
    }

    private void readHandlerMocks() {
        final ListSubscriptionsByTopicResponse listSubscriptionsByTopicResponse = ListSubscriptionsByTopicResponse.builder().build();
        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenReturn(listSubscriptionsByTopicResponse);
        final ListTagsForResourceResponse listTagsForStreamResponse = ListTagsForResourceResponse.builder().build();
        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(listTagsForStreamResponse);
    }

    private void validateResponseSuccess(final ProgressEvent<ResourceModel, CallbackContext> response) {
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
