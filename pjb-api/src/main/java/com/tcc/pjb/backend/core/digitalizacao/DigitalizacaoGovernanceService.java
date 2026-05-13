package com.tcc.pjb.backend.core.digitalizacao;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoGovernanceBatchResult;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoPaginaReviewResult;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoQueueCriteria;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoReviewQueueCommand;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoReviewQueueEntry;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoReviewQueueSnapshot;
import com.tcc.pjb.backend.core.digitalizacao.domain.RegistrarRevisaoPaginaCommand;
import com.tcc.pjb.backend.model.entity.digitalizacao.DigitalizacaoJob;
import com.tcc.pjb.backend.model.entity.digitalizacao.DigitalizacaoPagina;
import com.tcc.pjb.backend.model.repository.DigitalizacaoJobRepository;
import com.tcc.pjb.backend.model.repository.DigitalizacaoPaginaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DigitalizacaoGovernanceService {

    private final DigitalizacaoJobRepository jobRepository;
    private final DigitalizacaoPaginaRepository paginaRepository;
    private final DigitalizacaoProperties properties;
    private final AuditLedgerService auditLedger;
    private final ReadAfterWriteConsistencyPolicy readAfterWriteConsistencyPolicy;

    public DigitalizacaoGovernanceService(DigitalizacaoJobRepository jobRepository,
                                          DigitalizacaoPaginaRepository paginaRepository,
                                          DigitalizacaoProperties properties,
                                          AuditLedgerService auditLedger,
                                          ReadAfterWriteConsistencyPolicy readAfterWriteConsistencyPolicy) {
        this.jobRepository = Objects.requireNonNull(jobRepository);
        this.paginaRepository = Objects.requireNonNull(paginaRepository);
        this.properties = Objects.requireNonNull(properties);
        this.auditLedger = Objects.requireNonNull(auditLedger);
        this.readAfterWriteConsistencyPolicy = Objects.requireNonNull(readAfterWriteConsistencyPolicy);
    }

    @Transactional(readOnly = true)
    public DigitalizacaoReviewQueueSnapshot reviewQueue(DigitalizacaoReviewQueueCommand command) {
        Objects.requireNonNull(command);
        return reviewQueue(new DigitalizacaoQueueCriteria("REVISAO_HUMANA", command.limit()));
    }

    @Transactional(readOnly = true)
    public DigitalizacaoReviewQueueSnapshot reviewQueue(DigitalizacaoQueueCriteria criteria) {
        Objects.requireNonNull(criteria);
        int limit = criteria.limit() > 0 ? criteria.limit() : Math.max(1, properties.reviewBatchSize());
        List<DigitalizacaoReviewQueueEntry> entries = jobRepository.findTop100ByStatusOrderByCreatedAtAsc(criteria.status())
                .stream()
                .limit(limit)
                .map(job -> new DigitalizacaoReviewQueueEntry(
                        job.getId(),
                        job.getProcessoId(),
                        job.getNumeroProcessoOrigem(),
                        job.getPaginasProcessadas(),
                        job.getTotalPaginas()))
                .toList();
        return new DigitalizacaoReviewQueueSnapshot(entries, entries.size());
    }

    @Transactional(readOnly = true)
    public DigitalizacaoReviewQueueSnapshot reviewQueue() {
        List<DigitalizacaoReviewQueueEntry> entries = jobRepository.findReviewQueue(PageRequest.of(0, Math.max(1, properties.reviewBatchSize())))
                .stream()
                .map(job -> new DigitalizacaoReviewQueueEntry(
                        job.getId(),
                        job.getStatus(),
                        job.getPaginasProcessadas(),
                        job.isRevisaoRequerida()))
                .toList();
        return new DigitalizacaoReviewQueueSnapshot(entries, entries.size());
    }

    @Transactional
    public DigitalizacaoPaginaReviewResult registrarRevisaoPagina(RegistrarRevisaoPaginaCommand command) {
        Objects.requireNonNull(command);
        DigitalizacaoPagina pagina = paginaRepository.findById(command.paginaId())
                .orElseThrow(() -> new IllegalArgumentException("Página não encontrada: " + command.paginaId()));
        pagina.setConteudoOcr(command.conteudoOcrRevisado());
        pagina.setTipoPeca(command.tipoPecaRevisado());
        pagina.setRevisado(true);
        paginaRepository.save(pagina);

        long pendentes = paginaRepository.countByJobIdAndRevisadoFalse(pagina.getJobId());
        if (pendentes == 0) {
            DigitalizacaoJob job = jobRepository.findById(pagina.getJobId())
                    .orElseThrow(() -> new IllegalArgumentException("Job não encontrado: " + pagina.getJobId()));
            List<DigitalizacaoPagina> paginas = paginaRepository.findByJobIdOrderByNumeroPaginaAsc(job.getId());
            BigDecimal media = paginas.isEmpty()
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(paginas.stream()
                            .map(DigitalizacaoPagina::getConfianca)
                            .filter(Objects::nonNull)
                            .mapToDouble(BigDecimal::doubleValue)
                            .average()
                            .orElse(0.0));
            job.setRevisaoRequerida(false);
            job.setStatus("CONCLUIDO");
            job.setConfiancaMedia(media);
            if (job.getCompletedAt() == null) {
                job.setCompletedAt(Instant.now());
            }
            jobRepository.save(job);
            readAfterWriteConsistencyPolicy.markWrite();
            auditLedger.appendSafely("DIGITALIZACAO_REVISAO_CONCLUIDA", "JOB", String.valueOf(job.getId()), "paginas=" + paginas.size());
            return new DigitalizacaoPaginaReviewResult(pagina.getId(), job.getId(), true, 0L);
        }
        readAfterWriteConsistencyPolicy.markWrite();
        return new DigitalizacaoPaginaReviewResult(pagina.getId(), pagina.getJobId(), false, pendentes);
    }

    @Transactional
    public DigitalizacaoGovernanceBatchResult marcarProcessamentosEstagnadosResumo() {
        return new DigitalizacaoGovernanceBatchResult(marcarProcessamentosEstagnados());
    }

    @Transactional
    public int marcarProcessamentosEstagnados() {
        Instant cutoff = Instant.now().minus(Math.max(1L, properties.staleProcessingMinutes()), ChronoUnit.MINUTES);
        List<DigitalizacaoJob> jobs = jobRepository.findStaleProcessingJobs(cutoff);
        for (DigitalizacaoJob job : jobs) {
            job.setStatus("FALHA");
            job.setFailureReason("Processamento estagnado acima da janela de governança");
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);
            auditLedger.appendSafely("DIGITALIZACAO_STALE_FAIL", "JOB", String.valueOf(job.getId()), "cutoff=" + cutoff);
        }
        if (!jobs.isEmpty()) {
            readAfterWriteConsistencyPolicy.markWrite();
        }
        return jobs.size();
    }
}
