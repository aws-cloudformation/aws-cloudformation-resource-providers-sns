package software.amazon.sns.subscription;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

public final class SnsSubscriptionUtils {

    public static Map<String,Object> convertToJson(String jsonString) {
        final ObjectMapper objectMapper = new ObjectMapper(); 
        Map<String, Object> attribute = null;

        if (jsonString != null) {
            try {
                attribute = objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException ex) {
                // TODO temp until fix provided
            }
        }

        return attribute;
    } 

    public static Map<String,String> getAttributesForCreate(final ResourceModel model) {
        final Map<String,String> attributeMap = Maps.newHashMap();
     
       final ObjectMapper objectMapper = new ObjectMapper(); 
 
        putIfNotNull(objectMapper, attributeMap, SubscriptionAttribute.DeliveryPolicy, model.getDeliveryPolicy());
        putIfNotNull(objectMapper, attributeMap, SubscriptionAttribute.FilterPolicy,  model.getFilterPolicy());
        putIfNotNull(objectMapper, attributeMap, SubscriptionAttribute.RawMessageDelivery, model.getRawMessageDelivery());
        putIfNotNull(objectMapper, attributeMap, SubscriptionAttribute.RedrivePolicy,  model.getRedrivePolicy());

        return attributeMap;
    }

    public static Map<String,String> getAttributesForCreate(final SubscriptionAttribute subscriptionAttribute, final Map<String,Object> policy) {
        final Map<String,String> attributeMap = Maps.newHashMap();


             
       final ObjectMapper objectMapper = new ObjectMapper(); 
 
        putIfNotNull(objectMapper, attributeMap, subscriptionAttribute, policy);
        // putIfNotNull(objectMapper, attributeMap, SubscriptionAttribute.FilterPolicy,  subscription.getFilterPolicy());
        // putIfNotNull(objectMapper, attributeMap, SubscriptionAttribute.RawMessageDelivery, subscription.getRawMessageDelivery());
        // putIfNotNull(objectMapper, attributeMap, SubscriptionAttribute.RedrivePolicy,  subscription.getRedrivePolicy());

        return attributeMap;
    }

    public static Map<String,String> getAttributesForCreate(final SubscriptionAttribute subscriptionAttribute, final Boolean value) {
        final Map<String,String> attributeMap = Maps.newHashMap();


             
       final ObjectMapper objectMapper = new ObjectMapper(); 
 
        putIfNotNull(objectMapper, attributeMap, subscriptionAttribute, value);
        // putIfNotNull(objectMapper, attributeMap, SubscriptionAttribute.FilterPolicy,  subscription.getFilterPolicy());
        // putIfNotNull(objectMapper, attributeMap, SubscriptionAttribute.RawMessageDelivery, subscription.getRawMessageDelivery());
        // putIfNotNull(objectMapper, attributeMap, SubscriptionAttribute.RedrivePolicy,  subscription.getRedrivePolicy());

        return attributeMap;
    }

    private static void putIfNotNull(final ObjectMapper objectMapper, final Map<String,String> attributeMap, final SubscriptionAttribute key, final Map<String,Object> objectMap) {
        if (objectMap != null) {
            System.out.println(objectMap.values());

            try {
                String val = objectMapper.writeValueAsString(objectMap);
                System.out.println("snsutils " + val);
                attributeMap.put(key.name(), val);
            } catch(JsonProcessingException ex) {
                    // TODO temp until fix provided
            }

        }
    }

    private static void putIfNotNull(final Map<String,String> map, final SubscriptionAttribute key, final String value) {
        if (value != null) map.put(key.name(), value);
    }

    private static void putIfNotNull(final ObjectMapper objectMapper, final Map<String,String> map, final SubscriptionAttribute key, final Boolean value) {
        if (value != null) map.put(key.name(), value.toString());
    }

    private static void putIfChanged(final Map<String,String> map, final SubscriptionAttribute key, final String previousValue, final String currentValue) {
        if (!StringUtils.equals(previousValue, currentValue)) {
            map.put(key.name(), currentValue);
        }
    }
    
}
