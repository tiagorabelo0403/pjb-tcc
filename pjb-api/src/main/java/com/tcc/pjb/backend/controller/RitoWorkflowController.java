package com.tcc.pjb.backend.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.tcc.pjb.backend.service.rito.RitoWorkflowService;
import com.tcc.pjb.backend.service.rito.dto.AdvanceRitoRequest;
import com.tcc.pjb.backend.service.rito.dto.RitoPlanDto;

@RestController
@RequestMapping("/api/v1/ritos")
@PreAuthorize("isAuthenticated()")
public class RitoWorkflowController {

    private final RitoWorkflowService ritoWorkflowService;

    public RitoWorkflowController(RitoWorkflowService ritoWorkflowService) {
        this.ritoWorkflowService = ritoWorkflowService;
    }

    
    @PostMapping("/{processoId}/seed")
    public RitoPlanDto seed(@PathVariable Long processoId) {
        return ritoWorkflowService.seedIfNeeded(processoId);
    }

    
    @GetMapping("/{processoId}/plan")
    public RitoPlanDto plan(@PathVariable Long processoId) {
        return ritoWorkflowService.plan(processoId);
    }

    
    @PostMapping("/{processoId}/advance")
    public RitoPlanDto advance(@PathVariable Long processoId, @RequestBody AdvanceRitoRequest request) {
        return ritoWorkflowService.advance(processoId, request);
    }
}
