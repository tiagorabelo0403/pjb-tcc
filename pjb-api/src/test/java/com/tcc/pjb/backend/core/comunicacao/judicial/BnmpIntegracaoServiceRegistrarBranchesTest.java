package com.tcc.pjb.backend.core.comunicacao.judicial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerRepository;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.HsmTestFactory;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.PjbHardwareSecurityModule;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.PjbHsmProperties;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import com.tcc.pjb.backend.core.guard.MockGuardEnvironmentQuery;
import com.tcc.pjb.backend.core.guard.MockGuardProfile;
import com.tcc.pjb.backend.core.guard.MockGuardViolationException;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.platform.jusos.v2.notificacao.NotificacaoInteligentePJB;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionDescriptor;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BnmpIntegracaoServiceRegistrarBranchesTest {

    private BnmpApiGateway bnmpApiGateway;
    private PjbExecutionOrchestrator executionOrchestrator;
    private ComunicacaoJudicialStateStore stateStore;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        bnmpApiGateway = mock(BnmpApiGateway.class);
        executionOrchestrator = mock(PjbExecutionOrchestrator.class);
        stateStore = mock(ComunicacaoJudicialStateStore.class);

        when(executionOrchestrator.supply(any(PjbExecutionDescriptor.class), any()))
                .thenAnswer(inv -> {
                    Supplier<?> supplier = inv.getArgument(1);
                    return CompletableFuture.completedFuture(supplier.get());
                });
    }

    private BnmpIntegracaoService buildService(PjbBnmpProperties bnmpProps,
                                                MockGuardEnvironmentQuery query) {
        PjbHardwareSecurityModule hsm = HsmTestFactory.forTest(new PjbHsmProperties(
                false, true, null, null, null, null, null, null, null, null, 0, 0, false));
        return new BnmpIntegracaoService(
                bnmpApiGateway, hsm,
                mock(ProcessoRepository.class),
                new AuditLedgerService(mock(AuditLedgerRepository.class), mock(CurrentUserService.class), new SimpleMeterRegistry()),
                mock(NotificacaoInteligentePJB.class),
                stateStore,
                executionOrchestrator,
                bnmpProps,
                query,
                event -> {}
        );
    }

    private static BnmpIntegracaoService.MandadoPrisaoPjb mandado() {
        return new BnmpIntegracaoService.MandadoPrisaoPjb(
                "uuid-mandado-1", 1L, "0001234-56.2024.8.26.0100",
                BnmpIntegracaoService.TipoMandadoPrisao.PREVENTIVA,
                "Nome Preso", "12345678901", "RG-12345",
                "2024-01-01", "Juiz Teste", "TJSP", "SP",
                "Art. 312 CPP", false,
                "hash-integridade-test"
        );
    }

    @Test
    void caminho1_enabledTrueMockFalse_chamaApiReal() throws Exception {
        when(bnmpApiGateway.registrar(anyString())).thenReturn("BNMP-REAL-00001");
        when(stateStore.find(any(), any(), any())).thenReturn(java.util.Optional.empty());

        MockGuardEnvironmentQuery query = mock(MockGuardEnvironmentQuery.class);
        when(query.isRealEnvironment()).thenReturn(false);
        when(query.activeGuardProfile()).thenReturn(MockGuardProfile.DEV);

        BnmpIntegracaoService service = buildService(new PjbBnmpProperties(true, false, 15), query);
        BnmpIntegracaoService.RegistroBnmp registro = service.registrar(mandado());

        assertThat(registro.numeroBnmp()).isEqualTo("BNMP-REAL-00001");
        assertThat(registro.status()).isEqualTo(BnmpIntegracaoService.StatusMandadoBnmp.REGISTRADO_BNMP);
    }

    @Test
    void caminho2_enabledFalseMockTrueEnvDev_retornaNumeroBnmpMock() {
        when(stateStore.find(any(), any(), any())).thenReturn(java.util.Optional.empty());

        MockGuardEnvironmentQuery query = mock(MockGuardEnvironmentQuery.class);
        when(query.isRealEnvironment()).thenReturn(false);
        when(query.activeGuardProfile()).thenReturn(MockGuardProfile.DEV);

        BnmpIntegracaoService service = buildService(new PjbBnmpProperties(false, true, 15), query);
        BnmpIntegracaoService.RegistroBnmp registro = service.registrar(mandado());

        assertThat(registro.numeroBnmp()).startsWith("BNMP-MOCK-");
        assertThat(registro.status()).isEqualTo(BnmpIntegracaoService.StatusMandadoBnmp.REGISTRADO_BNMP);
    }

    @Test
    void caminho3_enabledFalseMockTrueEnvProd_lancaMockGuardViolationException() {
        MockGuardEnvironmentQuery query = mock(MockGuardEnvironmentQuery.class);
        when(query.isRealEnvironment()).thenReturn(true);
        when(query.activeGuardProfile()).thenReturn(MockGuardProfile.PROD);

        BnmpIntegracaoService service = buildService(new PjbBnmpProperties(false, true, 15), query);

        assertThatThrownBy(() -> service.registrar(mandado()))
                .isInstanceOf(MockGuardViolationException.class)
                .hasMessageContaining("pjb.bnmp.mock-enabled");
    }

    @Test
    void caminho4_enabledFalseMockFalse_retornaErroRegistro() {
        when(stateStore.find(any(), any(), any())).thenReturn(java.util.Optional.empty());

        MockGuardEnvironmentQuery query = mock(MockGuardEnvironmentQuery.class);
        when(query.isRealEnvironment()).thenReturn(false);
        when(query.activeGuardProfile()).thenReturn(MockGuardProfile.DEV);

        BnmpIntegracaoService service = buildService(new PjbBnmpProperties(false, false, 15), query);
        BnmpIntegracaoService.RegistroBnmp registro = service.registrar(mandado());

        assertThat(registro.status()).isEqualTo(BnmpIntegracaoService.StatusMandadoBnmp.ERRO_REGISTRO);
        assertThat(registro.numeroBnmp()).isBlank();
    }
}
