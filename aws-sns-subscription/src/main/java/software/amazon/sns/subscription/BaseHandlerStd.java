package software.amazon.sns.subscription;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidCredentialsException;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.GetSubscriptionAttributesResponse;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesResponse;
import software.amazon.awssdk.services.sns.model.InternalErrorException;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.InvalidSecurityException;
import software.amazon.awssdk.services.sns.model.NotFoundException;
import software.amazon.awssdk.services.sns.model.SubscriptionLimitExceededException;
import software.amazon.awssdk.services.sns.model.AuthorizationErrorException;
import software.amazon.awssdk.services.sns.model.FilterPolicyLimitExceededException;
import software.amazon.awssdk.services.sns.model.GetSubscriptionAttributesRequest;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.regions.Region;


public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {
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
      proxy.newProxy(() -> {return (request.getDesiredResourceState().getRegion() != null) ?
                           ClientBuilder.getClient(Region.of(request.getDesiredResourceState().getRegion())) :
                           ClientBuilder.getClient();}),
      logger
    );
  }

  protected ProgressEvent<ResourceModel, CallbackContext> checkTopicExists(
    AmazonWebServicesClientProxy proxy,
    ProxyClient<SnsClient> proxyClient,
    ResourceModel model,
    ProgressEvent<ResourceModel, CallbackContext> progress,
    Logger logger) {


    logger.log("checking if topic exists");
    return proxy.initiate("AWS-SNS-Subscription::CheckTopicExists", proxyClient, model, progress.getCallbackContext())
            .translateToServiceRequest(Translator::translateToCheckTopicRequest)
            .makeServiceCall(this::retrieveTopicAttributes)
            .progress();

  }

  protected ProgressEvent<ResourceModel, CallbackContext> checkSubscriptionExists(
    AmazonWebServicesClientProxy proxy,
    ProxyClient<SnsClient> proxyClient,
    ResourceModel model,
    ProgressEvent<ResourceModel, CallbackContext> progress,
    Logger logger) {

      logger.log(String.format("checking if subscription ARN %s exists ", model.getSubscriptionArn()));
      return proxy.initiate("AWS-SNS-Subscription::CheckSubscriptionExists", proxyClient, model, progress.getCallbackContext())
            .translateToServiceRequest(Translator::translateToReadRequest)
            .makeServiceCall(this::readSubscriptionAttributes)
            .progress();
  }

  protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
    final AmazonWebServicesClientProxy proxy,
    final ResourceHandlerRequest<ResourceModel> request,
    final CallbackContext callbackContext,
    final ProxyClient<SnsClient> proxyClient,
    final Logger logger);

  protected GetSubscriptionAttributesResponse readSubscriptionAttributes(GetSubscriptionAttributesRequest getSubscriptionAttributesRequest,
  final ProxyClient<SnsClient> proxyClient)  {

    final GetSubscriptionAttributesResponse getSubscriptionAttributesResponse;

    try {
        if (getSubscriptionAttributesRequest.subscriptionArn() == null) {
            throw new CfnNotFoundException(new Exception("Subscription is null"));
        }

        getSubscriptionAttributesResponse = proxyClient.injectCredentialsAndInvokeV2(getSubscriptionAttributesRequest, proxyClient.client()::getSubscriptionAttributes);
        if (!getSubscriptionAttributesResponse.hasAttributes()) {
            throw new CfnNotFoundException(new Exception(String.format("Subscription %s does not exist.", getSubscriptionAttributesRequest.subscriptionArn())));
        }
    } catch (final SubscriptionLimitExceededException e) {
        throw new CfnServiceLimitExceededException(e);
    } catch (final FilterPolicyLimitExceededException e) {
        throw new CfnServiceLimitExceededException(e);
    } catch (final InvalidParameterException e) {
        throw new CfnInvalidRequestException(e);
    } catch (final InternalErrorException e) {
        throw new CfnInternalFailureException(e);
    } catch (final NotFoundException e) {
        throw new CfnNotFoundException(e);
    } catch (final AuthorizationErrorException e) {
        throw new CfnAccessDeniedException(e);
    } catch (final InvalidSecurityException e) {
        throw new CfnInvalidCredentialsException(e);
    }

    return getSubscriptionAttributesResponse;
  }

  protected GetTopicAttributesResponse retrieveTopicAttributes(GetTopicAttributesRequest getTopicAttributesRequest,
  final ProxyClient<SnsClient> proxyClient)  {

    final GetTopicAttributesResponse getTopicAttributesResponse;

    try {
      getTopicAttributesResponse = proxyClient.injectCredentialsAndInvokeV2(getTopicAttributesRequest, proxyClient.client()::getTopicAttributes);

    } catch (final SubscriptionLimitExceededException e) {
        throw new CfnServiceLimitExceededException(e);
    } catch (final FilterPolicyLimitExceededException e) {
        throw new CfnServiceLimitExceededException(e);
    } catch (final InvalidParameterException e) {
        throw new CfnInvalidRequestException(e);
    } catch (final InternalErrorException e) {
        throw new CfnInternalFailureException(e);
    } catch (final NotFoundException e) {
        throw new CfnNotFoundException(e);
    } catch (final AuthorizationErrorException e) {
        throw new CfnAccessDeniedException(e);
    } catch (final InvalidSecurityException e) {
        throw new CfnInvalidCredentialsException(e);
    } catch (final Exception e) {
        throw new CfnInternalFailureException(e);
    }

    return getTopicAttributesResponse;
  }

  protected boolean checkSubscriptionNotPending(final String subscriptionArn, final ProxyClient<SnsClient> proxyClient, final Logger logger) {

    logger.log("Checking if a subscription is pending.");
    final GetSubscriptionAttributesRequest getSubscriptionAttributesRequest = GetSubscriptionAttributesRequest.builder()
                                                                .subscriptionArn(subscriptionArn)
                                                                .build();

    final GetSubscriptionAttributesResponse getSubscriptionAttributesResponse;
    getSubscriptionAttributesResponse = readSubscriptionAttributes(getSubscriptionAttributesRequest, proxyClient);

    if (getSubscriptionAttributesResponse.hasAttributes() &&
       getSubscriptionAttributesResponse.attributes().get(Definitions.pendingConfirmation).equals(Definitions.subscriptionNotPending))
        return true;

    return false;
  }

  protected boolean stabilizeSnsSubscription(
          final AwsRequest awsRequest,
          final AwsResponse awsResponse,
          final ProxyClient<SnsClient> proxyClient,
          final ResourceModel model,
          final CallbackContext callbackContext) {

          try {
            final GetSubscriptionAttributesRequest getSubscriptionAttributesRequest = GetSubscriptionAttributesRequest.builder()
                                                                                    .subscriptionArn(model.getSubscriptionArn())
                                                                                    .build();

            readSubscriptionAttributes(getSubscriptionAttributesRequest, proxyClient);
          } catch (CfnNotFoundException e) {
            return false;
          }

          return true;
  }
}
