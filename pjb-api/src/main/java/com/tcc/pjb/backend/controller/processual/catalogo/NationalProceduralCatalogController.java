package com.tcc.pjb.backend.controller.processual.catalogo;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.processual.catalog.NationalProceduralCatalogRequest;
import com.tcc.pjb.backend.model.dto.processual.catalog.NationalProceduralCatalogResponse;
import com.tcc.pjb.backend.service.processual.catalog.NationalProceduralCatalogService;

@RestController
@RequestMapping("/api/v1/processual/catalogo")
public class NationalProceduralCatalogController {

    private final NationalProceduralCatalogService service;

    public NationalProceduralCatalogController(NationalProceduralCatalogService service) {
        this.service = service;
    }

    @GetMapping("/consultar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalProceduralCatalogResponse> consultar(@RequestParam(required = false) String termo,
                                                                       @RequestParam(required = false) Long processoId,
                                                                       @RequestParam(required = false) Integer limite) {
        return ResponseEntity.ok(service.consultar(new NationalProceduralCatalogRequest(termo, processoId, limite)));
    }
}
