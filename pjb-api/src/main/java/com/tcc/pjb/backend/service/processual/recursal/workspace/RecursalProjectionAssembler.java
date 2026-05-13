package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalPlanningResult;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTramitationMode;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTramitationModeResolver;
import com.tcc.pjb.backend.core.processo.recursal.application.ProcessoRecursalDecisionCarryOverAssembler;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.RecursalGraphResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade.RecursalAdmissibilityResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import org.springframework.stereotype.Service;

@Service
public class RecursalProjectionAssembler {

    private final DocumentoProcessualRepository documentoProcessualRepository;
    private final DocumentoPaginaRepository documentoPaginaRepository;

    public RecursalProjectionAssembler(
            DocumentoProcessualRepository documentoProcessualRepository,
            DocumentoPaginaRepository documentoPaginaRepository
    ) {
        this.documentoProcessualRepository = Objects.requireNonNull(documentoProcessualRepository);
        this.documentoPaginaRepository = Objects.requireNonNull(documentoPaginaRepository);
    }

    public LinkedHashMap<String, Object> buildDistributionProjection(RecursalPlanningResult planning,
                                                                     RecursalAdmissibilityResponse admissibility) {
        LinkedHashMap<String, Object> projection = new LinkedHashMap<>();
        if (planning != null && planning.routePlan() != null) {
            var routePlan = planning.routePlan();
            projection.put("tribunalOrigem", routePlan.tribunalOrigem().name());
            projection.put("tribunalDetalhadoOrigem", routePlan.tribunalDetalhadoOrigem().name());
            projection.put("tribunalDestino", routePlan.tribunalDestino().name());
            projection.put("tribunalDetalhadoDestino", routePlan.tribunalDetalhadoDestino().name());
            projection.put("instanciaDestino", routePlan.instanciaDestino().name());
            projection.put("autoridadeOrigemAdmissibilidade", routePlan.autoridadeOrigemAdmissibilidade() == null ? null : routePlan.autoridadeOrigemAdmissibilidade().name());
            projection.put("autoridadeDestinoAdmissibilidade", routePlan.autoridadeDestinoAdmissibilidade() == null ? null : routePlan.autoridadeDestinoAdmissibilidade().name());
            projection.put("autoridadeJulgamentoMerito", routePlan.autoridadeJulgamentoMerito().name());
            projection.put("routeKind", routePlan.routeKind().name());
            projection.put("mesmaCorte", routePlan.mesmaCorte());
            projection.put("distribuicaoDestino", routePlan.remessa().distribuicaoDestino());
            projection.put("autuacaoDestino", routePlan.remessa().autuacaoDestino());
            projection.put("mesmosAutos", routePlan.remessa().mesmosAutos());
            projection.put("autosApartadosDependencia", routePlan.remessa().autosApartadosDependencia());
            RecursalTramitationMode tramitationMode = RecursalTramitationModeResolver.resolve(routePlan, planning.species());
            projection.put("tramitationMode", tramitationMode.name());
            projection.put("sourceTimelineFrozen", tramitationMode.freezeSourceTimeline());
            projection.put("sourceTimelineMode", tramitationMode.sourceTimelineMode());
            projection.put("targetTimelineMode", tramitationMode.targetTimelineMode());
            projection.put("prevencaoObrigatoria", routePlan.prevencao().obrigatoria());
            projection.put("mesmoRelator", routePlan.prevencao().mesmoRelator());
            projection.put("mesmaTurmaOuCamara", routePlan.prevencao().mesmaTurmaOuCamara());
        }
        if (admissibility != null) {
            putIfNotNull(projection, "secretariaOrigem", admissibility.secretariaOrigem());
            putIfNotNull(projection, "secretariaDestino", admissibility.secretariaDestino());
            putIfNotNull(projection, "admissibilityDesk", admissibility.admissibilityDesk());
            putIfNotNull(projection, "gabineteDestino", admissibility.gabineteDestino());
            putIfNotNull(projection, "supportDesk", admissibility.supportDesk());
            putIfNotNull(projection, "distributionDesk", admissibility.distributionDesk());
            putIfNotNull(projection, "reviewDesk", admissibility.reviewDesk());
            putIfNotNull(projection, "protocolDesk", admissibility.protocolDesk());
            putIfNotNull(projection, "remessaDesk", admissibility.remessaDesk());
            putIfNotNull(projection, "autuacaoDesk", admissibility.autuacaoDesk());
            putIfNotNull(projection, "counterReasonsDesk", admissibility.counterReasonsDesk());
            putIfNotNull(projection, "routeKind", admissibility.routeKind());
            putIfNotNull(projection, "authorityLabel", admissibility.autoridadeJulgamento());
        }
        return projection;
    }

