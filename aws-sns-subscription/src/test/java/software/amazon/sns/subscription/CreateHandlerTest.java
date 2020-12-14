package software.amazon.sns.subscription;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.cloudformation.exceptions.*;
import software.amazon.cloudformation.proxy.*;

import java.time.Duration;
import java.util.HashMap;
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
    SnsClient SnsClient;

    private CreateHandler handler;
    private ResourceModel model;
    private String filterPolicyString;
    private String redrivePolicyString;
    private String deliveryPolicyString;

    @BeforeEach
    public void setup() throws JsonProcessingException {
        handler = new CreateHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        SnsClient = mock(SnsClient.class);
        proxyClient = MOCK_PROXY(proxy, SnsClient);
        buildObjects();
    }

    @AfterEach
    public void tear_down() {
        verify(SnsClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(SnsClient);
    }

    private void buildObjects() throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();

        final Map<String, Object> filterPolicy = new HashMap<>();
        filterPolicy.put("store", "[\"example_corp\"]");
        filterPolicy.put("event", "[\"order_placed\"]");

        filterPolicyString = objectMapper.writeValueAsString(filterPolicy);

        final Map<String, Object> redrivePolicy = new HashMap<>();
        redrivePolicy.put("deadLetterTargetArn", "arn");
        redrivePolicy.put("maxReceiveCount", "1");

        redrivePolicyString = objectMapper.writeValueAsString(redrivePolicy);

        final Map<String, Object> deliveryPolicy = new HashMap<>();
        deliveryPolicy.put("minDelayTarget", 1);
        deliveryPolicy.put("maxDelayTarget", 2);
        deliveryPolicyString = objectMapper.writeValueAsString(deliveryPolicy);

        model = ResourceModel.builder()
                .protocol("email")
                .endpoint("end1")
                .topicArn("topicArn")
                .filterPolicy(filterPolicy)
                .redrivePolicy(redrivePolicy)
                .deliveryPolicy(deliveryPolicy)
                .rawMessageDelivery(false)
                .build();
    }

    private ResourceModel buildObjectsSimpleAttributes() throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();

        filterPolicyString = objectMapper.writeValueAsString(null);
        redrivePolicyString = objectMapper.writeValueAsString(null);
        deliveryPolicyString = objectMapper.writeValueAsString(null);

        model = ResourceModel.builder()
                .protocol("email")
                .endpoint("end1")
                .topicArn("topicArn")
                .filterPolicy(null)
                .redrivePolicy(null)
                .deliveryPolicy(null)
                .rawMessageDelivery(null)
                .build();

        return model;
    }

    @Test
    public void handleRequest_Success_TopicArnExists()  {

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn","topicarn");

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);


        final SubscribeResponse subscribeResponse = SubscribeResponse.builder().subscriptionArn("testarn").build();
        when(proxyClient.client().subscribe(any(SubscribeRequest.class))).thenReturn(subscribeResponse);

        final Map<String, String> attributes = new HashMap<>();

        attributes.put("SubscriptionArn", subscribeResponse.subscriptionArn());
        attributes.put("TopicArn", model.getTopicArn());
        attributes.put("Protocol", model.getProtocol());
        attributes.put("Endpoint", model.getEndpoint());
        attributes.put("RawMessageDelivery", Boolean.toString(model.getRawMessageDelivery()));
        attributes.put("FilterPolicy", filterPolicyString);
        attributes.put("RedrivePolicy", redrivePolicyString);
        attributes.put("DeliveryPolicy", deliveryPolicyString);

        final GetSubscriptionAttributesResponse getSubscriptionResponse = GetSubscriptionAttributesResponse.builder().attributes(attributes).build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenReturn(getSubscriptionResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();

        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();


        verify(proxyClient.client()).subscribe(any(SubscribeRequest.class));

        // create and read invocations
        verify(proxyClient.client(), times(2)).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client(), times(2)).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));

    }


    @Test
    public void handleRequest_Success_TopicArnExists_SimpleAttributes() throws JsonProcessingException  {

        final ResourceModel model = buildObjectsSimpleAttributes();

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn","topicarn");

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);


        final SubscribeResponse subscribeResponse = SubscribeResponse.builder().subscriptionArn("testarn").build();
        when(proxyClient.client().subscribe(any(SubscribeRequest.class))).thenReturn(subscribeResponse);

        final Map<String, String> attributes = new HashMap<>();

        attributes.put("SubscriptionArn", subscribeResponse.subscriptionArn());
        attributes.put("TopicArn", model.getTopicArn());
        attributes.put("Protocol", model.getProtocol());
        attributes.put("Endpoint", model.getEndpoint());
        attributes.put("RawMessageDelivery", null);
        attributes.put("FilterPolicy", null);
        attributes.put("RedrivePolicy", null);
        attributes.put("DeliveryPolicy", null);
        final GetSubscriptionAttributesResponse getSubscriptionResponse = GetSubscriptionAttributesResponse.builder().attributes(attributes).build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenReturn(getSubscriptionResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();

        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();


        verify(proxyClient.client()).subscribe(any(SubscribeRequest.class));

        // create and read invocations
        verify(proxyClient.client(), times(2)).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client(), times(2)).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));

    }

    @Test
    public void handleRequest_InvalidRequestExceptionThrown()  {

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn","topicarn");

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
        when(proxyClient.client().subscribe(any(SubscribeRequest.class))).thenThrow(InvalidParameterException.class);
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
        assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
        verify(proxyClient.client()).subscribe(any(SubscribeRequest.class));
    }

    @Test
    public void handleRequest_InternalErrorExceptionThrown()  {

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn","topicarn");

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
        when(proxyClient.client().subscribe(any(SubscribeRequest.class))).thenThrow(InternalErrorException.class);
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnInternalFailureException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
        verify(proxyClient.client()).subscribe(any(SubscribeRequest.class));
    }

    @Test
    public void handleRequest_SubscriptionLimitExceededExceptionThrown()  {

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn","topicarn");

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
        when(proxyClient.client().subscribe(any(SubscribeRequest.class))).thenThrow(SubscriptionLimitExceededException.class);
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnServiceLimitExceededException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
        verify(proxyClient.client()).subscribe(any(SubscribeRequest.class));
    }

    @Test
    public void handleRequest_FilterPolicyLimitExceededExceptionThrown()  {

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn","topicarn");

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
        when(proxyClient.client().subscribe(any(SubscribeRequest.class))).thenThrow(FilterPolicyLimitExceededException.class);
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnServiceLimitExceededException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
        verify(proxyClient.client()).subscribe(any(SubscribeRequest.class));
    }

    @Test
    public void handleRequest_NotFoundExceptionThrown()  {

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn","topicarn");

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
        when(proxyClient.client().subscribe(any(SubscribeRequest.class))).thenThrow(NotFoundException.class);
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
        verify(proxyClient.client()).subscribe(any(SubscribeRequest.class));
    }

    @Test
    public void handleRequest_AuthorizationErrorExceptionThrown()  {

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn","topicarn");

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
        when(proxyClient.client().subscribe(any(SubscribeRequest.class))).thenThrow(AuthorizationErrorException.class);
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnAccessDeniedException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
        verify(proxyClient.client()).subscribe(any(SubscribeRequest.class));
    }

    @Test
    public void handleRequest_GeneralRunTimeExceptionThrown()  {

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn","topicarn");

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
        when(proxyClient.client().subscribe(any(SubscribeRequest.class))).thenThrow(RuntimeException.class);
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnInternalFailureException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
        verify(proxyClient.client()).subscribe(any(SubscribeRequest.class));
    }

    @Test
    public void handleRequest_InvalidSecurityExceptionThrown()  {

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn","topicarn");

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
        when(proxyClient.client().subscribe(any(SubscribeRequest.class))).thenThrow(InvalidSecurityException.class);
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnInvalidCredentialsException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
        verify(proxyClient.client()).subscribe(any(SubscribeRequest.class));
    }

    @Test
    public void handleRequest_TopicArnDoesNotExist()  {

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenThrow(NotFoundException.class);


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(proxyClient.client()).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client(), never()).unsubscribe(any(UnsubscribeRequest.class));
        verify(proxyClient.client(), never()).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));

    }

    @Test
    public void handleRequest_Success_SubscriptionRoleArn() {

        final String FIREHOSE_PROTOCOL = "firehose";
        final String DELIVERY_STREAM_ARN = "deliverStreamArn";
        final String TOPIC_ARN = "topicarn";
        final String SUBSCRIPTION_ARN = "testarn";
        final String SUBSCRIPTION_ROLE_ARN = "subscription-role-arn";
        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn",TOPIC_ARN);

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);


        final SubscribeResponse subscribeResponse = SubscribeResponse.builder().subscriptionArn(SUBSCRIPTION_ARN).build();
        when(proxyClient.client().subscribe(any(SubscribeRequest.class))).thenReturn(subscribeResponse);

        final Map<String, String> attributes = new HashMap<>();

        attributes.put("SubscriptionArn", subscribeResponse.subscriptionArn());
        attributes.put("TopicArn", TOPIC_ARN);
        attributes.put("Protocol", FIREHOSE_PROTOCOL);
        attributes.put("Endpoint", DELIVERY_STREAM_ARN);
        attributes.put("SubscriptionRoleArn", SUBSCRIPTION_ROLE_ARN);

        final GetSubscriptionAttributesResponse getSubscriptionResponse = GetSubscriptionAttributesResponse.builder().attributes(attributes).build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenReturn(getSubscriptionResponse);

        ResourceModel desiredModel = ResourceModel.builder()
                .protocol(FIREHOSE_PROTOCOL)
                .endpoint(DELIVERY_STREAM_ARN)
                .topicArn(TOPIC_ARN)
                .subscriptionArn(SUBSCRIPTION_ARN)
                .subscriptionRoleArn(SUBSCRIPTION_ROLE_ARN)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();

        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();


        verify(proxyClient.client()).subscribe(any(SubscribeRequest.class));

        // create and read invocations
        verify(proxyClient.client(), times(2)).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client(), times(2)).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));

    }
}
