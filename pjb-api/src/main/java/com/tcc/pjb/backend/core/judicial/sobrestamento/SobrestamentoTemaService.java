package com.tcc.pjb.backend.core.judicial.sobrestamento;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.*;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.judicial.SobrestamentoTema;
import com.tcc.pjb.backend.model.entity.judicial.TemaRepercussaoGeral;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.SobrestamentoTemaRepository;
import com.tcc.pjb.backend.model.repository.TemaRepercussaoGeralRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionDescriptor;
import com.tcc.pjb.backend.platform.runtime.execution.PjbTransactionalExecutionSupport;

@Service
public class SobrestamentoTemaService {
    private static final int BATCH_SIZE = 200;
    private static final Duration SOBRESTAMENTO_BATCH_TIMEOUT = Duration.ofMinutes(3);

    private final ProcessoRepository processoRepository;
    private final TemaRepercussaoGeralRepository temaRepository;
    private final SobrestamentoTemaRepository sobrestamentoRepository;
    private final AuditLedgerService auditLedger;
    private final Counter sobrestadosCounter;
    private final Counter retomadosCounter;
    private final ReadAfterWriteConsistencyPolicy readAfterWriteConsistencyPolicy;
    private final PjbTransactionalExecutionSupport transactionalExecutionSupport;

