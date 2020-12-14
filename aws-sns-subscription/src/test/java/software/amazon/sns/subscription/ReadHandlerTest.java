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
public class ReadHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<SnsClient> proxyClient;

    @Mock
    SnsClient snsClient;

    private ReadHandler handler;
    private ResourceModel model;
    private Map<String, String> attributes;

    @BeforeEach
    public void setup() {
        handler = new ReadHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        snsClient = mock(SnsClient.class);
        proxyClient = MOCK_PROXY(proxy, snsClient);
        buildObjects();
    }

    @AfterEach
    public void tear_down() {
        verify(snsClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(snsClient);
    }

    private void buildObjects() {

        model = ResourceModel.builder().subscriptionArn("testArn").topicArn("topicArn").build();
        attributes = new HashMap<>();
        attributes.put("SubscriptionArn", model.getSubscriptionArn());
        attributes.put("TopicArn", "topicArn");
        attributes.put("Protocol", "email");
        attributes.put("Endpoint", "end1");
        attributes.put("RawMessageDelivery", "false");
        attributes.put("PendingConfirmation", "false");

    }

    @Test
    public void handleRequest_SimpleSuccess_SimpleAttributes() {

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn","topicarn");

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);

        final Map<String, String> attributes = new HashMap<>();

        attributes.put("SubscriptionArn", model.getSubscriptionArn());
        attributes.put("TopicArn", "topicArn");
        attributes.put("Protocol", "email");
        attributes.put("Endpoint", "end1");

        final GetSubscriptionAttributesResponse getSubscriptionResponse = GetSubscriptionAttributesResponse.builder().attributes(attributes).build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenReturn(getSubscriptionResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                .desiredResourceState(model)
                                                                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        final ResourceModel desiredModel = ResourceModel.builder()
                                                .subscriptionArn("testArn")
                                                .protocol("email")
                                                .endpoint("end1")
                                                .topicArn("topicArn")
                                                .build();


        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(desiredModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client(), times(2)).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));

    }

    @Test
    public void handleRequest_SimpleSuccess() throws JsonProcessingException {

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn","topicarn");

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);


        final ObjectMapper objectMapper = new ObjectMapper();


        final Map<String, Object> filterPolicy = new HashMap<>();
        filterPolicy.put("store", "[\"example_corp\"]");
        filterPolicy.put("event", "[\"order_placed\"]");

        final String filterPolicyString = objectMapper.writeValueAsString(filterPolicy);

        final Map<String, Object> redrivePolicy = new HashMap<>();
        redrivePolicy.put("deadLetterTargetArn", "arn");
        redrivePolicy.put("maxReceiveCount", "1");

        final String redrivePolicyString = objectMapper.writeValueAsString(redrivePolicy);

        final Map<String, Object> deliveryPolicy = new HashMap<>();
        deliveryPolicy.put("minDelayTarget", 1);
        deliveryPolicy.put("maxDelayTarget", 2);
        final String deliveryPolicyString = objectMapper.writeValueAsString(deliveryPolicy);

        final Map<String, String> attributes = new HashMap<>();

        attributes.put("SubscriptionArn", model.getSubscriptionArn());
        attributes.put("TopicArn", "topicArn");
        attributes.put("Protocol", "email");
        attributes.put("Endpoint", "end1");
        attributes.put("RawMessageDelivery", "false");
        attributes.put("FilterPolicy", filterPolicyString);
        attributes.put("RedrivePolicy", redrivePolicyString);
        attributes.put("DeliveryPolicy", deliveryPolicyString);

        final GetSubscriptionAttributesResponse getSubscriptionResponse = GetSubscriptionAttributesResponse.builder().attributes(attributes).build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenReturn(getSubscriptionResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                .desiredResourceState(model)
                                                                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        final ResourceModel desiredModel = ResourceModel.builder()
                                                .subscriptionArn("testArn")
                                                .protocol("email")
                                                .endpoint("end1")
                                                .topicArn("topicArn")
                                                .filterPolicy(filterPolicy)
                                                .redrivePolicy(redrivePolicy)
                                                .deliveryPolicy(deliveryPolicy)
                                                .rawMessageDelivery(false)
                                                .build();


        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(desiredModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client(), times(2)).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));

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
}
