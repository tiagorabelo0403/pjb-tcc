package com.tcc.pjb.backend.controller.processo;

import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingContentResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingFlowResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingNavigationResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingPresetCatalogResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingSearchHitResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingEcosystemResponse;
import com.tcc.pjb.backend.model.dto.intelligence.StructuredProcessSummaryResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingSpecializationResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingProceduralContextResponse;
import com.tcc.pjb.backend.service.document.reading.ProcessReadingWorkspaceService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ProcessReadingWorkspaceController {

    private final ProcessReadingWorkspaceService service;

    public ProcessReadingWorkspaceController(ProcessReadingWorkspaceService service) {
        this.service = service;
    }

    @GetMapping("/processos/{processoId}/painel-leitura")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessReadingWorkspaceResponse> painelPorProcesso(@PathVariable Long processoId) {
        return ResponseEntity.ok(service.assembleProcesso(processoId));
    }

    @GetMapping("/documentos/{documentoId}/painel-leitura")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessReadingWorkspaceResponse> painelPorDocumento(@PathVariable UUID documentoId) {
        return ResponseEntity.ok(service.assembleDocumento(documentoId));
    }

    @GetMapping("/processos/{processoId}/painel-leitura/navegacao")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessReadingNavigationResponse> navegacao(@PathVariable Long processoId) {
        return ResponseEntity.ok(service.navigation(processoId));
    }

    @GetMapping("/processos/{processoId}/painel-leitura/fluxo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessReadingFlowResponse> fluxo(@PathVariable Long processoId) {
        return ResponseEntity.ok(service.flow(processoId));
    }

    @GetMapping("/processos/{processoId}/painel-leitura/conteudo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessReadingContentResponse> conteudoFluxo(@PathVariable Long processoId,
                                                                       @RequestParam(name = "entryId", required = false) String entryId) {
        return ResponseEntity.ok(service.contentFluxo(processoId, entryId));
    }

    @GetMapping("/documentos/{documentoId}/painel-leitura/conteudo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessReadingContentResponse> conteudoDocumento(@PathVariable UUID documentoId) {
        return ResponseEntity.ok(service.contentDocumento(documentoId));
    }

    @GetMapping("/processos/{processoId}/painel-leitura/presets")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessReadingPresetCatalogResponse> presets(@PathVariable Long processoId) {
        return ResponseEntity.ok(service.presetCatalog(processoId));
    }

    @GetMapping("/processos/{processoId}/painel-leitura/contexto-procedimental")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessReadingProceduralContextResponse> contextoProcedimental(@PathVariable Long processoId) {
        return ResponseEntity.ok(service.proceduralContext(processoId));
    }

    @GetMapping("/processos/{processoId}/painel-leitura/especializacao")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessReadingSpecializationResponse> especializacao(@PathVariable Long processoId) {
        return ResponseEntity.ok(service.specialization(processoId));
    }

    @GetMapping("/processos/{processoId}/painel-leitura/ecossistema")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessReadingEcosystemResponse> ecossistema(@PathVariable Long processoId) {
        return ResponseEntity.ok(service.ecosystem(processoId));
    }

    @GetMapping("/processos/{processoId}/painel-leitura/busca")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProcessReadingSearchHitResponse>> buscar(@PathVariable Long processoId,
                                                                        @RequestParam(name = "q", required = false) String query) {
        return ResponseEntity.ok(service.search(processoId, query));
    }

    @GetMapping("/processos/{processoId}/painel-leitura/resumo-estruturado")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StructuredProcessSummaryResponse> resumoEstruturado(@PathVariable Long processoId) {
        return ResponseEntity.ok(service.structuredSummary(processoId));
    }
}
