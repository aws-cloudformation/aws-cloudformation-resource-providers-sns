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
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.regions.Region;

import java.util.Map;

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

  protected ProgressEvent<ResourceModel, CallbackContext> modifyPolicy(
    AmazonWebServicesClientProxy proxy,
    ProxyClient<SnsClient> proxyClient,
    Map<String, Object> desiredPolicy,
    ResourceModel model,
    SubscriptionAttribute subscriptionAttribute,
    Map<String, Object> previousPolicy,
    ProgressEvent<ResourceModel, CallbackContext> progress,
    Logger logger) {

    if (previousPolicy == null || desiredPolicy.equals(previousPolicy)) {
        return progress;
    }

    return proxy.initiate("AWS-Kinesis-Stream::"+subscriptionAttribute.name(), proxyClient, model, progress.getCallbackContext())
            .translateToServiceRequest((resouceModel) -> Translator.translateToUpdateRequest(subscriptionAttribute, resouceModel, previousPolicy, desiredPolicy))
            .makeServiceCall(this::updateSubscription)
            .stabilize(this::stabilizeSnsSubscription)
            .progress();
    
  }

  protected ProgressEvent<ResourceModel, CallbackContext> modifyRawMessageDelivery(
    AmazonWebServicesClientProxy proxy,
    ProxyClient<SnsClient> proxyClient,
    Boolean rawMessageDelivery,
    ResourceModel model,
    SubscriptionAttribute subscriptionAttribute,
    Boolean previousRawMessageDelivery,
    ProgressEvent<ResourceModel, CallbackContext> progress,
    Logger logger) {

    if (previousRawMessageDelivery == null || rawMessageDelivery.equals(previousRawMessageDelivery)) {
        return progress;
    }

    return proxy.initiate("AWS-Kinesis-Stream::RawMessageDelivery", proxyClient, model, progress.getCallbackContext())
            .translateToServiceRequest((resouceModel) -> Translator.translateToUpdateRequest(subscriptionAttribute, resouceModel, previousRawMessageDelivery, rawMessageDelivery))
            .makeServiceCall(this::updateSubscription)
            .stabilize(this::stabilizeSnsSubscription)
            .progress();
    
  }

  protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
    final AmazonWebServicesClientProxy proxy,
    final ResourceHandlerRequest<ResourceModel> request,
    final CallbackContext callbackContext,
    final ProxyClient<SnsClient> proxyClient,
    final Logger logger);

  protected boolean checkTopicExists(final String topicArn, final ProxyClient<SnsClient> proxyClient, final Logger logger) {

    logger.log("Checking topic exists");
    GetTopicAttributesRequest getTopicAttributesRequest = GetTopicAttributesRequest.builder()
                                                                .topicArn(topicArn)
                                                                .build();
    GetTopicAttributesResponse getTopicAttributesResponse = proxyClient.injectCredentialsAndInvokeV2(getTopicAttributesRequest, proxyClient.client()::getTopicAttributes);
    
    if (getTopicAttributesResponse.hasAttributes() && 
        getTopicAttributesResponse.attributes().get(Definitions.topicArn) != null)
        return true;

    return false;
  }

  protected boolean checkSubscriptionExists(final String subscriptionArn, final ProxyClient<SnsClient> proxyClient) {
    final GetSubscriptionAttributesRequest getSubscriptionAttributesRequest = GetSubscriptionAttributesRequest.builder()
                                                                .subscriptionArn(subscriptionArn)
                                                                .build();

    final GetSubscriptionAttributesResponse getSubscriptionAttributesResponse;
    getSubscriptionAttributesResponse = proxyClient.injectCredentialsAndInvokeV2(getSubscriptionAttributesRequest, proxyClient.client()::getSubscriptionAttributes);
 
    if (getSubscriptionAttributesResponse.hasAttributes())
        return true;

    return false;
  }

  protected boolean checkSubscriptionNotPending(final String subscriptionArn, final ProxyClient<SnsClient> proxyClient, final Logger logger) {

    logger.log("Checking if a subscription is pending.");
    final GetSubscriptionAttributesRequest getSubscriptionAttributesRequest = GetSubscriptionAttributesRequest.builder()
                                                                .subscriptionArn(subscriptionArn)
                                                                .build();

    final GetSubscriptionAttributesResponse getSubscriptionAttributesResponse;
    getSubscriptionAttributesResponse = proxyClient.injectCredentialsAndInvokeV2(getSubscriptionAttributesRequest, proxyClient.client()::getSubscriptionAttributes);
  
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
 
    return checkSubscriptionExists(model.getSubscriptionArn(), proxyClient);
  }

  private SetSubscriptionAttributesResponse updateSubscription(
      final SetSubscriptionAttributesRequest setSubscriptionAttributesRequest,
      final ProxyClient<SnsClient> proxyClient)  {


      SetSubscriptionAttributesResponse setSubscriptionAttributesResponse = null;
      try {
          setSubscriptionAttributesResponse = proxyClient.injectCredentialsAndInvokeV2(setSubscriptionAttributesRequest, proxyClient.client()::setSubscriptionAttributes);

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


      return setSubscriptionAttributesResponse;
  }
}