    public Map<String, Object> buildDecisionCarryOverProjection(Processo processo,
                                                                LegalAppealType appealType,
                                                                Map<String, Object> distribuicaoRecursal) {
        String scope = resolveDecisionCarryOverScope(appealType, distribuicaoRecursal);
        String sourceTimelineMode = distribuicaoRecursal == null ? null : asString(distribuicaoRecursal.get("sourceTimelineMode"));
        String targetTimelineMode = distribuicaoRecursal == null ? null : asString(distribuicaoRecursal.get("targetTimelineMode"));
        return ProcessoRecursalDecisionCarryOverAssembler.asMap(
                ProcessoRecursalDecisionCarryOverAssembler.assemble(
                        processo,
                        scope,
                        sourceTimelineMode,
                        targetTimelineMode,
                        documentoProcessualRepository,
                        documentoPaginaRepository)
        );
    }

    public LinkedHashMap<String, Object> buildEscalonamentoProjection(com.tcc.pjb.backend.model.dto.intelligence.recursal.RecursalFactIngestResponse ingestResponse,
                                                                      Map<String, Object> distribuicaoRecursal) {
        LinkedHashMap<String, Object> projection = new LinkedHashMap<>();
        projection.put("status", "ESCALONAMENTO_RECURSAL_AUTOMATICO_ATIVADO");
        projection.put("motor", ingestResponse.systemTag());
        projection.put("factId", ingestResponse.factId());
        projection.put("dedupKey", ingestResponse.dedupKey());
        projection.put("movimentoTimelineId", ingestResponse.timelineMovementId());
        if (ingestResponse.plan() != null) {
            projection.put("workItemsPlanejados", ingestResponse.plan().workItems().size());
            projection.put("syncsPlanejados", ingestResponse.plan().sync().size());
            projection.put("proceedingsPlanejados", ingestResponse.plan().proceedings().size());
            projection.put("edgesPlanejadas", ingestResponse.plan().edges().size());
            projection.put("notas", ingestResponse.plan().notes());
        }
        if (distribuicaoRecursal != null && !distribuicaoRecursal.isEmpty()) {
            projection.put("destino", distribuicaoRecursal);
        }
        return projection;
    }

