package com.amazonaws.sns.topic;

import com.amazonaws.cloudformation.exceptions.CfnAlreadyExistsException;
import com.amazonaws.cloudformation.exceptions.CfnNotFoundException;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesResponse;
import software.amazon.awssdk.services.sns.model.NotFoundException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest {

    private CreateHandler handler;
    final Map<String, String> attributes = new HashMap<>();
    final String TOPIC_ARN = "arn:aws:sns:us-east-1:123456789012:topic";

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        handler = new CreateHandler();

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

        final CreateTopicResponse createTopicResponse = CreateTopicResponse.builder().build();
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
            .attributes(attributes)
            .build();

        Mockito
            .doThrow(software.amazon.awssdk.services.sns.model.NotFoundException.builder().build())
            .doReturn(createTopicResponse, getTopicAttributesResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(
                    ArgumentMatchers.any(),
                    ArgumentMatchers.any()
                );

        final ResourceModel model = ResourceModel.builder()
            .topicName("topic")
            .displayName("DisplayName")
            .kmsMasterKeyId("KmsMasterKeyId")
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
            .awsAccountId("123456789012")
            .desiredResourceState(model)
            .region("us-east-1")
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
    public void handleRequest_AlreadyExists_Failure() {
        final GetTopicAttributesResponse getTopicAttributesResponse = GetTopicAttributesResponse.builder()
            .attributes(attributes)
            .build();

        Mockito
            .doReturn(getTopicAttributesResponse)
            .when(proxy)
            .injectCredentialsAndInvokeV2(
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
            );

        final ResourceModel model = ResourceModel.builder()
            .topicName("topic")
            .displayName("DisplayName")
            .kmsMasterKeyId("KmsMasterKeyId")
            .owner("Owner")
            .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .awsAccountId("123456789012")
            .desiredResourceState(model)
            .region("us-east-1")
            .build();

        assertThrows(CfnAlreadyExistsException.class,
            () -> handler.handleRequest(proxy, request, null, logger));
    }
}
