package com.tcc.pjb.backend.service.processual.malha.internal;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaSupportBridge;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoMalhaActorContext;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoMalhaSigiloContexto;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoMalhaViewLevel;
import com.tcc.pjb.backend.core.security.device.SecurityAlertService;
import com.tcc.pjb.backend.core.security.stepup.DecisionStepUpTokenPayload;
import com.tcc.pjb.backend.core.security.stepup.DecisionStepUpTokenService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

class ProcessoMalhaSigiloAuthorizationServiceTest {

    private final ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
    private final DecisionStepUpTokenService decisionStepUpTokenService = Mockito.mock(DecisionStepUpTokenService.class);
    private final SecurityAlertService securityAlertService = Mockito.mock(SecurityAlertService.class);
    private final ProcessoMalhaSupportBridge supportBridge = Mockito.mock(ProcessoMalhaSupportBridge.class);

    @Test
    void shouldGrantFullViewForPublicProcessWithoutEscalation() {
        Processo processo = processo(101L, NivelSigilo.PUBLICO);
        when(processoRepository.findContextoCompletoById(101L)).thenReturn(Optional.of(processo));
        when(supportBridge.possuiAuditLedger()).thenReturn(false);
        when(supportBridge.possuiDecisionTrace()).thenReturn(false);

        ProcessoMalhaSigiloAuthorizationService service = new ProcessoMalhaSigiloAuthorizationService(
                processoRepository,
                decisionStepUpTokenService,
                securityAlertService,
                supportBridge,
                provider(null)
        );

        ProcessoMalhaSigiloContexto contexto = service.avaliar(101L, actorElevado(), null, "REQ-1", null, "127.0.0.1");

        assertEquals(NivelSigilo.PUBLICO, contexto.nivelSigilo());
        assertEquals(ProcessoMalhaViewLevel.PLENO, contexto.viewLevel());
        assertFalse(contexto.stepUpExigido());
        assertFalse(contexto.stepUpAtivo());
        assertFalse(contexto.mascarado());
        verify(securityAlertService, never()).create(any(), any(), any(), any(), any(), any(Integer.class));
    }

    @Test
    void shouldRequireStepUpAndEmitAuditForSensitiveRestrictedRead() {
        Processo processo = processo(102L, NivelSigilo.SEGREDO_ESTADO);
        OutboxPublisher outboxPublisher = Mockito.mock(OutboxPublisher.class);
        AuditLedgerService auditLedgerService = Mockito.mock(AuditLedgerService.class);
        DecisionTraceService decisionTraceService = Mockito.mock(DecisionTraceService.class);
        when(processoRepository.findContextoCompletoById(102L)).thenReturn(Optional.of(processo));
        when(supportBridge.possuiAuditLedger()).thenReturn(true);
        when(supportBridge.auditLedgerService()).thenReturn(auditLedgerService);
        when(supportBridge.possuiDecisionTrace()).thenReturn(true);
        when(supportBridge.decisionTraceService()).thenReturn(decisionTraceService);

        ProcessoMalhaSigiloAuthorizationService service = new ProcessoMalhaSigiloAuthorizationService(
                processoRepository,
                decisionStepUpTokenService,
                securityAlertService,
                supportBridge,
                provider(outboxPublisher)
        );

        ProcessoMalhaSigiloContexto contexto = service.avaliar(102L, actorAdvogadoParte(), null, "REQ-2", null, "10.0.0.8");

        assertEquals(ProcessoMalhaViewLevel.RESTRITO, contexto.viewLevel());
        assertTrue(contexto.acessoSensivel());
        assertTrue(contexto.stepUpExigido());
        assertFalse(contexto.stepUpAtivo());
        assertTrue(contexto.mascarado());
        assertTrue(contexto.fundamentos().contains("sigilo.step_up_exigido=true"));
        verify(securityAlertService).create(eq(null), eq("MALHA_SIGILO_STEPUP"), eq("Tentativa de leitura sensível da malha"), any(), eq("10.0.0.8"), eq(72));
        verify(auditLedgerService).appendSafely(eq("MALHA_SIGILO_STEPUP_EXIGIDO"), eq("PROCESSO"), eq("102"), any(), any());
        verify(decisionTraceService).record(eq("MALHA_SIGILO_STEPUP_EXIGIDO"), eq("PROCESSO"), eq("102"), any(), any(), any(), any(), any(), eq("PJB_MALHA_SIGILO"), any());
        verify(outboxPublisher).enqueue(eq("processo.malha.sigilo"), eq("MALHA_SIGILO_STEPUP_EXIGIDO"), any(), any(), any(), eq("PROCESSO_MALHA_SIGILO"), eq("102"));
    }

