package com.tcc.pjb.backend.controller.intelligence;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.tcc.pjb.backend.model.dto.intelligence.CaseKitResponse;
import com.tcc.pjb.backend.model.dto.intelligence.CaseTriageRequest;
import com.tcc.pjb.backend.model.dto.intelligence.CaseTriageResponse;
import com.tcc.pjb.backend.model.dto.intelligence.MinutaPreviewRequest;
import com.tcc.pjb.backend.model.dto.intelligence.MinutaPreviewResponse;
import com.tcc.pjb.backend.model.dto.intelligence.RitoPlanRequest;
import com.tcc.pjb.backend.model.dto.intelligence.RitoPlanResponse;
import com.tcc.pjb.backend.service.intelligence.CaseKitService;
import com.tcc.pjb.backend.service.intelligence.CaseTriageService;
import com.tcc.pjb.backend.service.intelligence.MinutaGeneratorService;
import com.tcc.pjb.backend.service.rito.RitoPlanService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/intelligence")
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
public class CaseIntelligenceController {

    private final CaseTriageService triageService;
    private final CaseKitService caseKitService;
    private final MinutaGeneratorService minutaGeneratorService;
    private final RitoPlanService ritoPlanService;

    
    @PostMapping("/case/triage")
    public ResponseEntity<CaseTriageResponse> triage(@Valid @RequestBody CaseTriageRequest req) {
        return ResponseEntity.ok(triageService.triage(req));
    }

    
    @PostMapping("/case/kit")
    public ResponseEntity<CaseKitResponse> kit(@Valid @RequestBody CaseTriageRequest req) {
        return ResponseEntity.ok(caseKitService.build(req));
    }

    
    @PostMapping("/rito/plan")
    public ResponseEntity<RitoPlanResponse> ritoPlan(@Valid @RequestBody RitoPlanRequest req) {
        return ResponseEntity.ok(ritoPlanService.plan(req.rito()));
    }

    
    @PostMapping("/minuta/preview")
    public ResponseEntity<MinutaPreviewResponse> minutaPreview(@Valid @RequestBody MinutaPreviewRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(minutaGeneratorService.preview(req));
    }
}
