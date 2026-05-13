package com.tcc.pjb.backend.core.processo.producao.application;

import com.tcc.pjb.backend.core.processo.integracao.application.ProcessoIntegracaoApplicationService;
import com.tcc.pjb.backend.core.processo.integracao.domain.ProcessoIntegracaoAggregate;
import com.tcc.pjb.backend.core.processo.operacao.application.ProcessoOperacaoApplicationService;
import com.tcc.pjb.backend.core.processo.operacao.domain.ProcessoOperacaoAggregate;
import com.tcc.pjb.backend.core.processo.producao.domain.ProcessoOperacaoControle;
import com.tcc.pjb.backend.core.processo.producao.domain.ProcessoOperacaoTransversalAggregate;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoOperacaoTransversalApplicationService {

    private final ProcessoRepository processoRepository;
    private final ProcessoOperacaoApplicationService processoOperacaoApplicationService;
    private final ProcessoIntegracaoApplicationService processoIntegracaoApplicationService;

    public ProcessoOperacaoTransversalApplicationService(ProcessoRepository processoRepository,
                                                         ProcessoOperacaoApplicationService processoOperacaoApplicationService,
                                                         ProcessoIntegracaoApplicationService processoIntegracaoApplicationService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.processoOperacaoApplicationService = Objects.requireNonNull(processoOperacaoApplicationService);
        this.processoIntegracaoApplicationService = Objects.requireNonNull(processoIntegracaoApplicationService);
    }

    public ProcessoOperacaoTransversalAggregate detalhar(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        ProcessoOperacaoAggregate operacao = processoOperacaoApplicationService.detalhar(processoId);
        ProcessoIntegracaoAggregate integracao = processoIntegracaoApplicationService.detalhar(processoId);

        ArrayList<ProcessoOperacaoControle> controles = new ArrayList<>();
        controles.add(controle("IDEMPOTENCIA", "Idempotência transversal", operacao.totalBloqueios() == 0 ? "ATIVA" : "OBSERVAR", operacao.totalBloqueios() == 0 ? 95d : 78d,
                List.of("ProcessualOperationGuardService", "ActionIdempotencyService", "uk_job_type_idem"),
                List.of("PADRONIZAR_CHAVES_DE_ESCOPOS_SENSIVEIS")));
        controles.add(controle("RETRY_OUTBOX", "Retry e outbox", integracao.canais().isEmpty() ? "PROVISIONADO" : "ATIVO", integracao.canais().isEmpty() ? 72d : 90d,
                List.of("service/outbox", "connectorSubmissionAttempts", "connectorSyncAttempts"),
                List.of("UNIFICAR_POLITICA_DE_RETRY", "AMPLIAR_OUTBOX_AOS_CONECTORES_REMANESCENTES")));
        controles.add(controle("CORRELACAO_TRACING", "Correlação e tracing", "ATIVO", "STABLE".equalsIgnoreCase(operacao.observabilityState()) ? 92d : 76d,
                List.of("observabilityState=" + operacao.observabilityState(), "trilhaConnector=" + integracao.trilhaConnector()),
                List.of("ANEXAR_CORRELATION_ID_A_TODAS_AS_PONTAS_EXTERNAS")));
        controles.add(controle("CIRCUIT_BREAKER", "Circuit breaker e degradação graciosa", integracao.alertas().isEmpty() ? "PRONTO" : "ATENCAO", integracao.alertas().isEmpty() ? 88d : 68d,
                List.of("core/resilience", "NationalContingencyOrchestratorService", "alertasIntegracao=" + integracao.alertas().size()),
                List.of("PADRONIZAR_DEGRADACAO_POR_CANAL", "MATERIALIZAR_FAILOVER_DE_CONECTOR")));
        controles.add(controle("RATE_LIMIT_BACKPRESSURE", "Rate limit e backpressure", operacao.saturacaoMaxima() < 65d ? "CONTROLADO" : "ATENCAO", operacao.saturacaoMaxima() < 65d ? 86d : 63d,
                List.of("core/ratelimit", "saturacaoMaxima=" + operacao.saturacaoMaxima()),
                List.of("CALIBRAR_FILAS_QUENTES", "ENDURECER_LIMITES_FINOS_POR_EIXO")));
        controles.add(controle("REPLAY_FORENSE", "Replay controlado e trilha forense", integracao.proximasAcoes().isEmpty() ? "ATIVO" : "OBSERVAR", integracao.proximasAcoes().isEmpty() ? 90d : 74d,
                List.of("connector replay", "ledger", "audit trail"),
                List.of("UNIFICAR_REPLAY_CONTROLADO", "ENDURECER_FORENSE_EM_CADA_REPROCESSAMENTO")));
        controles.add(controle("AUDITORIA_IMUTAVEL", "Auditoria imutável", "ATIVO", 94d,
                List.of("core/audit/ledger", "core/audit/cross", "hardening-final"),
                List.of("PRESERVAR_HASH_E_CHAIN_DE_EVENTOS")));

        double coberturaGlobal = round(controles.stream().mapToDouble(ProcessoOperacaoControle::cobertura).average().orElse(0d));
        LinkedHashSet<String> alertas = new LinkedHashSet<>(operacao.alertas());
        if (operacao.saturacaoMaxima() > 70d) {
            alertas.add("Saturação operacional alta pode exigir backpressure e reescalonamento fino.");
        }
        if (!integracao.alertas().isEmpty()) {
            alertas.add("Integração externa ainda emite alertas que reduzem a prontidão transversal.");
        }
        LinkedHashSet<String> proximasAcoes = new LinkedHashSet<>(operacao.acoesImediatas());
        controles.stream().flatMap(item -> item.proximasAcoes().stream()).forEach(proximasAcoes::add);
        String readiness = coberturaGlobal >= 90d && operacao.totalBloqueios() == 0 ? "READY" : coberturaGlobal >= 75d ? "HARDENING_EM_ANDAMENTO" : "NOT_READY";
        return new ProcessoOperacaoTransversalAggregate(
                processoId,
                processo.getNumeroProcesso(),
                readiness,
                coberturaGlobal,
                operacao.saturacaoMaxima(),
                controles,
                List.copyOf(alertas),
                List.copyOf(proximasAcoes),
                Instant.now()
        );
    }

    private ProcessoOperacaoControle controle(String codigo,
                                              String titulo,
                                              String estado,
                                              double cobertura,
                                              List<String> evidencias,
                                              List<String> proximasAcoes) {
        return new ProcessoOperacaoControle(codigo, titulo, estado, round(cobertura), evidencias, proximasAcoes);
    }

    private double round(double value) {
        return Math.round(value * 100d) / 100d;
    }
}
