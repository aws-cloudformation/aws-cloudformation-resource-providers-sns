package software.amazon.sns.topic;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.Topic;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

import java.util.Map;

public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<SnsClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();
        final Map<String, String> desiredResourceTags = request.getDesiredResourceTags();
        String nextToken = request.getNextToken();

        if (StringUtils.isNotEmpty(model.getId())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Id is a read-only property.");
        }

        if (StringUtils.isBlank(model.getTopicName())) {
            model.setTopicName(IdentifierUtils.generateResourceIdentifier(request.getLogicalResourceIdentifier(), request.getClientRequestToken(), TOPIC_NAME_MAX_LENGTH).toLowerCase());
        }

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress ->
                proxy.initiate("AWS-SNS-Topic::Create::PreExistanceCheck" + (nextToken == null ? "" : nextToken), proxyClient, model, callbackContext)
                    .translateToServiceRequest(model1 -> Translator.translateToListTopicRequest(nextToken))
                    .makeServiceCall((listTopicRequest, client) -> proxy.injectCredentialsAndInvokeV2(listTopicRequest, client.client()::listTopics))
                    .done((listTopicsRequest, listTopicsResponse, sdkProxyClient, resourceModel, context) -> {
                        for(Topic t : listTopicsResponse.topics()) {
                            if(checkIfTopicNameWithArn(model.getTopicName(), t.topicArn())) {
                                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                                        .status(OperationStatus.FAILED)
                                        .errorCode(HandlerErrorCode.AlreadyExists)
                                        .message(model.getTopicName() + " already exists")
                                        .build();
                            }
                        }
                        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                                .resourceModel(model)
                                .callbackContext(callbackContext)
                                .nextToken(listTopicsResponse.nextToken())
                                .status(OperationStatus.IN_PROGRESS)
                                .build();
                    })
            )
            .then(progress ->
                proxy.initiate("AWS-SNS-Topic::Create", proxyClient, model, callbackContext)
                    .translateToServiceRequest(model1 -> Translator.translateToCreateTopicRequest(model1, desiredResourceTags))
                    .makeServiceCall((createTopicRequest, client) -> proxy.injectCredentialsAndInvokeV2(createTopicRequest, client.client()::createTopic))
                    .done((createTopicRequest, createTopicResponse, client, resourceModel, context) -> {
                        model.setId(createTopicResponse.topicArn());
                        return ProgressEvent.progress(model, context);
                    })
            )
            .then(progress -> addSubscription(proxy, proxyClient, progress, model.getSubscription(), logger))
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private boolean checkIfTopicNameWithArn(String topicName, String topicArn) {
        String[] arnSplit = topicArn.split(":");
        String arnTopicName = arnSplit[arnSplit.length - 1];
        return topicName.equals(arnTopicName);
    }
}
