package com.tcc.pjb.backend.controller.processo;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.publico.PrazoRealPredictionResponse;
import com.tcc.pjb.backend.service.prazo.CongestionScoreService;

@RestController
@RequestMapping("/api/v1/processos")
public class ProcessoPrazoRealController {

    private final CongestionScoreService congestionScoreService;

    public ProcessoPrazoRealController(CongestionScoreService congestionScoreService) {
        this.congestionScoreService = congestionScoreService;
    }

    @GetMapping("/{processoId}/prazo-real")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PrazoRealPredictionResponse> prazoReal(@PathVariable Long processoId,
                                                                 @RequestParam(defaultValue = "ATO_PROCESSUAL") String tipoAto) {
        return ResponseEntity.ok(congestionScoreService.predizer(processoId, tipoAto));
    }
}
