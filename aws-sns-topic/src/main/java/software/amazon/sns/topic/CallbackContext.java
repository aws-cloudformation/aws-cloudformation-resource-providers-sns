package software.amazon.sns.topic;

import software.amazon.cloudformation.proxy.StdCallbackContext;

import java.util.List;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {
    private List<String> subscriptionArnToUnsubscribe;
}
