package com.tcc.pjb.backend.controller.cidadao;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tcc.pjb.backend.model.dto.cidadao.CidadaoProcessoOverviewResponse;
import com.tcc.pjb.backend.service.cidadao.CidadaoProcessoOverviewService;

@RestController
@RequestMapping("/api/v1/cidadao/processos")
public class CidadaoProcessoOverviewController {

    private final CidadaoProcessoOverviewService service;

    public CidadaoProcessoOverviewController(CidadaoProcessoOverviewService service) {
        this.service = service;
    }

    @GetMapping("/{processoId}/overview")
    @PreAuthorize("hasRole('CIDADAO')")
    public CidadaoProcessoOverviewResponse overview(@PathVariable Long processoId) {
        return service.overview(processoId);
    }

        @GetMapping("/{processoId}")
    @PreAuthorize("hasRole('CIDADAO')")
    public CidadaoProcessoOverviewResponse processo(@PathVariable Long processoId) {
        return service.overview(processoId);
    }
}
