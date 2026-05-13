package com.tcc.pjb.backend.core.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceTimelineEntry;
import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceTimelineResult;
import com.tcc.pjb.backend.model.dto.security.govbr.GovBrAccountEntryGovernanceResponse;
import com.tcc.pjb.backend.service.security.govbr.GovBrSurfaceFacadeService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class GovBrAssuranceApplicationServiceTest {

    @Test
    void timeline_deveAuditarQuantidadeDeEntradas() {
        GovBrAssurancePolicy policy = mock(GovBrAssurancePolicy.class);
        GovBrSurfaceFacadeService facadeService = mock(GovBrSurfaceFacadeService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(policy.timeline("ouro", true)).thenReturn(new GovBrAssuranceTimelineResult(List.of(
                new GovBrAssuranceTimelineEntry("received", "ouro", true, Instant.parse("2026-04-11T10:00:00Z")),
                new GovBrAssuranceTimelineEntry("evaluated", "ouro", true, Instant.parse("2026-04-11T10:00:01Z")))));
        GovBrAssuranceApplicationService applicationService = new GovBrAssuranceApplicationService(policy, facadeService, auditLedgerService);

        var result = applicationService.timeline("OURO", true);

        assertThat(result.entries()).hasSize(2);
        verify(auditLedgerService).appendSafely(eq("GOVBR_ASSURANCE_TIMELINE_QUERY"), eq("GOVBR"), eq("ouro"), isNull(), eq("entries=2"));
    }

    @Test
    void owner_deveRefletirUsuarioDaReadiness() {
        GovBrAssurancePolicy policy = mock(GovBrAssurancePolicy.class);
        GovBrSurfaceFacadeService facadeService = mock(GovBrSurfaceFacadeService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(facadeService.readiness()).thenReturn(new GovBrAccountEntryGovernanceResponse(
                true,
                false,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                9L,
                true,
                true,
                true,
                true,
                "app.gov.br",
                "stepup.gov.br",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Instant.parse("2026-04-11T10:00:00Z")));
        GovBrAssuranceApplicationService applicationService = new GovBrAssuranceApplicationService(policy, facadeService, auditLedgerService);

        var result = applicationService.owner();

        assertThat(result.referenceId()).isEqualTo(9L);
        assertThat(result.status()).isEqualTo("BOUND");
    }

    @Test
    void stepUpHealth_deveAuditarNecessidade() {
        GovBrAssurancePolicy policy = mock(GovBrAssurancePolicy.class);
        GovBrSurfaceFacadeService facadeService = mock(GovBrSurfaceFacadeService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(policy.stepUpDecisionView("bronze", true)).thenReturn(new com.tcc.pjb.backend.core.security.domain.GovBrStepUpDecisionView("bronze", "ouro", true));
        GovBrAssuranceApplicationService applicationService = new GovBrAssuranceApplicationService(policy, facadeService, auditLedgerService);

        var result = applicationService.stepUpHealth("bronze", true);

        assertThat(result.status()).isEqualTo("STEP_UP_REQUIRED");
        verify(auditLedgerService).appendSafely(eq("GOVBR_STEPUP_HEALTH_QUERY"), eq("GOVBR"), eq("bronze"), isNull(), eq("required=true"));
    }
}
