package software.amazon.sns.topic;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigurationTest {

    @Test
    public void testMergeDuplicateKeys() {
        final ResourceModel model = ResourceModel.builder()
                .tags(ImmutableList.of(new Tag("sameKey", "value1"), new Tag("sameKey", "value2")))
                .build();

        final Configuration configuration = new Configuration();

        final Map<String, String> tags = configuration.resourceDefinedTags(model);

        assertEquals(ImmutableMap.of("sameKey", "value2"), tags);
    }
}
