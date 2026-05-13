package com.tcc.pjb.backend.ai.triad;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/triad")
@Validated
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class TriadIAController {

    private final TriadIAOrchestrator orchestrator;

    @PostMapping(value = "/executar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IAResponse> executar(@RequestBody IARequest request) {
        
        if (request == null) {
            throw new IllegalArgumentException("request é obrigatório");
        }
        if (!StringUtils.hasText(request.getOrigem())) {
            throw new IllegalArgumentException("origem é obrigatória");
        }
        if (!StringUtils.hasText(request.getAcao())) {
            throw new IllegalArgumentException("acao é obrigatória");
        }
        return ResponseEntity.ok(orchestrator.executar(request));
    }
}
