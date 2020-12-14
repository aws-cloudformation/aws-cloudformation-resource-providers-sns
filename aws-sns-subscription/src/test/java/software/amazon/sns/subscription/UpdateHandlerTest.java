package software.amazon.sns.subscription;

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
    public void setup() {
        handler = new UpdateHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        snsClient = mock(SnsClient.class);
        proxyClient = MOCK_PROXY(proxy, snsClient);
        currentModel = buildCurrentObjects();
        desiredModel = buildDesiredObjects();
    }

    @AfterEach
    public void tear_down() {
        verify(snsClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(snsClient);
    }

    private ResourceModel buildCurrentObjects() {

        final Map<String, Object> filterPolicy = new HashMap<>();
        filterPolicy.put("store", "[\"example_corp\"]");
        filterPolicy.put("event", "[\"order_placed\"]");

        final Map<String, Object> redrivePolicy = new HashMap<>();
        redrivePolicy.put("deadLetterTargetArn", "arn");
        redrivePolicy.put("maxReceiveCount", "1");

        final Map<String, Object> deliveryPolicy = new HashMap<>();
        deliveryPolicy.put("minDelayTarget", 1);
        deliveryPolicy.put("maxDelayTarget", 2);

        return ResourceModel.builder()
                            .protocol("email")
                            .endpoint("end")
                            .topicArn("topicArn")
                            .subscriptionArn("subarn")
                            .filterPolicy(filterPolicy)
                            .redrivePolicy(redrivePolicy)
                            .deliveryPolicy(deliveryPolicy)
                            .rawMessageDelivery(true)
                            .build();
    }

    private ResourceModel buildDesiredObjects() {

        final Map<String, Object> filterPolicy = new HashMap<>();
        filterPolicy.put("store", "[\"example_corp2\"]");
        filterPolicy.put("event", "[\"order_placed2\"]");

        final Map<String, Object> redrivePolicy = new HashMap<>();
        redrivePolicy.put("deadLetterTargetArn", "arn2");
        redrivePolicy.put("maxReceiveCount", "2");

        final Map<String, Object> deliveryPolicy = new HashMap<>();
        deliveryPolicy.put("minDelayTarget", 2);
        deliveryPolicy.put("maxDelayTarget", 4);

        return ResourceModel.builder()
                            .protocol("email")
                            .endpoint("end")
                            .topicArn("topicArn")
                            .subscriptionArn("subarn")
                            .filterPolicy(filterPolicy)
                            .redrivePolicy(redrivePolicy)
                            .deliveryPolicy(deliveryPolicy)
                            .rawMessageDelivery(false)
                            .build();
    }

    @Test
    public void handleRequest_UpdateBooleandAttributes() {
        final UpdateHandler handler = new UpdateHandler();

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn","topicarn");

        Map<String, String> subscriptionAttributes = new HashMap<>();
        subscriptionAttributes.put("SubscriptionArn", "arn");
        subscriptionAttributes.put("TopicArn", "topicArn");
        subscriptionAttributes.put("Protocol", "email");
        subscriptionAttributes.put("Endpoint", "end");
        subscriptionAttributes.put("RawMessageDelivery", "true");
        subscriptionAttributes.put("PendingConfirmation", "false");


        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);

        final GetSubscriptionAttributesResponse getSubscriptionResponse = GetSubscriptionAttributesResponse.builder().attributes(subscriptionAttributes).build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenReturn(getSubscriptionResponse);

        // only raw message deivery should be different
        ResourceModel currentModel = buildCurrentObjects();
        ResourceModel desiredModel = buildCurrentObjects();
        desiredModel.setRawMessageDelivery(false);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                            .desiredResourceState(desiredModel)
                                                            .previousResourceState(currentModel)
                                                            .build();

        final SetSubscriptionAttributesResponse setSubscriptionAttributesResponse = SetSubscriptionAttributesResponse.builder().build();
        when(proxyClient.client().setSubscriptionAttributes(any(SetSubscriptionAttributesRequest.class))).thenReturn(setSubscriptionAttributesResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNotNull();
        assertThat(response.getResourceModel().getRawMessageDelivery()).isEqualTo(true);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client()).setSubscriptionAttributes(any(SetSubscriptionAttributesRequest.class));
        verify(proxyClient.client(), times(4)).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));
    }

    @Test
    public void handleRequest_UpdateBooleandAttributes_RawMessageNotChanged() {
        final UpdateHandler handler = new UpdateHandler();

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn","topicarn");

        Map<String, String> subscriptionAttributes = new HashMap<>();
        subscriptionAttributes.put("SubscriptionArn", "arn");
        subscriptionAttributes.put("TopicArn", "topicArn");
        subscriptionAttributes.put("Protocol", "email");
        subscriptionAttributes.put("Endpoint", "end");
        subscriptionAttributes.put("RawMessageDelivery", "true");
        subscriptionAttributes.put("PendingConfirmation", "false");


        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);

        final GetSubscriptionAttributesResponse getSubscriptionResponse = GetSubscriptionAttributesResponse.builder().attributes(subscriptionAttributes).build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenReturn(getSubscriptionResponse);//.thenReturn(getSubscriptionResponse);

        // only raw message deivery should be different
        ResourceModel currentModel = buildCurrentObjects();
        ResourceModel desiredModel = buildCurrentObjects();
        desiredModel.setRawMessageDelivery(currentModel.getRawMessageDelivery());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                            .desiredResourceState(desiredModel)
                                                            .previousResourceState(currentModel)
                                                            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNotNull();
        assertThat(response.getResourceModel().getRawMessageDelivery()).isEqualTo(true);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client(), never()).setSubscriptionAttributes(any(SetSubscriptionAttributesRequest.class));
        verify(proxyClient.client(), times(3)).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));
    }

    @Test
    public void handleRequest_UpdateBooleandAttributes_RawMessageIsNullForLambda() {
        final UpdateHandler handler = new UpdateHandler();

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn","topicarn");

        Map<String, String> subscriptionAttributes = new HashMap<>();
        subscriptionAttributes.put("SubscriptionArn", "arn");
        subscriptionAttributes.put("TopicArn", "topicArn");
        subscriptionAttributes.put("Protocol", "lambda");
        subscriptionAttributes.put("Endpoint", "lambdaArn");
        subscriptionAttributes.put("PendingConfirmation", "false");


        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);

        final GetSubscriptionAttributesResponse getSubscriptionResponse = GetSubscriptionAttributesResponse.builder().attributes(subscriptionAttributes).build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenReturn(getSubscriptionResponse);//.thenReturn(getSubscriptionResponse);

        // only raw message deivery should be different
        ResourceModel currentModel = buildCurrentObjects();
        ResourceModel desiredModel = buildCurrentObjects();
        desiredModel.setRawMessageDelivery(currentModel.getRawMessageDelivery());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .previousResourceState(currentModel)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNotNull();
        assertThat(response.getResourceModel().getRawMessageDelivery()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client(), never()).setSubscriptionAttributes(any(SetSubscriptionAttributesRequest.class));
        verify(proxyClient.client(), times(3)).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));
    }


    @Test
    public void handleRequest_UpdateMapBasedAttributes() {
        final UpdateHandler handler = new UpdateHandler();

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn","topicarn");

        Map<String, String> subscriptionAttributes = new HashMap<>();
        subscriptionAttributes.put("SubscriptionArn", "arn1");
        subscriptionAttributes.put("TopicArn", "topicArn1");
        subscriptionAttributes.put("Protocol", "email1");
        subscriptionAttributes.put("Endpoint", "end1");
        subscriptionAttributes.put("RawMessageDelivery", "false");
        subscriptionAttributes.put("PendingConfirmation", "false");

        desiredModel.setRawMessageDelivery(currentModel.getRawMessageDelivery());

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);

        final GetSubscriptionAttributesResponse getSubscriptionResponse = GetSubscriptionAttributesResponse.builder().attributes(subscriptionAttributes).build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenReturn(getSubscriptionResponse).thenReturn(getSubscriptionResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                            .desiredResourceState(desiredModel)
                                                            .previousResourceState(currentModel)
                                                            .build();

        final SetSubscriptionAttributesResponse setSubscriptionAttributesResponse = SetSubscriptionAttributesResponse.builder().build();
        when(proxyClient.client().setSubscriptionAttributes(any(SetSubscriptionAttributesRequest.class))).thenReturn(setSubscriptionAttributesResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNotNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client(), times(3)).setSubscriptionAttributes(any(SetSubscriptionAttributesRequest.class));
        verify(proxyClient.client(), times(6)).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));
    }

    @Test
    public void handleRequest_TopicArnDoesNotExist()  {

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenThrow(NotFoundException.class);


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                .desiredResourceState(desiredModel)
                                                                .previousResourceState(currentModel)
                                                                .build();

        assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(proxyClient.client(), never()).unsubscribe(any(UnsubscribeRequest.class));
        verify(proxyClient.client(), never()).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));

    }

    @Test
    public void handleRequest_SubscriptionDoesNotExist()  {

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn", "topicArn");

      //  GetSubscriptionAttributesResponse getSubscriptionAttributesResponse = GetSubscriptionAttributesResponse.builder().build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenThrow(NotFoundException.class);

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                .desiredResourceState(desiredModel)
                                                                .previousResourceState(currentModel)
                                                                .build();

        assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        verify(proxyClient.client()).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client(), never()).unsubscribe(any(UnsubscribeRequest.class));
    }

    @Test
    public void handleRequest_InvalidParameterExceptionSetSubscription()  {

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn", "topicArn");

        Map<String, String> subscriptionAttributes = new HashMap<>();
        subscriptionAttributes.put("SubscriptionArn", "arn1");
        subscriptionAttributes.put("TopicArn", "topicArn1");
        subscriptionAttributes.put("Protocol", "email1");
        subscriptionAttributes.put("Endpoint", "end1");
        subscriptionAttributes.put("RawMessageDelivery", "false");
        subscriptionAttributes.put("PendingConfirmation", "false");


        when(proxyClient.client().setSubscriptionAttributes(any(SetSubscriptionAttributesRequest.class))).thenThrow(InvalidParameterException.class);

        final GetSubscriptionAttributesResponse getSubscriptionResponse = GetSubscriptionAttributesResponse.builder().attributes(subscriptionAttributes).build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenReturn(getSubscriptionResponse).thenReturn(getSubscriptionResponse);

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                .desiredResourceState(desiredModel)
                                                                .previousResourceState(currentModel)
                                                                .build();

         assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

    }

    @Test
    public void handleRequest_SubscriptionLimitExceededExceptionSetSubscription()  {

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn", "topicArn");

        Map<String, String> subscriptionAttributes = new HashMap<>();
        subscriptionAttributes.put("SubscriptionArn", "arn1");
        subscriptionAttributes.put("TopicArn", "topicArn1");
        subscriptionAttributes.put("Protocol", "email1");
        subscriptionAttributes.put("Endpoint", "end1");
        subscriptionAttributes.put("RawMessageDelivery", "false");
        subscriptionAttributes.put("PendingConfirmation", "false");


        when(proxyClient.client().setSubscriptionAttributes(any(SetSubscriptionAttributesRequest.class))).thenThrow(SubscriptionLimitExceededException.class);

        final GetSubscriptionAttributesResponse getSubscriptionResponse = GetSubscriptionAttributesResponse.builder().attributes(subscriptionAttributes).build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenReturn(getSubscriptionResponse).thenReturn(getSubscriptionResponse);

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                .desiredResourceState(desiredModel)
                                                                .previousResourceState(currentModel)
                                                                .build();

         assertThrows(CfnServiceLimitExceededException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

    }

    @Test
    public void handleRequest_FilterPolicyLimitExceededExceptionSetSubscription()  {

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn", "topicArn");

        Map<String, String> subscriptionAttributes = new HashMap<>();
        subscriptionAttributes.put("SubscriptionArn", "arn1");
        subscriptionAttributes.put("TopicArn", "topicArn1");
        subscriptionAttributes.put("Protocol", "email1");
        subscriptionAttributes.put("Endpoint", "end1");
        subscriptionAttributes.put("RawMessageDelivery", "false");
        subscriptionAttributes.put("PendingConfirmation", "false");


        when(proxyClient.client().setSubscriptionAttributes(any(SetSubscriptionAttributesRequest.class))).thenThrow(FilterPolicyLimitExceededException.class);

        final GetSubscriptionAttributesResponse getSubscriptionResponse = GetSubscriptionAttributesResponse.builder().attributes(subscriptionAttributes).build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenReturn(getSubscriptionResponse).thenReturn(getSubscriptionResponse);

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                .desiredResourceState(desiredModel)
                                                                .previousResourceState(currentModel)
                                                                .build();

         assertThrows(CfnServiceLimitExceededException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

    }

    @Test
    public void handleRequest_AuthorizationErrorExceptionSetSubscription()  {

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn", "topicArn");

        Map<String, String> subscriptionAttributes = new HashMap<>();
        subscriptionAttributes.put("SubscriptionArn", "arn1");
        subscriptionAttributes.put("TopicArn", "topicArn1");
        subscriptionAttributes.put("Protocol", "email1");
        subscriptionAttributes.put("Endpoint", "end1");
        subscriptionAttributes.put("RawMessageDelivery", "false");
        subscriptionAttributes.put("PendingConfirmation", "false");


        when(proxyClient.client().setSubscriptionAttributes(any(SetSubscriptionAttributesRequest.class))).thenThrow(AuthorizationErrorException.class);

        final GetSubscriptionAttributesResponse getSubscriptionResponse = GetSubscriptionAttributesResponse.builder().attributes(subscriptionAttributes).build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenReturn(getSubscriptionResponse).thenReturn(getSubscriptionResponse);

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                .desiredResourceState(desiredModel)
                                                                .previousResourceState(currentModel)
                                                                .build();

         assertThrows(CfnAccessDeniedException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

    }

    @Test
    public void handleRequest_InternalErrorExceptionSetSubscription()  {

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn", "topicArn");

        Map<String, String> subscriptionAttributes = new HashMap<>();
        subscriptionAttributes.put("SubscriptionArn", "arn1");
        subscriptionAttributes.put("TopicArn", "topicArn1");
        subscriptionAttributes.put("Protocol", "email1");
        subscriptionAttributes.put("Endpoint", "end1");
        subscriptionAttributes.put("RawMessageDelivery", "false");
        subscriptionAttributes.put("PendingConfirmation", "false");


        when(proxyClient.client().setSubscriptionAttributes(any(SetSubscriptionAttributesRequest.class))).thenThrow(InternalErrorException.class);

        final GetSubscriptionAttributesResponse getSubscriptionResponse = GetSubscriptionAttributesResponse.builder().attributes(subscriptionAttributes).build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenReturn(getSubscriptionResponse).thenReturn(getSubscriptionResponse);

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                .desiredResourceState(desiredModel)
                                                                .previousResourceState(currentModel)
                                                                .build();

         assertThrows(CfnInternalFailureException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

    }

    @Test
    public void handleRequest_NotFoundExceptionSetSubscription()  {

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn", "topicArn");

        Map<String, String> subscriptionAttributes = new HashMap<>();
        subscriptionAttributes.put("SubscriptionArn", "arn1");
        subscriptionAttributes.put("TopicArn", "topicArn1");
        subscriptionAttributes.put("Protocol", "email1");
        subscriptionAttributes.put("Endpoint", "end1");
        subscriptionAttributes.put("RawMessageDelivery", "false");
        subscriptionAttributes.put("PendingConfirmation", "false");


        when(proxyClient.client().setSubscriptionAttributes(any(SetSubscriptionAttributesRequest.class))).thenThrow(NotFoundException.class);

        final GetSubscriptionAttributesResponse getSubscriptionResponse = GetSubscriptionAttributesResponse.builder().attributes(subscriptionAttributes).build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenReturn(getSubscriptionResponse).thenReturn(getSubscriptionResponse);

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                .desiredResourceState(desiredModel)
                                                                .previousResourceState(currentModel)
                                                                .build();

         assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

    }

    @Test
    public void handleRequest_InvalidSecurityExceptionSetSubscription()  {

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn", "topicArn");

        Map<String, String> subscriptionAttributes = new HashMap<>();
        subscriptionAttributes.put("SubscriptionArn", "arn1");
        subscriptionAttributes.put("TopicArn", "topicArn1");
        subscriptionAttributes.put("Protocol", "email1");
        subscriptionAttributes.put("Endpoint", "end1");
        subscriptionAttributes.put("RawMessageDelivery", "false");
        subscriptionAttributes.put("PendingConfirmation", "false");


        when(proxyClient.client().setSubscriptionAttributes(any(SetSubscriptionAttributesRequest.class))).thenThrow(InvalidSecurityException.class);

        final GetSubscriptionAttributesResponse getSubscriptionResponse = GetSubscriptionAttributesResponse.builder().attributes(subscriptionAttributes).build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenReturn(getSubscriptionResponse).thenReturn(getSubscriptionResponse);

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                .desiredResourceState(desiredModel)
                                                                .previousResourceState(currentModel)
                                                                .build();

         assertThrows(CfnInvalidCredentialsException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

    }

    @Test
    public void handleRequest_InvalidParameterExceptionGetSubscription()  {

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn", "topicArn");

        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenThrow(InvalidParameterException.class);

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                .desiredResourceState(desiredModel)
                                                                .previousResourceState(currentModel)
                                                                .build();

         assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

    }

    @Test
    public void handleRequest_SubscriptionLimitExceededExceptionGetSubscription()  {

         final Map<String, String> topicAttributes = new HashMap<>();
         topicAttributes.put("TopicArn", "topicArn");

         when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenThrow(SubscriptionLimitExceededException.class);

         final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();
         when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);


         final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                 .desiredResourceState(desiredModel)
                                                                 .previousResourceState(currentModel)
                                                                 .build();

          assertThrows(CfnServiceLimitExceededException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

    }

    @Test
    public void handleRequest_FilterPolicyLimitExceededExceptionGetSubscription()  {

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn", "topicArn");

        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenThrow(FilterPolicyLimitExceededException.class);

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                .desiredResourceState(desiredModel)
                                                                .previousResourceState(currentModel)
                                                                .build();

         assertThrows(CfnServiceLimitExceededException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

    }

    @Test
    public void handleRequest_AuthorizationErrorExceptionGetSubscription()  {

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn", "topicArn");

        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenThrow(AuthorizationErrorException.class);

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                .desiredResourceState(desiredModel)
                                                                .previousResourceState(currentModel)
                                                                .build();

         assertThrows(CfnAccessDeniedException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

    }

    @Test
    public void handleRequest_InternalErrorExceptionGetSubscription()  {

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn", "topicArn");

        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenThrow(InternalErrorException.class);

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                .desiredResourceState(desiredModel)
                                                                .previousResourceState(currentModel)
                                                                .build();

         assertThrows(CfnInternalFailureException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

    }

    @Test
    public void handleRequest_NotFoundExceptionGetSubscription()  {

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn", "topicArn");

        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenThrow(NotFoundException.class);

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                .desiredResourceState(desiredModel)
                                                                .previousResourceState(currentModel)
                                                                .build();

         assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

    }

    @Test
    public void handleRequest_InvalidSecurityExceptionGetSubscription()  {

        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn", "topicArn");
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenThrow(InvalidSecurityException.class);

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                                                                .desiredResourceState(desiredModel)
                                                                .previousResourceState(currentModel)
                                                                .build();

         assertThrows(CfnInvalidCredentialsException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

    }

    @Test
    public void handleRequest_UpdateSubscriptionRoleArnAttribute() {

        setupUpdateMocks();

        // only subscription role arn should be different
        ResourceModel currentModel = buildCurrentObjects();
        currentModel.setSubscriptionRoleArn("Subscription-Role-Arn");
        ResourceModel desiredModel = buildCurrentObjects();
        desiredModel.setSubscriptionRoleArn("New-Subscription-Role-Arn");

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .previousResourceState(currentModel)
                .build();

        final SetSubscriptionAttributesResponse setSubscriptionAttributesResponse = SetSubscriptionAttributesResponse.builder().build();
        when(proxyClient.client().setSubscriptionAttributes(any(SetSubscriptionAttributesRequest.class))).thenReturn(setSubscriptionAttributesResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNotNull();
        assertThat(response.getResourceModel().getSubscriptionRoleArn()).isEqualTo("New-Subscription-Role-Arn");
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).getTopicAttributes(any(GetTopicAttributesRequest.class));
        verify(proxyClient.client()).setSubscriptionAttributes(any(SetSubscriptionAttributesRequest.class));
        verify(proxyClient.client(), times(4)).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));
    }

    @Test
    public void handleRequest_invalidUpdateEndpoint() {

        setupUpdateMocks();
        desiredModel.setEndpoint("updated");

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .previousResourceState(currentModel)
                .build();
        assertThrows(CfnNotUpdatableException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_invalidUpdateProtocol() {

        setupUpdateMocks();
        desiredModel.setProtocol("sqs");

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .previousResourceState(currentModel)
                .build();
        assertThrows(CfnNotUpdatableException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_invalidUpdateTopicArn() {

        setupUpdateMocks();
        desiredModel.setTopicArn("newTopicArn");

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .previousResourceState(currentModel)
                .build();
        assertThrows(CfnNotUpdatableException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_generalRuntimeException() {

        setupUpdateMocks();

        when(proxyClient.client().setSubscriptionAttributes(any(SetSubscriptionAttributesRequest.class))).thenThrow(RuntimeException.class);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .previousResourceState(currentModel)
                .build();
        assertThrows(CfnInternalFailureException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    private void setupUpdateMocks() {
        final Map<String, String> topicAttributes = new HashMap<>();
        topicAttributes.put("TopicArn","topicarn");

        Map<String, String> subscriptionAttributes = new HashMap<>();
        subscriptionAttributes.put("SubscriptionArn", "arn");
        subscriptionAttributes.put("TopicArn", "topicArn");
        subscriptionAttributes.put("Protocol", "email");
        subscriptionAttributes.put("Endpoint", "end");
        subscriptionAttributes.put("SubscriptionRoleArn", "New-Subscription-Role-Arn");


        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder().attributes(topicAttributes).build();
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class))).thenReturn(getTopicAttributesResponse);

        final GetSubscriptionAttributesResponse getSubscriptionResponse = GetSubscriptionAttributesResponse.builder().attributes(subscriptionAttributes).build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenReturn(getSubscriptionResponse);
    }
}
