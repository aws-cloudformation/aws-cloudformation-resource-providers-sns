package software.amazon.sns.subscription;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;

public final class SnsSubscriptionUtils {
    public static Map<String,String> getAttributesForCreate(final ResourceModel subscription) throws JsonProcessingException {
        final Map<String,String> attributes = Maps.newHashMap();
        ObjectMapper mapper = new ObjectMapper();
    
        //convert object to json
        // putIfNotNull(attributes, SubscriptionAttribute.DeliveryPolicy, mapper.writeValueAsString(subscription.getDeliveryPolicy()));
        // putIfNotNull(attributes, SubscriptionAttribute.FilterPolicy,  mapper.writeValueAsString(subscription.getFilterPolicy()));
        // putIfNotNull(attributes, SubscriptionAttribute.RawMessageDelivery,  mapper.writeValueAsString(subscription.getRawMessageDelivery()));
        // putIfNotNull(attributes, SubscriptionAttribute.RedrivePolicy,  mapper.writeValueAsString(subscription.getRedrivePolicy()));
        return attributes;
    }

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