    public InstanceLevel resolveTargetInstanceHint(LegalAppealType appealType, MeshBundle meshBundle) {
        if (meshBundle != null && meshBundle.plan() != null && meshBundle.plan().routePlan() != null) {
            return meshBundle.plan().routePlan().instanciaDestino();
        }
        if (meshBundle != null && meshBundle.admissibility() != null) {
            String instanciaDestino = normalizeNullable(meshBundle.admissibility().instanciaDestino());
            if (instanciaDestino != null) {
                try {
                    return InstanceLevel.valueOf(instanciaDestino);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return switch (appealType) {
            case RESP, AGRAVO_RESP_RE -> InstanceLevel.SUPERIOR;
            case RE -> InstanceLevel.EXTRAORDINARY;
            case APELACAO, APELACAO_PENAL, AGRAVO_INSTRUMENTO, EMBARGOS_INFRINGENTES, RECURSO_INOMINADO, PEDIDO_UNIFORMIZACAO, RESE, HABEAS_CORPUS -> InstanceLevel.SECOND_INSTANCE;
            case AGRAVO_INTERNO, AGRAVO_REGIMENTAL, EMBARGOS_DECLARACAO, EMBARGOS_EXECUCAO, EMBARGOS_EXECUCAO_FISCAL, EMBARGOS_TERCEIRO, AGRAVO_PETICAO, CORREICAO_PARCIAL -> InstanceLevel.FIRST_INSTANCE;
            default -> InstanceLevel.FIRST_INSTANCE;
        };
    }

    public String resolveTargetCourtHint(Processo processo, MeshBundle meshBundle, InstanceLevel targetInstance) {
        if (meshBundle != null && meshBundle.plan() != null && meshBundle.plan().routePlan() != null) {
            String detailed = meshBundle.plan().routePlan().tribunalDetalhadoDestino().name();
            if (!detailed.isBlank()) {
                return detailed;
            }
            return meshBundle.plan().routePlan().tribunalDestino().name();
        }
        if (meshBundle != null && meshBundle.admissibility() != null) {
            String tribunalDestino = normalizeNullable(meshBundle.admissibility().tribunalDestino());
            if (tribunalDestino != null) {
                return tribunalDestino;
            }
        }
        String tribunal = firstNonBlank(processo.getTribunalCodigoRoteado(), processo.getTribunal());
        if (targetInstance == InstanceLevel.SUPERIOR) {
            return "STJ";
        }
        if (targetInstance == InstanceLevel.EXTRAORDINARY) {
            return "STF";
        }
        return tribunal == null ? "UNKNOWN" : tribunal;
    }

    public boolean inferAutosApartadosLikely(LegalAppealType appealType, MeshBundle meshBundle) {
        if (meshBundle != null && meshBundle.plan() != null && meshBundle.plan().routePlan() != null) {
            return meshBundle.plan().routePlan().remessa().autosApartadosDependencia()
                    || meshBundle.plan().routePlan().remessa().autuacaoDestino();
        }
        return switch (appealType) {
            case EMBARGOS_EXECUCAO, EMBARGOS_EXECUCAO_FISCAL, EMBARGOS_TERCEIRO, RECLAMACAO_CONSTITUCIONAL, CONFLITO_COMPETENCIA -> true;
            default -> false;
        };
    }

    public String buildEscalonamentoNotes(String recursoNormalizado,
                                          com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshSpeciesType speciesType,
                                          String observacoes,
                                          MeshBundle meshBundle) {
        StringJoiner joiner = new StringJoiner(" | ");
        joiner.add("interposicao_principal=" + recursoNormalizado);
        if (speciesType != null) {
            joiner.add("species=" + speciesType.name());
        }
        if (meshBundle != null && meshBundle.admissibility() != null) {
            putIfPresent(joiner, "routeKind", meshBundle.admissibility().routeKind());
            putIfPresent(joiner, "distributionDesk", meshBundle.admissibility().distributionDesk());
            putIfPresent(joiner, "gabineteDestino", meshBundle.admissibility().gabineteDestino());
            putIfPresent(joiner, "secretariaDestino", meshBundle.admissibility().secretariaDestino());
        }
        if (observacoes != null) {
            joiner.add("observacoes=" + observacoes);
        }
        return joiner.toString();
    }

    public LinkedHashMap<String, Object> buildStrategy(RecursalAdmissibilityResponse admissibility,
                                                       LegalAppealType appealType,
                                                       boolean pedidoEfeitoSuspensivo,
                                                       boolean preparoDispensado) {
        LinkedHashMap<String, Object> strategy = new LinkedHashMap<>();
        strategy.put("eixo", appealType.isExecutoryIncident() ? "INCIDENTE_EXECUTIVO" : appealType.isInternalReview() ? "REVISAO_INTERNA" : "RECURSO");
        strategy.put("recursoExcepcional", appealType.isExceptional());
        strategy.put("pedidoEfeitoSuspensivo", pedidoEfeitoSuspensivo);
        strategy.put("preparoDispensado", preparoDispensado);
        if (admissibility != null) {
            putIfNotNull(strategy, "routeKind", admissibility.routeKind());
            putIfNotNull(strategy, "counterReasonsMode", admissibility.counterReasonsMode());
            putIfNotNull(strategy, "counterReasonsDesk", admissibility.counterReasonsDesk());
            putIfNotNull(strategy, "effectMode", admissibility.effectMode());
            putIfNotNull(strategy, "preventionMode", admissibility.preventionMode());
            putIfNotNull(strategy, "protocolDesk", admissibility.protocolDesk());
            putIfNotNull(strategy, "reviewDesk", admissibility.reviewDesk());
            putIfNotNull(strategy, "supportDesk", admissibility.supportDesk());
            putIfNotNull(strategy, "riskLevel", admissibility.riskLevel());
            strategy.put("stepUpRequired", admissibility.stepUpRequired());
            strategy.put("certificateRequired", admissibility.certificateRequired());
            strategy.put("automaticSuspensiveEffect", admissibility.automaticSuspensiveEffect());
            putIfNotNull(strategy, "connectorSystem", admissibility.connectorSystem());
            putIfNotNull(strategy, "integrationChannel", admissibility.integrationChannel());
            putIfNotNull(strategy, "payloadPolicy", admissibility.payloadPolicy());
            putIfNotNull(strategy, "transmissionMode", admissibility.transmissionMode());
            putIfNotNull(strategy, "proofBundleMode", admissibility.proofBundleMode());
        }
        return strategy;
    }

    public LinkedHashMap<String, Object> buildWorkspaceProjection(Long processoId,
                                                                  LegalAppealType appealType,
                                                                  RecursalAdmissibilityResponse admissibility) {
        LinkedHashMap<String, Object> workspace = new LinkedHashMap<>();
        workspace.put("painelLeitura", "/api/v1/processos/" + processoId + "/painel-leitura");
        workspace.put("conteudoLeitura", "/api/v1/processos/" + processoId + "/painel-leitura/conteudo");
        workspace.put("fluxoLeitura", "/api/v1/processos/" + processoId + "/painel-leitura/fluxo");
        workspace.put("especializacaoLeitura", "/api/v1/processos/" + processoId + "/painel-leitura/especializacao");
        workspace.put("ecossistemaLeitura", "/api/v1/processos/" + processoId + "/painel-leitura/ecossistema");
        workspace.put("buscaLeitura", "/api/v1/processos/" + processoId + "/painel-leitura/busca?q=" + appealType.name());
        if (admissibility != null) {
            putIfNotNull(workspace, "reviewDesk", admissibility.reviewDesk());
            putIfNotNull(workspace, "sessionMode", admissibility.sessionMode());
            putIfNotNull(workspace, "routingBucket", admissibility.routingBucket());
            putIfNotNull(workspace, "counterReasonsMode", admissibility.counterReasonsMode());
        }
        return workspace;
    }

    public Map<String, Object> buildAssistedFilingProjection(Processo processo,
                                                             LegalAppealType appealType,
                                                             RecursalAdmissibilityResponse admissibility,
                                                             boolean pedidoEfeitoSuspensivo,
                                                             boolean preparoDispensado) {
        return RecursalFilingBlueprintAssembler.assemble(
                processo,
                appealType,
                admissibility,
                pedidoEfeitoSuspensivo,
                preparoDispensado
        );
    }

    public LinkedHashMap<String, Object> buildGraphProjection(RecursalGraphResponse graphSnapshot, Long processoId) {
        LinkedHashMap<String, Object> graph = new LinkedHashMap<>();
        graph.put("graphEndpoint", "/api/v1/intelligence/recursal/processo/" + processoId + "/graph");
        graph.put("factsEndpoint", "/api/v1/intelligence/recursal/processo/" + processoId + "/facts");
        graph.put("autuacaoDestinoEndpoint", "/api/v1/intelligence/recursal/processo/" + processoId + "/autuacao-destino");
        graph.put("attachmentsEndpoint", "/api/v1/intelligence/recursal/processo/" + processoId + "/attachments");
        graph.put("meshPlanEndpoint", "/api/v1/intelligence/recursal/mesh/plan");
        graph.put("meshAggregateByProcessoEndpoint", "/api/v1/intelligence/recursal/mesh/aggregate/processo/" + processoId);
        graph.put("meshSearchEndpoint", "/api/v1/intelligence/recursal/mesh/aggregate/search");
        graph.put("meshDashboardEndpoint", "/api/v1/intelligence/recursal/mesh/aggregate/dashboard");
        if (graphSnapshot != null) {
            graph.put("graphSummary", graphSnapshot.summary());
            graph.put("graphNodes", graphSnapshot.nodes().size());
            graph.put("graphEdges", graphSnapshot.edges().size());
        }
        return graph;
    }

    public LinkedHashMap<String, Object> buildEndpoints(Long processoId) {
        LinkedHashMap<String, Object> endpoints = new LinkedHashMap<>();
        endpoints.put("painelLeitura", "/api/v1/processos/" + processoId + "/painel-leitura");
        endpoints.put("especializacaoLeitura", "/api/v1/processos/" + processoId + "/painel-leitura/especializacao");
        endpoints.put("graphRecursal", "/api/v1/intelligence/recursal/processo/" + processoId + "/graph");
        endpoints.put("factsRecursais", "/api/v1/intelligence/recursal/processo/" + processoId + "/facts");
        endpoints.put("autuacaoDestinoRecursal", "/api/v1/intelligence/recursal/processo/" + processoId + "/autuacao-destino");
        endpoints.put("attachmentsRecursais", "/api/v1/intelligence/recursal/processo/" + processoId + "/attachments");
        endpoints.put("meshPlan", "/api/v1/intelligence/recursal/mesh/plan");
        endpoints.put("meshTransition", "/api/v1/intelligence/recursal/mesh/transition");
        endpoints.put("meshAggregateByProcesso", "/api/v1/intelligence/recursal/mesh/aggregate/processo/" + processoId);
        endpoints.put("meshSearch", "/api/v1/intelligence/recursal/mesh/aggregate/search");
        endpoints.put("meshDashboard", "/api/v1/intelligence/recursal/mesh/aggregate/dashboard");
        endpoints.put("meshSearchReindex", "/api/v1/intelligence/recursal/mesh/aggregate/search/reindex");
        endpoints.put("meshSearchDrift", "/api/v1/intelligence/recursal/mesh/aggregate/search/drift");
        endpoints.put("meshOperationalAlerts", "/api/v1/intelligence/recursal/mesh/aggregate/operations/alerts");
        endpoints.put("admissibilidadeRecursal", "/api/v1/processual/recursal/admissibilidade");
        endpoints.put("iaConferenciaRecursal", "/api/v1/processual/recursal/ia/conferencia");
        endpoints.put("govBrStepUp", "/api/v1/auth/govbr/stepup/start");
        endpoints.put("passkeyStepUp", "/api/v1/auth/stepup/options");
        endpoints.put("laianeProtocolPreflight", "/api/v1/laiane/protocol/preflight");
        endpoints.put("laianeProtocolPackage", "/api/v1/laiane/lawyer/peticao/protocol-package");
        endpoints.put("temasRepetitivos", "/api/v1/ministro/temas-repetitivos");
        endpoints.put("precedentesQualificados", "/api/v1/processual/substituicao/precedentes-qualificados");
        endpoints.put("sigiloInteligente", "/api/v1/processual/unificado/" + processoId + "/sigilo-inteligente");
        endpoints.put("sigiloNotificacoes", "/api/v1/processual/unificado/" + processoId + "/sigilo-notificacoes");
        endpoints.put("sigiloZk", "/api/v1/processos/sigilo/zk");
        return endpoints;
    }

    public void enrichStrategyWithSigilo(Map<String, Object> strategy, Map<String, Object> sigiloRecursal) {
        if (strategy == null || sigiloRecursal == null || sigiloRecursal.isEmpty()) {
            return;
        }
        putIfNotNull(strategy, "sigiloStatus", sigiloRecursal.get("status"));
        putIfNotNull(strategy, "nivelSigiloRecursal", sigiloRecursal.get("nivelRecomendado"));
        putIfNotNull(strategy, "protocolSubmissionMode", sigiloRecursal.get("protocolSubmissionMode"));
        putIfNotNull(strategy, "certificateOrStrongCredentialRequired", sigiloRecursal.get("certificateOrStrongCredentialRequired"));
        putIfNotNull(strategy, "revisaoJudicialObrigatoria", sigiloRecursal.get("revisaoJudicialObrigatoria"));
        putIfNotNull(strategy, "contrarrazoesControladas", sigiloRecursal.get("contrarrazoesControladas"));
    }

    public void enrichWorkspaceWithSigilo(Map<String, Object> workspace, Long processoId, Map<String, Object> sigiloRecursal) {
        if (workspace == null || processoId == null || sigiloRecursal == null || sigiloRecursal.isEmpty()) {
            return;
        }
        putIfNotNull(workspace, "workspaceLeituraModo", sigiloRecursal.get("workspaceLeituraModo"));
        putIfNotNull(workspace, "nivelSigiloRecursal", sigiloRecursal.get("nivelRecomendado"));
        putIfNotNull(workspace, "stepUpAcessoRecurso", sigiloRecursal.get("stepUpAcessoRecurso"));
        workspace.put("sigiloInteligente", "/api/v1/processual/unificado/" + processoId + "/sigilo-inteligente");
        workspace.put("sigiloNotificacoes", "/api/v1/processual/unificado/" + processoId + "/sigilo-notificacoes");
    }

    private String resolveDecisionCarryOverScope(LegalAppealType appealType, Map<String, Object> distribuicaoRecursal) {
        if (appealType == LegalAppealType.EMBARGOS_DECLARACAO) {
            return "EMBARGOS_MESMO_GRAU";
        }
        if (Boolean.TRUE.equals(distribuicaoRecursal == null ? null : distribuicaoRecursal.get("autosApartadosDependencia"))) {
            return "INCIDENTE_APARTADO_DEPENDENCIA";
        }
        return "RECURSO_GRAU_SUPERIOR";
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private void putIfPresent(StringJoiner joiner, String key, String value) {
        if (joiner == null || value == null || value.isBlank()) {
            return;
        }
        joiner.add(key + '=' + value);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = normalizeNullable(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private void putIfNotNull(Map<String, Object> target, String key, Object value) {
        if (target == null || key == null || value == null) {
            return;
        }
        target.put(key, value);
    }
}
