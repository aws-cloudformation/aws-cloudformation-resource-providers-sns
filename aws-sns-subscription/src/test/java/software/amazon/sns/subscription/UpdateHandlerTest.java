package software.amazon.sns.subscription;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.GetSubscriptionAttributesResponse;
import software.amazon.awssdk.services.sns.model.GetSubscriptionAttributesRequest;
import software.amazon.awssdk.services.sns.model.SetSubscriptionAttributesRequest;
import software.amazon.awssdk.services.sns.model.SetSubscriptionAttributesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.OperationStatus;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.INVALID_REQUEST;
import static software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.NOT_FOUND;


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
        verify(snsClient, atLeast(0)).serviceName();
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

        Map<String, String> subscriptionAttributes = new HashMap<>();
        subscriptionAttributes.put("SubscriptionArn", "arn");
        subscriptionAttributes.put("TopicArn", "topicArn");
        subscriptionAttributes.put("Protocol", "email");
        subscriptionAttributes.put("Endpoint", "end");
        subscriptionAttributes.put("RawMessageDelivery", "true");
        subscriptionAttributes.put("PendingConfirmation", "false");

        final GetSubscriptionAttributesResponse getSubscriptionResponse = GetSubscriptionAttributesResponse.builder().attributes(subscriptionAttributes).build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenReturn(getSubscriptionResponse);

        // only raw message delivery should be different
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
        assertThat(response.getResourceModel().getRawMessageDelivery()).isEqualTo(false);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).setSubscriptionAttributes(any(SetSubscriptionAttributesRequest.class));
        verify(proxyClient.client(), times(1)).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));
    }

    @Test
    public void handleRequest_UpdateBooleandAttributes_RawMessageNotChanged() {
        final UpdateHandler handler = new UpdateHandler();

        Map<String, String> subscriptionAttributes = new HashMap<>();
        subscriptionAttributes.put("SubscriptionArn", "arn");
        subscriptionAttributes.put("TopicArn", "topicArn");
        subscriptionAttributes.put("Protocol", "email");
        subscriptionAttributes.put("Endpoint", "end");
        subscriptionAttributes.put("RawMessageDelivery", "true");
        subscriptionAttributes.put("PendingConfirmation", "false");

        final GetSubscriptionAttributesResponse getSubscriptionResponse = GetSubscriptionAttributesResponse.builder().attributes(subscriptionAttributes).build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenReturn(getSubscriptionResponse);

        // only raw message delivery should be different
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

        verify(proxyClient.client(), never()).setSubscriptionAttributes(any(SetSubscriptionAttributesRequest.class));
        verify(proxyClient.client(), times(1)).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));
    }

    @Test
    public void handleRequest_UpdateBooleandAttributes_RawMessageIsNullForLambda() {
        final UpdateHandler handler = new UpdateHandler();

        Map<String, String> subscriptionAttributes = new HashMap<>();
        subscriptionAttributes.put("SubscriptionArn", "arn");
        subscriptionAttributes.put("TopicArn", "topicArn");
        subscriptionAttributes.put("Protocol", "lambda");
        subscriptionAttributes.put("Endpoint", "lambdaArn");
        subscriptionAttributes.put("PendingConfirmation", "false");

        final GetSubscriptionAttributesResponse getSubscriptionResponse = GetSubscriptionAttributesResponse.builder().attributes(subscriptionAttributes).build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenReturn(getSubscriptionResponse);

        // only raw message delivery should be different
        ResourceModel currentModel = buildCurrentObjects();
        ResourceModel desiredModel = buildCurrentObjects();
        desiredModel.setRawMessageDelivery(false);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .previousResourceState(currentModel)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNotNull();
        assertThat(response.getResourceModel().getRawMessageDelivery()).isEqualTo(false);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).setSubscriptionAttributes(any(SetSubscriptionAttributesRequest.class));
        verify(proxyClient.client(), times(1)).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));
    }


    @Test
    public void handleRequest_UpdateMapBasedAttributes() {
        final UpdateHandler handler = new UpdateHandler();

        Map<String, String> subscriptionAttributes = new HashMap<>();
        subscriptionAttributes.put("SubscriptionArn", "arn1");
        subscriptionAttributes.put("TopicArn", "topicArn1");
        subscriptionAttributes.put("Protocol", "email1");
        subscriptionAttributes.put("Endpoint", "end1");
        subscriptionAttributes.put("RawMessageDelivery", "false");
        subscriptionAttributes.put("PendingConfirmation", "false");

        final Map<String, Object> DesiredRedrivePolicy = new HashMap<>();
        DesiredRedrivePolicy.put("deadLetterTargetArn", "arn2");
        DesiredRedrivePolicy.put("maxReceiveCount", "2");

        desiredModel.setRawMessageDelivery(currentModel.getRawMessageDelivery());
        desiredModel.setRedrivePolicy(DesiredRedrivePolicy);

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
        assertThat(response.getResourceModel().getRedrivePolicy()).isEqualTo(DesiredRedrivePolicy);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client(), times(3)).setSubscriptionAttributes(any(SetSubscriptionAttributesRequest.class));
        verify(proxyClient.client(), times(1)).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));
    }

    @Test
    public void handleRequest_UpdateStringAttributes() {
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

        verify(proxyClient.client()).setSubscriptionAttributes(any(SetSubscriptionAttributesRequest.class));
        verify(proxyClient.client(), times(1)).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));
    }

    @Test
    public void handleRequest_SubscriptionArnDoesNotExist()  {
        desiredModel.setSubscriptionArn(null);
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode().toString()).isEqualTo(INVALID_REQUEST.toString());
        verify(proxyClient.client(), never()).setSubscriptionAttributes(any(SetSubscriptionAttributesRequest.class));
        verify(proxyClient.client(), never()).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));
    }

    @Test
    public void handleRequest_SubscriptionDoesNotExist()  {
        AwsServiceException exception = AwsServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(BaseHandlerStd.NOT_FOUND).build())
                .build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenThrow(exception);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode().toString()).isEqualTo(NOT_FOUND.toString());
        verify(proxyClient.client(), never()).setSubscriptionAttributes(any(SetSubscriptionAttributesRequest.class));
        verify(proxyClient.client()).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));
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

        final GetSubscriptionAttributesResponse getSubscriptionResponse = GetSubscriptionAttributesResponse.builder().attributes(subscriptionAttributes).build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenReturn(getSubscriptionResponse);
    }
}
