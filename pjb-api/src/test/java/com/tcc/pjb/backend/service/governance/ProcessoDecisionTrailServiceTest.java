package com.tcc.pjb.backend.service.governance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerEntry;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerRepository;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.explainability.DecisionTrace;
import com.tcc.pjb.backend.core.explainability.DecisionTraceRepository;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.pericia.CadeiaCustodiaDigitalLedgerEntry;
import com.tcc.pjb.backend.model.repository.CadeiaCustodiaDigitalLedgerEntryRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class ProcessoDecisionTrailServiceTest {

    @Test
    void shouldMergeAuditTraceAndCustodyEvents() {
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        AuditLedgerRepository auditLedgerRepository = Mockito.mock(AuditLedgerRepository.class);
        AuditLedgerService auditLedgerService = Mockito.mock(AuditLedgerService.class);
        DecisionTraceRepository decisionTraceRepository = Mockito.mock(DecisionTraceRepository.class);
        DecisionTraceService decisionTraceService = Mockito.mock(DecisionTraceService.class);
        CadeiaCustodiaDigitalLedgerEntryRepository cadeiaRepository = Mockito.mock(CadeiaCustodiaDigitalLedgerEntryRepository.class);
        Processo processo = new Processo();
        processo.setId(4L);
        processo.setNumeroProcesso("0004");
        AuditLedgerEntry audit = new AuditLedgerEntry();
        audit.setAction("READ_PROCESSO");
        audit.setCreatedAt(LocalDateTime.of(2026, 3, 17, 10, 0));
        audit.setEntryHash("h1");
        DecisionTrace trace = new DecisionTrace();
        trace.setDecisionType("ROUTING_DECISION");
        trace.setCreatedAt(LocalDateTime.of(2026, 3, 17, 11, 0));
        trace.setOutputDigest("h2");
        CadeiaCustodiaDigitalLedgerEntry custody = CadeiaCustodiaDigitalLedgerEntry.builder().evidenceNome("Sentença").sealedAt(Instant.parse("2026-03-17T12:00:00Z")).entryHash("h3").build();
        when(processoRepository.findById(4L)).thenReturn(Optional.of(processo));
        when(auditLedgerRepository.search(null, null, "PROCESSO", "4", PageRequest.of(0, 100))).thenReturn(new PageImpl<>(List.of(audit)));
        when(auditLedgerRepository.search(null, null, "PROCESSO", "0004", PageRequest.of(0, 50))).thenReturn(new PageImpl<>(List.of()));
        when(decisionTraceRepository.findTop200BySubjectTypeAndSubjectIdOrderByCreatedAtDesc("PROCESSO", "4")).thenReturn(List.of(trace));
        when(decisionTraceRepository.findTop200BySubjectTypeAndSubjectIdOrderByCreatedAtDesc("PROCESSO", "0004")).thenReturn(List.of());
        when(cadeiaRepository.findTop200ByChaveCustodiaOrderBySealedAtDesc("proc:4")).thenReturn(List.of(custody));
        ProcessoDecisionTrailService service = new ProcessoDecisionTrailService(
                processoRepository,
                auditLedgerRepository,
                auditLedgerService,
                decisionTraceRepository,
                decisionTraceService,
                cadeiaRepository
        );
        var response = service.timeline(4L);
        assertEquals(3, response.totalEventos());
    }
}