    public SobrestamentoTemaService(ProcessoRepository processoRepository,
                                    TemaRepercussaoGeralRepository temaRepository,
                                    SobrestamentoTemaRepository sobrestamentoRepository,
                                    AuditLedgerService auditLedger,
                                    MeterRegistry meterRegistry,
                                    ReadAfterWriteConsistencyPolicy readAfterWriteConsistencyPolicy,
                                    PjbTransactionalExecutionSupport transactionalExecutionSupport) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.temaRepository = Objects.requireNonNull(temaRepository);
        this.sobrestamentoRepository = Objects.requireNonNull(sobrestamentoRepository);
        this.auditLedger = Objects.requireNonNull(auditLedger);
        this.sobrestadosCounter = Counter.builder("pjb.sobrestamento.processos").tag("operacao", "sobrestado").register(meterRegistry);
        this.retomadosCounter = Counter.builder("pjb.sobrestamento.processos").tag("operacao", "retomado").register(meterRegistry);
        this.readAfterWriteConsistencyPolicy = Objects.requireNonNull(readAfterWriteConsistencyPolicy);
        this.transactionalExecutionSupport = Objects.requireNonNull(transactionalExecutionSupport);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "sobrestamento.tema.persist-batch", maxMillis = 4000, critical = true)
    public SobrestamentoTemaResult sobrestar(SobrestamentoTemaCommand command) {
        Objects.requireNonNull(command);
        int total = processarSobrestamentoBatch(command.codigoTema());
        return new SobrestamentoTemaResult(command.codigoTema(), total, Instant.now());
    }

    public void sobrestamentoBatch(String codigoTema) {
        try {
            var future = transactionalExecutionSupport.runInTransaction(
                    PjbExecutionDescriptor.job("sobrestamento.tema.batch.sobrestar", SOBRESTAMENTO_BATCH_TIMEOUT),
                    () -> processarSobrestamentoBatch(codigoTema)
            );
            if (future == null) {
                processarSobrestamentoBatch(codigoTema);
            } else {
                future.join();
            }
        } catch (RuntimeException ex) {
            processarSobrestamentoBatch(codigoTema);
        }
    }

    @Transactional
    @PjbTransactionalBudget(operation = "sobrestamento.tema.retomada-batch", maxMillis = 4000, critical = true)
    public SobrestamentoRetomadaResult retomar(SobrestamentoRetomadaCommand command) {
        Objects.requireNonNull(command);
        int total = processarRetomadaBatch(command.codigoTema(), command.resultado());
        return new SobrestamentoRetomadaResult(command.codigoTema(), command.resultado(), total, Instant.now());
    }

    public void retomadaBatch(String codigoTema, String resultado) {
        try {
            var future = transactionalExecutionSupport.runInTransaction(
                    PjbExecutionDescriptor.job("sobrestamento.tema.batch.retomar", SOBRESTAMENTO_BATCH_TIMEOUT),
                    () -> processarRetomadaBatch(codigoTema, resultado)
            );
            if (future == null) {
                processarRetomadaBatch(codigoTema, resultado);
            } else {
                future.join();
            }
        } catch (RuntimeException ex) {
            processarRetomadaBatch(codigoTema, resultado);
        }
    }

    @Transactional(readOnly = true)
    public SobrestamentoTemaConsultaResult consultar(SobrestamentoTemaConsultaCommand command) {
        Objects.requireNonNull(command);
        TemaRepercussaoGeral tema = loadTema(command.codigoTema());
        return new SobrestamentoTemaConsultaResult(command.codigoTema(), tema.getProcessosSobrestados() == null ? 0L : tema.getProcessosSobrestados().longValue(), tema.getStatus());
    }

    @Transactional(readOnly = true)
    public SobrestamentoTemaTimelineResult timeline(String codigoTema) {
        TemaRepercussaoGeral tema = loadTema(codigoTema);
        List<SobrestamentoTema> eventos = sobrestamentoRepository.findTop100ByTemaIdOrderBySobrestadoEmDesc(tema.getId());
        List<SobrestamentoTemaTimelineEntry> entries = new ArrayList<>();
        entries.add(new SobrestamentoTemaTimelineEntry("TEMA", Instant.now(), codigoTema));
        for (SobrestamentoTema evento : eventos) {
            entries.add(new SobrestamentoTemaTimelineEntry("SOBRESTADO", evento.getSobrestadoEm(), "processo=" + evento.getProcessoId()));
            if (evento.getRetomadoEm() != null) {
                entries.add(new SobrestamentoTemaTimelineEntry("RETOMADO", evento.getRetomadoEm(), String.valueOf(evento.getResultadoAplicado())));
            }
        }
        return new SobrestamentoTemaTimelineResult(codigoTema, List.copyOf(entries));
    }

    @Transactional(readOnly = true)
    public SobrestamentoTemaCompatibilidadeSnapshot compatibilidade(Long processoId, String codigoTema) {
        TemaRepercussaoGeral tema = loadTema(codigoTema);
        var processo = processoRepository.findById(processoId).orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));
        return new SobrestamentoTemaCompatibilidadeSnapshot(processoId, processo.getClasseTpuCodigo(), codigoTema, compativel(processo.getClasseTpuCodigo(), tema));
    }

    @Transactional(readOnly = true)
    public SobrestamentoTemaHealthResult health(SobrestamentoTemaHealthQuery query) {
        Objects.requireNonNull(query);
        TemaRepercussaoGeral tema = temaRepository.findByCodigoIgnoreCase(query.codigoTema()).orElse(null);
        long pendentes = tema == null ? 0L : sobrestamentoRepository.countByTemaIdAndRetomadoEmIsNull(tema.getId());
        return new SobrestamentoTemaHealthResult(query.codigoTema(), tema != null, pendentes, Instant.now());
    }

    @Transactional(readOnly = true)
    public SobrestamentoTemaStatusView statusView(String codigoTema) {
        TemaRepercussaoGeral tema = loadTema(codigoTema);
        return new SobrestamentoTemaStatusView(codigoTema, tema.getProcessosSobrestados() == null ? 0L : tema.getProcessosSobrestados().longValue());
    }

    @Transactional(readOnly = true)
    public SobrestamentoTemaHealthView healthView(String codigoTema) {
        TemaRepercussaoGeral tema = loadTema(codigoTema);
        long pendentes = sobrestamentoRepository.countByTemaIdAndRetomadoEmIsNull(tema.getId());
        return new SobrestamentoTemaHealthView(codigoTema, true, pendentes);
    }

    @Transactional(readOnly = true)
    public SobrestamentoRetomadaHealthView retomadaHealthView(String codigoTema, String resultado) {
        TemaRepercussaoGeral tema = loadTema(codigoTema);
        long retomados = sobrestamentoRepository.countByTemaIdAndRetomadoEmIsNotNull(tema.getId());
        return new SobrestamentoRetomadaHealthView(codigoTema, resultado, retomados);
    }

    @Transactional(readOnly = true)
    public SobrestamentoTemaWindowResult window(SobrestamentoTemaWindowQuery query) {
        Objects.requireNonNull(query);
        return new SobrestamentoTemaWindowResult(query.codigoTema(), BATCH_SIZE, Instant.now());
    }

    @Transactional(readOnly = true)
    public SobrestamentoTemaAuditView auditView(String codigoTema, String evento) {
        return new SobrestamentoTemaAuditView(codigoTema, evento, Instant.now());
    }

    @Transactional(readOnly = true)
    public SobrestamentoTemaProjection projection(String codigoTema) {
        TemaRepercussaoGeral tema = loadTema(codigoTema);
        return new SobrestamentoTemaProjection(codigoTema, tema.getTeseFirmada(), tema.getProcessosSobrestados());
    }

    @Transactional(readOnly = true)
    public SobrestamentoTemaDecisionView decisionView(String codigoTema, String classeTpuCodigo) {
        TemaRepercussaoGeral tema = loadTema(codigoTema);
        return new SobrestamentoTemaDecisionView(codigoTema, compativel(classeTpuCodigo, tema), classeTpuCodigo);
    }

    @Transactional(readOnly = true)
    public SobrestamentoWindowHealthResult windowHealth(SobrestamentoWindowHealthQuery query) {
        Objects.requireNonNull(query);
        TemaRepercussaoGeral tema = loadTema(query.codigoTema());
        long pendentes = sobrestamentoRepository.countByTemaIdAndRetomadoEmIsNull(tema.getId());
        return new SobrestamentoWindowHealthResult(query.codigoTema(), query.resultado(), pendentes < BATCH_SIZE, "pendentes=" + pendentes);
    }

    @Transactional(readOnly = true)
    public SobrestamentoTimelineAuditView timelineAuditView(String codigoTema) {
        return new SobrestamentoTimelineAuditView(codigoTema, "TIMELINE", Instant.now());
    }

    @Transactional(readOnly = true)
    public SobrestamentoStatusHealthView statusHealthView(String codigoTema) {
        return new SobrestamentoStatusHealthView(codigoTema, statusView(codigoTema).totalSobrestados() > 0 ? "ATIVO" : "SEM_REGISTRO", Instant.now());
    }

    @Transactional(readOnly = true)
    public SobrestamentoConsistencyView consistencyView(String codigoTema) {
        TemaRepercussaoGeral tema = loadTema(codigoTema);
        long pendentes = sobrestamentoRepository.countByTemaIdAndRetomadoEmIsNull(tema.getId());
        long total = tema.getProcessosSobrestados() == null ? 0L : tema.getProcessosSobrestados().longValue();
        boolean consistent = total >= pendentes;
        return new SobrestamentoConsistencyView(codigoTema, consistent, "total=" + total + " pendentes=" + pendentes, "pjb_sobrestamento_tema");
    }

    @Transactional(readOnly = true)
    public SobrestamentoDecisionHealthResult decisionHealth(SobrestamentoDecisionHealthQuery query) {
        Objects.requireNonNull(query);
        TemaRepercussaoGeral tema = loadTema(query.reference());
        long total = tema.getProcessosSobrestados() == null ? 0L : tema.getProcessosSobrestados().longValue();
        return new SobrestamentoDecisionHealthResult(total > 0, "sobrestados=" + total, total);
    }

    @Transactional(readOnly = true)
    public SobrestamentoProjectionAuditView projectionAuditView(String codigoTema) {
        SobrestamentoTemaProjection projection = projection(codigoTema);
        return new SobrestamentoProjectionAuditView(codigoTema, projection.processosSobrestados() == null ? "SEM_DADOS" : "PROJETADO", String.valueOf(projection.teseFirmada()));
    }

    @Transactional(readOnly = true)
    public SobrestamentoWindowAuditView windowAuditView(String codigoTema) {
        return new SobrestamentoWindowAuditView(codigoTema, "WINDOW", "batch=" + BATCH_SIZE);
    }

    @Transactional(readOnly = true)
    public SobrestamentoTemaStatusView status(SobrestamentoTemaStatusQuery query) {
        Objects.requireNonNull(query);
        return statusView(query.reference());
    }

    @Transactional(readOnly = true)
    public SobrestamentoBudgetView budgetView(String codigoTema) {
        TemaRepercussaoGeral tema = loadTema(codigoTema);
        long pendentes = sobrestamentoRepository.countByTemaIdAndRetomadoEmIsNull(tema.getId());
        return new SobrestamentoBudgetView(codigoTema, pendentes >= BATCH_SIZE ? "ALTO" : "OK", "pendentes=" + pendentes);
    }

    @Transactional(readOnly = true)
    public SobrestamentoDecisionAuditView decisionAuditView(String codigoTema, String classeTpuCodigo) {
        SobrestamentoTemaDecisionView decision = decisionView(codigoTema, classeTpuCodigo);
        return new SobrestamentoDecisionAuditView(codigoTema, decision.compativel() ? "COMPATIVEL" : "INCOMPATIVEL", classeTpuCodigo);
    }

    @Transactional(readOnly = true)
    public SobrestamentoProjectionHealthResult projectionHealth(SobrestamentoProjectionHealthQuery query) {
        Objects.requireNonNull(query);
        TemaRepercussaoGeral tema = loadTema(query.reference());
        long total = tema.getProcessosSobrestados() == null ? 0L : tema.getProcessosSobrestados().longValue();
        return new SobrestamentoProjectionHealthResult(true, "sobrestados=" + total, total);
    }

    @Transactional(readOnly = true)
    public SobrestamentoTemaWindowHealthView windowHealthView(String codigoTema) {
        return new SobrestamentoTemaWindowHealthView(codigoTema, "WINDOW", "batch=" + BATCH_SIZE);
    }

    @Transactional(readOnly = true)
    public SobrestamentoAuditHealthView auditHealthView(String codigoTema) {
        TemaRepercussaoGeral tema = loadTema(codigoTema);
        long sobrestados = sobrestamentoRepository.countByTemaIdAndRetomadoEmIsNull(tema.getId());
        long retomados = sobrestamentoRepository.countByTemaIdAndRetomadoEmIsNotNull(tema.getId());
        return new SobrestamentoAuditHealthView(codigoTema, sobrestados, retomados, true);
    }

    @Transactional(readOnly = true)
    public SobrestamentoTimelineHealthView timelineHealthView(String codigoTema) {
        SobrestamentoTemaTimelineResult timeline = timeline(codigoTema);
        return new SobrestamentoTimelineHealthView(codigoTema, timeline.entries().size(), true, "eventos=" + timeline.entries().size());
    }

    @Transactional(readOnly = true)
    public SobrestamentoStatusEnvelopeView statusEnvelopeView(String codigoTema) {
        SobrestamentoTemaStatusView status = statusView(codigoTema);
        return new SobrestamentoStatusEnvelopeView("SOBRESTAMENTO_STATUS", status.totalSobrestados() > 0 ? "ATIVO" : "SEM_REGISTRO", codigoTema, Instant.now(), null);
    }

    @Transactional(readOnly = true)
    public SobrestamentoSignalView signalView(String codigoTema) {
        SobrestamentoTemaStatusView status = statusView(codigoTema);
        return new SobrestamentoSignalView("SOBRESTAMENTO_SIGNAL", status.totalSobrestados() > 0 ? "ATIVO" : "SEM_REGISTRO", codigoTema, Instant.now(), null);
    }

    @Transactional(readOnly = true)
    public SobrestamentoOwnerView ownerView(String codigoTema) {
        TemaRepercussaoGeral tema = loadTema(codigoTema);
        return new SobrestamentoOwnerView("SOBRESTAMENTO_OWNER", tema.getStatus(), codigoTema, Instant.now(), tema.getId());
    }

    @Transactional(readOnly = true)
    public SobrestamentoWindowView windowView(String codigoTema) {
        return new SobrestamentoWindowView("SOBRESTAMENTO_WINDOW", "OK", "batch=" + BATCH_SIZE, Instant.now(), null);
    }

    @Transactional(readOnly = true)
    public SobrestamentoAuditEntryView auditEntryView(String codigoTema) {
        TemaRepercussaoGeral tema = loadTema(codigoTema);
        return new SobrestamentoAuditEntryView("SOBRESTAMENTO_AUDIT", tema.getStatus(), codigoTema, Instant.now(), tema.getId());
    }

    private int processarSobrestamentoBatch(String codigoTema) {
        TemaRepercussaoGeral tema = loadTema(codigoTema);
        List<Processo> elegiveis = collectEligibleProcesses(tema);
        int total = 0;
        for (Processo processo : elegiveis) {
            Long processoId = processo.getId();
            if (sobrestamentoRepository.existsByProcessoIdAndTemaId(processoId, tema.getId())) {
                continue;
            }
            SobrestamentoTema registro = SobrestamentoTema.builder()
                    .processoId(processoId)
                    .temaId(tema.getId())
                    .statusAnterior(processo.getStatusProcesso().name())
                    .sobrestadoEm(Instant.now())
                    .build();
            sobrestamentoRepository.save(registro);
            processo.setStatusProcesso(StatusProcesso.SUSPENSO_TEMA_REPERCUSSAO);
            processoRepository.save(processo);
            sobrestadosCounter.increment();
            total++;
        }
        if (total > 0) {
            tema.setProcessosSobrestados((tema.getProcessosSobrestados() == null ? 0 : tema.getProcessosSobrestados()) + total);
            temaRepository.save(tema);
            readAfterWriteConsistencyPolicy.markWrite();
        }
        auditLedger.appendSafely("SOBRESTAMENTO_BATCH", "TEMA", codigoTema, "total=" + total);
        return total;
    }

    private int processarRetomadaBatch(String codigoTema, String resultado) {
        TemaRepercussaoGeral tema = loadTema(codigoTema);
        List<SobrestamentoTema> pendentes = sobrestamentoRepository.findByTemaIdAndRetomadoEmIsNull(tema.getId());
        int total = 0;
        for (SobrestamentoTema sobrestamento : pendentes) {
            sobrestamento.setRetomadoEm(Instant.now());
            sobrestamento.setResultadoAplicado(resultado);
            sobrestamentoRepository.save(sobrestamento);
            processoRepository.findById(sobrestamento.getProcessoId()).ifPresent(processo -> {
                StatusProcesso statusAnterior = StatusProcesso.fromString(sobrestamento.getStatusAnterior());
                processo.setStatusProcesso(statusAnterior != null ? statusAnterior : StatusProcesso.EM_ANDAMENTO);
                processoRepository.save(processo);
                retomadosCounter.increment();
            });
            total++;
        }
        if (total > 0) {
            readAfterWriteConsistencyPolicy.markWrite();
        }
        auditLedger.appendSafely("RETOMADA_BATCH", "TEMA", codigoTema, "total=" + total + " resultado=" + resultado);
        return total;
    }

    private List<Processo> collectEligibleProcesses(TemaRepercussaoGeral tema) {
        List<Processo> processos = new ArrayList<>();
        int page = 0;
        while (true) {
            var slice = processoRepository.findAllForPrazoScan(List.of(StatusProcesso.ARQUIVADO, StatusProcesso.TRANSITO_EM_JULGADO, StatusProcesso.SUSPENSO_TEMA_REPERCUSSAO), PageRequest.of(page, BATCH_SIZE));
            if (!slice.hasContent()) {
                break;
            }
            for (var processo : slice.getContent()) {
                if (compativel(processo.getClasseTpuCodigo(), tema) && !sobrestamentoRepository.existsByProcessoIdAndTemaId(processo.getId(), tema.getId())) {
                    processos.add(processo);
                }
            }
            if (!slice.hasNext()) {
                break;
            }
            page++;
        }
        return processos;
    }

    private TemaRepercussaoGeral loadTema(String codigoTema) {
        return temaRepository.findByCodigoIgnoreCase(codigoTema).orElseThrow(() -> new IllegalArgumentException("Tema não encontrado: " + codigoTema));
    }

    private boolean compativel(String classeTpuCodigo, TemaRepercussaoGeral tema) {
        return classeTpuCodigo != null && !classeTpuCodigo.isBlank() && tema != null;
    }
}
