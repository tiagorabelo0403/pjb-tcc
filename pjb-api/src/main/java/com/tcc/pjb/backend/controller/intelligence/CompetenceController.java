package com.tcc.pjb.backend.controller.intelligence;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveRequest;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionRequest;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionResponse;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceRedistributionResponse;
import com.tcc.pjb.backend.service.competencia.CompetenceResolverService;
import com.tcc.pjb.backend.service.competencia.MapaCompetenciaDinamicoEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/intelligence/competencia")
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
public class CompetenceController {

    private final CompetenceResolverService resolverService;
    private final MapaCompetenciaDinamicoEngine mapaCompetenciaDinamicoEngine;

    @PostMapping("/resolve")
    public ResponseEntity<CompetenceResolveResponse> resolve(@Valid @RequestBody CompetenceResolveRequest req) {
        return ResponseEntity.ok(resolverService.resolve(req));
    }

    @PostMapping("/distribute")
    public ResponseEntity<DynamicCompetenceDistributionResponse> distribute(@Valid @RequestBody DynamicCompetenceDistributionRequest req) {
        return mapaCompetenciaDinamicoEngine.distribuirERegistrar(req)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/redistribuicao/analise")
    public ResponseEntity<DynamicCompetenceRedistributionResponse> analyzeRedistribution(@RequestParam(name = "limiar", defaultValue = "0.85") double limiar) {
        return ResponseEntity.ok(mapaCompetenciaDinamicoEngine.analisarRedistribuicao(limiar));
    }
}