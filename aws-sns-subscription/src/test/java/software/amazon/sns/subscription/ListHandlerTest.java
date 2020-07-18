package software.amazon.sns.subscription;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.*;
import java.util.*;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @Mock
    private ProxyClient<SnsClient> proxyClient;

    private ListHandler handler;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
    }

    public void handleRequest_SimpleSuccess() {
        Subscription subscription1 = Subscription.builder().subscriptionArn("arn1").build();
        Subscription subscription2 = Subscription.builder().subscriptionArn("arn2").build();
        List<Subscription> listSubscriptions = new ArrayList<>();
        listSubscriptions.add(subscription1);
        listSubscriptions.add(subscription2);
        final ListSubscriptionsByTopicResponse listSubscriptionsByTopicResponse = ListSubscriptionsByTopicResponse.builder()
                                        .subscriptions(listSubscriptions)
                                        .build();
                
        when(proxyClient.client().listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class))).thenReturn(listSubscriptionsByTopicResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder().build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class));
    }

    // todo test with token
}
