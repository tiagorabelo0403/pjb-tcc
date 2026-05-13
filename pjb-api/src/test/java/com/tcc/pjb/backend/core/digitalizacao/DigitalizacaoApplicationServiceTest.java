package com.tcc.pjb.backend.core.digitalizacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoGovernanceBatchResult;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoReviewQueueSnapshot;
import com.tcc.pjb.backend.model.entity.digitalizacao.DigitalizacaoJob;
import com.tcc.pjb.backend.model.entity.digitalizacao.DigitalizacaoPagina;
import com.tcc.pjb.backend.model.repository.DigitalizacaoJobRepository;
import com.tcc.pjb.backend.model.repository.DigitalizacaoPaginaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DigitalizacaoApplicationServiceTest {

    @Test
    void timeline_deveAuditarConsultaEExporEtapasDoJob() {
        DigitalizacaoOcrService ocrService = mock(DigitalizacaoOcrService.class);
        DigitalizacaoGovernanceService governanceService = mock(DigitalizacaoGovernanceService.class);
        DigitalizacaoJobRepository jobRepository = mock(DigitalizacaoJobRepository.class);
        DigitalizacaoPaginaRepository paginaRepository = mock(DigitalizacaoPaginaRepository.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        Instant createdAt = Instant.parse("2026-04-11T12:00:00Z");
        Instant startedAt = Instant.parse("2026-04-11T12:05:00Z");
        Instant completedAt = Instant.parse("2026-04-11T12:10:00Z");
        when(jobRepository.findById(10L)).thenReturn(Optional.of(DigitalizacaoJob.builder()
                .id(10L)
                .numeroProcessoOrigem("0001234")
                .status("CONCLUIDO")
                .idioma("por")
                .createdAt(createdAt)
                .startedAt(startedAt)
                .completedAt(completedAt)
                .revisaoRequerida(false)
                .build()));
        DigitalizacaoApplicationService service = new DigitalizacaoApplicationService(
                ocrService,
                governanceService,
                jobRepository,
                paginaRepository,
                new DigitalizacaoProperties(true, 70.0, "por", 20, 60, 300000),
                auditLedgerService);

        var result = service.timeline(10L);

        assertThat(result.entries()).hasSize(3);
        assertThat(result.entries().get(0).etapa()).isEqualTo("CRIADO");
        assertThat(result.entries().get(2).etapa()).isEqualTo("CONCLUIDO");
        verify(auditLedgerService).appendSafely(eq("DIGITALIZACAO_TIMELINE_QUERY"), eq("JOB"), eq("10"), isNull(), eq("entries=3"));
    }

    @Test
    void confianca_deveExporMediaEPaginasPendentes() {
        DigitalizacaoOcrService ocrService = mock(DigitalizacaoOcrService.class);
        DigitalizacaoGovernanceService governanceService = mock(DigitalizacaoGovernanceService.class);
        DigitalizacaoJobRepository jobRepository = mock(DigitalizacaoJobRepository.class);
        DigitalizacaoPaginaRepository paginaRepository = mock(DigitalizacaoPaginaRepository.class);
        when(jobRepository.findById(11L)).thenReturn(Optional.of(DigitalizacaoJob.builder()
                .id(11L)
                .status("REVISAO_HUMANA")
                .confiancaMedia(BigDecimal.valueOf(66.5))
                .revisaoRequerida(true)
                .createdAt(Instant.now())
                .build()));
        when(paginaRepository.countByJobIdAndRevisadoFalse(11L)).thenReturn(2L);
        DigitalizacaoApplicationService service = new DigitalizacaoApplicationService(
                ocrService,
                governanceService,
                jobRepository,
                paginaRepository,
                new DigitalizacaoProperties(true, 70.0, "por", 20, 60, 300000),
                mock(AuditLedgerService.class));

        var result = service.confianca(11L);

        assertThat(result.confiancaMedia()).isEqualTo(66.5);
        assertThat(result.revisaoRequerida()).isTrue();
        assertThat(result.paginasComRevisao()).isEqualTo(2);
    }

    @Test
    void reconcileStaleProcessing_deveDelegarParaGovernancaEAuditar() {
        DigitalizacaoOcrService ocrService = mock(DigitalizacaoOcrService.class);
        DigitalizacaoGovernanceService governanceService = mock(DigitalizacaoGovernanceService.class);
        DigitalizacaoJobRepository jobRepository = mock(DigitalizacaoJobRepository.class);
        DigitalizacaoPaginaRepository paginaRepository = mock(DigitalizacaoPaginaRepository.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(governanceService.marcarProcessamentosEstagnadosResumo()).thenReturn(new DigitalizacaoGovernanceBatchResult(3));
        DigitalizacaoApplicationService service = new DigitalizacaoApplicationService(
                ocrService,
                governanceService,
                jobRepository,
                paginaRepository,
                new DigitalizacaoProperties(true, 70.0, "por", 20, 60, 300000),
                auditLedgerService);

        var result = service.reconcileStaleProcessing();

        assertThat(result.totalMarcadosComoFalha()).isEqualTo(3);
        verify(auditLedgerService).appendSafely(eq("DIGITALIZACAO_GOVERNANCA_RECONCILE"), eq("DIGITALIZACAO"), eq("REVIEW_QUEUE"), isNull(), eq("marcados=3"));
    }

    @Test
    void reviewQueue_deveRetornarSnapshotDaGovernanca() {
        DigitalizacaoOcrService ocrService = mock(DigitalizacaoOcrService.class);
        DigitalizacaoGovernanceService governanceService = mock(DigitalizacaoGovernanceService.class);
        DigitalizacaoJobRepository jobRepository = mock(DigitalizacaoJobRepository.class);
        DigitalizacaoPaginaRepository paginaRepository = mock(DigitalizacaoPaginaRepository.class);
        DigitalizacaoReviewQueueSnapshot snapshot = new DigitalizacaoReviewQueueSnapshot(List.of(), 0);
        when(governanceService.reviewQueue(any(com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoQueueCriteria.class))).thenReturn(snapshot);
        DigitalizacaoApplicationService service = new DigitalizacaoApplicationService(
                ocrService,
                governanceService,
                jobRepository,
                paginaRepository,
                new DigitalizacaoProperties(true, 70.0, "por", 20, 60, 300000),
                mock(AuditLedgerService.class));

        var result = service.reviewQueue(15);

        assertThat(result).isSameAs(snapshot);
    }

    @Test
    void pageConsistency_deveDetectarAutoClassificacaoConsistente() {
        DigitalizacaoOcrService ocrService = mock(DigitalizacaoOcrService.class);
        DigitalizacaoGovernanceService governanceService = mock(DigitalizacaoGovernanceService.class);
        DigitalizacaoJobRepository jobRepository = mock(DigitalizacaoJobRepository.class);
        DigitalizacaoPaginaRepository paginaRepository = mock(DigitalizacaoPaginaRepository.class);
        when(paginaRepository.findById(99L)).thenReturn(Optional.of(DigitalizacaoPagina.builder()
                .id(99L)
                .jobId(11L)
                .numeroPagina(4)
                .tipoPeca("CERTIDAO")
                .confianca(BigDecimal.valueOf(60))
                .revisado(false)
                .createdAt(Instant.now())
                .build()));
        DigitalizacaoApplicationService service = new DigitalizacaoApplicationService(
                ocrService,
                governanceService,
                jobRepository,
                paginaRepository,
                new DigitalizacaoProperties(true, 70.0, "por", 20, 60, 300000),
                mock(AuditLedgerService.class));

        var result = service.pageConsistency(99L);

        assertThat(result.status()).isEqualTo("CONSISTENT");
        assertThat(result.detalhe()).contains("threshold");
    }
}
