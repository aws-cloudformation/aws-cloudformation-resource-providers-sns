package software.amazon.sns.subscription;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
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
            } catch (JsonProcessingException e) {
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

    public static Map<String,String> getAttributesForUpdate(final ResourceModel previousModel, final ResourceModel currentmodel) {
        final Map<String,String> attributeMap = Maps.newHashMap();
     
        final ObjectMapper objectMapper = new ObjectMapper(); 
 
        putIfChanged(attributeMap, SubscriptionAttribute.DeliveryPolicy, convertJsonObjectToString(previousModel.getDeliveryPolicy()), convertJsonObjectToString(currentmodel.getDeliveryPolicy()));
        putIfChanged(attributeMap, SubscriptionAttribute.FilterPolicy,  convertJsonObjectToString(previousModel.getFilterPolicy()), convertJsonObjectToString(currentmodel.getFilterPolicy()));
        putIfChanged(attributeMap, SubscriptionAttribute.RawMessageDelivery, previousModel.getRawMessageDelivery() != null ? Boolean.toString(previousModel.getRawMessageDelivery()) : "" ,currentmodel.getRawMessageDelivery() != null ? Boolean.toString(currentmodel.getRawMessageDelivery()) : "");
        putIfChanged(attributeMap, SubscriptionAttribute.RedrivePolicy,  convertJsonObjectToString(previousModel.getRedrivePolicy()), convertJsonObjectToString(currentmodel.getRedrivePolicy()));

        return attributeMap;
    }

    public static Map<String,String> getAttributesForCreate(final ResourceModel currentmodel) {
        final Map<String,String> attributeMap = Maps.newHashMap();
     
        putIfNotEmpty(attributeMap, SubscriptionAttribute.DeliveryPolicy, convertJsonObjectToString(currentmodel.getDeliveryPolicy()));
        putIfNotEmpty(attributeMap, SubscriptionAttribute.FilterPolicy,  convertJsonObjectToString(currentmodel.getFilterPolicy()));
        putIfNotEmpty(attributeMap, SubscriptionAttribute.RawMessageDelivery, currentmodel.getRawMessageDelivery() != null ? Boolean.toString(currentmodel.getRawMessageDelivery()) : "");
        putIfNotEmpty(attributeMap, SubscriptionAttribute.RedrivePolicy,  convertJsonObjectToString(currentmodel.getRedrivePolicy()));

        return attributeMap;
    }

    // public static Map<String,String> getAttributesForCreate(final SubscriptionAttribute subscriptionAttribute, final Map<String,Object> policy) {
    //     final Map<String,String> attributeMap = Maps.newHashMap();
             
    //     final ObjectMapper objectMapper = new ObjectMapper(); 
 
    //     putIfNotEmpty(objectMapper, attributeMap, subscriptionAttribute, policy);
    //     // putIfNotNull(objectMapper, attributeMap, SubscriptionAttribute.FilterPolicy,  subscription.getFilterPolicy());
    //     // putIfNotNull(objectMapper, attributeMap, SubscriptionAttribute.RawMessageDelivery, subscription.getRawMessageDelivery());
    //     // putIfNotNull(objectMapper, attributeMap, SubscriptionAttribute.RedrivePolicy,  subscription.getRedrivePolicy());

    //     return attributeMap;
    // }

    // public static Map<String,String> getAttributesForCreate(final SubscriptionAttribute subscriptionAttribute, final Boolean value) {
    //     final Map<String,String> attributeMap = Maps.newHashMap();      
    //     final ObjectMapper objectMapper = new ObjectMapper(); 
 
    //     putIfNotNull(objectMapper, attributeMap, subscriptionAttribute, value);
    //     // putIfNotNull(objectMapper, attributeMap, SubscriptionAttribute.FilterPolicy,  subscription.getFilterPolicy());
    //     // putIfNotNull(objectMapper, attributeMap, SubscriptionAttribute.RawMessageDelivery, subscription.getRawMessageDelivery());
    //     // putIfNotNull(objectMapper, attributeMap, SubscriptionAttribute.RedrivePolicy,  subscription.getRedrivePolicy());

    //     return attributeMap;
    // }

    private static void putIfNotEmpty(final Map<String,String> attributeMap, final SubscriptionAttribute key, final String val) {
        if (val.length()>0) 
            attributeMap.put(key.name(), val);
    }

    // private static void putIfNotNull(final Map<String,String> map, final SubscriptionAttribute key, final String value) {
    //     if (value != null) map.put(key.name(), value);
    // }

    // private static void putIfNotNull(final ObjectMapper objectMapper, final Map<String,String> map, final SubscriptionAttribute key, final Boolean value) {
    //     if (value != null) map.put(key.name(), value.toString());
    // }

    private static void putIfChanged(final Map<String,String> map, final SubscriptionAttribute key, final String previousValue, final String currentValue) {
        if (!StringUtils.equals(previousValue, currentValue)) {
            map.put(key.name(), currentValue);
        }
    }
    
}
