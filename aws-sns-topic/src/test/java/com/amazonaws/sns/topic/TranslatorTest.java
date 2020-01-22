package com.amazonaws.sns.topic;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TranslatorTest {

    @Test
    public void getSubscriptionArnsToDelete_nothingToRemove() {

        List<Subscription> desiredSubscriptions = new ArrayList<>();
        desiredSubscriptions.add(Subscription.builder().endpoint("1.1.1.1").protocol("http").build());
        desiredSubscriptions.add(Subscription.builder().endpoint("2.2.2.2").protocol("http").build());
        desiredSubscriptions.add(Subscription.builder().endpoint("3.3.3.3").protocol("http").build());
        desiredSubscriptions.add(Subscription.builder().endpoint("4.4.4.4").protocol("http").build());

        List<software.amazon.awssdk.services.sns.model.Subscription> previousSubscriptions = new ArrayList<>();
        previousSubscriptions.add(software.amazon.awssdk.services.sns.model.Subscription.builder()
            .endpoint("1.1.1.1").protocol("http").subscriptionArn("arn1").build());
        previousSubscriptions.add(software.amazon.awssdk.services.sns.model.Subscription.builder()
            .endpoint("3.3.3.3").protocol("http").subscriptionArn("arn2").build());

        ResourceModel model = ResourceModel.builder()
            .subscription(desiredSubscriptions)
            .build();

        List<String> arns = Translator.getSubscriptionArnsToDelete(model, previousSubscriptions);

        assertThat(arns).isEmpty();
    }

    @Test
    public void getSubscriptionArnsToDelete_removeSome() {

        List<Subscription> desiredSubscriptions = new ArrayList<>();
        desiredSubscriptions.add(Subscription.builder().endpoint("1.1.1.1").protocol("http").build());
        desiredSubscriptions.add(Subscription.builder().endpoint("2.2.2.2").protocol("http").build());
        desiredSubscriptions.add(Subscription.builder().endpoint("3.3.3.3").protocol("http").build());
        desiredSubscriptions.add(Subscription.builder().endpoint("4.4.4.4").protocol("http").build());

        List<software.amazon.awssdk.services.sns.model.Subscription> previousSubscriptions = new ArrayList<>();
        previousSubscriptions.add(software.amazon.awssdk.services.sns.model.Subscription.builder()
            .endpoint("1.1.1.1").protocol("http").subscriptionArn("arn1").build());
        previousSubscriptions.add(software.amazon.awssdk.services.sns.model.Subscription.builder()
            .endpoint("3.3.3.3").protocol("http").subscriptionArn("arn2").build());
        previousSubscriptions.add(software.amazon.awssdk.services.sns.model.Subscription.builder()
            .endpoint("5.5.5.5").protocol("http").subscriptionArn("arn3").build());
        previousSubscriptions.add(software.amazon.awssdk.services.sns.model.Subscription.builder()
            .endpoint("3.3.3.3").protocol("https").subscriptionArn("arn4").build());

        ResourceModel model = ResourceModel.builder()
            .subscription(desiredSubscriptions)
            .build();

        List<String> arns = Translator.getSubscriptionArnsToDelete(model, previousSubscriptions);

        assertThat(arns).hasSize(2);
        assertThat(arns.get(0)).isEqualTo("arn3");
        assertThat(arns.get(0)).isEqualTo("arn4");
    }
}
