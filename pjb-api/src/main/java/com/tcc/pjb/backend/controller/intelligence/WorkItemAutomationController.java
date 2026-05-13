package com.tcc.pjb.backend.controller.intelligence;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.tcc.pjb.backend.model.dto.workitem.WorkItemGenerationRequest;
import com.tcc.pjb.backend.model.dto.workitem.WorkItemGenerationResponse;
import com.tcc.pjb.backend.service.workitem.ProcessWorkItemAutomationService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/intelligence/workitems")
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
public class WorkItemAutomationController {

    private final ProcessWorkItemAutomationService automationService;

    @PostMapping("/generate")
    public ResponseEntity<WorkItemGenerationResponse> generate(@Valid @RequestBody WorkItemGenerationRequest req) {
        return ResponseEntity.ok(automationService.generate(req));
    }
}
