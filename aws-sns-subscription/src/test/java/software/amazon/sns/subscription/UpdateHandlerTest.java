package software.amazon.sns.subscription;

import java.time.Duration;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.awssdk.services.sns.*;
import software.amazon.awssdk.services.sns.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<SnsClient> proxyClient;

    @Mock
    SnsClient snsClient;

    private UpdateHandler handler;
    private ResourceModel desiredModel;
    private ResourceModel currentModel;

    @BeforeEach
    public void setup() throws JsonProcessingException {
        handler = new UpdateHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        snsClient = mock(SnsClient.class);
        proxyClient = MOCK_PROXY(proxy, snsClient);
        buildCurrentObjects();
        buildDesiredObjects();
    }

    @AfterEach
    public void tear_down() {
        verify(snsClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(snsClient);
    }

    private void buildCurrentObjects() throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final String filterPolicyString;
        final String redrivePolicyString;
        final String deliveryPolicyString;

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

        desiredModel = ResourceModel.builder()
                            .protocol("email")
                            .endpoint("end1")
                            .topicArn("topicArn")
                            .filterPolicy(filterPolicy)
                            .redrivePolicy(redrivePolicy)
                            .deliveryPolicy(deliveryPolicy)
                            .rawMessageDelivery(false)
                            .build();
    }

    private void buildDesiredObjects() throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final String filterPolicyString;
        final String redrivePolicyString;
        final String deliveryPolicyString;

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

        desiredModel = ResourceModel.builder()
                            .protocol("email")
                            .endpoint("end1")
                            .topicArn("topicArn")
                            .filterPolicy(filterPolicy)
                            .redrivePolicy(redrivePolicy)
                            .deliveryPolicy(deliveryPolicy)
                            .rawMessageDelivery(false)
                            .build();
    }
    @Test
    public void handleRequest_SimpleSuccess() {
        final UpdateHandler handler = new UpdateHandler();

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn","topicarn");

        Map<String, String> subscriptionAttributes = new HashMap<>();
        subscriptionAttributes.put("SubscriptionArn", "arn1");
        subscriptionAttributes.put("TopicArn", "topicArn");
        subscriptionAttributes.put("Protocol", "email");
        subscriptionAttributes.put("Endpoint", "end1");
        subscriptionAttributes.put("RawMessageDelivery", "false");
        subscriptionAttributes.put("PendingConfirmation", "false");
   

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);

        final GetSubscriptionAttributesResponse getSubscriptionResponse = GetSubscriptionAttributesResponse.builder().attributes(subscriptionAttributes).build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenReturn(getSubscriptionResponse).thenReturn(getSubscriptionResponse).thenThrow(ResourceNotFoundException.class);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                            .desiredResourceState(desiredModel)
                                                            .previousResourceState(currentModel)
                                                            .build();

        final SetSubscriptionAttributesRequest setSubscriptionAttributesRequest = SetSubscriptionAttributesRequest.builder()
                                .attributeName("FilterPolicy").attributeValue("{\"store\": [\"example_corp\"]}")
                                .attributeName("RedrivePolicy").attributeValue("{\"rname\": \"value\"}")
                                .attributeName("DeliveryPolicy").attributeValue("{\"dname\": \"value\"}")
                                .attributeName("rawMessageDeliver").attributeValue("false")
                                .build();
                                                                            
        final SetSubscriptionAttributesResponse setSubscriptionAttributesResponse = SetSubscriptionAttributesResponse.builder().build();
        when(proxyClient.client().setSubscriptionAttributes(setSubscriptionAttributesRequest)).thenReturn(setSubscriptionAttributesResponse);

                                                            
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client()).setSubscriptionAttributes(setSubscriptionAttributesRequest);
        verify(proxyClient.client()).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));


    }
}
