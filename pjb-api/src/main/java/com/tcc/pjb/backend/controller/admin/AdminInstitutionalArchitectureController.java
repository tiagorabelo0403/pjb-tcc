package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalArchitectureResponse;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalPublicRecognitionResponse;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalShardResolutionResponse;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalVisibilitySimulationResponse;
import com.tcc.pjb.backend.service.institutional.architecture.InstitutionalArchitectureSurfaceFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/institucional/arquitetura")
public class AdminInstitutionalArchitectureController {

    private final InstitutionalArchitectureSurfaceFacadeService facadeService;

    public AdminInstitutionalArchitectureController(InstitutionalArchitectureSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/blueprint")
    @PreAuthorize("hasAnyRole('ADMIN','ADMINISTRADOR')")
    public ResponseEntity<AdminInstitutionalArchitectureResponse> blueprint() {
        return ResponseEntity.ok(facadeService.blueprint());
    }

    @GetMapping("/visibilidade/simular")
    @PreAuthorize("hasAnyRole('ADMIN','ADMINISTRADOR')")
    public ResponseEntity<AdminInstitutionalVisibilitySimulationResponse> simulateVisibility(@RequestParam(defaultValue = "false") boolean sameJurisdictionUnit,
                                                                                             @RequestParam(defaultValue = "false") boolean funcionalCompetence,
                                                                                             @RequestParam(defaultValue = "false") boolean cooperativeGrantActive,
                                                                                             @RequestParam(defaultValue = "false") boolean systemicSupervision,
                                                                                             @RequestParam(defaultValue = "false") boolean breakGlassActive,
                                                                                             @RequestParam(defaultValue = "false") boolean sigiloProcessual) {
        AdminInstitutionalArchitectureResponse.VisibilitySimulation simulation = facadeService.simulateVisibility(
                sameJurisdictionUnit,
                funcionalCompetence,
                cooperativeGrantActive,
                systemicSupervision,
                breakGlassActive,
                sigiloProcessual
        );
        return ResponseEntity.ok(new AdminInstitutionalVisibilitySimulationResponse(
                simulation.tierCode(),
                simulation.tierLabel(),
                simulation.allowed(),
                simulation.auditRequired(),
                simulation.timeBound(),
                simulation.reasons(),
                simulation.restrictions()
        ));
    }

    @GetMapping("/shards/resolver")
    @PreAuthorize("hasAnyRole('ADMIN','ADMINISTRADOR')")
    public ResponseEntity<AdminInstitutionalShardResolutionResponse> resolveShard(@RequestParam(required = false) String tribunalCodigo,
                                                                                   @RequestParam(required = false) String uf) {
        AdminInstitutionalArchitectureResponse.ClusterResolution resolution = facadeService.resolveShard(tribunalCodigo, uf);
        return ResponseEntity.ok(new AdminInstitutionalShardResolutionResponse(
                resolution.clusterCode(),
                resolution.clusterLabel(),
                resolution.metadataKey(),
                resolution.federated(),
                resolution.reasons()
        ));
    }

    @GetMapping("/reconhecimento-publico/simular")
    @PreAuthorize("hasAnyRole('ADMIN','ADMINISTRADOR')")
    public ResponseEntity<AdminInstitutionalPublicRecognitionResponse> simulatePublicRecognition(@RequestParam(defaultValue = "ESTADUAL_MUNICIPAL") String scope,
                                                                                                  @RequestParam(defaultValue = "false") boolean officialCatalogMatch,
                                                                                                  @RequestParam(defaultValue = "false") boolean publicCnpjActive,
                                                                                                  @RequestParam(defaultValue = "false") boolean publicNatureCompatible,
                                                                                                  @RequestParam(defaultValue = "false") boolean officialEmailChannel,
                                                                                                  @RequestParam(defaultValue = "false") boolean officialDomain,
                                                                                                  @RequestParam(defaultValue = "false") boolean legalActPresent,
                                                                                                  @RequestParam(defaultValue = "false") boolean territorialMatch,
                                                                                                  @RequestParam(defaultValue = "false") boolean representativeGovBrGold,
                                                                                                  @RequestParam(defaultValue = "false") boolean representativeIcpBrasilValid,
                                                                                                  @RequestParam(defaultValue = "false") boolean subordinateUnitWithoutOwnCnpj,
                                                                                                  @RequestParam(defaultValue = "false") boolean parentInstitutionRecognized) {
        return ResponseEntity.ok(facadeService.assessPublicRecognition(
                scope,
                officialCatalogMatch,
                publicCnpjActive,
                publicNatureCompatible,
                officialEmailChannel,
                officialDomain,
                legalActPresent,
                territorialMatch,
                representativeGovBrGold,
                representativeIcpBrasilValid,
                subordinateUnitWithoutOwnCnpj,
                parentInstitutionRecognized
        ));
    }
}
