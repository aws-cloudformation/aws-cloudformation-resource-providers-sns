package software.amazon.sns.topicinlinepolicy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.cloudformation.LambdaWrapper;
import java.time.Duration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetrySetting;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.EqualJitterBackoffStrategy;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientBuilder {

    private static SnsClient snsClient;

    private static final BackoffStrategy SNS_BACKOFF_THROTTLING_STRATEGY =
            EqualJitterBackoffStrategy.builder()
                    .baseDelay(Duration.ofMillis(2000)) //1st retry is ~2 sec
                    .maxBackoffTime(SdkDefaultRetrySetting.MAX_BACKOFF) //default is 20s
                    .build();

    private static final RetryPolicy SNS_RETRY_POLICY =
            RetryPolicy.builder()
                    .numRetries(4)
                    .retryCondition(RetryCondition.defaultRetryCondition())
                    .throttlingBackoffStrategy(SNS_BACKOFF_THROTTLING_STRATEGY)
                    .build();
    public static SnsClient getClient() {
        if (snsClient == null) {
            snsClient = SnsClient.builder()
                    .httpClient(LambdaWrapper.HTTP_CLIENT)
                    .overrideConfiguration(ClientOverrideConfiguration.builder()
                            .retryPolicy(SNS_RETRY_POLICY)
                            .build())
                    .build();
        }
        return snsClient;
    }

}
