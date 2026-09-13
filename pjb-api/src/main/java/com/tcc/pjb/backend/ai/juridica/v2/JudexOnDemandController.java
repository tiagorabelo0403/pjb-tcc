package com.tcc.pjb.backend.ai.juridica.v2;

import com.tcc.pjb.backend.ai.juridica.v2.dto.JudexGenerateMinutaRequest;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ia/judex")
@Validated
@PreAuthorize("isAuthenticated()")
public class JudexOnDemandController {

    private final JudexMinutaService judexMinutaService;

    public JudexOnDemandController(JudexMinutaService judexMinutaService) {
        this.judexMinutaService = Objects.requireNonNull(judexMinutaService);
    }

    @PostMapping("/analise-minuta")
    public ResponseEntity<String> gerarMinuta(@Valid @RequestBody(required = false) JudexGenerateMinutaRequest request) {
        try {
            return ResponseEntity.ok(judexMinutaService.gerar(request));
        } catch (JudexMinutaService.DadosInsuficientesException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
