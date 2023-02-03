package software.amazon.sns.topic;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.cloudformation.exceptions.*;
import software.amazon.cloudformation.proxy.*;
import software.amazon.cloudformation.resource.IdentifierUtils;

import java.util.Collections;
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
        logger.log("CreateHandler invoked in: "+ request.getStackId()+ " " +request.getLogicalResourceIdentifier());
        final ResourceModel model = request.getDesiredResourceState();
        final Map<String, String> tagsList = request.getDesiredResourceTags() != null? request.getDesiredResourceTags(): Collections.emptyMap();
        if (request.getSystemTags() != null)
            tagsList.putAll(request.getSystemTags());

        if (StringUtils.isBlank(model.getTopicName())) {
            String randomTopicName = IdentifierUtils.generateResourceIdentifier(request.getStackId(), request.getLogicalResourceIdentifier(), request.getClientRequestToken(), TOPIC_NAME_MAX_LENGTH);
            if (Boolean.TRUE.equals(model.getFifoTopic())) {
                randomTopicName = IdentifierUtils.generateResourceIdentifier(request.getStackId(), request.getLogicalResourceIdentifier(), request.getClientRequestToken(), TOPIC_NAME_MAX_LENGTH - FIFO_TOPIC_EXTENSION.length());
                randomTopicName += FIFO_TOPIC_EXTENSION;
            }
            model.setTopicName(randomTopicName);
        }

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> proxy.initiate("AWS-SNS-Topic::Create", proxyClient, model, callbackContext)
                        .translateToServiceRequest(Translator::translateToCreateTopicRequest)
                        .backoffDelay(BACKOFF_STRATEGY)
                        .makeServiceCall((createRequest, client) -> {
                            if (checkIfTopicAlreadyExist(request, proxyClient, model.getTopicName(), logger))
                                throw new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, model.getTopicName());
                            try {
                                CreateTopicResponse createTopicResponse = proxy.injectCredentialsAndInvokeV2(Translator.translateToCreateTopicRequest(model, tagsList), proxyClient.client()::createTopic);
                                model.setTopicArn(createTopicResponse.topicArn());
                                createSubscriptions(proxyClient, model.getSubscription(), model, logger);
                                return createTopicResponse;
                            } catch (AuthorizationErrorException e) {
                                throw new CfnAccessDeniedException(e);
                            } catch (SnsException e) {
                                throw new CfnGeneralServiceException(e);
                            }
                        })
                        .stabilize(this::stabilizeSubscriptions)
                        .progress()
                )
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}
