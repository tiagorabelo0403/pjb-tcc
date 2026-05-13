package com.tcc.pjb.backend.controller.intelligence;

import com.tcc.pjb.backend.model.dto.intelligence.ProcessAiDossierResponse;
import com.tcc.pjb.backend.service.intelligence.ProcessAiDossierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/intelligence")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ProcessAiDossierController {

    private final ProcessAiDossierService processAiDossierService;

    @GetMapping("/processos/{processoId}/dossie")
    public ResponseEntity<ProcessAiDossierResponse> dossier(@PathVariable Long processoId) {
        return ResponseEntity.ok(processAiDossierService.analyze(processoId));
    }
}
