package com.tcc.pjb.backend.core.prazos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.prazos.PrazoRegime;
import com.tcc.pjb.backend.core.prazos.auditoria.PrazoAuditTrail;
import com.tcc.pjb.backend.core.prazos.auditoria.PrazoAuditTrailService;
import com.tcc.pjb.backend.core.prazos.auditoria.domain.PrazoAuditQuery;
import com.tcc.pjb.backend.core.prazos.auditoria.domain.PrazoAuditResult;
import com.tcc.pjb.backend.core.prazos.auditoria.domain.PrazoTimelineEntry;
import com.tcc.pjb.backend.core.prazos.auditoria.domain.PrazoTimelineView;
import com.tcc.pjb.backend.core.prazos.calculo.PrazosEngine;
import com.tcc.pjb.backend.core.prazos.policy.PrazoPolicyRegistry;
import com.tcc.pjb.backend.core.prazos.policy.domain.PrazoPolicyView;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrazoApplicationServiceTest {

    @Test
    void timeline_deveAuditarQuantidade() {
        PrazosEngine prazosEngine = mock(PrazosEngine.class);
        PrazoAuditTrailService auditTrailService = mock(PrazoAuditTrailService.class);
        PrazoPolicyRegistry policyRegistry = mock(PrazoPolicyRegistry.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        PrazoAuditTrail trail = new PrazoAuditTrail(1L, "INTIMACAO", 2, PrazoRegime.UTEIS, LocalDateTime.of(2026,4,11,9,0), LocalDateTime.of(2026,4,15,9,0), "CE", "Fortaleza", 1, "hash", Instant.parse("2026-04-11T12:00:00Z"));
        PrazoAuditQuery query = new PrazoAuditQuery(1L, "INTIMACAO", 2, PrazoRegime.UTEIS, LocalDateTime.of(2026,4,11,9,0), LocalDateTime.of(2026,4,15,9,0), "CE", "Fortaleza");
        when(auditTrailService.query(query)).thenReturn(new PrazoAuditResult(trail, true));
        when(auditTrailService.timeline(trail)).thenReturn(new PrazoTimelineView(List.of(new PrazoTimelineEntry("inicio", Instant.parse("2026-04-11T12:00:00Z"), PrazoRegime.UTEIS.name()), new PrazoTimelineEntry("fim", Instant.parse("2026-04-15T12:00:00Z"), PrazoRegime.UTEIS.name()))));
        PrazoApplicationService applicationService = new PrazoApplicationService(prazosEngine, auditTrailService, policyRegistry, auditLedgerService);

        var result = applicationService.timeline(query);

        assertThat(result.entries()).hasSize(2);
        verify(auditLedgerService).appendSafely(eq("PRAZO_TIMELINE_QUERY"), eq("PRAZO"), eq("1"), isNull(), eq("entries=2"));
    }

    @Test
    void policy_deveResolverRegimePorPerfil() {
        PrazosEngine prazosEngine = mock(PrazosEngine.class);
        PrazoAuditTrailService auditTrailService = mock(PrazoAuditTrailService.class);
        PrazoPolicyRegistry policyRegistry = mock(PrazoPolicyRegistry.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(policyRegistry.resolveByPartyProfile(com.tcc.pjb.backend.model.entity.enums.RamoDireito.TRABALHISTA, null, false, false, false)).thenReturn(PrazoRegime.CLT_HORAS_UTEIS);
        when(prazosEngine.policyView("TRABALHISTA", null, PrazoRegime.CLT_HORAS_UTEIS)).thenReturn(new PrazoPolicyView("TRABALHISTA", null, PrazoRegime.CLT_HORAS_UTEIS));
        PrazoApplicationService applicationService = new PrazoApplicationService(prazosEngine, auditTrailService, policyRegistry, auditLedgerService);

        var result = applicationService.policy("TRABALHISTA", null, false, false, false);

        assertThat(result.regime()).isEqualTo(PrazoRegime.CLT_HORAS_UTEIS);
    }
}
