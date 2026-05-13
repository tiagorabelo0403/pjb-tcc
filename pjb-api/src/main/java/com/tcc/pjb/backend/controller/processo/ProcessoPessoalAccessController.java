package com.tcc.pjb.backend.controller.processo;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoMeusProcessosResponse;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoProcessoOverviewResponse;
import com.tcc.pjb.backend.service.cidadao.CidadaoProcessoOverviewService;
import com.tcc.pjb.backend.service.cidadao.surface.CidadaoSurfaceFacadeService;

@RestController
@RequestMapping("/api/v1/processos/pessoais")
public class ProcessoPessoalAccessController {

    private final CidadaoSurfaceFacadeService cidadaoSurfaceFacadeService;
    private final CidadaoProcessoOverviewService processoOverviewService;

    public ProcessoPessoalAccessController(CidadaoSurfaceFacadeService cidadaoSurfaceFacadeService,
                                           CidadaoProcessoOverviewService processoOverviewService) {
        this.cidadaoSurfaceFacadeService = cidadaoSurfaceFacadeService;
        this.processoOverviewService = processoOverviewService;
    }

    @GetMapping("/meus-processos")
    @PreAuthorize("isAuthenticated()")
    public CidadaoMeusProcessosResponse meusProcessos() {
        return cidadaoSurfaceFacadeService.meusProcessos();
    }

    @GetMapping("/{processoId}/overview")
    @PreAuthorize("isAuthenticated()")
    public CidadaoProcessoOverviewResponse overview(@PathVariable Long processoId) {
        return processoOverviewService.overview(processoId);
    }
}
