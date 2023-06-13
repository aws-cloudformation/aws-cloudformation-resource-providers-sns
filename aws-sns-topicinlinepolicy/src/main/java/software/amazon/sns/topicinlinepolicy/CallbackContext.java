package software.amazon.sns.topicinlinepolicy;

import software.amazon.cloudformation.proxy.StdCallbackContext;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {
    private boolean ignoreNotFound = false;

    private boolean propagationDelay;
    private int principalRetryAttempts = 5;

    protected void minusOneAttempts(){
        principalRetryAttempts--;
    }
}
