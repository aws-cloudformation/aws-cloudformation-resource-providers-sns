package software.amazon.sns.subscription;

import software.amazon.awssdk.services.sns.SnsClient;

public class ClientBuilder {
  public static SnsClient getClient() {
    return SnsClient.builder()
        .build();
  }
}
