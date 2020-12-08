package software.amazon.sns.subscription;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;

import java.util.Map;

public final class SnsSubscriptionUtils {

    public static Map<String,Object> convertToJson(String jsonString) {
        final ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> attribute = null;

        if (jsonString != null) {
            try {
                attribute = objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                throw new CfnInvalidRequestException(e);
            }
        }

        return attribute;
    }

    private static String convertJsonObjectToString(final Map<String,Object> objectMap) {
        final ObjectMapper objectMapper = new ObjectMapper();
        String val = "";
        if (objectMap != null) {
            try {
                val = objectMapper.writeValueAsString(objectMap);

            } catch(JsonProcessingException e) {
                throw new CfnInvalidRequestException(e);
            }
        }
        return val;
    }

    public static Map<String,String> getAttributesForUpdate(final SubscriptionAttribute subscriptionAttribute, final Map<String, Object> previousPolicy, final Map<String, Object> desiredPolicy) {
        return getAttributesForUpdate(subscriptionAttribute, convertJsonObjectToString(previousPolicy), convertJsonObjectToString(desiredPolicy));
    }

    public static Map<String, String> getAttributesForUpdate(SubscriptionAttribute subscriptionAttribute, String previousValue, String desiredValue) {
        final Map<String,String> attributeMap = Maps.newHashMap();
        putIfChanged(attributeMap, subscriptionAttribute, previousValue , desiredValue);
        return attributeMap;
    }

    public static Map<String,String> getAttributesForCreate(final ResourceModel currentmodel) {
        final Map<String,String> attributeMap = Maps.newHashMap();

        putIfNotEmpty(attributeMap, SubscriptionAttribute.DeliveryPolicy, convertJsonObjectToString(currentmodel.getDeliveryPolicy()));
        putIfNotEmpty(attributeMap, SubscriptionAttribute.FilterPolicy,  convertJsonObjectToString(currentmodel.getFilterPolicy()));
        putIfNotEmpty(attributeMap, SubscriptionAttribute.RawMessageDelivery, currentmodel.getRawMessageDelivery() != null ? Boolean.toString(currentmodel.getRawMessageDelivery()) : "");
        putIfNotEmpty(attributeMap, SubscriptionAttribute.RedrivePolicy,  convertJsonObjectToString(currentmodel.getRedrivePolicy()));
        putIfNotEmpty(attributeMap, SubscriptionAttribute.SubscriptionRoleArn,  currentmodel.getSubscriptionRoleArn());

        return attributeMap;
    }

    private static void putIfNotEmpty(final Map<String,String> attributeMap, final SubscriptionAttribute key, final String val) {
        if (!StringUtils.isEmpty(val)) {
            attributeMap.put(key.name(), val);
        }
    }

    private static void putIfChanged(final Map<String,String> map, final SubscriptionAttribute key, final String previousValue, final String currentValue) {
        if (!StringUtils.equals(previousValue, currentValue)) {
            map.put(key.name(), currentValue);
        }
    }

}
