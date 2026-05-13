package com.tcc.pjb.backend.controller.cidadao;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoMeusProcessosResponse;
import com.tcc.pjb.backend.service.cidadao.surface.CidadaoSurfaceFacadeService;

@RestController
@RequestMapping("/api/v1/cidadao")
public class CidadaoMeusProcessosController {

    private final CidadaoSurfaceFacadeService facadeService;

    public CidadaoMeusProcessosController(CidadaoSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/meus-processos")
    @PreAuthorize("hasRole('CIDADAO')")
    public CidadaoMeusProcessosResponse meusProcessos() {
        return facadeService.meusProcessos();
    }
}
