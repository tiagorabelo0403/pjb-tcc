package com.tcc.pjb.backend.core.digitalizacao;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoClassificationHealthSnapshot;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoConfiancaResult;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoConfiancaView;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoConsultaResult;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoEngineHealthQuery;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoEngineHealthResult;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoEngineSnapshot;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoGovernanceBatchResult;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoHealthQuery;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoHealthResult;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoIdiomaResult;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoIdiomaView;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoJobOwnershipView;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoPageAuditSnapshot;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoPageConsultaCommand;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoPageConsultaResult;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoPageWindowView;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoPendingReviewQuery;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoPendingReviewView;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoQueueConsistencyView;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoQueueCriteria;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoReviewHealthSnapshot;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoReviewOwnershipView;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoReviewQueueCommand;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoReviewQueueSnapshot;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoReviewWindowView;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoTimelineEntry;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoTimelineResult;
import com.tcc.pjb.backend.core.digitalizacao.domain.OcrEngineAuditView;
import com.tcc.pjb.backend.core.digitalizacao.domain.OcrPageAuditView;
import com.tcc.pjb.backend.core.digitalizacao.domain.OcrPageConsistencyView;
import com.tcc.pjb.backend.core.digitalizacao.domain.OcrReviewHealthResult;
import com.tcc.pjb.backend.model.entity.digitalizacao.DigitalizacaoJob;
import com.tcc.pjb.backend.model.entity.digitalizacao.DigitalizacaoPagina;
import com.tcc.pjb.backend.model.repository.DigitalizacaoJobRepository;
import com.tcc.pjb.backend.model.repository.DigitalizacaoPaginaRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DigitalizacaoApplicationService {

    private final DigitalizacaoOcrService digitalizacaoOcrService;
    private final DigitalizacaoGovernanceService digitalizacaoGovernanceService;
    private final DigitalizacaoJobRepository digitalizacaoJobRepository;
    private final DigitalizacaoPaginaRepository digitalizacaoPaginaRepository;
    private final DigitalizacaoProperties digitalizacaoProperties;
    private final AuditLedgerService auditLedgerService;

    public DigitalizacaoApplicationService(DigitalizacaoOcrService digitalizacaoOcrService,
                                           DigitalizacaoGovernanceService digitalizacaoGovernanceService,
                                           DigitalizacaoJobRepository digitalizacaoJobRepository,
                                           DigitalizacaoPaginaRepository digitalizacaoPaginaRepository,
                                           DigitalizacaoProperties digitalizacaoProperties,
                                           AuditLedgerService auditLedgerService) {
        this.digitalizacaoOcrService = Objects.requireNonNull(digitalizacaoOcrService);
        this.digitalizacaoGovernanceService = Objects.requireNonNull(digitalizacaoGovernanceService);
        this.digitalizacaoJobRepository = Objects.requireNonNull(digitalizacaoJobRepository);
        this.digitalizacaoPaginaRepository = Objects.requireNonNull(digitalizacaoPaginaRepository);
        this.digitalizacaoProperties = Objects.requireNonNull(digitalizacaoProperties);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional(readOnly = true)
    public DigitalizacaoReviewQueueSnapshot reviewQueue(int limit) {
        return digitalizacaoGovernanceService.reviewQueue(new DigitalizacaoQueueCriteria("REVISAO_HUMANA", Math.max(1, limit)));
    }

    @Transactional
    public DigitalizacaoGovernanceBatchResult reconcileStaleProcessing() {
        DigitalizacaoGovernanceBatchResult result = digitalizacaoGovernanceService.marcarProcessamentosEstagnadosResumo();
        auditLedgerService.appendSafely(
                "DIGITALIZACAO_GOVERNANCA_RECONCILE",
                "DIGITALIZACAO",
                "REVIEW_QUEUE",
                null,
                "marcados=" + result.totalMarcadosComoFalha());
        return result;
    }

    @Transactional(readOnly = true)
    public DigitalizacaoConsultaResult job(Long jobId) {
        return digitalizacaoOcrService.jobConsulta(new com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoJobConsultaCommand(jobId));
    }

    @Transactional(readOnly = true)
    public DigitalizacaoHealthResult jobHealth(Long jobId) {
        return digitalizacaoOcrService.health(new DigitalizacaoHealthQuery(jobId));
    }

    @Transactional(readOnly = true)
    public DigitalizacaoTimelineResult timeline(Long jobId) {
        DigitalizacaoJob job = loadJob(jobId);
        List<DigitalizacaoTimelineEntry> entries = new ArrayList<>();
        entries.add(new DigitalizacaoTimelineEntry("CRIADO", job.getCreatedAt(), safe(job.getNumeroProcessoOrigem())));
        if (job.getStartedAt() != null) {
            entries.add(new DigitalizacaoTimelineEntry("PROCESSANDO", job.getStartedAt(), safe(job.getIdioma())));
        }
        if (job.getCompletedAt() != null) {
            entries.add(new DigitalizacaoTimelineEntry(job.getStatus(), job.getCompletedAt(), safe(job.getFailureReason())));
        }
        if (job.isRevisaoRequerida()) {
            entries.add(new DigitalizacaoTimelineEntry("REVISAO_HUMANA", Instant.now(), "paginasPendentes=" + digitalizacaoPaginaRepository.countByJobIdAndRevisadoFalse(jobId)));
        }
        auditLedgerService.appendSafely("DIGITALIZACAO_TIMELINE_QUERY", "JOB", String.valueOf(jobId), null, "entries=" + entries.size());
        return new DigitalizacaoTimelineResult(jobId, entries);
    }

    @Transactional(readOnly = true)
    public DigitalizacaoConfiancaResult confianca(Long jobId) {
        DigitalizacaoJob job = loadJob(jobId);
        long paginasComRevisao = digitalizacaoPaginaRepository.countByJobIdAndRevisadoFalse(jobId);
        return new DigitalizacaoConfiancaResult(
                jobId,
                job.getConfiancaMedia() == null ? 0.0 : job.getConfiancaMedia().doubleValue(),
                job.isRevisaoRequerida(),
                (int) paginasComRevisao);
    }

    @Transactional(readOnly = true)
    public DigitalizacaoConfiancaView confiancaView(Long jobId) {
        DigitalizacaoConfiancaResult result = confianca(jobId);
        return new DigitalizacaoConfiancaView(result.jobId(), result.confiancaMedia(), result.revisaoRequerida());
    }

    @Transactional(readOnly = true)
    public DigitalizacaoIdiomaResult idioma(Long jobId) {
        DigitalizacaoJob job = loadJob(jobId);
        String idioma = normalize(job.getIdioma());
        return new DigitalizacaoIdiomaResult(jobId, idioma, normalize(job.getOcrEngine()), supportsIdioma(idioma));
    }

    @Transactional(readOnly = true)
    public DigitalizacaoIdiomaView idiomaView(Long jobId) {
        DigitalizacaoIdiomaResult result = idioma(jobId);
        return new DigitalizacaoIdiomaView(result.jobId(), result.idioma(), result.ocrEngine());
    }

    @Transactional(readOnly = true)
    public DigitalizacaoJobOwnershipView ownership(Long jobId) {
        DigitalizacaoJob job = loadJob(jobId);
        boolean owned = job.getProcessoId() != null || safe(job.getNumeroProcessoOrigem()).length() > 0;
        return new DigitalizacaoJobOwnershipView(
                String.valueOf(jobId),
                owned ? "OWNED" : "ORPHAN",
                job.getProcessoId() != null ? "processo=" + job.getProcessoId() : safe(job.getNumeroProcessoOrigem()));
    }

    @Transactional(readOnly = true)
    public DigitalizacaoReviewOwnershipView reviewOwnership(Long jobId) {
        DigitalizacaoJob job = loadJob(jobId);
        return new DigitalizacaoReviewOwnershipView(
                String.valueOf(jobId),
                job.isRevisaoRequerida() ? "REVIEW_QUEUE" : "AUTO_APPROVED",
                "pendentes=" + digitalizacaoPaginaRepository.countByJobIdAndRevisadoFalse(jobId));
    }

    @Transactional(readOnly = true)
    public DigitalizacaoClassificationHealthSnapshot classificationHealth(Long jobId) {
        DigitalizacaoConfiancaResult result = confianca(jobId);
        return new DigitalizacaoClassificationHealthSnapshot(jobId, result.revisaoRequerida(), result.confiancaMedia());
    }

    @Transactional(readOnly = true)
    public OcrReviewHealthResult reviewHealth(Long jobId) {
        DigitalizacaoReviewHealthSnapshot snapshot = digitalizacaoOcrService.reviewHealth(jobId);
        DigitalizacaoJob job = loadJob(jobId);
        boolean healthy = !snapshot.revisaoRequerida() || snapshot.pendentes() > 0;
        return new OcrReviewHealthResult(jobId, snapshot.revisaoRequerida(), healthy, job.getPaginasProcessadas() == null ? 0 : job.getPaginasProcessadas());
    }

    @Transactional(readOnly = true)
    public List<DigitalizacaoPendingReviewView> pendingReview(Long jobId) {
        return digitalizacaoOcrService.pendingReview(new DigitalizacaoPendingReviewQuery(jobId));
    }

    @Transactional(readOnly = true)
    public DigitalizacaoReviewWindowView reviewWindow(Long jobId) {
        DigitalizacaoReviewHealthSnapshot snapshot = digitalizacaoOcrService.reviewHealth(jobId);
        return new DigitalizacaoReviewWindowView(
                String.valueOf(jobId),
                snapshot.revisaoRequerida() ? "ABERTA" : "FECHADA",
                Instant.now());
    }

    @Transactional(readOnly = true)
    public DigitalizacaoPageWindowView pageWindow(Long jobId) {
        List<DigitalizacaoPagina> paginas = digitalizacaoPaginaRepository.findByJobIdOrderByNumeroPaginaAsc(jobId);
        int fromPage = paginas.isEmpty() ? 0 : paginas.getFirst().getNumeroPagina();
        int toPage = paginas.isEmpty() ? 0 : paginas.getLast().getNumeroPagina();
        return new DigitalizacaoPageWindowView(jobId, fromPage, toPage);
    }

    @Transactional(readOnly = true)
    public DigitalizacaoPageConsultaResult page(Long paginaId) {
        return digitalizacaoOcrService.pageConsulta(new DigitalizacaoPageConsultaCommand(paginaId));
    }

    @Transactional(readOnly = true)
    public OcrPageAuditView pageAudit(Long paginaId) {
        DigitalizacaoPageAuditSnapshot snapshot = digitalizacaoOcrService.pageAudit(new DigitalizacaoPageConsultaCommand(paginaId));
        return new OcrPageAuditView(String.valueOf(snapshot.paginaId()), snapshot.revisado() ? "REVISADA" : "PENDENTE", Instant.now());
    }

    @Transactional(readOnly = true)
    public OcrPageConsistencyView pageConsistency(Long paginaId) {
        DigitalizacaoPagina pagina = loadPagina(paginaId);
        boolean consistent = pagina.isRevisado() || pagina.getConfianca() == null || pagina.getConfianca().doubleValue() < digitalizacaoProperties.confiancaMinimaAuto();
        String detalhe = pagina.isRevisado()
                ? "pagina revisada"
                : consistent
                ? "auto classificada abaixo do threshold"
                : "pagina pendente de revisão acima do threshold";
        return new OcrPageConsistencyView(
                String.valueOf(paginaId),
                consistent ? "CONSISTENT" : "DRIFT",
                detalhe);
    }

    @Transactional(readOnly = true)
    public DigitalizacaoQueueConsistencyView queueConsistency(int limit) {
        DigitalizacaoReviewQueueSnapshot snapshot = reviewQueue(limit);
        String status = snapshot.hasJobs() ? "REVIEW_PENDING" : "STABLE";
        return new DigitalizacaoQueueConsistencyView("digitalizacao-review-queue", status, Instant.now());
    }

    @Transactional(readOnly = true)
    public DigitalizacaoEngineSnapshot engineSnapshot() {
        var health = digitalizacaoOcrService.engineHealth();
        return new DigitalizacaoEngineSnapshot(health.engine(), health.enabled(), health.idioma());
    }

    @Transactional(readOnly = true)
    public DigitalizacaoEngineHealthResult engineHealth(DigitalizacaoEngineHealthQuery query) {
        Objects.requireNonNull(query);
        var health = digitalizacaoOcrService.engineHealth();
        String mensagem = health.enabled() ? "ocr engine disponível" : "ocr mock em modo seguro";
        return new DigitalizacaoEngineHealthResult(health.enabled(), mensagem, Instant.now());
    }

    @Transactional(readOnly = true)
    public OcrEngineAuditView engineAudit() {
        var snapshot = engineSnapshot();
        return new OcrEngineAuditView(snapshot.engine(), snapshot.enabled() ? "READY" : "DISABLED", Instant.now());
    }

    private DigitalizacaoJob loadJob(Long jobId) {
        return digitalizacaoJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job não encontrado: " + jobId));
    }

    private DigitalizacaoPagina loadPagina(Long paginaId) {
        return digitalizacaoPaginaRepository.findById(paginaId)
                .orElseThrow(() -> new IllegalArgumentException("Página não encontrada: " + paginaId));
    }

    private boolean supportsIdioma(String idioma) {
        return List.of("por", "eng", "spa").contains(idioma);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "n/a";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
