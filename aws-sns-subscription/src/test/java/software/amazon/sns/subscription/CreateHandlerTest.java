package software.amazon.sns.subscription;

import java.time.Duration;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import java.util.Map;
// import com.fasterxml.jackson.core.JsonProcessingException;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<SnsClient> proxyClient;

    @Mock
    SnsClient SnsClient;

    private CreateHandler handler;

    @BeforeEach
    public void setup() {
        handler = new CreateHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        SnsClient = mock(SnsClient.class);
        proxyClient = MOCK_PROXY(proxy, SnsClient);
    }

    @AfterEach
    public void tear_down() {
        verify(SnsClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(SnsClient);
    }

    // @Test
    // public void handleRequest_SimpleSuccess() {
    //     final CreateHandler handler = new CreateHandler();

    //     final ResourceModel model = ResourceModel.builder()
    //                                 .protocol("email")
    //                           //      .endpoint("end1")
    //                                 .topicArn("topicArn")
    //                              //   .filterPolicy("{\"store\": [\"example_corp\"], \"event\": [\"order_placed\"]}")
    //                                 .build();

    //     final SubscribeResponse subscribeResponse = SubscribeResponse.builder().subscriptionArn("testarn").build();;
    //     when(proxyClient.client().subscribe(any(SubscribeRequest.class))).thenReturn(subscribeResponse);

    //     final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
    //                                                             .desiredResourceState(model)
    //                                                             .build();
        
    //     final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);



    //     assertThat(response).isNotNull();
 
    //     // in progress waiting for the user confirm subscription.
    //     assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    //     assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    //     assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
    //     assertThat(response.getResourceModels()).isNull();
    //     assertThat(response.getMessage()).isNull();
    //     assertThat(response.getErrorCode()).isNull();

    //     verify(proxyClient.client()).subscribe(any(SubscribeRequest.class));

    // }
}
