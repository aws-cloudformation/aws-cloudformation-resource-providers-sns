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
import software.amazon.awssdk.services.sns.model.UnsubscribeRequest;
import software.amazon.awssdk.services.sns.model.UnsubscribeResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.OperationStatus;
import static software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.INVALID_REQUEST;
import static software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.NOT_FOUND;

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


@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<SnsClient> proxyClient;

    @Mock
    SnsClient snsClient;

    private DeleteHandler handler;
    private Map<String, String> attributes;
    private ResourceModel model;

    @BeforeEach
    public void setup() {
        handler = new DeleteHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        snsClient = mock(SnsClient.class);
        proxyClient = MOCK_PROXY(proxy, snsClient);
        buildObjects();
    }

    @AfterEach
    public void tear_down() {
        verify(snsClient, atLeast(0)).serviceName();
        verifyNoMoreInteractions(snsClient);
    }

    private void buildObjects() {

        model = ResourceModel.builder().subscriptionArn("topicArn:testArn").topicArn("topicArn").build();
        attributes = new HashMap<>();
        attributes.put("SubscriptionArn", model.getSubscriptionArn());
        attributes.put("TopicArn", "topicArn");
        attributes.put("Protocol", "email");
        attributes.put("Endpoint", "end1");
        attributes.put("RawMessageDelivery", "false");
        attributes.put("PendingConfirmation", "false");
    }

    private HashMap<String, String> buildObjects_PendingTrue() {

        model = ResourceModel.builder().subscriptionArn("topicArn:testArn").topicArn("topicArn").build();
        HashMap<String, String> attributes = new HashMap<>();
        attributes.put("SubscriptionArn", model.getSubscriptionArn());
        attributes.put("TopicArn", "topicArn");
        attributes.put("Protocol", "email");
        attributes.put("Endpoint", "end1");
        attributes.put("RawMessageDelivery", "false");
        attributes.put("PendingConfirmation", "true");

        return attributes;
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final CallbackContext context = new CallbackContext();
        context.setPropagationDelay(true);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final UnsubscribeResponse unsubscribeResponse = UnsubscribeResponse.builder().build();
        when(proxyClient.client().unsubscribe(any(UnsubscribeRequest.class))).thenReturn(unsubscribeResponse);

        final GetSubscriptionAttributesResponse getSubscriptionResponse = GetSubscriptionAttributesResponse.builder().attributes(attributes).build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenReturn(getSubscriptionResponse).thenReturn(getSubscriptionResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, context, proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).unsubscribe(any(UnsubscribeRequest.class));
        verify(proxyClient.client(), times(1)).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));
    }

    @Test
    public void handleRequest_SubscriptionPending() {
        final CallbackContext context = new CallbackContext();
        context.setPropagationDelay(true);

        final HashMap<String, String> attributes = buildObjects_PendingTrue();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final GetSubscriptionAttributesResponse getSubscriptionResponse = GetSubscriptionAttributesResponse.builder().attributes(attributes).build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenReturn(getSubscriptionResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, context, proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getMessage()).isEqualTo(String.format("Invalid Arn \"%s\". Please verify that the subscription is confirmed before trying to update attributes", model.getSubscriptionArn()));
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client(), times(0)).unsubscribe(any(UnsubscribeRequest.class));
        verify(proxyClient.client(), times(1)).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));
    }

    @Test
    public void handleRequest_SubscriptionArnDoesNotExist()  {
        model.setSubscriptionArn(null);
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode().toString()).isEqualTo(INVALID_REQUEST.toString());
        verify(proxyClient.client(), never()).unsubscribe(any(UnsubscribeRequest.class));
        verify(proxyClient.client(), never()).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));
    }

    @Test
    public void handleRequest_SubscriptionDoesNotExist()  {
        AwsServiceException exception = AwsServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(BaseHandlerStd.NOT_FOUND).build())
                .build();
        when(proxyClient.client().getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class))).thenThrow(exception);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode().toString()).isEqualTo(NOT_FOUND.toString());
        verify(proxyClient.client(), never()).unsubscribe(any(UnsubscribeRequest.class));
        verify(proxyClient.client()).getSubscriptionAttributes(any(GetSubscriptionAttributesRequest.class));
    }
}
