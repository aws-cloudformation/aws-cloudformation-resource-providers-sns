package software.amazon.sns.topic;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.cloudformation.exceptions.*;
import software.amazon.cloudformation.proxy.*;
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
        logger.log("CreateHandler invoked in: "+ request.getStackId()+ " " +request.getLogicalResourceIdentifier());
        final ResourceModel model = request.getDesiredResourceState();
        final Map<String, String> desiredResourceTags = request.getDesiredResourceTags();

        if (StringUtils.isNotEmpty(model.getTopicArn())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "TopicArn is a read-only property.");
        }

        if (StringUtils.isBlank(model.getTopicName())) {
            String randomTopicName = IdentifierUtils.generateResourceIdentifier(request.getStackId(), request.getLogicalResourceIdentifier(), request.getClientRequestToken(), TOPIC_NAME_MAX_LENGTH);
            if (Boolean.TRUE.equals(model.getFifoTopic())) {
                randomTopicName = IdentifierUtils.generateResourceIdentifier(request.getStackId(), request.getLogicalResourceIdentifier(), request.getClientRequestToken(), TOPIC_NAME_MAX_LENGTH - FIFO_TOPIC_EXTENSION.length());
                randomTopicName += FIFO_TOPIC_EXTENSION;
            }
            model.setTopicName(randomTopicName);
        }

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> checkForPreCreateResourceExistence(request, proxyClient, progress))
                .then(progress -> createTopicWithTags(proxy, model, desiredResourceTags, callbackContext, proxyClient))
                .then(progress -> addSubscription(proxy, proxyClient, progress, model.getSubscription(), logger, true))
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private ProgressEvent<ResourceModel, CallbackContext> createTopicWithTags(
            final AmazonWebServicesClientProxy proxy,
            final ResourceModel model,
            final Map<String, String> desiredResourceTags,
            final CallbackContext callbackContext,
            final ProxyClient<SnsClient> proxyClient
    ) {
        try {
            CreateTopicResponse createTopicResponse = proxy.injectCredentialsAndInvokeV2(Translator.translateToCreateTopicRequest(model, desiredResourceTags), proxyClient.client()::createTopic);
            model.setTopicArn(createTopicResponse.topicArn());
            return ProgressEvent.progress(model, callbackContext);
        } catch (AuthorizationErrorException e) {
            throw new CfnAccessDeniedException(e);
        } catch (SnsException e) {
            throw new CfnGeneralServiceException(e);
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> checkForPreCreateResourceExistence(
            final ResourceHandlerRequest<ResourceModel> request,
            final ProxyClient<SnsClient> proxyClient,
            final ProgressEvent<ResourceModel, CallbackContext> progressEvent) {
        final ResourceModel model = progressEvent.getResourceModel();
        final CallbackContext callbackContext = progressEvent.getCallbackContext();

        String awsPartition = request.getAwsPartition();
        String region = request.getRegion();
        String accountId = request.getAwsAccountId();

        logger.log("Parsing Request INFO - Aws Paritiion: " + awsPartition + " - Region: " + region + " - Valid Account Id: " + !StringUtils.isEmpty(accountId));

        try {
            proxyClient.injectCredentialsAndInvokeV2(Translator.translateToGetTopicAttributes(awsPartition, region, accountId, model.getTopicName()), proxyClient.client()::getTopicAttributes);
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .status(OperationStatus.FAILED)
                    .errorCode(HandlerErrorCode.AlreadyExists)
                    .message(model.getTopicName() + " already exists")
                    .build();
        } catch (NotFoundException e) {
            logger.log(model.getTopicName() + " does not exist; creating the resource.");
            return ProgressEvent.progress(model, callbackContext);
        } catch (AuthorizationErrorException e) {
            throw new CfnAccessDeniedException(e);
        } catch (InternalError e) {
            throw new CfnInternalFailureException(e);
        } catch (InvalidParameterException e) {
            throw new CfnInvalidRequestException(e);
        } catch (InvalidSecurityException e) {
            throw new CfnInvalidCredentialsException(e);
        } catch (Exception e) {
            throw new CfnInternalFailureException(e);
        }
    }
}
