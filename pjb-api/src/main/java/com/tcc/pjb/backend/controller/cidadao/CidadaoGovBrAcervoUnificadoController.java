package com.tcc.pjb.backend.controller.cidadao;

import com.tcc.pjb.backend.model.dto.cidadao.govbr.CidadaoGovBrAcervoUnificadoResponse;
import com.tcc.pjb.backend.service.cidadao.govbr.CidadaoGovBrAcervoUnificadoService;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/cidadao/govbr")
public class CidadaoGovBrAcervoUnificadoController {

    private final CidadaoGovBrAcervoUnificadoService service;

    public CidadaoGovBrAcervoUnificadoController(CidadaoGovBrAcervoUnificadoService service) {
        this.service = Objects.requireNonNull(service);
    }

    @GetMapping("/acervo-unificado")
    @PreAuthorize("hasRole('CIDADAO')")
    public ResponseEntity<CidadaoGovBrAcervoUnificadoResponse> acervoUnificado(
            @RequestParam(name = "papel", required = false) String papel,
            @RequestParam(name = "rito", required = false) String rito,
            @RequestParam(name = "sistema", required = false) String sistema,
            @RequestParam(name = "somentePendencias", required = false) Boolean somentePendencias,
            @RequestParam(name = "somenteAudiencias", required = false) Boolean somenteAudiencias,
            @RequestParam(name = "somenteNovidades", required = false) Boolean somenteNovidades
    ) {
        return ResponseEntity.ok(service.carregar(papel, rito, sistema, somentePendencias, somenteAudiencias, somenteNovidades));
    }
}
