package com.tcc.pjb.backend.controller.cidadao;

import com.tcc.pjb.backend.model.dto.cidadao.govbr.CidadaoGovBrAcessoFederadoRequest;
import com.tcc.pjb.backend.model.dto.cidadao.govbr.CidadaoGovBrAcessoFederadoResponse;
import com.tcc.pjb.backend.model.dto.cidadao.govbr.CidadaoGovBrInteroperabilidadeFederadaResponse;
import com.tcc.pjb.backend.service.cidadao.govbr.CidadaoGovBrInteroperabilidadeFederadaService;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/cidadao/govbr")
public class CidadaoGovBrInteroperabilidadeFederadaController {

    private final CidadaoGovBrInteroperabilidadeFederadaService service;

    public CidadaoGovBrInteroperabilidadeFederadaController(CidadaoGovBrInteroperabilidadeFederadaService service) {
        this.service = Objects.requireNonNull(service);
    }

    @GetMapping("/interoperabilidade-federada")
    @PreAuthorize("hasRole('CIDADAO')")
    public ResponseEntity<CidadaoGovBrInteroperabilidadeFederadaResponse> panorama() {
        return ResponseEntity.ok(service.panorama());
    }

    @PostMapping("/acesso-federado/avaliar")
    @PreAuthorize("hasRole('CIDADAO')")
    public ResponseEntity<CidadaoGovBrAcessoFederadoResponse> avaliar(@Valid @RequestBody CidadaoGovBrAcessoFederadoRequest request) {
        return ResponseEntity.ok(service.avaliarAcesso(request));
    }
}
