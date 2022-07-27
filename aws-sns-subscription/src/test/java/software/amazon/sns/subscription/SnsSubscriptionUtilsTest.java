package software.amazon.sns.subscription;


import org.junit.jupiter.api.Test;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class SnsSubscriptionUtilsTest extends AbstractTestBase {


    @Test
    public void testConvertToJsonFromString() {
        Map<String, Object> result = SnsSubscriptionUtils.convertToJson("{\"hello\": \"world\"}");
        assertEquals("world", result.get("hello"));
    }

    @Test
    public void testConvertToJsonFromStringException() {
        assertThrows(CfnInvalidRequestException.class, () -> SnsSubscriptionUtils.convertToJson("{\"hello\": \"world"));
    }
}
