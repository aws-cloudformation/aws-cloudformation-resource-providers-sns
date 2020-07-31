package software.amazon.sns.topicpolicy;

import lombok.Builder;
import lombok.Data;
import software.amazon.cloudformation.proxy.StdCallbackContext;

@Builder(toBuilder = true)
@Data
@lombok.EqualsAndHashCode(callSuper = true)

public class CallbackContext extends StdCallbackContext {
}
