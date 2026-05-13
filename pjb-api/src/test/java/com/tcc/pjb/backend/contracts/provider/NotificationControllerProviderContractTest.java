package com.tcc.pjb.backend.contracts.provider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.spring.spring6.PactVerificationSpring6Provider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.tcc.pjb.backend.controller.notification.IntimacaoMulticanalController;
import com.tcc.pjb.backend.controller.notification.NotificationPreferenceController;
import com.tcc.pjb.backend.model.dto.notification.IntimacaoMulticanalDispatchRequest;
import com.tcc.pjb.backend.model.dto.notification.IntimacaoMulticanalDispatchResponse;
import com.tcc.pjb.backend.model.dto.notification.NotificationPreferenceRequest;
import com.tcc.pjb.backend.model.dto.notification.NotificationPreferenceResponse;
import com.tcc.pjb.backend.service.notification.surface.NotificationPreferenceSurfaceFacadeService;
import com.tcc.pjb.backend.service.notification.surface.NotificationSurfaceFacadeService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@Provider("PjbNotificationProvider")
@PactFolder("src/test/resources/pacts/provider")
class NotificationControllerProviderContractTest {

    private final NotificationPreferenceSurfaceFacadeService preferenceFacade = mock(NotificationPreferenceSurfaceFacadeService.class);
    private final NotificationSurfaceFacadeService notificationFacade = mock(NotificationSurfaceFacadeService.class);
    private final NotificationPreferenceController preferenceController = new NotificationPreferenceController(preferenceFacade);
    private final IntimacaoMulticanalController multicanalController = new IntimacaoMulticanalController(notificationFacade);

    @BeforeEach
    void setUp(PactVerificationContext context) {
        reset(preferenceFacade, notificationFacade);
        PactProviderSpring6Support.configure(context, preferenceController, multicanalController);
    }

    @TestTemplate
    @ExtendWith(PactVerificationSpring6Provider.class)
    void verify(PactVerificationContext context, MockHttpServletRequestBuilder request) {
        PactProviderSpring6Support.applyJsonBody(context, request);
        context.verifyInteraction();
    }

    @State("notification preference can be loaded")
    void notificationPreferenceCanBeLoaded() {
        when(preferenceFacade.consultar(77L)).thenReturn(new NotificationPreferenceResponse(
                77L,
                true,
                true,
                false,
                false,
                false,
                true,
                false,
                30,
                "https://push.pjb.test/device/77",
                null,
                null,
                null
        ));
    }

    @State("notification preference can be updated")
    void notificationPreferenceCanBeUpdated() {
        when(preferenceFacade.salvar(any(Long.class), any(NotificationPreferenceRequest.class))).thenReturn(new NotificationPreferenceResponse(
                77L,
                true,
                false,
                true,
                false,
                true,
                false,
                true,
                45,
                "https://push.pjb.test/device/77",
                "+5585999999999",
                null,
                "https://hooks.pjb.test/intimacoes"
        ));
    }

    @State("multichannel notification can be dispatched")
    void multichannelNotificationCanBeDispatched() {
        when(notificationFacade.dispatch(any(Long.class), any(Long.class), any(IntimacaoMulticanalDispatchRequest.class)))
                .thenReturn(new IntimacaoMulticanalDispatchResponse(
                        501L,
                        77L,
                        List.of("EMAIL"),
                        List.of(),
                        List.of(),
                        List.of("track-123"),
                        "ENTREGA_PARCIAL_OU_TOTAL",
                        Instant.parse("2026-04-16T15:30:00Z"),
                        List.of("Entrega formal registrada."),
                        Map.of("template", "INTIMACAO_FORMAL", "hashSha256", "abc123hash"),
                        Map.of("profile", "PAdES-LT"),
                        Map.of("ledger", "ok")
                ));
    }
}
