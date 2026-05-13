package com.tcc.pjb.backend.core.processo.transicao.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbFechamentoStatus;
import com.tcc.pjb.backend.core.processo.migracao.application.ProcessoMigracaoFactoryApplicationService;
import com.tcc.pjb.backend.core.processo.migracao.domain.ProcessoMigracaoFabricaAggregate;
import com.tcc.pjb.backend.core.processo.producao.application.ProcessoProducaoPesadaApplicationService;
import com.tcc.pjb.backend.core.processo.producao.domain.ProcessoProducaoPesadaAggregate;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaSupportBridge;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoRuntimePreparationApplicationService;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoRuntimeResolver;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimeContext;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimePreparationAggregate;
import com.tcc.pjb.backend.core.processo.transicao.domain.ProcessoConvivenciaTransicaoAggregate;
import com.tcc.pjb.backend.core.processo.transicao.domain.ProcessoConvivenciaTransicaoTrack;
import com.tcc.pjb.backend.core.util.Hashes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoConvivenciaTransicaoApplicationService {

    private final ProcessoRuntimeResolver processoRuntimeResolver;
    private final ProcessoRuntimePreparationApplicationService processoRuntimePreparationApplicationService;
    private final ProcessoProducaoPesadaApplicationService processoProducaoPesadaApplicationService;
    private final ProcessoMigracaoFactoryApplicationService processoMigracaoFactoryApplicationService;
    private final ProcessoMalhaSupportBridge processoMalhaSupportBridge;

    public ProcessoConvivenciaTransicaoApplicationService(ProcessoRuntimeResolver processoRuntimeResolver,
                                                          ProcessoRuntimePreparationApplicationService processoRuntimePreparationApplicationService,
                                                          ProcessoProducaoPesadaApplicationService processoProducaoPesadaApplicationService,
                                                          ProcessoMigracaoFactoryApplicationService processoMigracaoFactoryApplicationService,
                                                          ProcessoMalhaSupportBridge processoMalhaSupportBridge) {
        this.processoRuntimeResolver = Objects.requireNonNull(processoRuntimeResolver);
        this.processoRuntimePreparationApplicationService = Objects.requireNonNull(processoRuntimePreparationApplicationService);
        this.processoProducaoPesadaApplicationService = Objects.requireNonNull(processoProducaoPesadaApplicationService);
        this.processoMigracaoFactoryApplicationService = Objects.requireNonNull(processoMigracaoFactoryApplicationService);
        this.processoMalhaSupportBridge = Objects.requireNonNull(processoMalhaSupportBridge);
    }

    @Transactional(readOnly = true)
    public ProcessoConvivenciaTransicaoAggregate planejar(Long processoId) {
        ProcessoRuntimeContext contexto = processoRuntimeResolver.resolver(processoId);
        ProcessoRuntimePreparationAggregate runtime = processoRuntimePreparationApplicationService.avaliar(contexto);
        ProcessoProducaoPesadaAggregate producao = processoProducaoPesadaApplicationService.avaliar(processoId);
        ProcessoMigracaoFabricaAggregate migracao = processoMigracaoFactoryApplicationService.planejar(processoId);
        ProcessoConvivenciaTransicaoTrack shadow = track("transicao.shadow", "Shadow mode por tribunal e ramo", runtime.prontoParaMalhaCompleta() && migracao.scoreGeral() >= 70, runtime.prontoParaMalhaCompleta() ? 82 : 54, true, "shadow-mode", "comparacao de distribuição, sigilo e timeline", List.of("runtime=" + runtime.prontoParaMalhaCompleta(), "migracaoScore=" + migracao.scoreGeral()));
        ProcessoConvivenciaTransicaoTrack equivalencia = track("transicao.equivalencia", "Equivalência funcional", producao.scoreGeral() >= 75 && migracao.scoreGeral() >= 75, Math.min(100, (producao.scoreGeral() + migracao.scoreGeral()) / 2), false, "espelhamento funcional", "hash por movimento, documento e fila", List.of("producaoScore=" + producao.scoreGeral(), "migracaoScore=" + migracao.scoreGeral()));
        ProcessoConvivenciaTransicaoTrack reversibilidade = track("transicao.reversibilidade", "Reversibilidade controlada", runtime.integrationStatus().auditLedgerDisponivel() && runtime.integrationStatus().outboxDisponivel(), runtime.integrationStatus().auditLedgerDisponivel() && runtime.integrationStatus().outboxDisponivel() ? 80 : 48, true, "dual-write controlado", "rollback com checkpoint institucional", List.of("audit=" + runtime.integrationStatus().auditLedgerDisponivel(), "outbox=" + runtime.integrationStatus().outboxDisponivel()));
        List<ProcessoConvivenciaTransicaoTrack> tracks = List.of(shadow, equivalencia, reversibilidade);
        int scoreGeral = (int) Math.round(tracks.stream().mapToInt(ProcessoConvivenciaTransicaoTrack::score).average().orElse(0));
        PjbFechamentoStatus statusGeral = scoreGeral >= 85 && tracks.stream().allMatch(track -> track.status() == PjbFechamentoStatus.CONCLUIDA) ? PjbFechamentoStatus.CONCLUIDA : scoreGeral >= 60 ? PjbFechamentoStatus.PARCIAL : PjbFechamentoStatus.BLOQUEADA;
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("tribunalPiloto=" + contexto.tribunal());
        fundamentos.add("ramoPiloto=" + (contexto.ramoDireito() == null ? "NAO_INFORMADO" : contexto.ramoDireito().name()));
        fundamentos.add("shadowReady=" + (shadow.status() == PjbFechamentoStatus.CONCLUIDA));
        fundamentos.add("equivalenciaReady=" + (equivalencia.status() == PjbFechamentoStatus.CONCLUIDA));
        fundamentos.add("reversibilidadeReady=" + (reversibilidade.status() == PjbFechamentoStatus.CONCLUIDA));
        DecisionTraceService trace = processoMalhaSupportBridge.decisionTraceService();
        if (trace != null) {
            trace.record("plataforma.convivencia.transicao", "Processo", String.valueOf(processoId), BigDecimal.valueOf(scoreGeral), tracks.toString(), fundamentos.toString(), Hashes.sha256Hex(contexto.numeroReferencia()), Hashes.sha256Hex(tracks.toString()), "PJB-TRANSICAO", statusGeral.name());
        }
        AuditLedgerService audit = processoMalhaSupportBridge.auditLedgerService();
        if (audit != null) {
            audit.appendSafely("PJB_TRANSICAO_PLANEJADA", "Processo", String.valueOf(processoId), Hashes.sha256Hex(tracks.toString()), "status=" + statusGeral.name() + ";score=" + scoreGeral);
        }
        return new ProcessoConvivenciaTransicaoAggregate(
                processoId,
                contexto.numeroReferencia(),
                contexto.tribunal(),
                contexto.ramoDireito() == null ? "NAO_INFORMADO" : contexto.ramoDireito().name(),
                tracks,
                scoreGeral,
                statusGeral,
                shadow.status() == PjbFechamentoStatus.CONCLUIDA,
                reversibilidade.status() == PjbFechamentoStatus.CONCLUIDA,
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    private ProcessoConvivenciaTransicaoTrack track(String codigo,
                                                    String titulo,
                                                    boolean concluida,
                                                    int score,
                                                    boolean reversivel,
                                                    String modoExecucao,
                                                    String criterioEquivalencia,
                                                    List<String> fundamentos) {
        PjbFechamentoStatus status = concluida ? PjbFechamentoStatus.CONCLUIDA : score >= 60 ? PjbFechamentoStatus.PARCIAL : PjbFechamentoStatus.PENDENTE;
        return new ProcessoConvivenciaTransicaoTrack(codigo, titulo, status, score, reversivel, modoExecucao, criterioEquivalencia, fundamentos);
    }
}
