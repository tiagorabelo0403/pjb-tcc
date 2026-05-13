package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalCoverageCreateRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalCoverageResponse;
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
public class AdminInstitutionalOperationalCoverageController {

    private final AdminInstitutionalGovernanceFacadeService facadeService;

    public AdminInstitutionalOperationalCoverageController(AdminInstitutionalGovernanceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @PostMapping("/coberturas-operacionais")
    @PreAuthorize("hasAnyRole('ADMIN','ADMINISTRADOR')")
    public ResponseEntity<NationalCommunicationInstitutionalCoverageResponse> criar(@Valid @RequestBody NationalCommunicationInstitutionalCoverageCreateRequest request) {
        return ResponseEntity.ok(facadeService.criarCobertura(request));
    }

    @GetMapping("/coberturas-operacionais")
    @PreAuthorize("hasAnyRole('ADMIN','ADMINISTRADOR')")
    public ResponseEntity<List<NationalCommunicationInstitutionalCoverageResponse>> listar(@RequestParam(required = false) String unidadeCodigo) {
        return ResponseEntity.ok(facadeService.listarCoberturas(unidadeCodigo));
    }
}
