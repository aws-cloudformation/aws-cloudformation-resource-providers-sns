package software.amazon.sns.topicinlinepolicy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static software.amazon.sns.topicinlinepolicy.BaseHandlerStd.EMPTY_TOPICARN_ERROR_MESSAGE;
import static software.amazon.sns.topicinlinepolicy.BaseHandlerStd.EMPTY_POLICY_AND_TOPICARN_ERROR_MESSAGE;
import static software.amazon.sns.topicinlinepolicy.BaseHandlerStd.DEFAULT_POLICY_ERROR_MESSAGE;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<SnsClient> proxyClient;

    @Mock
    SnsClient snsClient;
    DeleteHandler handler;

    private final Map<String, String> attributes = getTestMap();

    private final Map<String, String> attributesWithWrongPolicy = getDefaultTestMap();

    @BeforeEach
    public void setup() {
        handler = new DeleteHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        snsClient = mock(SnsClient.class);
        proxyClient = MOCK_PROXY(proxy, snsClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenReturn(GetTopicAttributesResponse.builder()
                        .attributes(attributes)
                        .build());

        final String topic = "arn:aws:sns:us-east-1:123456789:my-topic1";

        final ResourceModel model = ResourceModel.builder()
                .policyDocument(new HashMap<>())
                .topicArn(topic)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        final SetTopicAttributesResponse setTopicAttributesResponse = SetTopicAttributesResponse.builder().build();
        when(proxyClient.client().setTopicAttributes(any(SetTopicAttributesRequest.class)))
                .thenReturn(setTopicAttributesResponse);

        CallbackContext callbackContext = new CallbackContext();
        final ProgressEvent<ResourceModel, CallbackContext> responseInProgress = handler.handleRequest(proxy, request,
                callbackContext, proxyClient, logger);

        assertThat(responseInProgress).isNotNull();
        assertThat(responseInProgress.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(responseInProgress.getCallbackDelaySeconds()).isEqualTo(30);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                callbackContext, proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Failure_TopicDoesNotExist() {
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenReturn(GetTopicAttributesResponse.builder()
                        .attributes(attributesWithWrongPolicy)
                        .build());

        String topic = "arn:aws:sns:us-east-1:123456789012:sns-topic-name";

        Map<String, Object> policyDocument = getSNSPolicy();

        final ResourceModel model = ResourceModel.builder()
                .topicArn(topic)
                .policyDocument(policyDocument)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void handleRequest_Success_TopicDoesNotExist() {
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenReturn(GetTopicAttributesResponse.builder()
                        .attributes(attributesWithWrongPolicy)
                        .build());

        String topic = "arn:aws:sns:us-east-1:123456789012:sns-topic-name";

        Map<String, Object> policyDocument = getSNSPolicy();

        final ResourceModel model = ResourceModel.builder()
                .topicArn(topic)
                .policyDocument(policyDocument)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        CallbackContext callbackContext = new CallbackContext();
        callbackContext.setPropagationDelay(true);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                callbackContext, proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    }

    @Test
    public void handleRequest_Failure_NotFoundException() {

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenReturn(GetTopicAttributesResponse.builder()
                        .attributes(attributes)
                        .build());

        final String topic = "arn:aws:sns:us-east-1:123456789:my-topic1";

        final ResourceModel model = ResourceModel.builder()
                .policyDocument(new HashMap<>())
                .topicArn(topic)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().setTopicAttributes(any(SetTopicAttributesRequest.class)))
                .thenThrow(NotFoundException.class);
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
    }

    @Test
    public void handleRequest_Failure_InvalidParameterException() {

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenReturn(GetTopicAttributesResponse.builder()
                        .attributes(attributes)
                        .build());

        final String topic = "arn:aws:sns:us-east-1:123456789:my-topic1";

        final ResourceModel model = ResourceModel.builder()
                .policyDocument(new HashMap<>())
                .topicArn(topic)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().setTopicAttributes(any(SetTopicAttributesRequest.class)))
                .thenThrow(InvalidParameterException.class);
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
    }

    @Test
    public void handleRequest_Failure_Null_Empty_Topics() {
        final String topic = "";

        final ResourceModel model = ResourceModel.builder()
                .policyDocument(new HashMap<>())
                .topicArn(topic)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertThat(response).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(response.getMessage()).isEqualTo(EMPTY_TOPICARN_ERROR_MESSAGE);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
    }

    @Test
    public void handleRequest_Failure_InternalErrorException() {

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenReturn(GetTopicAttributesResponse.builder()
                        .attributes(attributes)
                        .build());

        final String topic = "arn:aws:sns:us-east-1:123456789:my-topic1";

        final ResourceModel model = ResourceModel.builder()
                .policyDocument(new HashMap<>())
                .topicArn(topic)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().setTopicAttributes(any(SetTopicAttributesRequest.class)))
                .thenThrow(InternalErrorException.class);
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceInternalError);
    }

    @Test
    public void handleRequest_Failure_AuthorizationErrorException() {

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenReturn(GetTopicAttributesResponse.builder()
                        .attributes(attributes)
                        .build());

        final String topic = "arn:aws:sns:us-east-1:123456789:my-topic1";

        final ResourceModel model = ResourceModel.builder()
                .policyDocument(new HashMap<>())
                .topicArn(topic)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().setTopicAttributes(any(SetTopicAttributesRequest.class)))
                .thenThrow(AuthorizationErrorException.class);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);

    }

    @Test
    public void handleRequest_Failure_InvalidSecurityException() {

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenReturn(GetTopicAttributesResponse.builder()
                        .attributes(attributes)
                        .build());

        final String topic = "arn:aws:sns:us-east-1:123456789:my-topic1";

        final ResourceModel model = ResourceModel.builder()
                .policyDocument(new HashMap<>())
                .topicArn(topic)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().setTopicAttributes(any(SetTopicAttributesRequest.class)))
                .thenThrow(InvalidSecurityException.class);
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidCredentials);
    }

    @Test
    public void handleRequest_Failure_ThrottledException() {

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenReturn(GetTopicAttributesResponse.builder()
                        .attributes(attributes)
                        .build());

        final String topic = "arn:aws:sns:us-east-1:123456789:my-topic1";

        final ResourceModel model = ResourceModel.builder()
                .policyDocument(new HashMap<>())
                .topicArn(topic)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().setTopicAttributes(any(SetTopicAttributesRequest.class)))
                .thenThrow(ThrottledException.class);
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.Throttling);
    }

    @Test
    public void handleRequest_Failure_Null_ResourceModel() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(null)
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertThat(response).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(response.getMessage()).isEqualTo(EMPTY_TOPICARN_ERROR_MESSAGE);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);

    }

    @Test
    public void handleRequest_Failure_GeneralException() {
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenReturn(GetTopicAttributesResponse.builder()
                        .attributes(attributes)
                        .build());

        final String topic = "arn:aws:sns:us-east-1:123456789:my-topic1";

        final ResourceModel model = ResourceModel.builder()
                .policyDocument(new HashMap<>())
                .topicArn(topic)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().setTopicAttributes(any(SetTopicAttributesRequest.class)))
                .thenThrow(ConcurrentAccessException.class);
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }
}
