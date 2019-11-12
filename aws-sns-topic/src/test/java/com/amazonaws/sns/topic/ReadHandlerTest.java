package com.amazonaws.sns.topic;

import com.amazonaws.cloudformation.exceptions.ResourceNotFoundException;
import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.OperationStatus;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesResponse;
import software.amazon.awssdk.services.sns.model.Topic;

import java.util.HashMap;
import java.util.Map;

import static com.amazonaws.sns.topic.Translator.ATTRIBUTE_NAME_DISPLAY_NAME;
import static com.amazonaws.sns.topic.Translator.ATTRIBUTE_NAME_KMS_MASTER_KEY_ID;
import static com.amazonaws.sns.topic.Translator.ATTRIBUTE_NAME_OWNER;
import static com.amazonaws.sns.topic.Translator.ATTRIBUTE_NAME_SUBSCRIPTIONS_CONFIRMED;
import static com.amazonaws.sns.topic.Translator.ATTRIBUTE_NAME_SUBSCRIPTIONS_DELETED;
import static com.amazonaws.sns.topic.Translator.ATTRIBUTE_NAME_SUBSCRIPTIONS_PENDING;
import static com.amazonaws.sns.topic.Translator.ATTRIBUTE_NAME_TOPIC_ARN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest {

    ReadHandler handler;
    final Map<String, String> attributes = new HashMap<>();
    final String TOPIC_ARN = "arn:aws:sns:us-east-1:123456789012:topic";

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        handler = new ReadHandler();

        attributes.put(ATTRIBUTE_NAME_DISPLAY_NAME, "DisplayName");
        attributes.put(ATTRIBUTE_NAME_KMS_MASTER_KEY_ID, "KmsMasterKeyId");
        attributes.put(ATTRIBUTE_NAME_OWNER, "Owner");
        attributes.put(ATTRIBUTE_NAME_SUBSCRIPTIONS_CONFIRMED, "1");
        attributes.put(ATTRIBUTE_NAME_SUBSCRIPTIONS_DELETED, "2");
        attributes.put(ATTRIBUTE_NAME_SUBSCRIPTIONS_PENDING, "3");
        attributes.put(ATTRIBUTE_NAME_TOPIC_ARN, TOPIC_ARN);
    }

    @Test
    public void handleRequest_Success() {
        final Topic topic = Topic.builder()
            .topicArn(TOPIC_ARN)
            .build();

        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
            .attributes(attributes)
            .build();

        doReturn(getTopicAttributesResponse)
            .when(proxy)
            .injectCredentialsAndInvokeV2(
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
            );

        final ResourceModel model = ResourceModel.builder()
            .arn(topic.topicArn())
            .build();

        final ResourceModel expectedModel = ResourceModel.builder()
            .arn(topic.topicArn())
            .topicName("topic")
            .displayName("DisplayName")
            .kmsMasterKeyId("KmsMasterKeyId")
            .owner("Owner")
            .subscriptionsConfirmed(1)
            .subscriptionsDeleted(2)
            .subscriptionsPending(3)
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_FailureNotFound_EmptyTopicResponse() {
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
            .build();

        doReturn(getTopicAttributesResponse)
            .when(proxy)
            .injectCredentialsAndInvokeV2(
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
            );

        final ResourceModel model = ResourceModel.builder()
            .arn(TOPIC_ARN)
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        assertThrows(ResourceNotFoundException.class,
            () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_FailureNotFound_WithException() {
        doThrow(software.amazon.awssdk.services.sns.model.NotFoundException.class)
            .when(proxy)
            .injectCredentialsAndInvokeV2(
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
            );

        final ResourceModel model = ResourceModel.builder()
            .arn(TOPIC_ARN)
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        assertThrows(ResourceNotFoundException.class,
            () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_FailureNotFound_NullTopicInput() {
        final ResourceModel model = ResourceModel.builder()
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        assertThrows(ResourceNotFoundException.class,
            () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_FailureNotFound_NullModel() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .build();

        assertThrows(ResourceNotFoundException.class,
            () -> handler.handleRequest(proxy, request, null, logger));
    }
}
