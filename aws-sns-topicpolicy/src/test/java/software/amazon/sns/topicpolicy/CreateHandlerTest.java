package software.amazon.sns.topicpolicy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.SetTopicAttributesResponse;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    @BeforeEach
    public void setup() {
        handler = new CreateHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        snsClient = mock(SnsClient.class);
        proxyClient = MOCK_PROXY(proxy, snsClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {

        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic1");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic2");

        Map<String, Object> policyDocument = getSNSPolicy();

        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id")
                .topics(topics)
                .policyDocument(policyDocument)
                .build();

        final SetTopicAttributesResponse setTopicAttributesResponse = SetTopicAttributesResponse.builder().build();
        when(proxyClient.client().setTopicAttributes(any(SetTopicAttributesRequest.class)))
                .thenReturn(setTopicAttributesResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);


        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_SimpleSuccess_WithStringPolicy() {

        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic1");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic2");

        String policyDocument = policDocument("abcd");

        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id")
                .topics(topics)
                .policyDocument(policyDocument)
                .build();

        final SetTopicAttributesResponse setTopicAttributesResponse = SetTopicAttributesResponse.builder().build();
        when(proxyClient.client().setTopicAttributes(any(SetTopicAttributesRequest.class)))
                .thenReturn(setTopicAttributesResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);


        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Failure_NotFoundException() {

        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic110");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic220");

        Map<String, Object> policyDocument = getSNSPolicy();

        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-cfnnotfound")
                .topics(topics)
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
        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic110");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic220");
        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-InternalErrorException")
                .topics(topics)
                .policyDocument(null)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertThat(response).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(response.getMessage()).isEqualTo("Invalid parameter: Policy Error: null");
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
    }


    @Test
    public void handleRequest_Failure_Empty_Topics() {
        final List<String> topics = new ArrayList<>();
        Map<String, Object> policyDocument = getSNSPolicy();
        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-CfnInvalidRequestException")
                .topics(topics)
                .policyDocument(policyDocument)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertThat(response).isNotNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getMessage()).isEqualTo("Value of property Topics must be of type List of String");
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);

    }

    @Test
    public void handleRequest_Failure_Null_ResourceModel() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(null)
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertThat(response).isNotNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getMessage()).isEqualTo("Property PolicyDocument cannot be empty");
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);

    }

    @Test
    public void handleRequest_Failure_PolicyDocument_JsonProcessingException() {
        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic110");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic220");

        Map<String, Object> policyDocument = new HashMap<>();
        policyDocument.put(null, "");
        policyDocument.put("event", "[\"order_placed\"]");

        final ResourceModel model = ResourceModel.builder()
                .id("handleRequest_Failure_JsonProcessingException")
                .topics(topics)
                .policyDocument(policyDocument)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_Failure_InvalidParameterException() {

        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic110");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic220");

        Map<String, Object> policyDocument = getSNSPolicy();

        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-CfnInvalidRequestException")
                .topics(topics)
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

        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic110");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic220");

        Map<String, Object> policyDocument = getSNSPolicy();

        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-CfnInvalidRequestException")
                .topics(topics)
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

        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic110");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic220");

        Map<String, Object> policyDocument = getSNSPolicy();

        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-CfnInvalidRequestException")
                .topics(topics)
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

        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic110");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic220");

        Map<String, Object> policyDocument = getSNSPolicy();

        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-CfnInvalidRequestException")
                .topics(topics)
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

        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic110");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic220");

        Map<String, Object> policyDocument = getSNSPolicy();

        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-InternalErrorException")
                .topics(topics)
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

        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic110");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic220");

        Map<String, Object> policyDocument = getSNSPolicy();

        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-CfnAccessDeniedException")
                .topics(topics)
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

        final List<String> topics = new ArrayList<>();
        topics.add("ARN");

        Map<String, Object> policyDocument = getSNSPolicy();

        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-CfnInvalidCredentialsException")
                .topics(topics)
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

        final List<String> topics = new ArrayList<>();
        topics.add("ARN");

        Map<String, Object> policyDocument = getSNSPolicy();

        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-CfnThrottlingException")
                .topics(topics)
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

        final List<String> topics = new ArrayList<>();
        topics.add("ARN");

        Map<String, Object> policyDocument = getSNSPolicy();

        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-CfnGeneralServiceException")
                .topics(topics)
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
