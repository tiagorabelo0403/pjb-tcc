package com.tcc.pjb.backend.controller.cidadao;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tcc.pjb.backend.model.dto.cidadao.CidadaoPendenciasResponse;
import com.tcc.pjb.backend.service.cidadao.CidadaoPendenciasService;

@RestController
@RequestMapping("/api/v1/cidadao")
public class CidadaoPendenciasController {

    private final CidadaoPendenciasService service;

    public CidadaoPendenciasController(CidadaoPendenciasService service) {
        this.service = service;
    }

        @GetMapping("/pendencias")
    @PreAuthorize("hasRole('CIDADAO')")
    public CidadaoPendenciasResponse pendencias() {
        return service.pendencias();
    }
}
