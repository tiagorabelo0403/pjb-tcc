package com.tcc.pjb.backend.controller;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.service.SigiloService;
import com.tcc.pjb.backend.service.processo.ProcessoAccessApplicationService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sigilo")
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
public class SigiloController {

    private final SigiloService sigiloService;
    private final ProcessoAccessApplicationService processoAccessApplicationService;

    @GetMapping("/definir/{id}")
    public ResponseEntity<Integer> definirNivelSigilo(@PathVariable @Positive Long id) {
        Processo processo = processoAccessApplicationService.loadAndRequireRead(id);
        int nivel = sigiloService.definirNivelSigilo(processo);
        return ResponseEntity.ok(nivel);
    }
}
