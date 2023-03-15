package software.amazon.sns.topic;

/**
 * Type of tracing mode in AWS service integration.
 */
public enum TracingMode {
    /**
     * Indicates that the current AWS service takes the user's trace context from upstream and passes it to downstream.
     */
    PASS_THROUGH("PassThrough"),

    /**
     * Indicates that the current AWS service creates vended segments representing itself by using user's trace context from
     * upstream, and pass vended segment's trace context to downstream.
     */
    ACTIVE("Active");

    private final String value;

    TracingMode(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
