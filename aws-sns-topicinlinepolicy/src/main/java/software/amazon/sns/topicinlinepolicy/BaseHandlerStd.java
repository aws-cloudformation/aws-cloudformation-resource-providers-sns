package software.amazon.sns.topicinlinepolicy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnInvalidCredentialsException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.awssdk.awscore.exception.AwsServiceException;


import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.regex.Pattern;

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

    private final SnsClient snsClient;

    public static final String EMPTY_POLICY_AND_TOPICARN_ERROR_MESSAGE = "Policy and TopicArn cannot be empty";
    public static final String DEFAULT_POLICY_ERROR_MESSAGE = "Cannot set policy to the default policy";
    public static final int STABILIZATION_DELAY_IN_SECONDS = 5;

    protected BaseHandlerStd() {
        this(ClientBuilder.getClient());
    }

    protected BaseHandlerStd(SnsClient snsClient) {
        this.snsClient = requireNonNull(snsClient);
    }

    private SnsClient getSnsClient() {
        return snsClient;
    }

    private final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern PRINCIPAL_NOT_FOUND_PATTERN = Pattern.compile("Invalid parameter: Policy Error: PrincipalNotFound");
    private static final int EVENTUAL_CONSISTENCY_DELAY_SECONDS = 5;

    @Override
    public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {
        return handleRequest(
                proxy,
                request,
                callbackContext != null ? callbackContext : new CallbackContext(),
                proxy.newProxy(this::getSnsClient),
                logger
        );
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<SnsClient> proxyClient,
            final Logger logger);

    protected ProgressEvent<ResourceModel, CallbackContext> handleError(
            final SnsRequest request,
            final Exception e,
            final ProxyClient<SnsClient> proxyClient,
            final ResourceModel resourceModel,
            final CallbackContext callbackContext) {

        BaseHandlerException ex = null;

        if (e instanceof NotFoundException) {
            ex = new CfnNotFoundException(e);
        } else if (e instanceof InternalErrorException) {
            ex = new CfnServiceInternalErrorException(e);
        } else if (e instanceof AuthorizationErrorException) {
            ex = new CfnAccessDeniedException(e);
        } else if (e instanceof InvalidSecurityException) {
            ex = new CfnInvalidCredentialsException(e);
        } else if (e instanceof ThrottledException) {
            ex = new CfnThrottlingException(e);
        } else if (e instanceof InvalidParameterException) {
            ex = new CfnInvalidRequestException(e);
            final String errorMessage = ((AwsServiceException) e).awsErrorDetails().errorMessage();
            if (PRINCIPAL_NOT_FOUND_PATTERN.matcher(errorMessage).matches() && callbackContext.getPrincipalRetryAttempts() > 0) {
                callbackContext.minusOneAttempts();
                return ProgressEvent.defaultInProgressHandler(callbackContext,
                            EVENTUAL_CONSISTENCY_DELAY_SECONDS, resourceModel);
            }
        } else {
            ex = new CfnGeneralServiceException(e);
        }
        return ProgressEvent.failed(resourceModel, callbackContext, ex.getErrorCode(), ex.getMessage());
    }


    /**
     * Invocation of getPolicyDocument returns the policy document .
     *
     * @param request {@link ResourceHandlerRequest<ResourceModel>}
     * @return Returns policy document
     */
    protected String getPolicyDocument(final ResourceHandlerRequest<ResourceModel> request) {
        try {
            return MAPPER.writeValueAsString(request.getDesiredResourceState().getPolicyDocument());
        } catch (JsonProcessingException e) {
            throw new CfnInvalidRequestException(e);
        }
    }

    protected boolean doesTopicPolicyExist(final ProxyClient<SnsClient> proxyClient,
                                           final ResourceHandlerRequest<ResourceModel> request,
                                           final ResourceModel model) {

        GetTopicAttributesRequest getGroupPolicyRequest = Translator.translateToGetRequest(model.getTopicArn());
        GetTopicAttributesResponse response = proxyClient.injectCredentialsAndInvokeV2(getGroupPolicyRequest, proxyClient.client()::getTopicAttributes);
        String Policy = response.attributes().get("Policy");
        Map<String, Object> existedPolicy = Translator.convertStringToObject(Policy);
        Map<String, Object> defaultPolicy = Translator.convertStringToObject(getDefaultPolicy(request, model.getTopicArn()));
        return !existedPolicy.equals(defaultPolicy);
    }

    protected boolean checkDefaultPolicy(final ResourceHandlerRequest<ResourceModel> request,
                                           final ResourceModel model) {
        Map<String, Object> curPolicy = model.getPolicyDocument();
        Map<String, Object> defaultPolicy = Translator.convertStringToObject(getDefaultPolicy(request, model.getTopicArn()));
        return (curPolicy.equals(defaultPolicy));
    }

    /*
     * Unfortunately, SNS requires a policy for a topic, so we must reset to the default policy for now. The original
     * default policy, created by the SNS service (not CloudFormation) when the topic was created, specified an
     * uppercase SNS: asin spite of the public AWS documentation always using a lowercase sns:. So this method uses SNS:
     * as the service does. The Ruby integration tests will fail if the code below uses sns: instead.
     */
    static String getDefaultPolicy(final ResourceHandlerRequest<ResourceModel> request, String topicArn) {
        String accountId = request.getAwsAccountId();
        StringBuilder sb = new StringBuilder()
                .append("{")
                .append("    \"Version\": \"2008-10-17\",")
                .append("    \"Id\": \"__default_policy_ID\",")
                .append("    \"Statement\": [")
                .append("      {")
                .append("        \"Effect\": \"Allow\",")
                .append("        \"Sid\": \"__default_statement_ID\",")
                .append("        \"Principal\": {")
                .append("          \"AWS\": \"*\"")
                .append("        },")
                .append("        \"Action\": [")
                .append("          \"SNS:GetTopicAttributes\",")
                .append("          \"SNS:SetTopicAttributes\",")
                .append("          \"SNS:AddPermission\",")
                .append("          \"SNS:RemovePermission\",")
                .append("          \"SNS:DeleteTopic\",")
                .append("          \"SNS:Subscribe\",")
                .append("          \"SNS:ListSubscriptionsByTopic\",")
                .append("          \"SNS:Publish\"")
                .append("        ],")
                .append("        \"Resource\": \"").append(topicArn).append("\",")
                .append("        \"Condition\": {")
                .append("          \"StringEquals\": {")
                .append("            \"AWS:SourceOwner\": \"").append(accountId).append("\"")
                .append("          }")
                .append("        }")
                .append("      }")
                .append("    ]")
                .append("}");
        return sb.toString();
    }

    protected String alreadyExistsErrorMessage(ResourceModel resourceModel) {
        return String.format("Topic: %s, Policy already exists", resourceModel.getTopicArn());
    }

    protected String noSuchPolicyErrorMessage(ResourceModel resourceModel) {
        return String.format("Topic: %s, Policy does not exist", resourceModel.getTopicArn());
    }
}
