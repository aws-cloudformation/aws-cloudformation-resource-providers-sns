package software.amazon.sns.subscription;

import com.google.common.collect.Lists;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.sns.*;
import software.amazon.awssdk.services.sns.model.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

  /**
   * Request to create a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static SubscribeRequest translateToCreateRequest(final ResourceModel model) {
    System.out.println("model " + model.getTopicArn());
    return SubscribeRequest.builder()
        .attributes(SnsSubscriptionUtils.getAttributesForCreate(model))
        .protocol(model.getProtocol())
        .topicArn(model.getTopicArn())
        .endpoint(model.getEndpoint())
        .returnSubscriptionArn(true)
        .build();
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  static GetSubscriptionAttributesRequest translateToReadRequest(final ResourceModel model) {
    System.out.println("translator " + model.getSubscriptionArn());
    return GetSubscriptionAttributesRequest.builder()
        .subscriptionArn(model.getSubscriptionArn())
        .build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param awsResponse the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final GetSubscriptionAttributesResponse getSubscriptionAttributesResponse) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L58-L73
    final Map<String, String> attributes = getSubscriptionAttributesResponse.attributes();
    //final ResourceModel resourceModel = new ResourceModel();

    attributes.entrySet().forEach(key -> System.out.println("val: " + attributes.get(key)));
    //anyway to prevent hard coding??
    return ResourceModel.builder().subscriptionArn(attributes.get("SubscriptionArn"))
                            .topicArn(attributes.get("TopicArn"))
                            .endpoint(attributes.get("Endpoint"))
                            .protocol(attributes.get("Protocol"))
                            .filterPolicy(attributes.get("FilterPolicy") != null ? SnsSubscriptionUtils.convertToJson(attributes.get("FilterPolicy")) : null)
                            .redrivePolicy(attributes.get("RedrivePolicy") != null ? SnsSubscriptionUtils.convertToJson(attributes.get("RedrivePolicy")) : null)
                            .deliveryPolicy(attributes.get("DeliveryPolicy") != null ? SnsSubscriptionUtils.convertToJson(attributes.get("DeliveryPolicy")) : null)
                            .rawMessageDelivery(attributes.get("RawMessageDelivery") != null ? Boolean.valueOf(attributes.get("RawMessageDelivery")) : null)
                            .build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static UnsubscribeRequest translateToDeleteRequest(final ResourceModel model) {

    return UnsubscribeRequest.builder()
        .subscriptionArn(model.getSubscriptionArn())
        .build();
  }

  /**
   * Request to update properties of a previously created resource
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  static SetSubscriptionAttributesRequest translateToFirstUpdateRequest(final ResourceModel model) {
    System.out.println("!!!!!!!!!!!!!!! translateToFirstUpdateRequest");

    Map<String, String> mapAttributes = SnsSubscriptionUtils.getAttributesForCreate(model);
    SetSubscriptionAttributesRequest.Builder builder = SetSubscriptionAttributesRequest.builder();
    builder.subscriptionArn(model.getSubscriptionArn())
           .attributeName(SubscriptionAttribute.FilterPolicy.name()).attributeValue(mapAttributes.get(SubscriptionAttribute.FilterPolicy.name()))
           .attributeName(SubscriptionAttribute.RedrivePolicy.name()).attributeValue(mapAttributes.get(SubscriptionAttribute.RedrivePolicy.name()))
           .attributeName(SubscriptionAttribute.DeliveryPolicy.name()).attributeValue(mapAttributes.get(SubscriptionAttribute.DeliveryPolicy.name()));

    if (model.getRawMessageDelivery() != null)                                      
        builder.attributeName(SubscriptionAttribute.RawMessageDelivery.name()).attributeValue(mapAttributes.get(SubscriptionAttribute.RawMessageDelivery.name()));
    
    return builder.build();
  }

  // static SetSubscriptionAttributesRequest translateToUpdateFilterPolicyRequest(final SubscriptionAttribute subscriptionAttribute, final ResourceModel model) {
  //   return translateToUpdateRequest(SubscriptionAttribute.FilterPolicy, model);
  // }

  static SetSubscriptionAttributesRequest translateToUpdateRequest(final SubscriptionAttribute subscriptionAttribute, final ResourceModel model,  final Map<String, Object> policy) {
    Map<String, String> mapAttributes = SnsSubscriptionUtils.getAttributesForCreate(subscriptionAttribute, policy);
    SetSubscriptionAttributesRequest setSubscriptionAttributesRequest = SetSubscriptionAttributesRequest.builder().subscriptionArn(model.getSubscriptionArn())
           .attributeName(subscriptionAttribute.name()).attributeValue(mapAttributes.get(subscriptionAttribute.name()))
           .build();

    return setSubscriptionAttributesRequest;
  }

  static SetSubscriptionAttributesRequest translateToUpdateRequest(final SubscriptionAttribute subscriptionAttribute, final ResourceModel model, final Boolean value) {
    Map<String, String> mapAttributes = SnsSubscriptionUtils.getAttributesForCreate(subscriptionAttribute, value);
    SetSubscriptionAttributesRequest setSubscriptionAttributesRequest = SetSubscriptionAttributesRequest.builder().subscriptionArn(model.getSubscriptionArn())
           .attributeName(subscriptionAttribute.name()).attributeValue(mapAttributes.get(subscriptionAttribute.name()))
           .build();

    return setSubscriptionAttributesRequest;
  }

  /**
   * Request to update some other properties that could not be provisioned through first update request
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  static AwsRequest translateToSecondUpdateRequest(final ResourceModel model) {
    final AwsRequest awsRequest = null;
    // TODO: construct a request
    return awsRequest;
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
            .map(Collection::stream)
            .orElseGet(Stream::empty);
  }
                  
  static List<ResourceModel>  translateFromListRequest(final ListSubscriptionsByTopicResponse listSubscriptionsByTopicResponse) {
    return streamOfOrEmpty(listSubscriptionsByTopicResponse.subscriptions()).map(subscription -> 
      ResourceModel.builder().subscriptionArn(subscription.subscriptionArn()).build())
      .collect(Collectors.toList());
   }

}
