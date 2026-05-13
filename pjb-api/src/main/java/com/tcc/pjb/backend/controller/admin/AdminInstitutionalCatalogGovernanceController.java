package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalCatalogCoverageSummaryResponse;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalCatalogGovernanceResponse;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalCatalogGovernanceSummaryResponse;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalCatalogGovernanceUpsertRequest;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalCompetenceRuleResponse;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalCompetenceRuleUpsertRequest;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalRegionalBaselineExpansionRequest;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalRegionalBaselineExpansionResponse;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.service.admin.surface.AdminInstitutionalGovernanceFacadeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/institucional/catalogo")
public class AdminInstitutionalCatalogGovernanceController {

    private final AdminInstitutionalGovernanceFacadeService facadeService;

    public AdminInstitutionalCatalogGovernanceController(AdminInstitutionalGovernanceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/governancas")
    @PreAuthorize("hasAnyRole('ADMIN','ADMINISTRADOR')")
    public ResponseEntity<List<AdminInstitutionalCatalogGovernanceResponse>> listGovernances(@RequestParam(required = false) DestinatarioInstitucionalKind destinatarioKind,
                                                                                              @RequestParam(required = false) String uf) {
        return ResponseEntity.ok(facadeService.listGovernances(destinatarioKind, uf));
    }

    @PostMapping("/governancas")
    @PreAuthorize("hasAnyRole('ADMIN','ADMINISTRADOR')")
    public ResponseEntity<AdminInstitutionalCatalogGovernanceResponse> upsertGovernance(@Valid @RequestBody AdminInstitutionalCatalogGovernanceUpsertRequest request) {
        return ResponseEntity.ok(facadeService.upsertGovernance(request));
    }

    @GetMapping("/regras-competencia")
    @PreAuthorize("hasAnyRole('ADMIN','ADMINISTRADOR')")
    public ResponseEntity<List<AdminInstitutionalCompetenceRuleResponse>> listCompetenceRules(@RequestParam(required = false) DestinatarioInstitucionalKind destinatarioKind,
                                                                                               @RequestParam(required = false) String uf) {
        return ResponseEntity.ok(facadeService.listCompetenceRules(destinatarioKind, uf));
    }

    @PostMapping("/regras-competencia")
    @PreAuthorize("hasAnyRole('ADMIN','ADMINISTRADOR')")
    public ResponseEntity<AdminInstitutionalCompetenceRuleResponse> upsertCompetenceRule(@Valid @RequestBody AdminInstitutionalCompetenceRuleUpsertRequest request) {
        return ResponseEntity.ok(facadeService.upsertCompetenceRule(request));
    }

    @GetMapping("/sumario")
    @PreAuthorize("hasAnyRole('ADMIN','ADMINISTRADOR')")
    public ResponseEntity<AdminInstitutionalCatalogGovernanceSummaryResponse> summary() {
        return ResponseEntity.ok(facadeService.summary());
    }

    @GetMapping("/cobertura-nacional")
    @PreAuthorize("hasAnyRole('ADMIN','ADMINISTRADOR')")
    public ResponseEntity<AdminInstitutionalCatalogCoverageSummaryResponse> nationalCoverage() {
        return ResponseEntity.ok(facadeService.nationalCoverage());
    }

    @PostMapping("/expandir-baseline-regional")
    @PreAuthorize("hasAnyRole('ADMIN','ADMINISTRADOR')")
    public ResponseEntity<AdminInstitutionalRegionalBaselineExpansionResponse> expandRegionalBaseline(@Valid @RequestBody AdminInstitutionalRegionalBaselineExpansionRequest request) {
        return ResponseEntity.ok(facadeService.expandRegionalBaseline(request));
    }
}
