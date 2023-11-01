package software.amazon.sns.subscription;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.OperationStatus;
import static software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.GENERAL_SERVICE_EXCEPTION;
import static software.amazon.awssdk.services.cloudformation.model.HandlerErrorCode.INVALID_REQUEST;
import static software.amazon.sns.subscription.BaseHandlerStd.INTERNAL_ERROR;
import static software.amazon.sns.subscription.BaseHandlerStd.INVALID_PARAMETER;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<SnsClient> proxyClient;

    @Mock
    SnsClient SnsClient;

    private CreateHandler handler;
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
        verify(SnsClient, atLeast(0)).serviceName();
        verifyNoMoreInteractions(SnsClient);
    }

    private void buildObjects() throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();

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
    public void handleRequest_SimpleSuccess()  {
        final SubscribeResponse subscribeResponse = SubscribeResponse.builder().subscriptionArn("testarn").build();
        when(proxyClient.client().subscribe(any(SubscribeRequest.class))).thenReturn(subscribeResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).subscribe(any(SubscribeRequest.class));
    }

    @Test
    public void handleRequest_Error() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        AwsServiceException exceptionGeneral = AwsServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(GENERAL_SERVICE_EXCEPTION.toString()).build())
                .build();
        AwsServiceException exceptionInvalidRequest = AwsServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(INVALID_PARAMETER).build())
                .build();
        AwsServiceException exceptionUnauth = AwsServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(BaseHandlerStd.AUTHORIZATION_ERROR).build())
                .build();
        AwsServiceException exceptionInvalidSecurity = AwsServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(BaseHandlerStd.INVALID_SECURITY).build())
                .build();
        AwsServiceException exceptionInternalError = AwsServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(INTERNAL_ERROR).build())
                .build();
        AwsServiceException exceptionFilterPolicyLimit = AwsServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(BaseHandlerStd.FILTER_POLICY_LIMIT_EXCEEDED).build())
                .build();
        AwsServiceException exceptionSubsciptionLimit = AwsServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(BaseHandlerStd.SUBSCRIPTION_LIMIT_EXCEEDED).build())
                .build();
        AwsServiceException exceptionNotFound = AwsServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(BaseHandlerStd.NOT_FOUND).build())
                .build();

        List<Exception> exceptions = new ArrayList<>();
        exceptions.add(exceptionGeneral);
        exceptions.add(exceptionInvalidRequest);
        exceptions.add(exceptionUnauth);
        exceptions.add(exceptionInvalidSecurity);
        exceptions.add(exceptionInternalError);
        exceptions.add(exceptionFilterPolicyLimit);
        exceptions.add(exceptionSubsciptionLimit);
        exceptions.add(exceptionNotFound);

        for(Exception e : exceptions){
            lenient().when(proxyClient.client().subscribe(any(SubscribeRequest.class)))
                    .thenThrow(e);
            final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
            assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        }

        String ex = BaseHandlerStd.getErrorCode(new Exception("exception"));
        assertThat(ex).isEqualTo("exception");
        verify(proxyClient.client(), atLeast(8)).subscribe(any(SubscribeRequest.class));
    }

    @Test
    public void handleRequest_TopicArnDoesNotExist()  {
        model = ResourceModel.builder()
                .protocol("email")
                .endpoint("end1")
                .topicArn(null)
                .filterPolicy(null)
                .redrivePolicy(null)
                .deliveryPolicy(null)
                .rawMessageDelivery(null)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode().toString()).isEqualTo(INVALID_REQUEST.toString());
        verify(proxyClient.client(), never()).subscribe(any(SubscribeRequest.class));
    }

    @Test
    public void handleRequest_ProtocolDoesNotExist()  {
        model = ResourceModel.builder()
                .protocol(null)
                .endpoint("end1")
                .topicArn("topicArn")
                .filterPolicy(null)
                .redrivePolicy(null)
                .deliveryPolicy(null)
                .rawMessageDelivery(null)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode().toString()).isEqualTo(INVALID_REQUEST.toString());
        verify(proxyClient.client(), never()).subscribe(any(SubscribeRequest.class));
    }
}
