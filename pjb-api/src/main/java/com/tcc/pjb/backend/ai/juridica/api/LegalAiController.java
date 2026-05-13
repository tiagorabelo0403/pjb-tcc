package com.tcc.pjb.backend.ai.juridica.api;

import com.tcc.pjb.backend.model.dto.ai.legal.LegalDraftRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalDraftResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalResearchDossierRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalResearchDossierResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalTriageRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalTriageResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationResponse;
import com.tcc.pjb.backend.service.intelligence.surface.LegalAiSurfaceFacadeService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/ai/legal", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@PreAuthorize("isAuthenticated()")
public class LegalAiController {

    private final LegalAiSurfaceFacadeService surfaceFacadeService;

    public LegalAiController(LegalAiSurfaceFacadeService surfaceFacadeService) {
        this.surfaceFacadeService = surfaceFacadeService;
    }

    @PostMapping(value = "/triage", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LegalTriageResponse> triage(@Valid @RequestBody LegalTriageRequest request) {
        return ResponseEntity.ok(surfaceFacadeService.triage(request));
    }

    @PostMapping(value = "/minuta", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LegalDraftResponse> minuta(@Valid @RequestBody LegalDraftRequest request) {
        return ResponseEntity.ok(surfaceFacadeService.minuta(request));
    }

    @PostMapping(value = "/research/dossier", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LegalResearchDossierResponse> researchDossier(@Valid @RequestBody LegalResearchDossierRequest request) {
        return ResponseEntity.ok(surfaceFacadeService.researchDossier(request));
    }

    @PostMapping(value = "/validate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LegalValidationResponse> validate(@Valid @RequestBody LegalValidationRequest request) {
        return ResponseEntity.ok(surfaceFacadeService.validate(request));
    }

    @PostMapping(value = "/grounding/check", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LegalHallucinationGuardResponse> groundingCheck(@Valid @RequestBody LegalHallucinationGuardRequest request) {
        return ResponseEntity.ok(surfaceFacadeService.hallucinationGuard(request));
    }

}
