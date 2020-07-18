package software.amazon.sns.subscription;

import java.time.Duration;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
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
public class CreateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<SnsClient> proxyClient;

    @Mock
    SnsClient SnsClient;

    private CreateHandler handler;
    private ObjectMapper objectMapper;
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
        objectMapper = new ObjectMapper();
        //   Object ob1 = new String("[\"example_corp\"]");
   
        Map<String, Object> filterPolicy = new HashMap<>();
        filterPolicy.put("store", "[\"example_corp\"]");
        filterPolicy.put("event", "[\"order_placed\"]");

        filterPolicyString = objectMapper.writeValueAsString(filterPolicy);

        Map<String, Object> redrivePolicy = new HashMap<>();
        redrivePolicy.put("deadLetterTargetArn", "arn");
        redrivePolicy.put("maxReceiveCount", "1");

        redrivePolicyString = objectMapper.writeValueAsString(redrivePolicy);

        System.out.println(filterPolicyString);

        Map<String, Object> deliveryPolicy = new HashMap<>();
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
    @Test
    public void handleRequest_TopicArnExists()  {

        Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn","topicarn");

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);


        final SubscribeResponse subscribeResponse = SubscribeResponse.builder().subscriptionArn("testarn").build();;
        when(proxyClient.client().subscribe(any(SubscribeRequest.class))).thenReturn(subscribeResponse);

        Map<String, String> attributes = new HashMap<>();
        // dont want to hard code these
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

        //System.out.println()

        assertThat(response).isNotNull();
 
        // in progress waiting for the user confirm subscription.
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();


        verify(proxyClient.client()).subscribe(any(SubscribeRequest.class));

    }

    @Test
    public void handleRequest_TopicArnDoesNotExist() {

        Map<String, String> topicAttributes = new HashMap<>();

        GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                .desiredResourceState(model)
                                                                .build();
        boolean exceptionThrown = false;
        try {
         final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(CfnNotFoundException.class);
            assertThat(e).hasMessage("topic topicArn not found!");
            exceptionThrown = true;
        }

        assertThat(exceptionThrown).isTrue();
    }
}
