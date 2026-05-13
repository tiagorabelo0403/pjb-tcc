package com.tcc.pjb.backend.ai.juridica.api;

import com.tcc.pjb.backend.ai.juridica.v3.core.AjuizamentoIntent;
import com.tcc.pjb.backend.ai.juridica.v3.core.RamoDescriptor;
import com.tcc.pjb.backend.ai.juridica.v3.core.UnifiedProcessoIntentRouter.RouterDecision;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.intelligence.surface.AjuizamentoIntentSurfaceFacadeService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/ai/ajuizamento", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@PreAuthorize("isAuthenticated()")
public class AjuizamentoIntentController {

    private final AjuizamentoIntentSurfaceFacadeService surfaceFacadeService;

    public AjuizamentoIntentController(AjuizamentoIntentSurfaceFacadeService surfaceFacadeService) {
        this.surfaceFacadeService = surfaceFacadeService;
    }

    @PostMapping(value = "/infer-intent", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AjuizamentoIntent> inferIntent(@RequestBody(required = false) Map<String, Object> payload) {
        return ResponseEntity.ok(surfaceFacadeService.inferIntent(normalizePayload(payload)));
    }

    @PostMapping(value = "/infer-map", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SurfaceSnapshotResponse> inferIntentMap(@RequestBody(required = false) Map<String, Object> payload) {
        return ResponseEntity.ok(surfaceFacadeService.inferIntentMap(normalizePayload(payload)));
    }

    @PostMapping(value = "/route", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RouterDecision> route(@RequestBody(required = false) Map<String, Object> payload) {
        return ResponseEntity.ok(surfaceFacadeService.route(normalizePayload(payload)));
    }

    @PostMapping(value = "/preflight", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SurfaceSnapshotResponse> preflight(@RequestBody(required = false) Map<String, Object> payload) {
        return ResponseEntity.ok(surfaceFacadeService.preflight(normalizePayload(payload)));
    }

    @PostMapping(value = "/canonical", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SurfaceSnapshotResponse> canonical(@RequestBody(required = false) Map<String, Object> payload) {
        return ResponseEntity.ok(surfaceFacadeService.canonical(normalizePayload(payload)));
    }

    @PostMapping(value = "/routing", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SurfaceSnapshotResponse> routing(@RequestBody(required = false) Map<String, Object> payload) {
        return ResponseEntity.ok(surfaceFacadeService.routing(normalizePayload(payload)));
    }

    @PostMapping(value = "/diagnostico", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SurfaceSnapshotResponse> diagnostico(@RequestBody(required = false) Map<String, Object> payload) {
        return ResponseEntity.ok(surfaceFacadeService.diagnostico(normalizePayload(payload)));
    }

    @GetMapping("/ramos")
    public ResponseEntity<SurfaceCollectionResponse> listarRamos() {
        return ResponseEntity.ok(surfaceFacadeService.listarRamos());
    }

    @GetMapping("/ramos/{ramo}")
    public ResponseEntity<RamoDescriptor> detalharRamo(@PathVariable String ramo) {
        return ResponseEntity.ok(surfaceFacadeService.detalharRamo(ramo));
    }

    @GetMapping("/ramos/{ramo}/curriculum")
    public ResponseEntity<SurfaceSnapshotResponse> curriculumPorRamo(@PathVariable String ramo, @RequestParam(required = false) String subRamo) {
        return ResponseEntity.ok(surfaceFacadeService.curriculumPorRamo(ramo, subRamo));
    }

    @GetMapping("/curriculum/search")
    public ResponseEntity<SurfaceCollectionResponse> buscarCurriculum(@RequestParam String q, @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(surfaceFacadeService.buscarCurriculum(q, limit));
    }

    @GetMapping("/curriculum/programs")
    public ResponseEntity<SurfaceCollectionResponse> curriculumPrograms() {
        return ResponseEntity.ok(surfaceFacadeService.curriculumPrograms());
    }

    @GetMapping("/ritos")
    public ResponseEntity<SurfaceCollectionResponse> listarRitos() {
        return ResponseEntity.ok(surfaceFacadeService.listarRitos());
    }

    @GetMapping("/ritos-v2")
    public ResponseEntity<SurfaceCollectionResponse> listarRitosV2() {
        return ResponseEntity.ok(surfaceFacadeService.listarRitosV2());
    }

    @GetMapping("/catalog/ritos")
    public ResponseEntity<SurfaceCollectionResponse> catalogoRitos() {
        return ResponseEntity.ok(surfaceFacadeService.catalogoRitos());
    }

    @GetMapping("/catalog/classes")
    public ResponseEntity<SurfaceCollectionResponse> catalogoClasses(@RequestParam(required = false) String ramo) {
        return ResponseEntity.ok(surfaceFacadeService.catalogoClasses(ramo));
    }

    @GetMapping("/catalog/classes/{codigoOuNome}")
    public ResponseEntity<SurfaceSnapshotResponse> detalharClasse(@PathVariable String codigoOuNome,
                                                                  @RequestParam(required = false) String rito) {
        return ResponseEntity.ok(surfaceFacadeService.detalharClasse(codigoOuNome, rito));
    }

    @GetMapping("/catalog/tribunais")
    public ResponseEntity<SurfaceCollectionResponse> catalogoTribunais(@RequestParam(required = false) String uf) {
        return ResponseEntity.ok(surfaceFacadeService.catalogoTribunais(uf));
    }

    @GetMapping("/catalog/tribunais/{codigo}")
    public ResponseEntity<SurfaceSnapshotResponse> detalharTribunal(@PathVariable String codigo) {
        return ResponseEntity.ok(surfaceFacadeService.detalharTribunal(codigo));
    }

    @GetMapping("/catalog/cnj/health")
    public ResponseEntity<SurfaceSnapshotResponse> cnjHealth() {
        return ResponseEntity.ok(surfaceFacadeService.cnjHealth());
    }

    @PostMapping("/catalog/cnj/sync")
    public ResponseEntity<SurfaceSnapshotResponse> cnjSync() {
        return ResponseEntity.ok(surfaceFacadeService.cnjSync());
    }

    @GetMapping("/catalog/sanity")
    public ResponseEntity<SurfaceSnapshotResponse> arquiteturaSanity() {
        return ResponseEntity.ok(surfaceFacadeService.arquiteturaSanity());
    }

    @GetMapping("/catalog/coverage")
    public ResponseEntity<SurfaceSnapshotResponse> coberturaCatalogo() {
        return ResponseEntity.ok(surfaceFacadeService.coberturaCatalogo());
    }

    @GetMapping("/catalog/ritos/{rito}")
    public ResponseEntity<SurfaceSnapshotResponse> detalharRitoCatalogo(@PathVariable String rito) {
        return ResponseEntity.ok(surfaceFacadeService.detalharRitoCatalogo(rito));
    }

    @GetMapping("/catalog/ritos/{rito}/parties")
    public ResponseEntity<SurfaceCollectionResponse> partiesPorRito(@PathVariable String rito) {
        return ResponseEntity.ok(surfaceFacadeService.partiesPorRito(rito));
    }

    @GetMapping("/catalog/ritos/{rito}/documents")
    public ResponseEntity<SurfaceCollectionResponse> documentosPorRito(@PathVariable String rito) {
        return ResponseEntity.ok(surfaceFacadeService.documentosPorRito(rito));
    }

    @GetMapping("/catalog/ritos/{rito}/workflow")
    public ResponseEntity<SurfaceSnapshotResponse> workflowPorRito(@PathVariable String rito) {
        return ResponseEntity.ok(surfaceFacadeService.workflowPorRito(rito));
    }

    @PostMapping(value = "/catalog/tribunais/capabilities", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SurfaceSnapshotResponse> capabilitiesTribunal(@RequestBody(required = false) Map<String, Object> payload) {
        return ResponseEntity.ok(surfaceFacadeService.capabilitiesTribunal(normalizePayload(payload)));
    }

    @GetMapping("/health")
    public ResponseEntity<SurfaceSnapshotResponse> health() {
        return ResponseEntity.ok(surfaceFacadeService.health());
    }


    private Map<String, Object> normalizePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(payload));
    }
}
