package com.tcc.pjb.backend.core.icp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilTimelineResult;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilTrustAnchorAuditSnapshot;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilTrustAnchorResult;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilTrustAnchorView;
import java.util.List;
import org.junit.jupiter.api.Test;

class IcpBrasilApplicationServiceTest {

    @Test
    void trustAnchor_deveAuditarConsulta() {
        IcpBrasilChainValidator validator = mock(IcpBrasilChainValidator.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(validator.trustAnchorResult(new com.tcc.pjb.backend.core.icp.domain.IcpBrasilTrustAnchorQuery("SERPRO")))
                .thenReturn(new IcpBrasilTrustAnchorResult(new IcpBrasilTrustAnchorView("SERPRO", "SERPRO", true), new IcpBrasilTrustAnchorAuditSnapshot("SERPRO", "SERPRO", true)));
        IcpBrasilApplicationService applicationService = new IcpBrasilApplicationService(validator, auditLedgerService);

        var result = applicationService.trustAnchor("serpro");

        assertThat(result.anchor().acSigla()).isEqualTo("SERPRO");
        verify(auditLedgerService).appendSafely(eq("ICP_TRUST_ANCHOR_QUERY"), eq("ICP"), eq("SERPRO"), isNull(), eq("true"));
    }

    @Test
    void timeline_deveAuditarQuantidade() {
        IcpBrasilChainValidator validator = mock(IcpBrasilChainValidator.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(validator.timeline(new com.tcc.pjb.backend.core.icp.domain.IcpBrasilTimelineQuery("hash-1")))
                .thenReturn(new IcpBrasilTimelineResult("hash-1", List.of()));
        IcpBrasilApplicationService applicationService = new IcpBrasilApplicationService(validator, auditLedgerService);

        var result = applicationService.timeline("hash-1");

        assertThat(result.docHash()).isEqualTo("hash-1");
        verify(auditLedgerService).appendSafely(eq("ICP_TIMELINE_QUERY"), eq("ICP"), eq("hash-1"), isNull(), eq("entries=0"));
    }
}
