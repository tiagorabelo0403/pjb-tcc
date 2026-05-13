package com.tcc.pjb.backend.controller.admin;





import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.tcc.pjb.backend.model.dto.admin.RitoRuleProposalCreateRequest;
import com.tcc.pjb.backend.model.dto.admin.RitoRuleProposalDecisionRequest;
import com.tcc.pjb.backend.model.dto.admin.RitoRuleProposalDto;
import com.tcc.pjb.backend.model.dto.admin.RitoRuleProposalListResponse;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoRuleProposalStatus;
import com.tcc.pjb.backend.service.rito.RitoRuleProposalService;
import jakarta.validation.Valid;




@RestController
@RequestMapping("/api/admin/ritos/rules/proposals")
@Validated
public class AdminRitoRuleProposalController {

    private final RitoRuleProposalService service;

    public AdminRitoRuleProposalController(RitoRuleProposalService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RitoRuleProposalDto> create(@Valid @RequestBody RitoRuleProposalCreateRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RitoRuleProposalListResponse> list(
            @RequestParam(name = "status", required = false) RitoRuleProposalStatus status,
            @RequestParam(name = "top", defaultValue = "50") int top
    ) {
        List<RitoRuleProposalDto> items = service.list(status, top);
        return ResponseEntity.ok(new RitoRuleProposalListResponse(items));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RitoRuleProposalDto> approve(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RitoRuleProposalDecisionRequest req
    ) {
        return ResponseEntity.ok(service.approve(id, req));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RitoRuleProposalDto> reject(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RitoRuleProposalDecisionRequest req
    ) {
        return ResponseEntity.ok(service.reject(id, req));
    }
}
