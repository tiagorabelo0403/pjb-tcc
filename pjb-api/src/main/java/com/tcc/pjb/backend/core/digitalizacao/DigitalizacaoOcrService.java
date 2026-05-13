package com.tcc.pjb.backend.core.digitalizacao;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoClassificationAuditSnapshot;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoConsultaCommand;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoConsultaResult;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoHealthQuery;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoHealthResult;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoJobAuditSnapshot;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoJobHealthSnapshot;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoJobView;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoJobWindowSnapshot;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoPageAuditSnapshot;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoPageConsultaCommand;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoPageConsultaResult;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoPageView;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoPendingReviewQuery;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoPendingReviewView;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoProcessamentoCommand;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoProcessamentoResult;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoQueueQueryCommand;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoQueueQueryResult;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoReviewAuditSnapshot;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoReviewHealthSnapshot;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoReviewQueueEntry;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoStatusSnapshot;
import com.tcc.pjb.backend.core.digitalizacao.domain.OcrEngineHealthSnapshot;
import com.tcc.pjb.backend.core.digitalizacao.domain.OcrExecutionAuditSnapshot;
import com.tcc.pjb.backend.core.digitalizacao.domain.OcrPageSnapshot;
import com.tcc.pjb.backend.model.entity.digitalizacao.DigitalizacaoJob;
import com.tcc.pjb.backend.model.entity.digitalizacao.DigitalizacaoPagina;
import com.tcc.pjb.backend.model.repository.DigitalizacaoJobRepository;
import com.tcc.pjb.backend.model.repository.DigitalizacaoPaginaRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DigitalizacaoOcrService {

    private final DigitalizacaoJobRepository jobRepository;
    private final DigitalizacaoPaginaRepository paginaRepository;
    private final OcrEnginePort ocrEngine;
    private final PecaClassificadorService classificador;
    private final AuditLedgerService auditLedger;
    private final DigitalizacaoProperties properties;
    private final ReadAfterWriteConsistencyPolicy readAfterWriteConsistencyPolicy;
    private final Counter paginasProcessadas;
    private final Counter paginasRevisao;

    public DigitalizacaoOcrService(DigitalizacaoJobRepository jobRepository,
                                   DigitalizacaoPaginaRepository paginaRepository,
                                   OcrEnginePort ocrEngine,
                                   PecaClassificadorService classificador,
                                   AuditLedgerService auditLedger,
                                   DigitalizacaoProperties properties,
                                   ReadAfterWriteConsistencyPolicy readAfterWriteConsistencyPolicy,
                                   MeterRegistry meterRegistry) {
        this.jobRepository = Objects.requireNonNull(jobRepository);
        this.paginaRepository = Objects.requireNonNull(paginaRepository);
        this.ocrEngine = Objects.requireNonNull(ocrEngine);
        this.classificador = Objects.requireNonNull(classificador);
        this.auditLedger = Objects.requireNonNull(auditLedger);
        this.properties = Objects.requireNonNull(properties);
        this.readAfterWriteConsistencyPolicy = Objects.requireNonNull(readAfterWriteConsistencyPolicy);
        this.paginasProcessadas = Counter.builder("pjb.digitalizacao.paginas").tag("resultado", "processada").register(meterRegistry);
        this.paginasRevisao = Counter.builder("pjb.digitalizacao.paginas").tag("resultado", "revisao_requerida").register(meterRegistry);
    }

    @Transactional
    public DigitalizacaoProcessamentoResult processar(DigitalizacaoProcessamentoCommand command) {
        Objects.requireNonNull(command);
        return processar(command.jobId(), command.imagensPaginas(), normalizeIdioma(command.idioma()));
    }

    @Transactional
    public DigitalizacaoProcessamentoResult processar(Long jobId, List<byte[]> imagensPaginas) {
        return processar(jobId, imagensPaginas, properties.idiomaDefault());
    }

    @Transactional
    public DigitalizacaoProcessamentoResult processar(Long jobId, List<byte[]> imagensPaginas, String idioma) {
        DigitalizacaoJob job = loadJob(jobId);
        List<byte[]> paginas = imagensPaginas == null ? List.of() : imagensPaginas;
        job.setStatus("PROCESSANDO");
        job.setStartedAt(Instant.now());
        job.setTotalPaginas(paginas.size());
        job.setOcrEngine(resolveEngine(job));
        job.setIdioma(normalizeIdioma(idioma));
        jobRepository.save(job);

        int revisaoRequerida = 0;
        double somaConfianca = 0.0;
        String idiomaProcessamento = normalizeIdioma(idioma);

        for (int i = 0; i < paginas.size(); i++) {
            OcrPageResult ocr = ocrEngine.processar(paginas.get(i), idiomaProcessamento);
            String tipoPeca = classificador.classificar(ocr.texto());
            boolean precisaRevisao = ocr.confianca() < properties.confiancaMinimaAuto();
            paginaRepository.save(DigitalizacaoPagina.builder()
                    .jobId(jobId)
                    .numeroPagina(i + 1)
                    .conteudoOcr(ocr.texto())
                    .confianca(BigDecimal.valueOf(ocr.confianca()))
                    .tipoPeca(tipoPeca)
                    .revisado(!precisaRevisao)
                    .createdAt(Instant.now())
                    .build());
            paginasProcessadas.increment();
            if (precisaRevisao) {
                revisaoRequerida++;
                paginasRevisao.increment();
            }
            somaConfianca += ocr.confianca();
        }

        double confiancaMedia = paginas.isEmpty() ? 0.0 : somaConfianca / paginas.size();
        job.setPaginasProcessadas(paginas.size());
        job.setConfiancaMedia(BigDecimal.valueOf(confiancaMedia));
        job.setRevisaoRequerida(revisaoRequerida > 0);
        job.setStatus(revisaoRequerida > 0 ? "REVISAO_HUMANA" : "CONCLUIDO");
        job.setCompletedAt(Instant.now());
        jobRepository.save(job);
        readAfterWriteConsistencyPolicy.markWrite();
        auditLedger.appendSafely(
                "DIGITALIZACAO_CONCLUIDA",
                "JOB",
                String.valueOf(jobId),
                "paginas=" + paginas.size() + " revisao=" + revisaoRequerida + " confiancaMedia=" + String.format(Locale.ROOT, "%.1f", confiancaMedia));
        return new DigitalizacaoProcessamentoResult(jobId, paginas.size(), revisaoRequerida, confiancaMedia);
    }

    @Transactional(readOnly = true)
    public DigitalizacaoJobView jobView(Long jobId) {
        DigitalizacaoJob job = loadJob(jobId);
        return new DigitalizacaoJobView(
                job.getId(),
                job.getStatus(),
                nullSafe(job.getTotalPaginas()),
                nullSafe(job.getPaginasProcessadas()),
                job.isRevisaoRequerida());
    }

    @Transactional(readOnly = true)
    public DigitalizacaoPageView pageView(Long paginaId) {
        DigitalizacaoPagina pagina = loadPagina(paginaId);
        return new DigitalizacaoPageView(pagina.getId(), pagina.getNumeroPagina(), pagina.getTipoPeca(), pagina.isRevisado());
    }

    public com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoReviewCommandResult reviewResult(Long paginaId,
                                                                                                        boolean revisado,
                                                                                                        String tipoPeca) {
        return new com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoReviewCommandResult(paginaId, revisado, tipoPeca);
    }

    @Transactional(readOnly = true)
    public DigitalizacaoConsultaResult consultar(DigitalizacaoConsultaCommand command) {
        Objects.requireNonNull(command);
        DigitalizacaoJob job = loadJob(command.jobId());
        return new DigitalizacaoConsultaResult(
                job.getId(),
                job.getStatus(),
                nullSafe(job.getPaginasProcessadas()),
                job.getTotalPaginas(),
                job.isRevisaoRequerida(),
                job.getConfiancaMedia());
    }

    @Transactional(readOnly = true)
    public OcrPageSnapshot pageSnapshot(Long paginaId) {
        DigitalizacaoPagina pagina = loadPagina(paginaId);
        return new OcrPageSnapshot(pagina.getId(), pagina.getNumeroPagina(), pagina.getTipoPeca(), pagina.getConfianca(), pagina.isRevisado());
    }

    public DigitalizacaoReviewAuditSnapshot reviewAudit(Long paginaId, boolean revisado, String tipoPeca) {
        return new DigitalizacaoReviewAuditSnapshot(paginaId, revisado, tipoPeca, revisado ? "REVISADO" : "PENDENTE");
    }

    @Transactional(readOnly = true)
    public DigitalizacaoJobAuditSnapshot jobAudit(Long jobId) {
        DigitalizacaoJob job = loadJob(jobId);
        return new DigitalizacaoJobAuditSnapshot(job.getId(), job.getStatus(), job.getPaginasProcessadas(), job.getConfiancaMedia());
    }

    @Transactional(readOnly = true)
    public DigitalizacaoPageAuditSnapshot pageAudit(DigitalizacaoPageConsultaCommand command) {
        Objects.requireNonNull(command);
        DigitalizacaoPagina pagina = loadPagina(command.paginaId());
        return new DigitalizacaoPageAuditSnapshot(
                pagina.getId(),
                pagina.getNumeroPagina(),
                pagina.getTipoPeca(),
                pagina.getConfianca(),
                pagina.isRevisado());
    }

    @Transactional(readOnly = true)
    public DigitalizacaoPageConsultaResult pageConsulta(DigitalizacaoPageConsultaCommand command) {
        Objects.requireNonNull(command);
        return new DigitalizacaoPageConsultaResult(pageView(command.paginaId()), pageSnapshot(command.paginaId()));
    }

    @Transactional(readOnly = true)
    public DigitalizacaoConsultaResult jobConsulta(com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoJobConsultaCommand command) {
        Objects.requireNonNull(command);
        return consultar(new DigitalizacaoConsultaCommand(command.jobId()));
    }

    @Transactional(readOnly = true)
    public DigitalizacaoQueueQueryResult queueQuery(DigitalizacaoQueueQueryCommand command) {
        Objects.requireNonNull(command);
        int limit = Math.max(1, command.limit());
        List<DigitalizacaoReviewQueueEntry> entries = jobRepository.findReviewQueue(PageRequest.of(0, limit)).stream()
                .limit(limit)
                .map(job -> new DigitalizacaoReviewQueueEntry(job.getId(), job.getPaginasProcessadas(), job.getCreatedAt()))
                .toList();
        return new DigitalizacaoQueueQueryResult(entries, entries.size());
    }

    @Transactional(readOnly = true)
    public DigitalizacaoJobHealthSnapshot health(Long jobId) {
        DigitalizacaoJob job = loadJob(jobId);
        return new DigitalizacaoJobHealthSnapshot(
                job.getId(),
                job.getStatus(),
                job.isRevisaoRequerida(),
                job.getConfiancaMedia() == null ? 0.0 : job.getConfiancaMedia().doubleValue());
    }

    @Transactional(readOnly = true)
    public List<DigitalizacaoPendingReviewView> pendingReview(DigitalizacaoPendingReviewQuery query) {
        Objects.requireNonNull(query);
        return paginaRepository.findByJobIdOrderByNumeroPaginaAsc(query.jobId()).stream()
                .filter(pagina -> !pagina.isRevisado())
                .map(pagina -> new DigitalizacaoPendingReviewView(pagina.getId(), pagina.getNumeroPagina(), pagina.getTipoPeca()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DigitalizacaoStatusSnapshot statusSnapshot(Long jobId) {
        DigitalizacaoJob job = loadJob(jobId);
        return new DigitalizacaoStatusSnapshot(job.getId(), job.getStatus(), job.isRevisaoRequerida());
    }

    public OcrExecutionAuditSnapshot executionAudit(Long jobId, int totalPaginas) {
        return new OcrExecutionAuditSnapshot(jobId, totalPaginas, Instant.now());
    }

    @Transactional(readOnly = true)
    public DigitalizacaoClassificationAuditSnapshot classificationAudit(Long paginaId) {
        DigitalizacaoPagina pagina = loadPagina(paginaId);
        return new DigitalizacaoClassificationAuditSnapshot(pagina.getId(), pagina.getTipoPeca(), pagina.getConfianca());
    }

    @Transactional(readOnly = true)
    public DigitalizacaoHealthResult health(DigitalizacaoHealthQuery query) {
        Objects.requireNonNull(query);
        return new DigitalizacaoHealthResult(health(query.jobId()), statusSnapshot(query.jobId()));
    }

    @Transactional(readOnly = true)
    public DigitalizacaoJobWindowSnapshot jobWindow(Long jobId) {
        DigitalizacaoJob job = loadJob(jobId);
        return new DigitalizacaoJobWindowSnapshot(job.getId(), job.getStartedAt(), job.getCompletedAt(), nullSafe(job.getTotalPaginas()));
    }

    @Transactional(readOnly = true)
    public DigitalizacaoReviewHealthSnapshot reviewHealth(Long jobId) {
        DigitalizacaoJob job = loadJob(jobId);
        long pendentes = paginaRepository.countByJobIdAndRevisadoFalse(jobId);
        return new DigitalizacaoReviewHealthSnapshot(jobId, (int) pendentes, job.isRevisaoRequerida());
    }

    public OcrEngineHealthSnapshot engineHealth() {
        return new OcrEngineHealthSnapshot(resolveEngine(null), properties.enabled(), properties.idioma());
    }

    private DigitalizacaoJob loadJob(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job não encontrado: " + jobId));
    }

    private DigitalizacaoPagina loadPagina(Long paginaId) {
        return paginaRepository.findById(paginaId)
                .orElseThrow(() -> new IllegalArgumentException("Página não encontrada: " + paginaId));
    }

    private String normalizeIdioma(String idioma) {
        String value = idioma == null || idioma.isBlank() ? properties.idiomaDefault() : idioma.trim();
        return value.toLowerCase(Locale.ROOT);
    }

    private String resolveEngine(DigitalizacaoJob job) {
        if (job != null && job.getOcrEngine() != null && !job.getOcrEngine().isBlank()) {
            return job.getOcrEngine();
        }
        return properties.enabled() ? "TESSERACT_5" : "OCR_MOCK";
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }
}
