package software.amazon.sns.subscription;

import java.time.Duration;
import software.amazon.awssdk.services.sns.SnsClient;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<SnsClient> proxyClient;

    @Mock
    SnsClient snsClient;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        snsClient = mock(SnsClient.class);
        proxyClient = MOCK_PROXY(proxy, snsClient);
    }

    @AfterEach
    public void tear_down() {
        verify(snsClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(snsClient);
    }

    // @Test
    // public void handleRequest_SimpleSuccess() {
    //     final UpdateHandler handler = new UpdateHandler();

    //     final ResourceModel model = ResourceModel.builder().build();

    //     final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
    //         .desiredResourceState(model)
    //         .build();

    //     final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    //     assertThat(response).isNotNull();
    //     assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    //     assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    //     assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
    //     assertThat(response.getResourceModels()).isNull();
    //     assertThat(response.getMessage()).isNull();
    //     assertThat(response.getErrorCode()).isNull();
    // }
}
