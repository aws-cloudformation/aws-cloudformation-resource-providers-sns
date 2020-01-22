package com.amazonaws.sns.topic;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonDeserialize(builder = CallbackContext.CallbackContextBuilder.class)
public class CallbackContext {
    private boolean createStarted;
    private boolean createStabilized;
    private boolean deleteStarted;
    private boolean updateStarted;
    private boolean updateStabilized;
    private List<Subscription> subscriptionsToAdd;
    private List<String> subscriptionsToDelete;
    private String topicArn;

    @JsonPOJOBuilder(withPrefix = "")
    public static class CallbackContextBuilder {
    }
}
