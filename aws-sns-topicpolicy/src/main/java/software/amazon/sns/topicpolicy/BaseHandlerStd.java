package software.amazon.sns.topicpolicy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SnsRequest;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnInvalidCredentialsException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.cloudformation.exceptions.*;


import static java.util.Objects.requireNonNull;

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

  private final SnsClient snsClient;

  protected BaseHandlerStd() {
    this(ClientBuilder.getClient());
  }

  protected BaseHandlerStd(SnsClient snsClient) {
    this.snsClient = requireNonNull(snsClient);
  }

  private SnsClient getSnsClient() {
    return snsClient;
  }
  private  final ObjectMapper MAPPER = new ObjectMapper();

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
    } else if(e instanceof InvalidParameterException){
      ex = new CfnInvalidRequestException(e);
    } else if (e instanceof InternalErrorException) {
      ex = new CfnServiceInternalErrorException(e);
    } else if (e instanceof AuthorizationErrorException) {
      ex = new CfnAccessDeniedException(e);
    } else if (e instanceof InvalidSecurityException) {
      ex = new CfnInvalidCredentialsException(e);
    } else if (e instanceof ThrottledException){
      ex = new CfnThrottlingException(e);
    } else {
      ex = new CfnGeneralServiceException(e);
    }
    return ProgressEvent.failed(resourceModel, callbackContext, ex.getErrorCode(), ex.getMessage());
  }

  protected String getPolicyDocument(final ResourceHandlerRequest<ResourceModel> request) {
    try {
      return MAPPER.writeValueAsString(request.getDesiredResourceState().getPolicyDocument());
    } catch (JsonProcessingException e) {
      throw new CfnInvalidRequestException(e);
    }
  }


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
            .append("          \"SNS:Publish\",")
            .append("          \"SNS:Receive\"")
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

}
