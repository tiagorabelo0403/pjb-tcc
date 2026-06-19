package com.tcc.pjb.backend.query.consumer;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierReport;
import com.tcc.pjb.backend.core.lgpd.PjbProcessoSigiloRlsEntryPointSupport;
import com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingService;
import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintService;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionReport;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionService;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.query.ProcessoQueryModel;
import com.tcc.pjb.backend.query.ProcessoQueryRepository;
import com.tcc.pjb.backend.platform.runtime.execution.PjbTransactionalExecutionSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnProperty(prefix = "pjb.kafka", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "pjb.search", name = "enabled", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
public class ProcessoMaterializadoConsumer {

    public static final String TOPIC = "evento.processo.materializado.v1";
    public static final String GROUP_ID = "grupo-query-materializer";

    private static final Duration MATERIALIZATION_TX_BUDGET = Duration.ofSeconds(20);

    private final ProcessoRepository processoWriteRepository;
    private final ProcessoQueryRepository processoQueryRepository;
    private final ProcessMaterialStrategyService processMaterialStrategyService;
    private final NationalProceduralRoutingService nationalProceduralRoutingService;
    private final ProceduralSubmissionBlueprintService proceduralSubmissionBlueprintService;
    private final ProceduralConnectorExecutionService proceduralConnectorExecutionService;
    private final PjbProcessoSigiloRlsEntryPointSupport processoSigiloRlsEntryPointSupport;
    private final PjbTransactionalExecutionSupport transactionalExecutionSupport;

    @KafkaListener(containerFactory = "pjbKafkaListenerContainerFactory", topics = ProcessoMaterializadoConsumer.TOPIC, groupId = ProcessoMaterializadoConsumer.GROUP_ID)
    public void handleProcessoMaterializado(@Payload Map<String, Object> evento) {
        String correlationId = evento != null && evento.get("correlationId") != null
                ? String.valueOf(evento.get("correlationId"))
                : "(sem_correlationId)";

        Long processoId = toLong(evento != null ? evento.get("processoId") : null);
        if (processoId == null) {
            log.warn("[{}] Evento de materialização ignorado: processoId ausente/inválido. Payload={}", correlationId, evento);
            return;
        }

        log.info("[{}] Recebido evento de materialização para processo ID: {}", correlationId, processoId);
        processoSigiloRlsEntryPointSupport.runWithProcessoContext(
                processoId,
                "KAFKA",
                () -> transactionalExecutionSupport.executeReadOnly(
                        "processo-materializado.kafka",
                        MATERIALIZATION_TX_BUDGET,
                        () -> materializeProcesso(correlationId, processoId)
                )
        );
    }

    private void materializeProcesso(String correlationId, Long processoId) {
        try {
            Processo processo = processoWriteRepository.findProcessoCompletoById(processoId)
                    .orElseThrow(() -> new NoSuchElementException("Processo não encontrado no DB principal: " + processoId));

            ProcessoQueryModel queryModel = new ProcessoQueryModel();
            queryModel.setId(processo.getId());
            queryModel.setNumeroUnificado(processo.getNumeroUnificado());
            queryModel.setStatusAtual(processo.getStatus() != null ? processo.getStatus().name() : null);
            queryModel.setCorrelationId(correlationId);

            if (processo.getUsuario() != null) {
                queryModel.setUsuarioId(processo.getUsuario().getId());
                queryModel.setNomeUsuario(processo.getUsuario().getNome());
            }

            if (processo.getJurisdicao() != null) {
                queryModel.setJurisdicaoId(processo.getJurisdicao().getId());
                queryModel.setNomeJurisdicao(processo.getJurisdicao().getNome());
                queryModel.setMateria(processo.getJurisdicao().getMateria() != null
                        ? processo.getJurisdicao().getMateria().name()
                        : null);
            }

            queryModel.setAssunto(processo.getAssunto());
            queryModel.setObjetoProcessual(processo.getObjetoProcessual());
            queryModel.setPedidoPrincipal(processo.getPedidoPrincipal());
            queryModel.setPedidosConsolidados(processo.getPedidosConsolidados());
            queryModel.setMaterialProbatorioResumo(processo.getMaterialProbatorioResumo());
            queryModel.setJanelaAcordoResumo(processo.getJanelaAcordoResumo());
            queryModel.setPotencialAcordoScore(processo.getPotencialAcordoScore());
            queryModel.setMaterialProbatorioScore(processo.getMaterialProbatorioScore());
            queryModel.setClassificacaoProbatoria(classifyProbatory(processo.getMaterialProbatorioScore()));
            queryModel.setClassificacaoNegocial(classifyNegotiation(processo.getPotencialAcordoScore()));

            ProcessMaterialDossierReport materialDossier = buildDossier(processo);
            ProcessMaterialStrategyReport materialStrategy = processMaterialStrategyService.analyzeProcess(processo, materialDossier, buildSignals(processo));
            ProceduralRoutingReport proceduralRouting = nationalProceduralRoutingService.analyzeProcess(processo);
            ProceduralSubmissionBlueprintReport submissionBlueprint = proceduralSubmissionBlueprintService.analyzeProcess(processo, proceduralRouting);
            ProceduralConnectorExecutionReport connectorExecution = proceduralConnectorExecutionService.analyzeProcess(processo, proceduralRouting, submissionBlueprint);
            queryModel.setEstrategiaContenciosa(materialStrategy.litigationPosture());
            queryModel.setProntidaoProtocolar(materialStrategy.protocolReadiness());
            queryModel.setPosturaNegocial(materialStrategy.negotiationStance());
            queryModel.setMaturidadeProbatoria(materialStrategy.evidenceReadiness());
            queryModel.setGapsEstrategicos(join(materialStrategy.protocolBlockers(), materialStrategy.evidenceAgenda()));
            queryModel.setPlanoEstrutural(join(materialStrategy.pleadingBlueprint(), materialStrategy.executionChecklist(), materialStrategy.controlPoints()));
            queryModel.setResumoPublico(buildResumoPublico(processo, materialStrategy, proceduralRouting, submissionBlueprint, connectorExecution));
            if (proceduralRouting != null) {
                queryModel.setTipoJusticaSugerida(proceduralRouting.tipoJusticaSugerida());
                queryModel.setRitoProcessual(proceduralRouting.ritoSugerido());
                queryModel.setRegimeProcedimental(proceduralRouting.proceduralRegime());
                queryModel.setProceduralTrack(proceduralRouting.proceduralTrack());
                String naturezaJuridicaCanonical = proceduralRouting.metadata() != null ? java.util.Objects.toString(proceduralRouting.metadata().get("naturezaJuridicaCanonical"), null) : null;
                queryModel.setAcaoNatureza(proceduralRouting.actionNature() != null && !proceduralRouting.actionNature().isBlank() ? proceduralRouting.actionNature() : naturezaJuridicaCanonical);
                queryModel.setPerfilProbatorio(proceduralRouting.probatoryProfile());
                queryModel.setFaixaComplexidade(proceduralRouting.complexityBand());
                queryModel.setAderenciaEconomica(proceduralRouting.economicGate() != null ? proceduralRouting.economicGate().economicBand() : null);
                queryModel.setCodigoDiagnosticoEconomico(proceduralRouting.economicGate() != null ? proceduralRouting.economicGate().thresholdCode() : null);
                queryModel.setVaraSugerida(proceduralRouting.varaSugerida());
                if (proceduralRouting.forumAllocation() != null) {
                    queryModel.setClasseTpuCodigo(proceduralRouting.forumAllocation().classeTpuCodigo());
                    queryModel.setCompetenciaTerritorialModo(proceduralRouting.forumAllocation().competenciaTerritorialModo());
                    queryModel.setPreventionMode(proceduralRouting.forumAllocation().preventionMode());
                    queryModel.setLinkageMode(proceduralRouting.forumAllocation().linkageMode());
                    queryModel.setUnidadeJudiciariaCodigo(proceduralRouting.forumAllocation().unidadeJudiciariaCodigo());
                    queryModel.setSistemaConector(proceduralRouting.forumAllocation().connectorSystem());
                    queryModel.setPreProtocoloStatus(proceduralRouting.forumAllocation().preProtocoloStatus());
                }
                queryModel.setTribunalSugerido(proceduralRouting.tribunalCodigo());
                queryModel.setConfiancaRoteamento(java.math.BigDecimal.valueOf(proceduralRouting.confidence()));
            }
            if (submissionBlueprint != null) {
                queryModel.setSubmissionBlueprintStatus(submissionBlueprint.blueprintStatus());
                queryModel.setSubmissionDryRunStatus(submissionBlueprint.dryRunStatus());
                queryModel.setLocalCorrelationMode(submissionBlueprint.localCorrelationMode());
            }
            if (connectorExecution != null) {
                queryModel.setConnectorExecutionMode(connectorExecution.executionMode());
                queryModel.setConnectorSubmissionLane(connectorExecution.submissionLane());
                queryModel.setConnectorTargetKey(connectorExecution.tribunalTargetKey());
            }
            queryModel.setConnectorSubmissionStatus(processo.getConnectorSubmissionStatus());
            queryModel.setConnectorProtocolReference(processo.getConnectorProtocolReference());
            queryModel.setConnectorSubmissionMessage(processo.getConnectorSubmissionMessage());
            queryModel.setConnectorSubmissionProcessedAt(processo.getConnectorSubmissionProcessedAt());
            queryModel.setConnectorSubmissionAttempts(processo.getConnectorSubmissionAttempts());
            queryModel.setConnectorLastSubmissionAttemptAt(processo.getConnectorLastSubmissionAttemptAt());
            queryModel.setConnectorSyncStatus(processo.getConnectorSyncStatus());
            queryModel.setConnectorSyncMessage(processo.getConnectorSyncMessage());
            queryModel.setConnectorSnapshotSyncedAt(processo.getConnectorSnapshotSyncedAt());
            queryModel.setConnectorEventsSyncedAt(processo.getConnectorEventsSyncedAt());
            queryModel.setConnectorSyncAttempts(processo.getConnectorSyncAttempts());
            queryModel.setValorCausa(processo.getValorCausa());
            queryModel.setDataCriacao(processo.getDataCriacao());
            queryModel.setPoloAtivo(processo.getNomesPoloAtivo());
            queryModel.setPoloPassivo(processo.getNomesPoloPassivo());

            if (processo.getPotencialAcordoScore() != null) {
                if (processo.getPotencialAcordoScore() >= 75) {
                    queryModel.addTag("NEGOCIACAO_ALTA");
                } else if (processo.getPotencialAcordoScore() >= 55) {
                    queryModel.addTag("NEGOCIACAO_MODERADA");
                }
            }
            if (processo.getMaterialProbatorioScore() != null && processo.getMaterialProbatorioScore() >= 70) {
                queryModel.addTag("PROVA_ESTRUTURADA");
            }
            queryModel.addTag(materialStrategy.litigationPosture());
            queryModel.addTag(materialStrategy.protocolReadiness());
            queryModel.addTag(materialStrategy.negotiationStance());
            if (proceduralRouting != null) {
                queryModel.addTag(proceduralRouting.tipoJusticaSugerida());
                queryModel.addTag(proceduralRouting.proceduralRegime());
                queryModel.addTag(proceduralRouting.proceduralTrack());
                queryModel.addTag(proceduralRouting.actionNature());
                queryModel.addTag(proceduralRouting.metadata() != null ? java.util.Objects.toString(proceduralRouting.metadata().get("naturezaJuridicaCanonical"), null) : null);
                queryModel.addTag(proceduralRouting.metadata() != null ? java.util.Objects.toString(proceduralRouting.metadata().get("operatingModeHint"), null) : null);
                queryModel.addTag(proceduralRouting.metadata() != null ? java.util.Objects.toString(proceduralRouting.metadata().get("automationMode"), null) : null);
                queryModel.addTag(proceduralRouting.metadata() != null ? java.util.Objects.toString(proceduralRouting.metadata().get("automationDomain"), null) : null);
                queryModel.addTag(proceduralRouting.metadata() != null ? java.util.Objects.toString(proceduralRouting.metadata().get("executiveActionFrame"), null) : null);
                queryModel.addTag(proceduralRouting.metadata() != null ? java.util.Objects.toString(proceduralRouting.metadata().get("accelerationTrack"), null) : null);
                queryModel.addTag(proceduralRouting.metadata() != null ? java.util.Objects.toString(proceduralRouting.metadata().get("accelerationLane"), null) : null);
                queryModel.addTag(Boolean.TRUE.equals(proceduralRouting.metadata() != null ? proceduralRouting.metadata().get("queueBypassEligible") : null) ? "QUEUE_BYPASS_ELIGIBLE" : null);
                queryModel.addTag(Boolean.TRUE.equals(proceduralRouting.metadata() != null ? proceduralRouting.metadata().get("immediateHumanGate") : null) ? "IMMEDIATE_HUMAN_GATE" : null);
                queryModel.addTag(Boolean.TRUE.equals(proceduralRouting.metadata() != null ? proceduralRouting.metadata().get("publicationLocked") : null) ? "PUBLICATION_LOCKED" : null);
                queryModel.addTag(Boolean.TRUE.equals(proceduralRouting.metadata() != null ? proceduralRouting.metadata().get("safeAutomationEligible") : null) ? "AUTOMACAO_SEGURA" : null);
                queryModel.addTag(Boolean.TRUE.equals(proceduralRouting.metadata() != null ? proceduralRouting.metadata().get("autoRouteEligible") : null) ? "AUTO_ROUTE_ELIGIBLE" : null);
                queryModel.addTag(Boolean.TRUE.equals(proceduralRouting.metadata() != null ? proceduralRouting.metadata().get("autoProtocolBlueprintEligible") : null) ? "AUTO_BLUEPRINT_ELIGIBLE" : null);
                queryModel.addTag(proceduralRouting.probatoryProfile());
                queryModel.addTag(proceduralRouting.complexityBand());
                queryModel.addTag(proceduralRouting.economicGate() != null ? proceduralRouting.economicGate().economicBand() : null);
                queryModel.addTag(proceduralRouting.economicGate() != null ? proceduralRouting.economicGate().thresholdKind() : null);
                queryModel.addTag(proceduralRouting.varaSugerida());
                if (proceduralRouting.forumAllocation() != null) {
                    queryModel.addTag(proceduralRouting.forumAllocation().classeTpuCodigo());
                    queryModel.addTag(proceduralRouting.forumAllocation().competenciaTerritorialModo());
                    queryModel.addTag(proceduralRouting.forumAllocation().preventionMode());
                    queryModel.addTag(proceduralRouting.forumAllocation().linkageMode());
                    queryModel.addTag(proceduralRouting.forumAllocation().connectorSystem());
                    queryModel.addTag(proceduralRouting.forumAllocation().preProtocoloStatus());
                    queryModel.addTag(proceduralRouting.forumAllocation().varaSugerida());
                }
            }
            if (submissionBlueprint != null) {
                queryModel.addTag(submissionBlueprint.blueprintStatus());
                queryModel.addTag(submissionBlueprint.localCorrelationMode());
                queryModel.addTag(submissionBlueprint.dryRunStatus());
            }
            if (connectorExecution != null) {
                queryModel.addTag(connectorExecution.executionMode());
                queryModel.addTag(connectorExecution.submissionLane());
                queryModel.addTag(connectorExecution.signerMode());
            }
            queryModel.addTag(processo.getConnectorSubmissionStatus());
            queryModel.addTag(processo.getConnectorProtocolReference());
            queryModel.addTag(processo.getConnectorSyncStatus());
            if (processo.getRamoDireito() != null) {
                queryModel.addTag(processo.getRamoDireito().name());
            }

            processoQueryRepository.save(queryModel);
            log.info("[{}] Processo ID: {} materializado com sucesso no ElasticSearch.", correlationId, processoId);

        } catch (Exception e) {
            log.error("[{}] Falha ao materializar processo ID: {}. Erro: {}", correlationId, processoId, e.getMessage(), e);
        }
    }

    private static String buildResumoPublico(Processo processo, ProcessMaterialStrategyReport materialStrategy, ProceduralRoutingReport proceduralRouting, ProceduralSubmissionBlueprintReport submissionBlueprint, ProceduralConnectorExecutionReport connectorExecution) {
        StringBuilder sb = new StringBuilder();
        append(sb, processo.getClasseProcessual());
        append(sb, processo.getAssunto());
        append(sb, processo.getObjetoProcessual());
        append(sb, processo.getPedidoPrincipal());
        append(sb, processo.getPedidosConsolidados());
        append(sb, processo.getMaterialProbatorioResumo());
        append(sb, processo.getJanelaAcordoResumo());
        if (materialStrategy != null) {
            append(sb, materialStrategy.litigationPosture());
            append(sb, materialStrategy.protocolReadiness());
            append(sb, materialStrategy.negotiationStance());
            append(sb, materialStrategy.evidenceReadiness());
            append(sb, join(materialStrategy.protocolBlockers(), materialStrategy.evidenceAgenda(), materialStrategy.pleadingBlueprint()));
        }
        if (proceduralRouting != null) {
            append(sb, proceduralRouting.actionNature());
            append(sb, proceduralRouting.proceduralTrack());
            append(sb, proceduralRouting.complexityBand());
            append(sb, proceduralRouting.metadata() != null ? java.util.Objects.toString(proceduralRouting.metadata().get("prioritySummary"), null) : null);
            append(sb, proceduralRouting.metadata() != null ? java.util.Objects.toString(proceduralRouting.metadata().get("priorityDecisionBlueprint"), null) : null);
            append(sb, proceduralRouting.probatoryProfile());
            append(sb, join(proceduralRouting.blockingIssues(), proceduralRouting.reasons(), proceduralRouting.reviewChecklist()));
            if (proceduralRouting.economicGate() != null) {
                append(sb, proceduralRouting.economicGate().economicBand());
                append(sb, join(proceduralRouting.economicGate().reasons(), proceduralRouting.economicGate().rerouteOptions()));
            }
            if (proceduralRouting.forumAllocation() != null) {
                append(sb, proceduralRouting.forumAllocation().classeTpuCodigo());
                append(sb, proceduralRouting.forumAllocation().competenciaTerritorialModo());
                append(sb, proceduralRouting.forumAllocation().preventionMode());
                append(sb, proceduralRouting.forumAllocation().linkageMode());
                append(sb, proceduralRouting.forumAllocation().varaSugerida());
                append(sb, proceduralRouting.forumAllocation().connectorSystem());
                append(sb, join(proceduralRouting.forumAllocation().incompatibilities(), proceduralRouting.forumAllocation().warnings(), proceduralRouting.forumAllocation().reviewChecklist()));
            }
        }
        if (submissionBlueprint != null) {
            append(sb, submissionBlueprint.blueprintStatus());
            append(sb, submissionBlueprint.localCorrelationMode());
            append(sb, submissionBlueprint.tribunalCodigo());
            append(sb, submissionBlueprint.unidadeJudiciariaCodigo());
            append(sb, submissionBlueprint.judicialSystem() != null ? submissionBlueprint.judicialSystem().name() : null);
            append(sb, submissionBlueprint.dryRunStatus());
            append(sb, join(submissionBlueprint.relatedLocalProcessNumbers(), submissionBlueprint.blockingIssues(), submissionBlueprint.reviewChecklist(), submissionBlueprint.warnings()));
        }
        if (connectorExecution != null) {
            append(sb, connectorExecution.executionMode());
            append(sb, connectorExecution.submissionLane());
            append(sb, connectorExecution.tribunalTargetKey());
            append(sb, connectorExecution.signerMode());
            append(sb, connectorExecution.retryPolicy());
            append(sb, join(connectorExecution.phases(), connectorExecution.executionChecklist(), connectorExecution.blockers(), connectorExecution.warnings()));
        }
        append(sb, processo.getConnectorSubmissionStatus());
        append(sb, processo.getConnectorProtocolReference());
        append(sb, processo.getConnectorSubmissionMessage());
        append(sb, processo.getConnectorSyncStatus());
        append(sb, processo.getConnectorSyncMessage());
        String out = sb.toString().trim();
        return out.isEmpty() ? null : out;
    }

    private static ProcessMaterialDossierReport buildDossier(Processo processo) {
        List<String> evidenceAnchors = splitStructured(processo.getMaterialProbatorioResumo());
        List<String> settlementLevers = splitStructured(processo.getJanelaAcordoResumo());
        List<String> protocolChecklist = splitStructured(processo.getPedidosConsolidados());
        String evidentiaryBracket = classifyProbatory(processo.getMaterialProbatorioScore());
        String negotiationBracket = classifyNegotiation(processo.getPotencialAcordoScore());
        Map<String, Object> diagnostics = buildDossierDiagnostics(processo, evidenceAnchors, evidentiaryBracket, negotiationBracket);
        return new ProcessMaterialDossierReport(
                "QUERY",
                processo.getFaseAtual() == null ? null : processo.getFaseAtual().name(),
                processo.getObjetoProcessual(),
                processo.getPedidoPrincipal(),
                evidentiaryBracket,
                negotiationBracket,
                List.of(),
                List.of(),
                evidenceAnchors,
                List.of(),
                List.of(),
                settlementLevers,
                protocolChecklist,
                diagnostics
        );
    }


    private static Map<String, Object> buildDossierDiagnostics(Processo processo,
                                                               List<String> evidenceAnchors,
                                                               String evidentiaryBracket,
                                                               String negotiationBracket) {
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("evidenceScore", processo.getMaterialProbatorioScore());
        diagnostics.put("negotiationScore", processo.getPotencialAcordoScore());
        diagnostics.put("claimsCount", splitStructured(processo.getPedidosConsolidados()).size());
        diagnostics.put("evidenceCount", evidenceAnchors.size());
        diagnostics.put("valorCausa", processo.getValorCausa());
        diagnostics.put("evidentiaryBracket", evidentiaryBracket);
        diagnostics.put("negotiationBracket", negotiationBracket);
        diagnostics.put("executiveSummary", buildDossierExecutiveSummary(processo, evidentiaryBracket, negotiationBracket, evidenceAnchors));
        diagnostics.put("attentionBand", evidenceAnchors.isEmpty() ? "ATIVA" : "ESTAVEL");
        diagnostics.put("strategicFocus", buildDossierStrategicFocus(processo, evidenceAnchors));
        return Map.copyOf(diagnostics);
    }

    private static String buildDossierExecutiveSummary(Processo processo,
                                                       String evidentiaryBracket,
                                                       String negotiationBracket,
                                                       List<String> evidenceAnchors) {
        String objeto = processo.getObjetoProcessual() == null || processo.getObjetoProcessual().isBlank()
                ? "objeto não consolidado"
                : processo.getObjetoProcessual().trim();
        String pedido = processo.getPedidoPrincipal() == null || processo.getPedidoPrincipal().isBlank()
                ? "pedido principal pendente de síntese"
                : processo.getPedidoPrincipal().trim();
        StringBuilder summary = new StringBuilder();
        summary.append("Consulta materializada com objeto ").append(objeto);
        summary.append(" e pedido ").append(pedido).append(".");
        if (evidentiaryBracket != null) {
            summary.append(" Prova ").append(evidentiaryBracket.toLowerCase()).append('.');
        }
        if (negotiationBracket != null) {
            summary.append(" Negociação ").append(negotiationBracket.toLowerCase()).append('.');
        }
        summary.append(evidenceAnchors.isEmpty() ? " Falta ancoragem probatória resumida." : " Há lastro probatório resumido disponível.");
        return summary.toString();
    }

    private static String buildDossierStrategicFocus(Processo processo,
                                                     List<String> evidenceAnchors) {
        if (!evidenceAnchors.isEmpty()) {
            return evidenceAnchors.get(0);
        }
        if (processo.getPedidoPrincipal() != null && !processo.getPedidoPrincipal().isBlank()) {
            return processo.getPedidoPrincipal().trim();
        }
        if (processo.getObjetoProcessual() != null && !processo.getObjetoProcessual().isBlank()) {
            return processo.getObjetoProcessual().trim();
        }
        return "Consolidar objeto, pedido e prova mínima da consulta materializada.";
    }

    @SafeVarargs
    private static String join(List<String>... groups) {
        StringBuilder sb = new StringBuilder();
        if (groups != null) {
            for (List<String> group : groups) {
                if (group == null) {
                    continue;
                }
                for (String value : group) {
                    append(sb, value);
                }
            }
        }
        String out = sb.toString().trim();
        return out.isEmpty() ? null : out;
    }

    private static List<String> splitStructured(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String[] parts = value.replace('\r', '\n').split("\\R|;|\\|");
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String part : parts) {
            if (part == null) {
                continue;
            }
            String cleaned = part.replaceFirst("^[\\-•*\\d.)\\s]+", "").trim();
            if (!cleaned.isBlank()) {
                out.add(cleaned);
            }
        }
        return List.copyOf(out);
    }

    private static List<String> buildSignals(Processo processo) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (processo.getObjetoProcessual() != null && !processo.getObjetoProcessual().isBlank()) {
            out.add(processo.getObjetoProcessual());
        }
        if (processo.getPedidoPrincipal() != null && !processo.getPedidoPrincipal().isBlank()) {
            out.add(processo.getPedidoPrincipal());
        }
        if (processo.getJanelaAcordoResumo() != null && !processo.getJanelaAcordoResumo().isBlank()) {
            out.add(processo.getJanelaAcordoResumo());
        }
        if (processo.getStatusProcesso() != null) {
            out.add(processo.getStatusProcesso().name());
        }
        return List.copyOf(out);
    }

    private static void append(StringBuilder sb, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(value.trim());
    }

    private static String classifyProbatory(Integer score) {
        if (score == null) {
            return null;
        }
        if (score >= 75) {
            return "FORTE";
        }
        if (score >= 50) {
            return "MODERADA";
        }
        return "INICIAL";
    }

    private static String classifyNegotiation(Integer score) {
        if (score == null) {
            return null;
        }
        if (score >= 75) {
            return "ALTA";
        }
        if (score >= 55) {
            return "MODERADA";
        }
        return "RESTRITA";
    }

    private static Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Long l) return l;
        if (v instanceof Integer i) return i.longValue();
        if (v instanceof Number n) return n.longValue();
        try {
            String s = String.valueOf(v).trim();
            if (s.isBlank()) return null;
            return Long.parseLong(s);
        } catch (Exception ignored) {
            return null;
        }
    }
}
