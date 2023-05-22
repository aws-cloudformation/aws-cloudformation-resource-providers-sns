package software.amazon.sns.topicinlinepolicy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesResponse;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.awssdk.services.sns.model.NotFoundException;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.InternalErrorException;
import software.amazon.awssdk.services.sns.model.AuthorizationErrorException;
import software.amazon.awssdk.services.sns.model.InvalidSecurityException;
import software.amazon.awssdk.services.sns.model.ThrottledException;
import software.amazon.awssdk.services.sns.model.ConcurrentAccessException;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<SnsClient> proxyClient;

    @Mock
    SnsClient snsClient;

    private CreateHandler handler;

    private final Map<String, String> attributes = getDefaultTestMap();

    private final Map<String, String> attributesWithWrongPolicy = getTestMap();

    @BeforeEach
    public void setup() {
        handler = new CreateHandler();
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
        final ProgressEvent<ResourceModel, CallbackContext> responseInProgress = handler.handleRequest(proxy, request,
                callbackContext, proxyClient, logger);

        assertThat(responseInProgress).isNotNull();
        assertThat(responseInProgress.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(responseInProgress.getCallbackDelaySeconds()).isEqualTo(5);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                callbackContext, proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Failure_TopicExisted() {
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
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AlreadyExists);
    }

    @Test
    public void handleRequest_Success_TopicExisted() {
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

        String topic = "arn:aws:sns:us-east-1:123456789012:sns-topic-name";

        Map<String, Object> policyDocument = getSNSPolicy();

        final ResourceModel model = ResourceModel.builder()
                .topicArn(topic)
                .policyDocument(policyDocument)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().setTopicAttributes(any(SetTopicAttributesRequest.class)))
                        .thenThrow(NotFoundException.class);
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void handleRequest_Failure_Empty_policyDocument() {

        String topic = "arn:aws:sns:us-east-1:123456789012:sns-topic-name";

        final ResourceModel model = ResourceModel.builder()
                .topicArn(topic)
                .policyDocument(new HashMap<>())
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertThat(response).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(response.getMessage()).isEqualTo(EMPTY_POLICY_AND_TOPICARN_ERROR_MESSAGE);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
    }

    @Test
    public void handleRequest_Failure_Create_Default_policyDocument() {
        String topic = "arn:aws:sns:us-east-1:123456789012:sns-topic-name";

        Map<String, Object> policyDocument = convertStringToObject(policDocument(null, topic));

        final ResourceModel model = ResourceModel.builder()
                .topicArn(topic)
                .policyDocument(policyDocument)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        CallbackContext callbackContext = new CallbackContext();
        callbackContext.setPropagationDelay(true);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, callbackContext, proxyClient, logger);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(response.getMessage()).isEqualTo(DEFAULT_POLICY_ERROR_MESSAGE);
    }


    @Test
    public void handleRequest_Failure_Empty_Topics() {

        String topic = "";

        Map<String, Object> policyDocument = getSNSPolicy();

        final ResourceModel model = ResourceModel.builder()
                .topicArn(topic)
                .policyDocument(policyDocument)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertThat(response).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(response.getMessage()).isEqualTo(EMPTY_POLICY_AND_TOPICARN_ERROR_MESSAGE);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);

    }

    @Test
    public void handleRequest_Failure_Null_ResourceModel() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(null)
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertThat(response).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(response.getMessage()).isEqualTo(EMPTY_POLICY_AND_TOPICARN_ERROR_MESSAGE);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);

    }

    @Test
    public void handleRequest_Failure_PolicyDocument_JsonProcessingException() {
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenReturn(GetTopicAttributesResponse.builder()
                        .attributes(attributes)
                        .build());
        String topic = "arn:aws:sns:us-east-1:123456789012:sns-topic-name";

        Map<String, Object> policyDocument = new HashMap<>();
        policyDocument.put(null, "");
        policyDocument.put("event", "[\"order_placed\"]");

        final ResourceModel model = ResourceModel.builder()
                .topicArn(topic)
                .policyDocument(policyDocument)
                .build();


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_Failure_PolicyDocument_IOException() {
        Map<String, String> wrongAttributes = new HashMap<>();
        wrongAttributes.put("Policy", "ABCD");
        wrongAttributes.put("TopicArn", "arn:aws:sns:us-east-1:123456789012:sns-topic-name");
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenReturn(GetTopicAttributesResponse.builder()
                        .attributes(wrongAttributes)
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

        assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_Failure_InvalidParameterException() {
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenReturn(GetTopicAttributesResponse.builder()
                        .attributes(attributes)
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

        when(proxyClient.client().setTopicAttributes(any(SetTopicAttributesRequest.class)))
                        .thenThrow(InvalidParameterException.class);
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);

        verify(proxyClient.client(), times(1)).setTopicAttributes(any(SetTopicAttributesRequest.class));
    }

    @Test
    public void handleRequest_INPROCESS_InvalidParameterExceptionWithWrongPrincipal() {
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenReturn(GetTopicAttributesResponse.builder()
                        .attributes(attributes)
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

        InvalidParameterException exception = InvalidParameterException.builder()
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("InvalidParameter")
                        .errorMessage("Invalid parameter: Policy Error: PrincipalNotFound").build())
                .build();

        when(proxyClient.client().setTopicAttributes(any(SetTopicAttributesRequest.class)))
                .thenThrow(exception);
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Failure_FinalAttempt_InvalidParameterExceptionWithWrongPrincipal() {

        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenReturn(GetTopicAttributesResponse.builder()
                        .attributes(attributes)
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

        InvalidParameterException exception = InvalidParameterException.builder()
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("InvalidParameter")
                        .errorMessage("Invalid parameter: Policy Error: PrincipalNotFound").build())
                .build();

        when(proxyClient.client().setTopicAttributes(any(SetTopicAttributesRequest.class)))
                .thenThrow(exception);

        CallbackContext callbackContext = new CallbackContext();
        callbackContext.setPrincipalRetryAttempts(1);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                callbackContext, proxyClient, logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Failure_NoAttempt_InvalidParameterExceptionWithWrongPrincipal() {
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenReturn(GetTopicAttributesResponse.builder()
                        .attributes(attributes)
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

        InvalidParameterException exception = InvalidParameterException.builder()
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("InvalidParameter")
                        .errorMessage("Invalid parameter: Policy Error: PrincipalNotFound").build())
                .build();

        when(proxyClient.client().setTopicAttributes(any(SetTopicAttributesRequest.class)))
                .thenThrow(exception);

        CallbackContext callbackContext = new CallbackContext();
        callbackContext.setPrincipalRetryAttempts(0);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                callbackContext, proxyClient, logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    @Test
    public void handleRequest_Failure_InternalErrorException() {
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenReturn(GetTopicAttributesResponse.builder()
                        .attributes(attributes)
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

        String topic = "arn:aws:sns:us-east-1:123456789012:sns-topic-name";

        Map<String, Object> policyDocument = getSNSPolicy();

        final ResourceModel model = ResourceModel.builder()
                .topicArn(topic)
                .policyDocument(policyDocument)
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

        String topic = "arn:aws:sns:us-east-1:123456789012:sns-topic-name";

        Map<String, Object> policyDocument = getSNSPolicy();

        final ResourceModel model = ResourceModel.builder()
                .topicArn(topic)
                .policyDocument(policyDocument)
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

        String topic = "arn:aws:sns:us-east-1:123456789012:sns-topic-name";

        Map<String, Object> policyDocument = getSNSPolicy();

        final ResourceModel model = ResourceModel.builder()
                .topicArn(topic)
                .policyDocument(policyDocument)
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
    public void handleRequest_Failure_GeneralException() {
        when(proxyClient.client().getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenReturn(GetTopicAttributesResponse.builder()
                        .attributes(attributes)
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

        when(proxyClient.client().setTopicAttributes(any(SetTopicAttributesRequest.class)))
                .thenThrow(ConcurrentAccessException.class);
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }

}
