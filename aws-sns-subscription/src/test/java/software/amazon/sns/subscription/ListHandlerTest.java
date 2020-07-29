package software.amazon.sns.subscription;

import java.time.Duration;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.awssdk.services.sns.*;
import software.amazon.awssdk.services.sns.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.util.*;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<SnsClient> proxyClient;

    @Mock
    SnsClient snsClient;

    private ListHandler handler;
    private Map<String, String> attributes;
    private ResourceModel model;

    @BeforeEach
    public void setup() {
        handler = new ListHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        snsClient = mock(SnsClient.class);
        proxyClient = MOCK_PROXY(proxy, snsClient);
        buildObjects();
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
    public void handleRequest_SimpleSuccess() {
        final Subscription subscription1 = Subscription.builder().subscriptionArn("arn1").build();
        final Subscription subscription2 = Subscription.builder().subscriptionArn("arn2").build();
        final List<Subscription> listSubscriptions = new ArrayList<>();
        listSubscriptions.add(subscription1);
        listSubscriptions.add(subscription2);

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn","topicarn");

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);


        final ListSubscriptionsByTopicResponse listSubscriptionsByTopicResponse = ListSubscriptionsByTopicResponse.builder()
                                        .subscriptions(listSubscriptions)
                                        .build();

        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenReturn(listSubscriptionsByTopicResponse);
//
//        final GetSubscriptionAttributesResponse getSubscriptionResponse = GetSubscriptionAttributesResponse.builder().attributes(attributes).build();
//        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenReturn(getSubscriptionResponse).thenReturn(getSubscriptionResponse).thenThrow(ResourceNotFoundException.class);


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                .desiredResourceState(model)
                                                                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getNextToken()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client(), times(1)).getTopicAttributes(any(GetTopicAttributesRequest.class));
       // verify(proxyClient.client(), times(1)).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));
        verify(proxyClient.client(), times(1)).listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class));
    }

    @Test
    public void handleRequest_WithToken() {
        final Subscription subscription1 = Subscription.builder().protocol("email").topicArn("topicArn").subscriptionArn("arn1").build();
        final Subscription subscription2 = Subscription.builder().protocol("email").topicArn("topicArn").subscriptionArn("arn2").build();
        final List<Subscription> listSubscriptions = new ArrayList<>();
        listSubscriptions.add(subscription1);
        listSubscriptions.add(subscription2);

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn","topicarn");

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);


        final ListSubscriptionsByTopicResponse listSubscriptionsByTopicResponse = ListSubscriptionsByTopicResponse.builder()
                                        .subscriptions(listSubscriptions)
                                        .nextToken("nextToken")
                                        .build();

        final Subscription subscription3 = Subscription.builder().protocol("email").topicArn("topicArn").subscriptionArn("arn3").build();
        final List<Subscription> listSubscriptions2 = new ArrayList<>();
        listSubscriptions2.add(subscription3);

        final ListSubscriptionsByTopicResponse listSubscriptionsByTopicResponse2 = ListSubscriptionsByTopicResponse.builder()
                                        .subscriptions(listSubscriptions2)
                                        .nextToken("")
                                        .build();

        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenReturn(listSubscriptionsByTopicResponse).thenReturn(listSubscriptionsByTopicResponse2);

//        final GetSubscriptionAttributesResponse getSubscriptionResponse = GetSubscriptionAttributesResponse.builder().attributes(attributes).build();
//        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenReturn(getSubscriptionResponse).thenReturn(getSubscriptionResponse);

        final ResourceModel model = ResourceModel.builder()
                                    .topicArn("topicArn")
                                    .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                .desiredResourceState(model)
                                                                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getResourceModels().size()).isEqualTo(2);
        assertThat(response.getResourceModels().get(0).getTopicArn()).isEqualTo("topicArn");
        assertThat(response.getResourceModels().get(0).getSubscriptionArn()).isEqualTo("arn1");
        assertThat(response.getResourceModels().get(0).getProtocol()).isEqualTo("email");
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getNextToken()).isEqualTo("nextToken");

        final ListSubscriptionsByTopicRequest listSubscriptionsByTopicRequest = ListSubscriptionsByTopicRequest.builder()
                                                                    .topicArn("topicArn")
                                                                    .nextToken("nextToken")
                                                                    .build();

        // test 2nd iteration with token returned
        final ResourceHandlerRequest<ResourceModel> request2 = ResourceHandlerRequest.<ResourceModel>builder()
                                                                .desiredResourceState(model)
                                                                .build();


        final ProgressEvent<ResourceModel, CallbackContext> response2 = handler.handleRequest(proxy, request2, new CallbackContext(), proxyClient, logger);
        assertThat(response2).isNotNull();
        assertThat(response2.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response2.getCallbackContext()).isNull();
        assertThat(response2.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response2.getResourceModel()).isNull();
        assertThat(response2.getResourceModels()).isNotNull();
        assertThat(response2.getResourceModels().size()).isEqualTo(1);
        assertThat(response2.getResourceModels().get(0).getTopicArn()).isEqualTo("topicArn");
        assertThat(response2.getResourceModels().get(0).getSubscriptionArn()).isEqualTo("arn3");
        assertThat(response2.getResourceModels().get(0).getProtocol()).isEqualTo("email");
        assertThat(response2.getMessage()).isNull();
        assertThat(response2.getErrorCode()).isNull();
        assertThat(response2.getNextToken()).isEqualTo("");

        verify(proxyClient.client(), times(2)).getTopicAttributes(any(GetTopicAttributesRequest.class));
     //   verify(proxyClient.client(), times(2)).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));
        verify(proxyClient.client(), times(2)).listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class));
    }

    @Test
    public void handleRequest_TopicArnDoesNotExist()  {

        final Map<String, String> topicAttributes = new HashMap<>();

      //  final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenThrow(NotFoundException.class);


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                .desiredResourceState(model)
                                                                .build();

        assertThrows(CfnNotFoundException.class, () -> {handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);});

        verify(proxyClient.client()).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client(), never()).unsubscribe(any(UnsubscribeRequest.class));
        verify(proxyClient.client(), never()).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));

    }


}
