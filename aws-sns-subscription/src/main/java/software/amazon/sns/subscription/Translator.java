package software.amazon.sns.subscription;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sns.model.GetSubscriptionAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.SetSubscriptionAttributesRequest;
import software.amazon.awssdk.services.sns.model.SetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.cloudformation.resource.Serializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class Translator {

    static final String ATTRIBUTE_NAME_DELIVERY_POLICY = "DeliveryPolicy";
    static final String ATTRIBUTE_NAME_FILTER_POLICY = "FilterPolicy";
    static final String ATTRIBUTE_NAME_RAW_MESSAGE_DELIVERY = "RawMessageDelivery";
    static final String ATTRIBUTE_NAME_REDRIVE_POLICY = "RedrivePolicy";

    private Translator() {
    }

    static SubscribeRequest translateToCreateRequest(final ResourceModel model) throws JsonProcessingException {
        return SubscribeRequest.builder()
            .endpoint(model.getEndpoint())
            .protocol(model.getProtocol())
            .topicArn(model.getTopicArn())
            .attributes(translateAttributesForCreate(model))
            .returnSubscriptionArn(true)
            .build();
    }

    private static Map<String, String> translateAttributesForCreate(final ResourceModel model) throws JsonProcessingException {
        final Serializer serializer = new Serializer();
        final Map<String, String> attributes = new HashMap<>();
        attributes.put(ATTRIBUTE_NAME_DELIVERY_POLICY, serializer.serialize(model.getDeliveryPolicy()));
        attributes.put(ATTRIBUTE_NAME_FILTER_POLICY, serializer.serialize(model.getFilterPolicy()));
        attributes.put(ATTRIBUTE_NAME_RAW_MESSAGE_DELIVERY, serializer.serialize(model.getRawMessageDelivery()));
        attributes.put(ATTRIBUTE_NAME_REDRIVE_POLICY, serializer.serialize(model.getRedrivePolicy()));
        return attributes;
    }

    static GetSubscriptionAttributesRequest translateToReadRequest(final ResourceModel model) {
        return GetSubscriptionAttributesRequest.builder()
            .subscriptionArn(model.getArn())
            .build();
    }

    static List<SetSubscriptionAttributesRequest> translateToUpdateRequests(final ResourceModel desiredModel,
                                                                            final ResourceModel previousModel) {
        final List<SetSubscriptionAttributesRequest> requests = new ArrayList<>();

        if (!desiredModel.getDisplayName().equals(previousModel.getDisplayName())) {
            requests.add(SetTopicAttributesRequest.builder()
                .topicArn(desiredModel.getArn())
                .attributeName(ATTRIBUTE_NAME_DISPLAY_NAME)
                .attributeValue(desiredModel.getDisplayName())
                .build());
        }

        if (!desiredModel.getKmsMasterKeyId().equals(previousModel.getKmsMasterKeyId())) {
            requests.add(SetTopicAttributesRequest.builder()
                .topicArn(desiredModel.getArn())
                .attributeName(ATTRIBUTE_NAME_KMS_MASTER_KEY_ID)
                .attributeValue(desiredModel.getKmsMasterKeyId())
                .build());
        }

        return requests;
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
            .map(Collection::stream)
            .orElseGet(Stream::empty);
    }
}
