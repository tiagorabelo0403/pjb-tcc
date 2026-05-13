package com.tcc.pjb.backend.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.model.dto.jurisprudencia.PrecedentFoundationQueryRequest;
import com.tcc.pjb.backend.model.dto.jurisprudencia.PrecedentFoundationResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import com.tcc.pjb.backend.service.jurisprudencia.PrecedentFoundationCatalogService;

@RestController
@RequestMapping("/api/v1/jurisprudencia/fundamentos")
@PreAuthorize("isAuthenticated()")
public class JurisprudenciaFoundationController {

    private final PrecedentFoundationCatalogService service;
    private final ApiResponseFactory responseFactory;

    public JurisprudenciaFoundationController(PrecedentFoundationCatalogService service,
                                              ApiResponseFactory responseFactory) {
        this.service = service;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/consultar")
    public ResponseEntity<ApiQueryResponse<PrecedentFoundationResponse>> consultar(@Valid @RequestBody PrecedentFoundationQueryRequest request) {
        return ResponseEntity.ok(responseFactory.queryOk(service.search(request), List.of()));
    }
}
