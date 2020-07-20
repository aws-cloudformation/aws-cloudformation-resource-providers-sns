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
import java.util.function.Supplier;
import com.amazonaws.regions.Regions;

// Placeholder for the functionality that could be shared across Create/Read/Update/Delete/List Handlers

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
      proxy.newProxy(() -> {return request.getRegion() != null ? 
                           ClientBuilder.getClient(Region.of(request.getRegion())) :  
                           ClientBuilder.getClient();}),
      logger
    );
  }

  // protected ProgressEvent<ResourceModel, CallbackContext> modifyFilterPolicy(
  //   AmazonWebServicesClientProxy proxy,
  //   ProxyClient<SnsClient> proxyClient,
  //   ResourceModel model,
  //   Map<String, String> previousFilterPolicy,
  //   ProgressEvent<ResourceModel, CallbackContext> progress,
  //   Logger logger) {

  //   Map<String, Object> desiredFilterPolicy = model.getFilterPolicy();
    
  //   if (previousFilterPolicy == null || desiredFilterPolicy.equals(previousFilterPolicy)) {
  //       return progress;
  //   }

  //   return proxy.initiate("AWS-Kinesis-Stream::UpdateFilterPolicy", proxyClient, model, progress.getCallbackContext())
  //           .translateToServiceRequest(Translator::translateToUpdateFilterPolicyRequest)
  //           .makeServiceCall(this::updateSubscription)
  //           .stabilize(this::stabilizeSnsSubscriptionUpdate)
  //           .progress();
    
  // }

  protected ProgressEvent<ResourceModel, CallbackContext> modifyAttributes(
    AmazonWebServicesClientProxy proxy,
    ProxyClient<SnsClient> proxyClient,
    ResourceModel currentModel,
    ResourceModel previousModel,
    ProgressEvent<ResourceModel, CallbackContext> progress,
    Logger logger) {

    return proxy.initiate("AWS-Kinesis-Stream::Update", proxyClient, currentModel, progress.getCallbackContext())
            .translateToServiceRequest((resouceModel) -> Translator.translateToUpdateRequest(previousModel, currentModel))
            .makeServiceCall(this::updateSubscription)
            .stabilize(this::stabilizeSnsSubscriptionUpdate)
            .progress();
    
  }

  // protected ProgressEvent<ResourceModel, CallbackContext> modifyPolicy(
  //   AmazonWebServicesClientProxy proxy,
  //   ProxyClient<SnsClient> proxyClient,
  //   Map<String, Object> desiredPolicy,
  //   ResourceModel model,
  //   SubscriptionAttribute subscriptionAttribute,
  //   Map<String, Object> previousPolicy,
  //   ProgressEvent<ResourceModel, CallbackContext> progress,
  //   Logger logger) {

  //   if (previousPolicy == null || desiredPolicy.equals(previousPolicy)) {
  //       return progress;
  //   }

  //   return proxy.initiate("AWS-Kinesis-Stream::"+subscriptionAttribute.name(), proxyClient, model, progress.getCallbackContext())
  //           .translateToServiceRequest((resouceModel) -> Translator.translateToUpdateRequest(subscriptionAttribute, resouceModel, desiredPolicy))
  //           .makeServiceCall(this::updateSubscription)
  //           .stabilize(this::stabilizeSnsSubscriptionUpdate)
  //           .progress();
    
  // }

  // protected ProgressEvent<ResourceModel, CallbackContext> modifyRawMessageDelivery(
  //   AmazonWebServicesClientProxy proxy,
  //   ProxyClient<SnsClient> proxyClient,
  //   Boolean rawMessageDelivery,
  //   ResourceModel model,
  //   SubscriptionAttribute subscriptionAttribute,
  //   Boolean previousRawMessageDelivery,
  //   ProgressEvent<ResourceModel, CallbackContext> progress,
  //   Logger logger) {

  //   if (previousRawMessageDelivery == null || rawMessageDelivery.equals(previousRawMessageDelivery)) {
  //       return progress;
  //   }

  //   return proxy.initiate("AWS-Kinesis-Stream::RawMessageDelivery", proxyClient, model, progress.getCallbackContext())
  //           .translateToServiceRequest((resouceModel) -> Translator.translateToUpdateRequest(subscriptionAttribute, resouceModel, rawMessageDelivery))
  //           .makeServiceCall(this::updateSubscription)
  //           .stabilize(this::stabilizeSnsSubscriptionUpdate)
  //           .progress();
    
  // }

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
        getTopicAttributesResponse.attributes().get("TopicArn") != null)
        return true;

    return false;
  }

  protected boolean checkSubscriptionExists(final String subscriptionArn, final ProxyClient<SnsClient> proxyClient, final Logger logger) {

    logger.log("Checking subscription exists");
    final GetSubscriptionAttributesRequest getSubscriptionAttributesRequest = GetSubscriptionAttributesRequest.builder()
                                                                .subscriptionArn(subscriptionArn)
                                                                .build();

    final GetSubscriptionAttributesResponse getSubscriptionAttributesResponse;
    getSubscriptionAttributesResponse = proxyClient.injectCredentialsAndInvokeV2(getSubscriptionAttributesRequest, proxyClient.client()::getSubscriptionAttributes);
  
    if (getSubscriptionAttributesResponse.hasAttributes() && 
    getSubscriptionAttributesResponse.attributes().get("Protocol") != null)
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

  protected boolean stabilizeSnsSubscriptionUpdate(
    final AwsRequest awsRequest,
    final AwsResponse awsResponse,
    final ProxyClient<SnsClient> proxyClient,
    final ResourceModel model,
    final CallbackContext callbackContext) {

  // TODO!!!!
  
    GetSubscriptionAttributesResponse getSubscriptionAttributesResponse = 
         proxyClient.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(model), proxyClient.client()::getSubscriptionAttributes);

    return true;
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
      } 


      return setSubscriptionAttributesResponse;
  }
}
