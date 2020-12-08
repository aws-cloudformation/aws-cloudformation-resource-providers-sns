package software.amazon.sns.subscription;

import software.amazon.awssdk.services.sns.model.*;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Translator {

  static SubscribeRequest translateToCreateRequest(final ResourceModel model) {
    return SubscribeRequest.builder()
        .attributes(SnsSubscriptionUtils.getAttributesForCreate(model))
        .protocol(model.getProtocol())
        .topicArn(model.getTopicArn())
        .endpoint(model.getEndpoint())
        .returnSubscriptionArn(true)
        .build();
  }

  static GetSubscriptionAttributesRequest translateToReadRequest(final ResourceModel model) {
    return GetSubscriptionAttributesRequest.builder()
        .subscriptionArn(model.getSubscriptionArn())
        .build();
  }


  static ResourceModel translateFromReadResponse(final GetSubscriptionAttributesResponse getSubscriptionAttributesResponse) {
    final Map<String, String> attributes = getSubscriptionAttributesResponse.attributes();

    Boolean rawMessageDelivery = attributes.get(Definitions.rawMessageDelivery) != null ? Boolean.valueOf(attributes.get(Definitions.rawMessageDelivery)) : null;
    return ResourceModel.builder().subscriptionArn(attributes.get(Definitions.subscriptionArn))
                            .topicArn(attributes.get(Definitions.topicArn))
                            .endpoint(attributes.get(Definitions.endpoint))
                            .protocol(attributes.get(Definitions.protocol))
                            .filterPolicy(attributes.get(Definitions.filterPolicy) != null ? SnsSubscriptionUtils.convertToJson(attributes.get(Definitions.filterPolicy)) : null)
                            .redrivePolicy(attributes.get(Definitions.redrivePolicy) != null ? SnsSubscriptionUtils.convertToJson(attributes.get(Definitions.redrivePolicy)) : null)
                            .deliveryPolicy(attributes.get(Definitions.deliveryPolicy) != null ? SnsSubscriptionUtils.convertToJson(attributes.get(Definitions.deliveryPolicy)) : null)
                            .rawMessageDelivery(rawMessageDelivery)
                            .subscriptionRoleArn(attributes.get(Definitions.subscriptionRoleArn))
                            .build();
  }

  static UnsubscribeRequest translateToDeleteRequest(final ResourceModel model) {
    return UnsubscribeRequest.builder()
        .subscriptionArn(model.getSubscriptionArn())
        .build();
  }

  static GetTopicAttributesRequest translateToCheckTopicRequest(final ResourceModel model) {
    return GetTopicAttributesRequest.builder()
        .topicArn(model.getTopicArn())
        .build();
  }

  static SetSubscriptionAttributesRequest translateToUpdateRequest(final SubscriptionAttribute subscriptionAttribute, final ResourceModel currentModel, final Map<String, Object> previousPolicy, final Map<String, Object> desiredPolicy) {
    Map<String, String> mapAttributes = SnsSubscriptionUtils.getAttributesForUpdate(subscriptionAttribute, previousPolicy, desiredPolicy);
    SetSubscriptionAttributesRequest.Builder builder = SetSubscriptionAttributesRequest.builder().subscriptionArn(currentModel.getSubscriptionArn());
    mapAttributes.forEach((name, value) -> builder.attributeName(name).attributeValue(value));
    return builder.build();
  }

  static SetSubscriptionAttributesRequest translateToUpdateRequest(final SubscriptionAttribute subscriptionAttribute, final ResourceModel currentModel, final String previousValue, final String desiredValue) {
    Map<String, String> mapAttributes = SnsSubscriptionUtils.getAttributesForUpdate(subscriptionAttribute, previousValue, desiredValue);
    SetSubscriptionAttributesRequest.Builder builder = SetSubscriptionAttributesRequest.builder().subscriptionArn(currentModel.getSubscriptionArn());
    mapAttributes.forEach((name, value) -> builder.attributeName(name).attributeValue(value));
    return builder.build();
  }

  static SetSubscriptionAttributesRequest translateToUpdateRequest(final SubscriptionAttribute subscriptionAttribute, final ResourceModel currentModel, final Boolean previousValue, final Boolean desiredValue) {
    final String previousVal = Boolean.valueOf(String.valueOf(previousValue)).toString();
    final String currentVal = Boolean.valueOf(String.valueOf(desiredValue)).toString();

    return translateToUpdateRequest(subscriptionAttribute, currentModel, previousVal, currentVal);
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
            .map(Collection::stream)
            .orElseGet(Stream::empty);
  }


  static List<ResourceModel>  translateFromListRequest(final ListSubscriptionsByTopicResponse listSubscriptionsByTopicResponse) {
    return streamOfOrEmpty(listSubscriptionsByTopicResponse.subscriptions()).map(subscription ->
      ResourceModel.builder().protocol(subscription.protocol()).topicArn(subscription.topicArn()).subscriptionArn(subscription.subscriptionArn()).build())
      .collect(Collectors.toList());
   }

  static ListSubscriptionsByTopicRequest translateToListSubscriptionsByTopicRequest(final ResourceHandlerRequest<ResourceModel> request) {
      return ListSubscriptionsByTopicRequest.builder()
              .nextToken(request.getNextToken())
              .topicArn(request.getDesiredResourceState().getTopicArn())
              .build();
  }
}
