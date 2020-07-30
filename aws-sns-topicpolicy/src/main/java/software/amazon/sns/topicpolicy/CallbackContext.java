package software.amazon.sns.topicpolicy;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.cloudformation.proxy.StdCallbackContext;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@lombok.EqualsAndHashCode(callSuper = true)

public class CallbackContext extends StdCallbackContext {

    private List<String> topics;
    private List<String> previousTopics;
    private String policyDocument;
}
