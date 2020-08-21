package software.amazon.sns.subscription;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.cloudformation.LambdaWrapper;
import software.amazon.awssdk.regions.Region;

public class ClientBuilder {

  public static SnsClient getClient() {
    return SnsClient.builder()
    .httpClient(LambdaWrapper.HTTP_CLIENT)
    .build();
  }

  public static SnsClient getClient(Region region) {
    return SnsClient.builder()
    .httpClient(LambdaWrapper.HTTP_CLIENT)
    .region(region)
    .build();
  }

}
