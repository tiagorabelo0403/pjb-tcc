package com.tcc.pjb.backend.controller;

import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.*;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import com.tcc.pjb.backend.modules.auditoria.AuditoriaInteligenteService;
import com.tcc.pjb.backend.service.jurisprudencia.JurisprudenciaService;
import com.tcc.pjb.backend.service.jurisprudencia.search.JurisprudenceContextualSearchService;
import com.tcc.pjb.backend.service.jurisprudencia.search.JurisprudenceSearchEngine;
import com.tcc.pjb.backend.service.jurisprudencia.search.JurisprudenceSearchHit;
import com.tcc.pjb.backend.model.dto.jurisprudencia.JurisprudenceContextualSearchResponse;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

@RestController
@RequestMapping("/api/v1/jurisprudencia")
@Validated
public class JurisprudenciaController {

    private final JurisprudenciaService service;
    private final JurisprudenceSearchEngine searchEngine;
    private final CurrentUserService currentUserService;
    private final AuditoriaInteligenteService auditoria;
    private final JurisprudenceContextualSearchService contextualSearchService;

    public JurisprudenciaController(JurisprudenciaService service,
                                    JurisprudenceSearchEngine searchEngine,
                                    CurrentUserService currentUserService,
                                    AuditoriaInteligenteService auditoria,
                                    JurisprudenceContextualSearchService contextualSearchService) {
        this.service = service;
        this.searchEngine = searchEngine;
        this.currentUserService = currentUserService;
        this.auditoria = auditoria;
        this.contextualSearchService = contextualSearchService;
    }

    @GetMapping("/search")
    public Page<Precedente> search(
            @RequestParam(required = false) TribunalFonte fonte,
            @RequestParam(required = false) TipoPrecedente tipo,
            @RequestParam(required = false) RamoDireito ramo,
            @RequestParam(required = false) RitoProcessual rito,
            @RequestParam(required = false, name = "q") String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        auditIfLawyer("search", fonte, tipo, ramo, rito, q, size);
        return service.search(fonte, tipo, ramo, rito, q, page, size);
    }

    @GetMapping("/search-scored")
    public List<JurisprudenceSearchHit> searchScored(
            @RequestParam(name = "q") String q,
            @RequestParam(required = false) RamoDireito ramo,
            @RequestParam(required = false) RitoProcessual rito,
            @RequestParam(defaultValue = "10") @Min(1) @Max(25) int topK
    ) {
        auditIfLawyer("search_scored", null, null, ramo, rito, q, topK);
        return searchEngine.search(q, ramo, rito, topK);
    }


    @GetMapping("/search-contextual")
    public JurisprudenceContextualSearchResponse searchContextual(
            @RequestParam(name = "q") String q,
            @RequestParam(required = false) RamoDireito ramo,
            @RequestParam(required = false) RitoProcessual rito,
            @RequestParam(defaultValue = "10") @Min(1) @Max(25) int topK
    ) {
        auditIfLawyer("search_contextual", null, null, ramo, rito, q, topK);
        return contextualSearchService.search(q, ramo, rito, topK);
    }

    @GetMapping("/index/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ADMINISTRADOR')")
    public ResponseEntity<JurisprudenceSearchEngine.IndexStatus> indexStatus() {
        return ResponseEntity.ok(searchEngine.status());
    }

    @PostMapping("/index/rebuild")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ADMINISTRADOR')")
    public ResponseEntity<JurisprudenceSearchEngine.IndexStatus> indexRebuild() {
        searchEngine.rebuild();
        return ResponseEntity.status(HttpStatus.CREATED).body(searchEngine.status());
    }

    @PostMapping("/index/refresh")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ADMINISTRADOR')")
    public ResponseEntity<JurisprudenceSearchEngine.RefreshStats> indexRefresh() {
        return ResponseEntity.ok(searchEngine.refreshIncremental());
    }

    private void auditIfLawyer(String mode,
                              TribunalFonte fonte,
                              TipoPrecedente tipo,
                              RamoDireito ramo,
                              RitoProcessual rito,
                              String q,
                              int limit) {
        try {
            Usuario u = currentUserService.getOrNull();
            if (u == null || !u.isAdvogado()) return;

            String rid = UUID.randomUUID().toString();
            int qLen = q != null ? q.trim().length() : 0;
            String detalhes = "mode=" + mode
                    + ";qLen=" + qLen
                    + ";fonte=" + (fonte != null ? fonte.name() : "")
                    + ";tipo=" + (tipo != null ? tipo.name() : "")
                    + ";ramo=" + (ramo != null ? ramo.name() : "")
                    + ";rito=" + (rito != null ? rito.name() : "")
                    + ";limit=" + limit;

            auditoria.registrarEventoImutavel(
                    "ADV_JURISPRUDENCIA_CONSULTADA",
                    "JURISPRUDENCIA",
                    rid,
                    detalhes
            );
        } catch (Exception ignored) {
        }
    }
}
