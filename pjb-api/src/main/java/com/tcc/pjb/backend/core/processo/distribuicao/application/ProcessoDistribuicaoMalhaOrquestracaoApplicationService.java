package com.tcc.pjb.backend.core.processo.distribuicao.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.distribuicao.DistribuicaoProcessualNacionalEngine;
import com.tcc.pjb.backend.core.distribuicao.DistribuicaoProcessualNacionalEngine.DistribuicaoRequest;
import com.tcc.pjb.backend.core.distribuicao.DistribuicaoProcessualNacionalEngine.DistribuicaoResult;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.processo.anomalia.application.ProcessoAnomaliaGovernancaApplicationService;
import com.tcc.pjb.backend.core.processo.anomalia.domain.ProcessoAnomaliaGovernancaAggregate;
import com.tcc.pjb.backend.core.processo.anomalia.domain.ProcessoAnomaliaMalhaAggregate;
import com.tcc.pjb.backend.core.processo.anomalia.application.ProcessoAnomaliaMalhaApplicationService;
import com.tcc.pjb.backend.core.processo.distribuicao.domain.ProcessoDistribuicaoMalhaAggregate;
import com.tcc.pjb.backend.core.processo.distribuicao.domain.ProcessoDistribuicaoMalhaOrquestracaoAggregate;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaParallelExecutor;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoRuntimeResolver;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimeContext;
import com.tcc.pjb.backend.core.processo.timeline.application.ProcessoTimelineMalhaMaterializacaoApplicationService;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineMalhaMaterializacaoAggregate;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoDistribuicaoMalhaOrquestracaoApplicationService {

    private final ProcessoRuntimeResolver processoRuntimeResolver;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final DistribuicaoProcessualNacionalEngine distribuicaoProcessualNacionalEngine;
    private final ProcessoDistribuicaoMalhaApplicationService processoDistribuicaoMalhaApplicationService;
    private final ProcessoAnomaliaMalhaApplicationService processoAnomaliaMalhaApplicationService;
    private final ProcessoAnomaliaGovernancaApplicationService processoAnomaliaGovernancaApplicationService;
    private final ProcessoTimelineMalhaMaterializacaoApplicationService processoTimelineMalhaMaterializacaoApplicationService;
    private final OutboxPublisher outboxPublisher;
    private final DecisionTraceService decisionTraceService;
    private final AuditLedgerService auditLedgerService;
    private final ProcessoMalhaParallelExecutor processoMalhaParallelExecutor;

    public ProcessoDistribuicaoMalhaOrquestracaoApplicationService(ProcessoRuntimeResolver processoRuntimeResolver,
                                                                   ProcessoRepository processoRepository,
                                                                   WorkItemRepository workItemRepository,
                                                                   DistribuicaoProcessualNacionalEngine distribuicaoProcessualNacionalEngine,
                                                                   ProcessoDistribuicaoMalhaApplicationService processoDistribuicaoMalhaApplicationService,
                                                                   ProcessoAnomaliaMalhaApplicationService processoAnomaliaMalhaApplicationService,
                                                                   ProcessoAnomaliaGovernancaApplicationService processoAnomaliaGovernancaApplicationService,
                                                                   ProcessoTimelineMalhaMaterializacaoApplicationService processoTimelineMalhaMaterializacaoApplicationService,
                                                                   OutboxPublisher outboxPublisher,
                                                                   ObjectProvider<DecisionTraceService> decisionTraceServiceProvider,
                                                                   ObjectProvider<AuditLedgerService> auditLedgerServiceProvider,
                                                                   ProcessoMalhaParallelExecutor processoMalhaParallelExecutor) {
        this.processoRuntimeResolver = Objects.requireNonNull(processoRuntimeResolver);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.distribuicaoProcessualNacionalEngine = Objects.requireNonNull(distribuicaoProcessualNacionalEngine);
        this.processoDistribuicaoMalhaApplicationService = Objects.requireNonNull(processoDistribuicaoMalhaApplicationService);
        this.processoAnomaliaMalhaApplicationService = Objects.requireNonNull(processoAnomaliaMalhaApplicationService);
        this.processoAnomaliaGovernancaApplicationService = Objects.requireNonNull(processoAnomaliaGovernancaApplicationService);
        this.processoTimelineMalhaMaterializacaoApplicationService = Objects.requireNonNull(processoTimelineMalhaMaterializacaoApplicationService);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
        this.decisionTraceService = decisionTraceServiceProvider.getIfAvailable();
        this.auditLedgerService = auditLedgerServiceProvider.getIfAvailable();
        this.processoMalhaParallelExecutor = Objects.requireNonNull(processoMalhaParallelExecutor);
    }

    @Transactional
    public ProcessoDistribuicaoMalhaOrquestracaoAggregate executar(Long processoId) {
        ProcessoRuntimeContext contexto = processoRuntimeResolver.resolver(processoId);
        Processo processo = contexto.processo();
        ProcessoMalhaParallelExecutor.Dupla<ProcessoDistribuicaoMalhaAggregate, ProcessoAnomaliaMalhaAggregate> consolidado = processoMalhaParallelExecutor.executar2(
                "orquestracao-distribuicao-malha",
                () -> processoDistribuicaoMalhaApplicationService.detalhar(processoId),
                () -> processoAnomaliaMalhaApplicationService.detalhar(processoId)
        );
        ProcessoDistribuicaoMalhaAggregate distribuicaoMalha = consolidado.primeiro();
        ProcessoAnomaliaMalhaAggregate anomaliaMalha = consolidado.segundo();

        ProcessoAnomaliaGovernancaAggregate governanca = processoAnomaliaGovernancaApplicationService.escalarSeNecessario(processoId);
        ProcessoTimelineMalhaMaterializacaoAggregate timelineMaterializacao;
        Long workItemId = null;
        String statusOrquestracao;
        String acaoExecutada;
        boolean bloqueada = distribuicaoMalha.travaDistribuicao();
        boolean remessaManual = distribuicaoMalha.exigeRemessa();
        boolean redistribuicaoManual = distribuicaoMalha.exigeReuniao() || distribuicaoMalha.acaoPrimaria().contains("REDISTRIBUIR");
        String filaOperacional = distribuicaoMalha.filaSugerida();
        String inboxOperacional = distribuicaoMalha.inboxSugerida();
        String unidadeDestino = distributedUnit(distribuicaoMalha, processo);
        int prioridade = governanca.exigiuPersistencia() ? 0 : distribuicaoMalha.prioridade();

        if (bloqueada || remessaManual || redistribuicaoManual) {
            WorkItem workItem = ensureMalhaWorkItem(processo, distribuicaoMalha, governanca, prioridade);
            workItemId = workItem.getId();
            statusOrquestracao = bloqueada ? "TRAVADO_POR_MALHA" : remessaManual ? "REMESSA_MANUAL_GERADA" : "REDISTRIBUICAO_MANUAL_GERADA";
            acaoExecutada = distribuicaoMalha.acaoPrimaria();
            filaOperacional = workItem.getQueueCode();
            inboxOperacional = workItem.getInboxKey();
            unidadeDestino = firstNonBlank(distribuicaoMalha.destinoUnidade(), processo.getVara());
            aplicarSnapshotProcessual(processo, distribuicaoMalha, governanca);
            processoRepository.save(processo);
            publicarOrquestracao(processoId, processo.getNumero(), statusOrquestracao, acaoExecutada, filaOperacional, inboxOperacional, unidadeDestino, workItemId, governanca.exigiuPersistencia());
        } else {
            DistribuicaoResult result = distribuicaoProcessualNacionalEngine.distribuir(buildRequest(processo, distribuicaoMalha, anomaliaMalha));
            workItemId = result.workItemId();
            statusOrquestracao = result.status();
            acaoExecutada = "DISTRIBUICAO_NATIVA_COM_MALHA";
            filaOperacional = result.filaDistribuicao();
            inboxOperacional = result.inboxKey();
            unidadeDestino = firstNonBlank(result.varaDestino(), distribuicaoMalha.destinoUnidade(), processo.getVara());
            aplicarSnapshotProcessual(processo, distribuicaoMalha, governanca);
            processo.setUnidadeJudiciariaCodigo(firstNonBlank(result.varaDestino(), processo.getUnidadeJudiciariaCodigo()));
            processoRepository.save(processo);
            publicarOrquestracao(processoId, processo.getNumero(), statusOrquestracao, acaoExecutada, filaOperacional, inboxOperacional, unidadeDestino, workItemId, governanca.exigiuPersistencia());
        }

        timelineMaterializacao = processoTimelineMalhaMaterializacaoApplicationService.materializar(processoId);
        registrarRastro(processoId, processo.getNumero(), statusOrquestracao, acaoExecutada, distribuicaoMalha, governanca, timelineMaterializacao, workItemId, filaOperacional, inboxOperacional, unidadeDestino);
        return new ProcessoDistribuicaoMalhaOrquestracaoAggregate(
                processoId,
                processo.getNumero(),
                statusOrquestracao,
                acaoExecutada,
                bloqueada,
                remessaManual,
                redistribuicaoManual,
                workItemId,
                filaOperacional,
                inboxOperacional,
                unidadeDestino,
                prioridade,
                timelineMaterializacao.atualizouSnapshotTemporal(),
                governanca.exigiuPersistencia(),
                fundamentos(distribuicaoMalha, anomaliaMalha, governanca, timelineMaterializacao),
                Instant.now()
        );
    }

    private WorkItem ensureMalhaWorkItem(Processo processo,
                                         ProcessoDistribuicaoMalhaAggregate distribuicaoMalha,
                                         ProcessoAnomaliaGovernancaAggregate governanca,
                                         int prioridade) {
        String templateCode = "MALHA_DIST:" + processo.getId() + ':' + distribuicaoMalha.acaoPrimaria() + ':' + normalize(distribuicaoMalha.filaSugerida());
        return workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(processo.getId(), templateCode, WorkItemStatus.CANCELADO)
                .orElseGet(() -> workItemRepository.save(WorkItem.builder()
                        .processo(processo)
                        .faseOrigem(processo.getFaseAtual())
                        .templateCode(templateCode)
                        .type(WorkItemType.DISTRIBUICAO)
                        .titulo("Malha nacional — " + distribuicaoMalha.acaoPrimaria() + " — " + processo.getNumero())
                        .descricao(descricaoWorkItem(distribuicaoMalha, governanca))
                        .queueCode(distribuicaoMalha.filaSugerida())
                        .inboxKey(distribuicaoMalha.inboxSugerida())
                        .assignedRole(TipoUsuario.SERVIDOR_FORUM)
                        .status(WorkItemStatus.PENDENTE)
                        .prioridade(prioridade)
                        .blocking(true)
                        .uf(processo.getUf())
                        .comarca(processo.getComarca())
                        .baseLegal(baseLegal(distribuicaoMalha))
                        .dueAt(Instant.now().plus(governanca.exigiuPersistencia() ? 2 : 6, ChronoUnit.HOURS))
                        .build()));
    }

    private String descricaoWorkItem(ProcessoDistribuicaoMalhaAggregate distribuicaoMalha,
                                     ProcessoAnomaliaGovernancaAggregate governanca) {
        LinkedHashSet<String> partes = new LinkedHashSet<>();
        partes.add("Ação=" + distribuicaoMalha.acaoPrimaria());
        partes.add("Fila=" + distribuicaoMalha.filaSugerida());
        partes.add("Inbox=" + distribuicaoMalha.inboxSugerida());
        if (distribuicaoMalha.exigeRemessa()) {
            partes.add("Remessa preventiva exigida");
        }
        if (distribuicaoMalha.exigeReuniao()) {
            partes.add("Reunião/redistribuição exigida");
        }
        if (distribuicaoMalha.exigeSigiloReforcado()) {
            partes.add("Sigilo reforçado antes da distribuição");
        }
        if (governanca.exigiuPersistencia()) {
            partes.add("Escalada de governança ativa");
        }
        distribuicaoMalha.fundamentos().stream().limit(5).forEach(partes::add);
        return String.join(" | ", partes);
    }

    private String baseLegal(ProcessoDistribuicaoMalhaAggregate distribuicaoMalha) {
        LinkedHashSet<String> bases = new LinkedHashSet<>();
        if (distribuicaoMalha.exigeRemessa()) {
            bases.add("Prevenção e competência por conexão");
        }
        if (distribuicaoMalha.exigeReuniao()) {
            bases.add("Conexão, continência e dependência processual");
        }
        if (distribuicaoMalha.exigeSigiloReforcado()) {
            bases.add("Proteção reforçada de prova e cadeia de custódia");
        }
        if (bases.isEmpty()) {
            bases.add("Distribuição nacional guiada por malha processual");
        }
        return String.join(" | ", bases);
    }

    private void aplicarSnapshotProcessual(Processo processo,
                                           ProcessoDistribuicaoMalhaAggregate distribuicaoMalha,
                                           ProcessoAnomaliaGovernancaAggregate governanca) {
        processo.setPreventionMode(distribuicaoMalha.exigeRemessa() ? "PREVENTO_MALHA" : "NORMAL_MALHA");
        processo.setLinkageMode(distribuicaoMalha.exigeReuniao() ? "CONEXAO_DEPENDENCIA_MALHA" : "SEM_VINCULO_BLOQUEANTE");
        processo.setRoutingRiskLevel(governanca.nivelGlobal());
        if (distribuicaoMalha.exigeSigiloReforcado()) {
            processo.setSubmissionBlueprintStatus("SIGILO_REFORCADO_PRE_DISTRIBUICAO");
        }
    }

    private void publicarOrquestracao(Long processoId,
                                      String numeroProcesso,
                                      String status,
                                      String acao,
                                      String fila,
                                      String inbox,
                                      String unidadeDestino,
                                      Long workItemId,
                                      boolean anomaliaEscalada) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("processoId", processoId);
        payload.put("numeroProcesso", numeroProcesso);
        payload.put("status", status);
        payload.put("acao", acao);
        payload.put("fila", fila);
        payload.put("inbox", inbox);
        payload.put("unidadeDestino", unidadeDestino);
        payload.put("workItemId", workItemId);
        payload.put("anomaliaEscalada", anomaliaEscalada);
        outboxPublisher.enqueue(
                "processo.distribuicao.orquestrada." + processoId,
                "PROCESSO_DISTRIBUICAO_MALHA_ORQUESTRADA",
                payload,
                Map.of(
                        "boundedContext", "processo.distribuicao",
                        "eventVersion", "1",
                        "orquestrador", "MALHA_NACIONAL"
                ),
                "processo-distribuicao-orquestrada:" + processoId + ':' + normalize(acao) + ':' + normalize(status),
                "PROCESSO",
                String.valueOf(processoId)
        );
    }

    private void registrarRastro(Long processoId,
                                 String numeroProcesso,
                                 String statusOrquestracao,
                                 String acaoExecutada,
                                 ProcessoDistribuicaoMalhaAggregate distribuicaoMalha,
                                 ProcessoAnomaliaGovernancaAggregate governanca,
                                 ProcessoTimelineMalhaMaterializacaoAggregate timelineMaterializacao,
                                 Long workItemId,
                                 String filaOperacional,
                                 String inboxOperacional,
                                 String unidadeDestino) {
        if (decisionTraceService != null) {
            decisionTraceService.record(
                    "PROCESSO_DISTRIBUICAO_MALHA_ORQUESTRADA",
                    "PROCESSO",
                    String.valueOf(processoId),
                    BigDecimal.valueOf(confianca(distribuicaoMalha, governanca)),
                    jsonList(fundamentos(distribuicaoMalha, null, governanca, timelineMaterializacao)),
                    jsonList(List.of(statusOrquestracao, acaoExecutada, filaOperacional, inboxOperacional, unidadeDestino)),
                    numeroProcesso,
                    acaoExecutada + ':' + statusOrquestracao + ':' + workItemId,
                    "PJB_DISTRIBUICAO_ORQUESTRADA_V1",
                    jsonMap(metadataMap(timelineMaterializacao.atualizouSnapshotTemporal(), governanca.exigiuPersistencia(), workItemId))
            );
        }
        if (auditLedgerService != null) {
            auditLedgerService.appendSafely(
                    "PROCESSO_DISTRIBUICAO_MALHA_ORQUESTRADA",
                    "PROCESSO",
                    String.valueOf(processoId),
                    acaoExecutada + ':' + statusOrquestracao + ':' + workItemId,
                    "Distribuição orquestrada pela malha nacional"
            );
        }
    }

    private DistribuicaoRequest buildRequest(Processo processo,
                                             ProcessoDistribuicaoMalhaAggregate distribuicaoMalha,
                                             ProcessoAnomaliaMalhaAggregate anomaliaMalha) {
        return ProcessoDistribuicaoEngineRequestFactory.fromProcesso(processo, distribuicaoMalha, anomaliaMalha);
    }

    private String distributedUnit(ProcessoDistribuicaoMalhaAggregate distribuicaoMalha, Processo processo) {
        return firstNonBlank(distribuicaoMalha.destinoUnidade(), processo.getVara(), processo.getTribunal());
    }

    private List<String> fundamentos(ProcessoDistribuicaoMalhaAggregate distribuicaoMalha,
                                     ProcessoAnomaliaMalhaAggregate anomaliaMalha,
                                     ProcessoAnomaliaGovernancaAggregate governanca,
                                     ProcessoTimelineMalhaMaterializacaoAggregate timelineMaterializacao) {
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(distribuicaoMalha.fundamentos());
        if (anomaliaMalha != null) {
            fundamentos.addAll(anomaliaMalha.fundamentos());
        }
        fundamentos.addAll(governanca.fundamentos());
        fundamentos.addAll(timelineMaterializacao.fundamentos());
        return List.copyOf(fundamentos.stream().limit(70).toList());
    }

    private double confianca(ProcessoDistribuicaoMalhaAggregate distribuicaoMalha,
                             ProcessoAnomaliaGovernancaAggregate governanca) {
        double base = 0.70d;
        base += Math.min(0.14d, distribuicaoMalha.motivos().size() * 0.02d);
        if (distribuicaoMalha.travaDistribuicao()) {
            base += 0.06d;
        }
        if (governanca.exigiuPersistencia()) {
            base += 0.04d;
        }
        return Math.min(0.97d, base);
    }

    private String jsonList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .map(text -> '"' + text.replace("\\", "\\\\").replace("\"", "\\\"") + '"')
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private String jsonMap(Map<String, Object> values) {
        LinkedHashMap<String, Object> normalized = new LinkedHashMap<>(values);
        return normalized.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> '"' + entry.getKey() + '\"' + ':' + formatValue(entry.getValue()))
                .collect(java.util.stream.Collectors.joining(",", "{", "}"));
    }


    private Map<String, Object> metadataMap(boolean timelineMaterializada, boolean anomaliaEscalada, Long workItemId) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("timelineMaterializada", timelineMaterializada);
        out.put("anomaliaEscalada", anomaliaEscalada);
        out.put("workItemId", workItemId);
        return out;
    }

    private String formatValue(Object value) {
        return switch (value) {
            case Number number -> String.valueOf(number);
            case Boolean bool -> String.valueOf(bool);
            default -> '"' + String.valueOf(value).replace("\\", "\\\\").replace("\"", "\\\"") + '"';
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
