package software.amazon.sns.topicpolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.AuthorizationErrorException;
import software.amazon.awssdk.services.sns.model.InternalErrorException;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.InvalidSecurityException;
import software.amazon.awssdk.services.sns.model.NotFoundException;
import software.amazon.awssdk.services.sns.model.SetTopicAttributesRequest;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidCredentialsException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<SnsClient> proxyClient;

    @Mock
    SnsClient snsClient;
    DeleteHandler handler;

    @BeforeEach
    public void setup() {
        handler = new DeleteHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        snsClient = mock(SnsClient.class);
        proxyClient = MOCK_PROXY(proxy, snsClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {

        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic1");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic2");

        final ResourceModel model = ResourceModel.builder()
                .id("TempPrimaryIdentifier")
                .policyDocument(new HashMap<>())
                .topics(topics)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Failure_No_PrimaryIdentifier() {

        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic110");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic220");

        final ResourceModel model = ResourceModel.builder()
                .topics(topics)
                .policyDocument(new HashMap<>())
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, null, proxyClient, logger));
    }

    @Test
    public void handleRequest_Failure_NotFoundException() {

        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic110");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic220");

        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-cfnnotfound")
                .topics(topics)
                .policyDocument(new HashMap<>())
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client()
                .setTopicAttributes(any(SetTopicAttributesRequest.class)))
                        .thenThrow(NotFoundException.class);
        assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_Failure_InvalidParameterException() {

        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic110");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic220");

        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-CfnInvalidRequestException")
                .topics(topics)
                .policyDocument(new HashMap<>())
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client()
                .setTopicAttributes(any(SetTopicAttributesRequest.class)))
                        .thenThrow(InvalidParameterException.class);
        assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_Failure_NullOrEmpty() {

        final List<String> topics = new ArrayList<>();

        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-InternalErrorException")
                .topics(topics)
                .policyDocument(new HashMap<>())
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_Failure_Null_Empty_Topics() {
        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-CfnInvalidRequestException")
                .policyDocument(new HashMap<>())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();
        assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, null, proxyClient, logger));
    }

    @Test
    public void handleRequest_Failure_InternalErrorException() {

        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic110");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic220");

        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-InternalErrorException")
                .topics(topics)
                .policyDocument(new HashMap<>())
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client()
                .setTopicAttributes(any(SetTopicAttributesRequest.class)))
                .thenThrow(InternalErrorException.class);
        assertThrows(CfnServiceInternalErrorException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_Failure_AuthorizationErrorException() {

        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic110");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic220");

        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-CfnAccessDeniedException")
                .topics(topics)
                .policyDocument(new HashMap<>())
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client()
                .setTopicAttributes(any(SetTopicAttributesRequest.class)))
                        .thenThrow(AuthorizationErrorException.class);
        assertThrows(CfnAccessDeniedException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

    }

    @Test
    public void handleRequest_Failure_InvalidSecurityException() {

        final List<String> topics = new ArrayList<>();
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic110");
        topics.add("arn:aws:sns:us-east-1:123456789:my-topic220");

        final ResourceModel model = ResourceModel.builder()
                .id("aws-sns-topic-policy-id-CfnInvalidCredentialsException")
                .topics(topics)
                .policyDocument(new HashMap<>())
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel> builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client()
                .setTopicAttributes(any(SetTopicAttributesRequest.class)))
                        .thenThrow(InvalidSecurityException.class);
        assertThrows(CfnInvalidCredentialsException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

}
