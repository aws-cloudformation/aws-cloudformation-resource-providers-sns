package software.amazon.sns.topicpolicy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.cloudformation.LambdaWrapper;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientBuilder {
    public static SnsClient getClient() {
        return SnsClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();
    }

}