    @Test
    void shouldAcceptValidTokenForElevatedSecretStateRead() {
        Processo processo = processo(103L, NivelSigilo.SEGREDO_ESTADO);
        when(processoRepository.findContextoCompletoById(103L)).thenReturn(Optional.of(processo));
        when(supportBridge.possuiAuditLedger()).thenReturn(false);
        when(supportBridge.possuiDecisionTrace()).thenReturn(false);
        when(decisionStepUpTokenService.verifyAndDecode("TOKEN-OK")).thenReturn(
                new DecisionStepUpTokenPayload("jti-1", 900L, 1L, Long.MAX_VALUE, "HIGH", "READ", 103L, 55L, null, null, null, null)
        );

        ProcessoMalhaSigiloAuthorizationService service = new ProcessoMalhaSigiloAuthorizationService(
                processoRepository,
                decisionStepUpTokenService,
                securityAlertService,
                supportBridge,
                provider(null)
        );

        ProcessoMalhaSigiloContexto contexto = service.avaliar(103L, actorElevado(), "TOKEN-OK", "REQ-3", null, "127.0.0.1");

        assertEquals(ProcessoMalhaViewLevel.PLENO, contexto.viewLevel());
        assertTrue(contexto.stepUpAtivo());
        assertFalse(contexto.stepUpExigido());
        assertFalse(contexto.mascarado());
        verify(securityAlertService, never()).create(any(), any(), any(), any(), any(), any(Integer.class));
    }

    @Test
    void shouldAcceptStrongPasswordFallbackForElevatedJusticeSecret() {
        Processo processo = processo(104L, NivelSigilo.SEGREDO_JUSTICA);
        when(processoRepository.findContextoCompletoById(104L)).thenReturn(Optional.of(processo));
        when(supportBridge.possuiAuditLedger()).thenReturn(false);
        when(supportBridge.possuiDecisionTrace()).thenReturn(false);

        ProcessoMalhaSigiloAuthorizationService service = new ProcessoMalhaSigiloAuthorizationService(
                processoRepository,
                decisionStepUpTokenService,
                securityAlertService,
                supportBridge,
                provider(null)
        );

        ProcessoMalhaSigiloContexto contexto = service.avaliar(104L, actorElevado(), null, "REQ-4", "senha-forte-123", "127.0.0.1");

        assertEquals(ProcessoMalhaViewLevel.PLENO, contexto.viewLevel());
        assertTrue(contexto.stepUpAtivo());
        assertFalse(contexto.stepUpExigido());
    }

    private static Processo processo(Long id, NivelSigilo nivelSigilo) {
        Processo processo = new Processo();
        processo.setId(id);
        processo.setNivelSigilo(nivelSigilo);
        processo.setRamoDireito(RamoDireito.PENAL);
        return processo;
    }

    private static ProcessoMalhaActorContext actorElevado() {
        return new ProcessoMalhaActorContext(900L, "Servidor Estratégico", "12312312312", TipoUsuario.SERVIDOR_FORUM, TipoUsuario.SERVIDOR_FORUM, RamoDireito.PENAL, List.of("SERVIDOR_FORUM"), true, true, false);
    }

    private static ProcessoMalhaActorContext actorAdvogadoParte() {
        return new ProcessoMalhaActorContext(901L, "Advogado da Parte", "99999999999", TipoUsuario.ADVOGADO, TipoUsuario.ADVOGADO, RamoDireito.CIVIL, List.of("ADVOGADO", "MALHA_PARTE_RELACIONADA"), false, true, true);
    }

    private static ObjectProvider<OutboxPublisher> provider(OutboxPublisher outboxPublisher) {
        if (outboxPublisher == null) {
            return new StaticListableBeanFactory().getBeanProvider(OutboxPublisher.class);
        }
        return new StaticListableBeanFactory(java.util.Map.of("outboxPublisher", outboxPublisher)).getBeanProvider(OutboxPublisher.class);
    }
}
