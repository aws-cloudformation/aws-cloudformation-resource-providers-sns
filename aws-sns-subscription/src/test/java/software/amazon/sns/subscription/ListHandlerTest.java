package software.amazon.sns.subscription;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

        setupGetTopicAttributeMock();


        final ListSubscriptionsByTopicResponse listSubscriptionsByTopicResponse = ListSubscriptionsByTopicResponse.builder()
                                        .subscriptions(listSubscriptions)
                                        .build();

        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenReturn(listSubscriptionsByTopicResponse);

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

        setupGetTopicAttributeMock();

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
        verify(proxyClient.client(), times(2)).listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class));
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
    public void handleRequest_SubscriptionLimitExceededException()  {

        setupGetTopicAttributeMock();
        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenThrow(SubscriptionLimitExceededException.class);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnServiceLimitExceededException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_FilterPolicyLimitExceededException()  {

        setupGetTopicAttributeMock();
        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenThrow(FilterPolicyLimitExceededException.class);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnServiceLimitExceededException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_InvalidParameterException()  {

        setupGetTopicAttributeMock();
        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenThrow(InvalidParameterException.class);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
        assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_InternalErrorException()  {

        setupGetTopicAttributeMock();
        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenThrow(InternalErrorException.class);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
        assertThrows(CfnInternalFailureException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_NotFoundException()  {

        setupGetTopicAttributeMock();
        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenThrow(NotFoundException.class);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
        assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_AuthorizationErrorException()  {

        setupGetTopicAttributeMock();
        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenThrow(AuthorizationErrorException.class);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
        assertThrows(CfnAccessDeniedException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_InvalidSecurityException()  {

        setupGetTopicAttributeMock();
        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenThrow(InvalidSecurityException.class);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
        assertThrows(CfnInvalidCredentialsException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    private void setupGetTopicAttributeMock() {
        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn","topicarn");
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);
    }
}
