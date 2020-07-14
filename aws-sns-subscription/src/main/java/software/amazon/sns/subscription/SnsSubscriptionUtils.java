package software.amazon.sns.subscription;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;

public final class SnsSubscriptionUtils {
    public static Map<String,String> getAttributesForCreate(final ResourceModel subscription) {
        final Map<String,String> attributes = Maps.newHashMap();
    
        // final Map<String, Object> policy = subscription.getFilterPolicy();
        // if (policy != null)
        //  System.out.println(policy.values());
      //  ObjectMapper objectMapper = new ObjectMapper(); 
        // try {
        //     // putIfNotNull(attributes, SubscriptionAttribute.DeliveryPolicy, subscription.getDeliveryPolicy());
        //     // putIfNotNull(attributes, SubscriptionAttribute.FilterPolicy,  subscription.getFilterPolicy());
        //     // putIfNotNull(attributes, SubscriptionAttribute.RawMessageDelivery, subscription.getRawMessageDelivery());
        //     // putIfNotNull(attributes, SubscriptionAttribute.RedrivePolicy,  subscription.getRedrivePolicy());
    
        // } catch(JsonProcessingException ex) {

        // }
         return attributes;
    }

    // private static void putIfNotNull(final Map<String,String> map, final Map<String,Object> objectMap) {
    //     if (objectMap != null) map.putAll(objectMap);
    // }

    private static void putIfNotNull(final Map<String,String> map, final SubscriptionAttribute key, final String value) {
        if (value != null) map.put(key.name(), value);
    }

    private static void putIfNotNull(final Map<String,String> map, final SubscriptionAttribute key, final Boolean value) {
        if (value != null) map.put(key.name(), value.toString());
    }

    private static void putIfChanged(final Map<String,String> map, final SubscriptionAttribute key, final String previousValue, final String currentValue) {
        if (!StringUtils.equals(previousValue, currentValue)) {
            map.put(key.name(), currentValue);
        }
    }
    
}
