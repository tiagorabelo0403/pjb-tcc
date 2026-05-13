package com.tcc.pjb.backend.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalCatalogGovernanceApplicationService;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalNationalExpansionResponse;

@RestController
@RequestMapping("/api/v1/admin/institucional/catalogo")
public class AdminInstitutionalCatalogNationalExpansionController {

    private final InstitutionalCatalogGovernanceApplicationService service;

    public AdminInstitutionalCatalogNationalExpansionController(InstitutionalCatalogGovernanceApplicationService service) {
        this.service = service;
    }

    @PostMapping("/expandir-baseline-nacional")
    @PreAuthorize("hasAnyRole('ADMIN','ADMINISTRADOR')")
    public ResponseEntity<AdminInstitutionalNationalExpansionResponse> expandirBaselineNacional() {
        var result = service.seedNationalSpecializedBaseline();
        return ResponseEntity.ok(new AdminInstitutionalNationalExpansionResponse(
                result.regrasCriadas(),
                result.governancasCriadas(),
                result.catalogVersion(),
                result.generatedAt(),
                result.observacoes()
        ));
    }
}
