package software.amazon.sns.topicpolicy;

import software.amazon.cloudformation.proxy.StdCallbackContext;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {
    private boolean propagationDelay = false;
    private int principalRetryAttempts = 5;

    protected void minusOneAttempts(){
        principalRetryAttempts--;
    }
}