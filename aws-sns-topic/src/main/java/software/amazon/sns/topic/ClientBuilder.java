package software.amazon.sns.topic;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {
    public static SnsClient getClient() {
        return SnsClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();
    }
}
