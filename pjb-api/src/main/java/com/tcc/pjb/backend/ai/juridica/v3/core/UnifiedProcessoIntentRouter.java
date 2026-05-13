package com.tcc.pjb.backend.ai.juridica.v3.core;

import com.tcc.pjb.backend.ai.academy.CurriculumKnowledgeService;
import com.tcc.pjb.backend.ai.academy.CurriculumSnapshot;
import com.tcc.pjb.backend.ai.juridica.v3.core.AjuizamentoIntent;
import com.tcc.pjb.backend.ai.juridica.v3.core.RamoDescriptor;
import com.tcc.pjb.backend.core.procedural.CanonicalRitoSelector;
import com.tcc.pjb.backend.core.procedural.CanonicalRitoSelector.SelectedRito;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.integration.judicial.routing.TribunalProtocolRoutingService;
import com.tcc.pjb.backend.integration.judicial.routing.TribunalProtocolRoutingService.RoutingDecision;
import com.tcc.pjb.backend.modules.laiane.service.LaianeNationalPreflightService;
import com.tcc.pjb.backend.modules.laiane.service.LaianeNationalPreflightService.PreflightResult;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.service.procedural.ProceduralArchitectureSanityService;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import com.tcc.pjb.backend.service.rito.workflow.ProceduralWorkflowBpmnService;
import com.tcc.pjb.backend.service.rito.workflow.ProceduralWorkflowBpmnService.GeneratedWorkflow;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionDescriptor;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class UnifiedProcessoIntentRouter {

    private static final Duration ASYNC_AWAIT_TIMEOUT = Duration.ofSeconds(5);

    public record RouterDecision(
            String requestId,
            Instant timestamp,
            AjuizamentoIntent intent,
            Map<String, Object> ritoWorkflow,
            RamoDescriptor ramoDescriptor,
            String ramoProjeto,
            String ritoGrupo,
            Map<String, Object> curriculum,
            Map<String, Object> knowledge,
            Map<String, Object> tribunalRouting,
            Map<String, Object> preflight,
            Map<String, Object> canonicalContext,
            Map<String, Object> architectureSanity,
            Map<String, Object> generatedWorkflow,
            String recomendacaoFinal,
            List<String> alertasCriticos,
            List<String> camposObrigatorios,
            List<String> documentosEssenciais,
            List<String> proximosPassos,
            double confiancaGlobal,
            String sistemaProtocolo,
            String status,
            boolean segredoJustica,
            boolean exigeMP,
            boolean admiteConciliacao
    ) {}

    private final AjuizamentoIntentEngine intentEngine;
    private final LegalRitosEngine ritosEngine;
    private final CurriculumKnowledgeService curriculumKnowledgeService;
    private final ProceduralCatalogService proceduralCatalogService;
    private final TribunalProtocolRoutingService tribunalProtocolRoutingService;
    private final LaianeNationalPreflightService nationalPreflightService;
    private final ProceduralWorkflowBpmnService proceduralWorkflowBpmnService;
    private final CanonicalRitoSelector canonicalRitoSelector;
    private final ProceduralArchitectureSanityService proceduralArchitectureSanityService;
    private final PjbExecutionOrchestrator executionOrchestrator;

    public UnifiedProcessoIntentRouter(AjuizamentoIntentEngine intentEngine,
                                       LegalRitosEngine ritosEngine,
                                       CurriculumKnowledgeService curriculumKnowledgeService,
                                       ProceduralCatalogService proceduralCatalogService,
                                       TribunalProtocolRoutingService tribunalProtocolRoutingService,
                                       LaianeNationalPreflightService nationalPreflightService,
                                       ProceduralWorkflowBpmnService proceduralWorkflowBpmnService,
                                       CanonicalRitoSelector canonicalRitoSelector,
                                       ProceduralArchitectureSanityService proceduralArchitectureSanityService,
                                       PjbExecutionOrchestrator executionOrchestrator) {
        this.intentEngine = Objects.requireNonNull(intentEngine);
        this.ritosEngine = Objects.requireNonNull(ritosEngine);
        this.curriculumKnowledgeService = Objects.requireNonNull(curriculumKnowledgeService);
        this.proceduralCatalogService = Objects.requireNonNull(proceduralCatalogService);
        this.tribunalProtocolRoutingService = Objects.requireNonNull(tribunalProtocolRoutingService);
        this.nationalPreflightService = Objects.requireNonNull(nationalPreflightService);
        this.proceduralWorkflowBpmnService = Objects.requireNonNull(proceduralWorkflowBpmnService);
        this.canonicalRitoSelector = Objects.requireNonNull(canonicalRitoSelector);
        this.proceduralArchitectureSanityService = Objects.requireNonNull(proceduralArchitectureSanityService);
        this.executionOrchestrator = Objects.requireNonNull(executionOrchestrator, "executionOrchestrator");
    }

    public RouterDecision route(Map<String, Object> requestPayload) {
        Objects.requireNonNull(requestPayload, "requestPayload");
        String requestId = UUID.randomUUID().toString();
        Map<String, Object> safePayload = new LinkedHashMap<>(requestPayload);
        AjuizamentoIntent intent = intentEngine.inferir(safePayload);
        Map<String, Object> canonicalPayload = mergePayloadWithIntent(safePayload, intent);
        SelectedRito selectedRito = canonicalRitoSelector.select(
                canonicalPayload,
                intent.rito(),
                "unified_processo_intent_router"
        );
        CanonicalContext canonicalContext = selectedRito.canonicalContext();
        var effectiveRito = selectedRito.rito();
        String ramoCodigo = BrazilianLegalKnowledgeBase.resolveRamoCodigo(intent.ramoDireito());
        RamoDescriptor ramoDescriptor = BrazilianLegalKnowledgeBase.resolve(ramoCodigo);
        CurriculumSnapshot curriculumSnapshot = curriculumKnowledgeService.snapshot(ramoCodigo, intent.subRamo(), effectiveRito);

        CompletableFuture<Map<String, Object>> ritoFuture = executionOrchestrator
                .supply(PjbExecutionDescriptor.burst("router.rito-workflow", ASYNC_AWAIT_TIMEOUT), () -> computeRitoWorkflow(intent, canonicalPayload))
                .exceptionally(ex -> PayloadMaps.ofEntries("status", "rito_engine_error", "erro", safeErrorMessage(ex)));
        CompletableFuture<PreflightResult> preflightFuture = executionOrchestrator
                .supply(PjbExecutionDescriptor.burst("router.preflight", ASYNC_AWAIT_TIMEOUT), () -> nationalPreflightService.analyze(canonicalPayload, canonicalContext))
                .exceptionally(ex -> new PreflightResult(requestId, Instant.now(), effectiveRito != null ? effectiveRito.name() : null, intent.ramoDireito(), null, null, null, null, null, false, List.of(), List.of(), List.of(), List.of(), List.of(), PayloadMaps.ofEntries("status", "preflight_error", "erro", safeErrorMessage(ex))));
        CompletableFuture<GeneratedWorkflow> generatedWorkflowFuture = executionOrchestrator
                .supply(PjbExecutionDescriptor.burst("router.generated-workflow", ASYNC_AWAIT_TIMEOUT), () -> proceduralWorkflowBpmnService.generate(canonicalContext))
                .exceptionally(ex -> new GeneratedWorkflow(effectiveRito != null ? effectiveRito.name() : null, "erro", "", "", PayloadMaps.ofEntries("status", "workflow_generation_error", "erro", safeErrorMessage(ex), "canonicalContext", canonicalContext != null ? canonicalContext.toMap() : null)));

        Map<String, Object> ritoWorkflow = await(ritoFuture, PayloadMaps.ofEntries("status", "rito_engine_error"));
        PreflightResult preflight = await(preflightFuture, null);
        GeneratedWorkflow generatedWorkflow = await(generatedWorkflowFuture, null);

        String ramoProjeto = BrazilianLegalKnowledgeBase.toProjetoRamo(ramoCodigo, intent.subRamo(), intent.esfera()).name();
        Map<String, Object> curriculum = curriculumKnowledgeService.describe(ramoCodigo, intent.subRamo(), effectiveRito);
        Map<String, Object> knowledge = BrazilianLegalKnowledgeBase.describeForRouter(ramoCodigo, intent.subRamo(), intent.esfera(), toDouble(safePayload.get("valor_causa")));
        RoutingDecision routing = tribunalProtocolRoutingService.resolve(
                canonicalPayload,
                effectiveRito,
                canonicalContext.ramoDireito() != null ? canonicalContext.ramoDireito() : intent.ramoDireito(),
                preflight != null ? preflight.competencia() : firstNonBlank(intent.competencia(), canonicalContext.ramoJusticaNacional()),
                false
        );
        List<String> alertasCriticos = buildAlertasCriticos(intent, ramoDescriptor, safePayload, ritoWorkflow, curriculumSnapshot, preflight, routing);
        List<String> camposObrigatorios = mergeDistinct(intent.camposObrigatorios(), castStringList(ritoWorkflow.get("requiredInputFields")));
        if (preflight != null) {
            camposObrigatorios = mergeDistinct(camposObrigatorios, preflight.requiredPartyRoles(), preflight.requiredDocuments());
        }
        List<String> documentosEssenciais = mergeDistinct(intent.documentosEssenciais(), castStringList(ritoWorkflow.get("requiredDocuments")));
        if (preflight != null) {
            documentosEssenciais = mergeDistinct(documentosEssenciais, preflight.requiredDocuments());
        }
        List<String> proximosPassos = mergeDistinct(intent.proximosPassos(), castStringList(ritoWorkflow.get("passos")), routing.warnings());
        String recomendacao = buildRecomendacao(intent, ramoDescriptor, ritoWorkflow, curriculumSnapshot, knowledge, preflight, routing, generatedWorkflow);
        double confiancaGlobal = computeConfiancaGlobal(intent, ritoWorkflow, ramoDescriptor, preflight);
        String sistemaProtocolo = routing.judicialSystem().name();
        String status = resolveStatus(confiancaGlobal, alertasCriticos, preflight);
        return new RouterDecision(
                requestId,
                Instant.now(),
                intent,
                Collections.unmodifiableMap(new LinkedHashMap<>(ritoWorkflow)),
                ramoDescriptor,
                ramoProjeto,
                effectiveRito != null ? effectiveRito.group() : "CIVIL",
                curriculum,
                knowledge,
                routingToMap(routing),
                preflightToMap(preflight),
                canonicalContextMap(canonicalContext, selectedRito),
                proceduralArchitectureSanityService.report().toMap(),
                generatedWorkflowToMap(generatedWorkflow),
                recomendacao,
                List.copyOf(alertasCriticos),
                List.copyOf(camposObrigatorios),
                List.copyOf(documentosEssenciais),
                List.copyOf(proximosPassos),
                confiancaGlobal,
                sistemaProtocolo,
                status,
                intent.segredoJustica(),
                intent.exigeMP(),
                intent.admiteConciliacao()
        );
    }


    private Map<String, Object> canonicalContextMap(CanonicalContext canonicalContext, SelectedRito selectedRito) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (canonicalContext != null) {
            out.putAll(canonicalContext.toMap());
        }
        if (selectedRito != null) {
            out.put("ritoSelection", selectedRito.toMap());
            out.put("sanityGate", selectedRito.sanityGate() != null ? selectedRito.sanityGate().toMap() : Map.of());
            out.put("status", selectedRito.status());
        }
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> computeRitoWorkflow(AjuizamentoIntent intent, Map<String, Object> payload) {
        if (intent.rito() == null) {
            return Map.of("status", "rito_indefinido");
        }
        Map<String, Object> ritoCtx = new LinkedHashMap<>(payload);
        ritoCtx.put("rito", intent.rito());
        ritoCtx.put("rito_processual", intent.rito());
        ritoCtx.put("ambito_direito", intent.ramoDireito());
        ritoCtx.put("ramo_direito", intent.ramoDireito());
        ritoCtx.put("sub_ramo", intent.subRamo());
        try {
            Map<String, Object> out = new LinkedHashMap<>(ritosEngine.inferRito(ritoCtx));
            out.put("catalog", proceduralCatalogService.describe(intent.rito()));
            out.put("requiredPartyRoles", proceduralCatalogService.requiredParties(intent.rito()).stream().map(p -> p.code()).toList());
            out.put("requiredDocuments", proceduralCatalogService.requiredDocuments(intent.rito()));
            return out;
        } catch (Exception ex) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("status", "rito_engine_error");
            if (!intent.rito().isBlank()) {
                out.put("rito", intent.rito());
            }
            out.put("erro", String.valueOf(ex.getMessage()));
            Map<String, Object> catalog = intent.rito().isBlank()
                    ? Map.of()
                    : proceduralCatalogService.describe(intent.rito());
            out.put("catalog", catalog == null ? Map.of() : catalog);
            return Collections.unmodifiableMap(out);
        }
    }

    private List<String> buildAlertasCriticos(AjuizamentoIntent intent,
                                              RamoDescriptor ramoDescriptor,
                                              Map<String, Object> payload,
                                              Map<String, Object> ritoWorkflow,
                                              CurriculumSnapshot curriculumSnapshot,
                                              PreflightResult preflight,
                                              RoutingDecision routing) {
        List<String> alertas = new ArrayList<>(intent.alertas());
        if (intent.confianca() < 0.55) {
            alertas.add("Confiança baixa. Revise assunto, ramo_direito, classe TPU e rito_processual.");
        }
        if (intent.rito() == null) {
            alertas.add("Rito processual indeterminado. Informe o tipo de ação e a classe processual.");
        }
        if (ramoDescriptor != null && !ramoDescriptor.prazosPrincipais().isEmpty()) {
            alertas.add("Prazo crítico inicial: " + ramoDescriptor.prazosPrincipais().get(0));
        }
        if (intent.segredoJustica()) {
            alertas.add("Matéria com controle reforçado de sigilo. Classifique o acesso antes do protocolo.");
        }
        if (intent.exigeMP()) {
            alertas.add("Atuação obrigatória do Ministério Público identificada.");
        }
        if (intent.admiteConciliacao()) {
            alertas.add("Matéria compatível com autocomposição. Avalie CEJUSC ou mediação prévia.");
        }
        if (!"ok".equals(String.valueOf(ritoWorkflow.get("status"))) && intent.rito() != null) {
            alertas.add("Workflow do rito não confirmado integralmente no pack processual.");
        }
        curriculumSnapshot.prazosCriticos().stream().limit(2).forEach(p -> alertas.add("Radar curricular: " + p));
        checkPrescricao(intent, alertas);
        checkValorCausa(payload, alertas);
        if (preflight != null) {
            preflight.issues().stream().limit(6).forEach(issue -> alertas.add(issue.severity() + ": " + issue.message()));
        }
        if (routing != null) {
            alertas.addAll(routing.warnings());
            if (routing.stepUpRequired()) {
                alertas.add("O conector judicial selecionado exige autenticação reforçada Gov.br.");
            }
            if (routing.certificateRequired()) {
                alertas.add("O conector judicial selecionado exige certificado/assinatura qualificada.");
            }
        }
        return mergeDistinct(alertas);
    }

    private void checkPrescricao(AjuizamentoIntent intent, List<String> alertas) {
        switch (intent.ramoDireito()) {
            case "TRABALHISTA" -> alertas.add("Prescrição trabalhista: 2 anos após a rescisão e 5 anos retroativos.");
            case "PENAL" -> alertas.add("Verifique prescrição penal antes do protocolo de medidas ou impugnações.");
            case "TRIBUTARIO" -> alertas.add("Verifique decadência e prescrição tributárias antes do ajuizamento.");
            case "ELEITORAL" -> alertas.add("Prazos eleitorais tendem a ser fatais e contados em dias corridos.");
            case "CONSUMIDOR" -> alertas.add("Vício do produto: 30/90 dias. Danos ao consumidor: 5 anos.");
            case "AMBIENTAL" -> alertas.add("Dano ambiental tem regime prescricional diferenciado e alta criticidade.");
            case "INFANCIA_JUVENTUDE" -> alertas.add("Matéria da infância exige prioridade absoluta e despacho célere.");
            case "AGRARIO" -> alertas.add("Conflitos agrários coletivos podem exigir mediação e audiência prévia.");
            default -> {
            }
        }
    }

    private void checkValorCausa(Map<String, Object> payload, List<String> alertas) {
        Object v = payload.get("valor_causa");
        if (v == null || String.valueOf(v).isBlank() || "0".equals(String.valueOf(v))) {
            alertas.add("Valor da causa ausente ou zerado. Sistema não calcula custas e competência especial sem esse dado.");
        }
    }

    private String buildRecomendacao(AjuizamentoIntent intent,
                                     RamoDescriptor ramoDescriptor,
                                     Map<String, Object> ritoWorkflow,
                                     CurriculumSnapshot curriculumSnapshot,
                                     Map<String, Object> knowledge,
                                     PreflightResult preflight,
                                     RoutingDecision routing,
                                     GeneratedWorkflow generatedWorkflow) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== RECOMENDACAO UNIFICADA PJB ===\n\n");
        sb.append("Ramo do Direito: ").append(ramoDescriptor != null ? ramoDescriptor.nome() : intent.ramoDireito()).append("\n");
        sb.append("Ramo Projeto: ").append(BrazilianLegalKnowledgeBase.toProjetoRamo(intent.ramoDireito(), intent.subRamo(), intent.esfera()).name()).append("\n");
        sb.append("Sub-ramo: ").append(intent.subRamo()).append("\n");
        sb.append("Esfera: ").append(intent.esfera()).append("\n");
        sb.append("Rito Processual: ").append(intent.rito() != null ? intent.rito() : "INDEFINIDO").append("\n");
        sb.append("Tipo de Acao: ").append(intent.tipoAcao()).append("\n");
        sb.append("Competencia: ").append(preflight != null && preflight.competencia() != null ? preflight.competencia() : intent.competencia()).append("\n");
        sb.append("Confianca: ").append(Math.round(intent.confianca() * 100)).append("%\n");
        sb.append("Segredo de Justica: ").append(intent.segredoJustica() ? "SIM" : "NAO").append("\n");
        sb.append("Exige MP: ").append(intent.exigeMP() ? "SIM" : "NAO").append("\n");
        sb.append("Admite Conciliacao: ").append(intent.admiteConciliacao() ? "SIM" : "NAO").append("\n");
        sb.append("Sistema de Protocolo Selecionado: ").append(routing.judicialSystem().name()).append("\n");
        sb.append("Tribunal de Destino: ").append(routing.tribunalCodigo()).append(" - ").append(routing.tribunalNome()).append("\n\n");
        sb.append("Fundamentos Legais:\n").append(intent.fundamento()).append("\n\n");
        if (ramoDescriptor != null && !ramoDescriptor.precedentesEstruturantes().isEmpty()) {
            sb.append("Precedentes Estruturantes:\n");
            ramoDescriptor.precedentesEstruturantes().stream().limit(5).forEach(p -> sb.append(" - ").append(p).append("\n"));
            sb.append("\n");
        }
        if (!curriculumSnapshot.materiasPrioritarias().isEmpty()) {
            sb.append("Trilha Prioritaria de Estudo/Validacao:\n");
            curriculumSnapshot.materiasPrioritarias().stream().limit(4).forEach(c -> sb.append(" - ").append(c).append("\n"));
            sb.append("\n");
        }
        if (preflight != null) {
            sb.append("Preflight Nacional:\n");
            sb.append(" Status: ").append(preflight.readyForSubmission() ? "PRONTO" : "AJUSTES NECESSARIOS").append("\n");
            sb.append(" Tribunal: ").append(preflight.tribunalCodigo()).append("\n");
            sb.append(" Unidade: ").append(preflight.unidadeJudiciariaCodigo()).append("\n");
            sb.append(" Sistema: ").append(preflight.connectorSystem()).append("\n");
            if (!preflight.missingPartyRoles().isEmpty()) {
                sb.append(" Partes faltantes: ").append(String.join(", ", preflight.missingPartyRoles())).append("\n");
            }
            if (!preflight.missingDocuments().isEmpty()) {
                sb.append(" Documentos faltantes: ").append(String.join(", ", preflight.missingDocuments())).append("\n");
            }
            sb.append("\n");
        }
        if (generatedWorkflow != null) {
            sb.append("Workflow Gerado Dinamicamente:\n");
            sb.append(" ProcessId: ").append(generatedWorkflow.processId()).append("\n");
            sb.append(" Checksum: ").append(generatedWorkflow.checksum()).append("\n\n");
        }
        Object competenceHints = knowledge.get("competenceHints");
        if (competenceHints instanceof List<?> hints && !hints.isEmpty()) {
            sb.append("Pistas de Competencia:\n");
            hints.stream().limit(3).forEach(h -> sb.append(" - ").append(h).append("\n"));
            sb.append("\n");
        }
        return sb.toString();
    }

    private double computeConfiancaGlobal(AjuizamentoIntent intent,
                                          Map<String, Object> ritoWorkflow,
                                          RamoDescriptor ramoDescriptor,
                                          PreflightResult preflight) {
        double base = intent.confianca();
        if (ritoWorkflow.containsKey("title")) {
            base = Math.min(1.0, base + 0.08);
        }
        if ("ok".equals(ritoWorkflow.get("status"))) {
            base = Math.min(1.0, base + 0.05);
        }
        if (intent.rito() != null) {
            base = Math.min(1.0, base + 0.05);
        }
        if (ramoDescriptor != null) {
            base = Math.min(1.0, base + 0.03);
        }
        if (!"GERAL".equals(intent.subRamo()) && !intent.subRamo().endsWith("_GERAL")) {
            base = Math.min(1.0, base + 0.03);
        }
        if (preflight != null && preflight.readyForSubmission()) {
            base = Math.min(1.0, base + 0.08);
        }
        if (preflight != null && !preflight.readyForSubmission()) {
            base = Math.max(0.0, base - 0.12);
        }
        return Math.round(base * 100.0) / 100.0;
    }

    private String resolveStatus(double confiancaGlobal, List<String> alertasCriticos, PreflightResult preflight) {
        boolean hasCritical = alertasCriticos.stream().anyMatch(a -> a != null && a.toUpperCase().contains("ERROR"));
        if (preflight != null && !preflight.readyForSubmission()) {
            return "PRECHECK_PENDING";
        }
        if (confiancaGlobal >= 0.85 && !hasCritical) {
            return "ROTEADO_ALTA_CONFIANCA";
        }
        if (confiancaGlobal >= 0.70) {
            return "ROTEADO";
        }
        if (confiancaGlobal >= 0.50) {
            return "INCERTO";
        }
        return "REVISAR_MANUALMENTE";
    }

    private Map<String, Object> mergePayloadWithIntent(Map<String, Object> payload, AjuizamentoIntent intent) {
        Map<String, Object> merged = new LinkedHashMap<>(payload);
        if (intent.rito() != null) {
            merged.putIfAbsent("rito_processual", intent.rito());
            merged.putIfAbsent("rito", intent.rito());
        }
        merged.putIfAbsent("ramo_direito", intent.ramoDireito());
        merged.putIfAbsent("esfera", intent.esfera());
        merged.putIfAbsent("competencia", intent.competencia());
        if (intent.proceduralRouting() != null) {
            merged.putIfAbsent("tribunalCodigo", intent.proceduralRouting().tribunalCodigo());
            merged.putIfAbsent("varaPretendida", intent.proceduralRouting().varaSugerida());
            merged.putIfAbsent("tipoJustica", intent.proceduralRouting().tipoJusticaSugerida());
            merged.putIfAbsent("proceduralTrack", intent.proceduralRouting().proceduralTrack());
        }
        return merged;
    }

    private Map<String, Object> routingToMap(RoutingDecision routing) {
        if (routing == null) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("tribunalCodigo", routing.tribunalCodigo());
        out.put("tribunalNome", routing.tribunalNome());
        out.put("judicialSystem", routing.judicialSystem().name());
        out.put("stepUpRequired", routing.stepUpRequired());
        out.put("certificateRequired", routing.certificateRequired());
        out.put("warnings", routing.warnings());
        out.put("capability", routing.capability());
        out.put("metadata", routing.metadata());
        return out;
    }

    private Map<String, Object> preflightToMap(PreflightResult preflight) {
        if (preflight == null) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("readyForSubmission", preflight.readyForSubmission());
        out.put("tribunalCodigo", preflight.tribunalCodigo());
        out.put("unidadeJudiciariaCodigo", preflight.unidadeJudiciariaCodigo());
        out.put("connectorSystem", preflight.connectorSystem());
        out.put("requiredPartyRoles", preflight.requiredPartyRoles());
        out.put("missingPartyRoles", preflight.missingPartyRoles());
        out.put("requiredDocuments", preflight.requiredDocuments());
        out.put("missingDocuments", preflight.missingDocuments());
        out.put("issues", preflight.issues());
        out.put("details", preflight.details());
        return out;
    }

    private Map<String, Object> generatedWorkflowToMap(GeneratedWorkflow generatedWorkflow) {
        if (generatedWorkflow == null) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("rito", generatedWorkflow.rito());
        out.put("processId", generatedWorkflow.processId());
        out.put("checksum", generatedWorkflow.checksum());
        out.put("blueprint", generatedWorkflow.blueprint());
        out.put("bpmnXml", generatedWorkflow.bpmnXml());
        return out;
    }

    private <T> T await(CompletableFuture<T> future, T fallback) {
        try {
            return future.get(ASYNC_AWAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            future.cancel(true);
            return fallback;
        }
    }

    private String safeErrorMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof java.util.concurrent.CompletionException completionException && completionException.getCause() != null) {
            current = completionException.getCause();
        }
        return current == null || current.getMessage() == null || current.getMessage().isBlank()
                ? "async_execution_failed"
                : current.getMessage();
    }

    private static List<String> castStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (Object item : list) {
            if (item != null) {
                String text = String.valueOf(item).trim();
                if (!text.isBlank()) {
                    out.add(text);
                }
            }
        }
        return out;
    }

    @SafeVarargs
    private static List<String> mergeDistinct(List<String>... values) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (List<String> list : values) {
            if (list == null) {
                continue;
            }
            for (String item : list) {
                if (item == null) {
                    continue;
                }
                String text = item.trim();
                if (!text.isBlank()) {
                    out.add(text);
                }
            }
        }
        return new ArrayList<>(out);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            String normalized = String.valueOf(value).replace("R$", "").replace(".", "").replace(",", ".").trim();
            return normalized.isBlank() ? null : Double.parseDouble(normalized);
        } catch (Exception ignored) {
            return null;
        }
    }
}
