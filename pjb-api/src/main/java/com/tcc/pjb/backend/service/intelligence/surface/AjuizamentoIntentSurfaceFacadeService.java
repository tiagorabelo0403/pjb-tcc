package com.tcc.pjb.backend.service.intelligence.surface;

import com.tcc.pjb.backend.ai.academy.CurriculumKnowledgeService;
import com.tcc.pjb.backend.ai.juridica.v3.core.AjuizamentoIntent;
import com.tcc.pjb.backend.ai.juridica.v3.core.AjuizamentoIntentEngine;
import com.tcc.pjb.backend.ai.juridica.v3.core.BrazilianLegalKnowledgeBase;
import com.tcc.pjb.backend.ai.juridica.v3.core.RamoDescriptor;
import com.tcc.pjb.backend.ai.juridica.v3.core.UnifiedProcessoIntentRouter;
import com.tcc.pjb.backend.ai.juridica.v3.core.UnifiedProcessoIntentRouter.RouterDecision;
import com.tcc.pjb.backend.core.procedural.CanonicalRitoSelector;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.integration.cnj.CnjTpuSyncService;
import com.tcc.pjb.backend.integration.judicial.routing.TribunalProtocolRoutingService;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.modules.laiane.service.LaianeNationalPreflightService;
import com.tcc.pjb.backend.service.procedural.ProceduralArchitectureSanityService;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import com.tcc.pjb.backend.service.rito.workflow.ProceduralWorkflowBpmnService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class AjuizamentoIntentSurfaceFacadeService {

    private final AjuizamentoIntentEngine intentEngine;
    private final UnifiedProcessoIntentRouter router;
    private final CurriculumKnowledgeService curriculumKnowledgeService;
    private final ProceduralCatalogService proceduralCatalogService;
    private final LaianeNationalPreflightService nationalPreflightService;
    private final TribunalProtocolRoutingService tribunalProtocolRoutingService;
    private final ProceduralWorkflowBpmnService proceduralWorkflowBpmnService;
    private final CnjTpuSyncService cnjTpuSyncService;
    private final ProceduralCanonicalResolver proceduralCanonicalResolver;
    private final CanonicalRitoSelector canonicalRitoSelector;
    private final ProceduralArchitectureSanityService proceduralArchitectureSanityService;
    private final SurfaceProjectionSupport projectionSupport;

    public AjuizamentoIntentSurfaceFacadeService(AjuizamentoIntentEngine intentEngine,
                                                 UnifiedProcessoIntentRouter router,
                                                 CurriculumKnowledgeService curriculumKnowledgeService,
                                                 ProceduralCatalogService proceduralCatalogService,
                                                 LaianeNationalPreflightService nationalPreflightService,
                                                 TribunalProtocolRoutingService tribunalProtocolRoutingService,
                                                 ProceduralWorkflowBpmnService proceduralWorkflowBpmnService,
                                                 CnjTpuSyncService cnjTpuSyncService,
                                                 ProceduralCanonicalResolver proceduralCanonicalResolver,
                                                 CanonicalRitoSelector canonicalRitoSelector,
                                                 ProceduralArchitectureSanityService proceduralArchitectureSanityService,
                                                 SurfaceProjectionSupport projectionSupport) {
        this.intentEngine = Objects.requireNonNull(intentEngine);
        this.router = Objects.requireNonNull(router);
        this.curriculumKnowledgeService = Objects.requireNonNull(curriculumKnowledgeService);
        this.proceduralCatalogService = Objects.requireNonNull(proceduralCatalogService);
        this.nationalPreflightService = Objects.requireNonNull(nationalPreflightService);
        this.tribunalProtocolRoutingService = Objects.requireNonNull(tribunalProtocolRoutingService);
        this.proceduralWorkflowBpmnService = Objects.requireNonNull(proceduralWorkflowBpmnService);
        this.cnjTpuSyncService = Objects.requireNonNull(cnjTpuSyncService);
        this.proceduralCanonicalResolver = Objects.requireNonNull(proceduralCanonicalResolver);
        this.canonicalRitoSelector = Objects.requireNonNull(canonicalRitoSelector);
        this.proceduralArchitectureSanityService = Objects.requireNonNull(proceduralArchitectureSanityService);
        this.projectionSupport = Objects.requireNonNull(projectionSupport);
    }

    public AjuizamentoIntent inferIntent(Map<String, Object> payload) {
        return intentEngine.inferir(payload);
    }

    public SurfaceSnapshotResponse inferIntentMap(Map<String, Object> payload) {
        return projectionSupport.snapshot("ai.ajuizamento.intent-map", intentEngine.inferIntent(payload));
    }

    public RouterDecision route(Map<String, Object> payload) {
        return router.route(payload);
    }

    public SurfaceSnapshotResponse preflight(Map<String, Object> payload) {
        return projectionSupport.snapshot("ai.ajuizamento.preflight", nationalPreflightService.analyze(payload));
    }

    public SurfaceSnapshotResponse canonical(Map<String, Object> payload) {
        return projectionSupport.snapshot("ai.ajuizamento.canonical", proceduralCanonicalResolver.resolve(payload).toMap());
    }

    public SurfaceSnapshotResponse routing(Map<String, Object> payload) {
        var selectedRito = canonicalRitoSelector.select(payload, stringValue(payload.get("rito_processual")), "ajuizamento_intent_controller_routing");
        return projectionSupport.snapshot("ai.ajuizamento.routing", tribunalProtocolRoutingService.resolve(
                payload,
                selectedRito.rito(),
                stringValue(payload.get("ramo_direito")),
                stringValue(payload.get("competencia")),
                false
        ));
    }

    public SurfaceSnapshotResponse diagnostico(Map<String, Object> payload) {
        AjuizamentoIntent intent = intentEngine.inferir(payload);
        RouterDecision decision = router.route(payload);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("intent", intent);
        out.put("router", decision);
        out.put("knowledge", BrazilianLegalKnowledgeBase.describeForRouter(intent.ramoDireito(), intent.subRamo(), intent.esfera(), toDouble(payload.get("valor_causa"))));
        out.put("curriculum", curriculumKnowledgeService.describe(intent.ramoDireito(), intent.subRamo(), intent.rito()));
        return projectionSupport.snapshot("ai.ajuizamento.diagnostico", out);
    }

    public SurfaceCollectionResponse listarRamos() {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        BrazilianLegalKnowledgeBase.getRamosDisponiveis().stream().sorted().forEach(ramo -> {
            RamoDescriptor desc = BrazilianLegalKnowledgeBase.resolve(ramo);
            LinkedHashMap<String, Object> item = new LinkedHashMap<>();
            item.put("nome", desc.nome());
            item.put("regraPrincipal", desc.regraPrincipal());
            item.put("codigoProcessual", desc.codigoProcessual());
            item.put("subRamos", desc.subRamos());
            item.put("tribunaisCompetentes", desc.tribunaisCompetentes());
            item.put("segredoJustica", desc.segredoJustica());
            item.put("exigeMP", desc.exigeMP());
            item.put("admiteConciliacao", desc.admiteConciliacao());
            item.put("ramoProjeto", BrazilianLegalKnowledgeBase.toProjetoRamo(ramo).name());
            item.put("ritosRelacionados", BrazilianLegalKnowledgeBase.relatedRitos(ramo));
            result.put(ramo, item);
        });
        return projectionSupport.collection("ai.ajuizamento.ramos", result);
    }

    public RamoDescriptor detalharRamo(String ramo) {
        return BrazilianLegalKnowledgeBase.resolve(ramo);
    }

    public SurfaceSnapshotResponse curriculumPorRamo(String ramo, String subRamo) {
        return projectionSupport.snapshot("ai.ajuizamento.curriculum.ramo", curriculumKnowledgeService.describe(ramo, subRamo, (com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual) null));
    }

    public SurfaceCollectionResponse buscarCurriculum(String query, int limit) {
        return projectionSupport.collection("ai.ajuizamento.curriculum.search", curriculumKnowledgeService.search(query, limit));
    }

    public SurfaceCollectionResponse curriculumPrograms() {
        return projectionSupport.collection("ai.ajuizamento.curriculum.programs", curriculumKnowledgeService.programs());
    }

    public SurfaceCollectionResponse listarRitos() {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (var rito : proceduralCatalogService.catalogDrivenRitos()) {
            LinkedHashMap<String, Object> item = new LinkedHashMap<>(proceduralCatalogService.describe(rito));
            item.put("grupo", rito.group());
            item.put("ramoProjeto", rito.suggestedRamo().name());
            item.put("segredoPadrao", rito.requiresSegredoByDefault());
            item.put("workflow", proceduralWorkflowBpmnService.blueprint(rito));
            result.put(rito.name(), item);
        }
        return projectionSupport.collection("ai.ajuizamento.ritos", result);
    }

    public SurfaceCollectionResponse listarRitosV2() {
        List<Map<String, Object>> items = proceduralCatalogService.catalogDrivenRitos().stream()
                .map(rito -> {
                    LinkedHashMap<String, Object> item = new LinkedHashMap<>();
                    item.put("group", rito.group());
                    item.put("value", rito.name());
                    item.put("name", rito.name());
                    return Map.<String, Object>copyOf(item);
                })
                .toList();
        return projectionSupport.collection("ai.ajuizamento.ritos.v2", items);
    }

    public SurfaceCollectionResponse catalogoRitos() {
        return projectionSupport.collection("ai.ajuizamento.catalogo.ritos", proceduralCatalogService.catalog());
    }

    public SurfaceCollectionResponse catalogoClasses(String ramo) {
        return projectionSupport.collection(
                "ai.ajuizamento.catalogo.classes",
                ramo == null || ramo.isBlank() ? proceduralCatalogService.listTpuClasses() : proceduralCatalogService.listTpuClassesByRamo(ramo)
        );
    }

    public SurfaceSnapshotResponse detalharClasse(String codigoOuNome, String rito) {
        return projectionSupport.snapshot("ai.ajuizamento.catalogo.classe", proceduralCatalogService.describeClasseTpu(codigoOuNome, rito, null));
    }

    public SurfaceCollectionResponse catalogoTribunais(String uf) {
        return projectionSupport.collection(
                "ai.ajuizamento.catalogo.tribunais",
                uf == null || uf.isBlank() ? proceduralCatalogService.listNationalTribunals() : proceduralCatalogService.listNationalTribunalsByUf(uf)
        );
    }

    public SurfaceSnapshotResponse detalharTribunal(String codigo) {
        return projectionSupport.snapshot(
                "ai.ajuizamento.catalogo.tribunal",
                proceduralCatalogService.resolveNationalTribunal(codigo)
                        .map(t -> t.toMap())
                        .orElseGet(() -> Map.of("codigo", codigo, "resolved", false))
        );
    }

    public SurfaceSnapshotResponse cnjHealth() {
        return projectionSupport.snapshot("ai.ajuizamento.catalogo.cnj.health", cnjTpuSyncService.health());
    }

    public SurfaceSnapshotResponse cnjSync() {
        return projectionSupport.snapshot("ai.ajuizamento.catalogo.cnj.sync", cnjTpuSyncService.forceSync());
    }

    public SurfaceSnapshotResponse arquiteturaSanity() {
        return projectionSupport.snapshot("ai.ajuizamento.catalogo.sanity", proceduralArchitectureSanityService.report().toMap());
    }

    public SurfaceSnapshotResponse coberturaCatalogo() {
        return projectionSupport.snapshot("ai.ajuizamento.catalogo.coverage", proceduralCatalogService.coverage());
    }

    public SurfaceSnapshotResponse detalharRitoCatalogo(String rito) {
        return projectionSupport.snapshot("ai.ajuizamento.catalogo.rito", proceduralCatalogService.describe(rito));
    }

    public SurfaceCollectionResponse partiesPorRito(String rito) {
        return projectionSupport.collection("ai.ajuizamento.catalogo.rito.parties", proceduralCatalogService.requiredParties(rito));
    }

    public SurfaceCollectionResponse documentosPorRito(String rito) {
        return projectionSupport.collection("ai.ajuizamento.catalogo.rito.documents", proceduralCatalogService.requiredDocuments(rito));
    }

    public SurfaceSnapshotResponse workflowPorRito(String rito) {
        return projectionSupport.snapshot("ai.ajuizamento.catalogo.rito.workflow", proceduralWorkflowBpmnService.generate(rito));
    }

    public SurfaceSnapshotResponse capabilitiesTribunal(Map<String, Object> payload) {
        var selectedRito = canonicalRitoSelector.select(payload, stringValue(payload.get("rito_processual")), "ajuizamento_intent_controller_capabilities");
        return projectionSupport.snapshot("ai.ajuizamento.catalogo.tribunal.capabilities", tribunalProtocolRoutingService.resolve(
                payload,
                selectedRito.rito(),
                stringValue(payload.get("ramo_direito")),
                stringValue(payload.get("competencia")),
                false
        ));
    }

    public SurfaceSnapshotResponse health() {
        return projectionSupport.snapshot("ai.ajuizamento.health", Map.of(
                "status", "UP",
                "ramosIndexados", BrazilianLegalKnowledgeBase.getRamosDisponiveis().size(),
                "ramosProjeto", RamoDireito.values().length,
                "ritosIndexados", proceduralCatalogService.catalogDrivenRitos().size(),
                "curriculumIndexado", curriculumKnowledgeService.getRamosDisponiveis().size(),
                "versao", "v60++"
        ));
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

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }
}
