package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.model.dto.profile.operational.AdminEmergenciaRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.admin.surface.AdminOperationalSurfaceFacadeService;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/governance-national")
@Validated
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
public class AdministradorNacionalGovernanceController {

    private final AdminOperationalSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public AdministradorNacionalGovernanceController(AdminOperationalSurfaceFacadeService facadeService,
                                                     CapabilityRateLimiter rateLimiter) {
        this.facadeService = Objects.requireNonNull(facadeService);
        this.rateLimiter = Objects.requireNonNull(rateLimiter);
    }

    @GetMapping("/snapshot")
    public ResponseEntity<SurfaceSnapshotResponse> snapshot(Authentication authentication) {
        enforce(authentication, "admin_governance_snapshot");
        return ResponseEntity.ok(facadeService.governanceSnapshot());
    }

    @GetMapping("/tribunais/{uf}")
    public ResponseEntity<SurfaceSnapshotResponse> metricasPorTribunal(@PathVariable String uf,
                                                                       Authentication authentication) {
        enforce(authentication, "admin_governance_tribunal");
        return ResponseEntity.ok(facadeService.governanceMetricasTribunal(uf));
    }

    @GetMapping("/comarcas")
    public ResponseEntity<SurfaceSnapshotResponse> metricasPorComarca(@RequestParam String uf,
                                                                      @RequestParam String comarca,
                                                                      Authentication authentication) {
        enforce(authentication, "admin_governance_comarca");
        return ResponseEntity.ok(facadeService.governanceMetricasComarca(uf, comarca));
    }

    @PostMapping("/reconciliacao-global")
    public ResponseEntity<SurfaceActionResponse> executarReconciliacaoGlobal(Authentication authentication) {
        enforce(authentication, "admin_governance_reconciliacao");
        return ResponseEntity.ok(facadeService.governanceExecutarReconciliacaoGlobal());
    }

    @PostMapping("/modo-emergencia")
    public ResponseEntity<SurfaceActionResponse> ativarModoEmergencia(@Valid @RequestBody AdminEmergenciaRequest request,
                                                                      Authentication authentication) {
        enforce(authentication, "admin_governance_modo_emergencia");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.governanceAtivarModoEmergencia(request));
    }


    @GetMapping("/substituicao-programa")
    public ResponseEntity<SurfaceSnapshotResponse> programaSubstituicao(Authentication authentication) {
        enforce(authentication, "admin_governance_substituicao_programa");
        return ResponseEntity.ok(facadeService.governanceProgramaSubstituicao());
    }


    @GetMapping("/substituicao-centro-comando")
    public ResponseEntity<SurfaceSnapshotResponse> centroComandoSubstituicao(Authentication authentication) {
        enforce(authentication, "admin_governance_substituicao_centro_comando");
        return ResponseEntity.ok(facadeService.governanceCentroComandoSubstituicao());
    }

    @GetMapping("/substituicao-centro-comando/tribunal/{tribunalCodigo}")
    public ResponseEntity<SurfaceSnapshotResponse> centroComandoSubstituicaoTribunal(@PathVariable String tribunalCodigo,
                                                                                      Authentication authentication) {
        enforce(authentication, "admin_governance_substituicao_centro_comando_tribunal");
        return ResponseEntity.ok(facadeService.governanceCentroComandoTribunal(tribunalCodigo));
    }


    @GetMapping("/substituicao-war-room")
    public ResponseEntity<SurfaceSnapshotResponse> warRoomSubstituicao(Authentication authentication) {
        enforce(authentication, "admin_governance_substituicao_war_room");
        return ResponseEntity.ok(facadeService.governanceWarRoomSubstituicao());
    }

    @GetMapping("/substituicao-war-room/tribunal/{tribunalCodigo}")
    public ResponseEntity<SurfaceSnapshotResponse> warRoomSubstituicaoTribunal(@PathVariable String tribunalCodigo,
                                                                                Authentication authentication) {
        enforce(authentication, "admin_governance_substituicao_war_room_tribunal");
        return ResponseEntity.ok(facadeService.governanceWarRoomTribunal(tribunalCodigo));
    }

    @GetMapping("/substituicao-cutover-matrix")
    public ResponseEntity<SurfaceSnapshotResponse> cutoverMatrixSubstituicao(Authentication authentication) {
        enforce(authentication, "admin_governance_substituicao_cutover_matrix");
        return ResponseEntity.ok(facadeService.governanceCutoverMatrixSubstituicao());
    }

    @GetMapping("/substituicao-cutover-matrix/tribunal/{tribunalCodigo}")
    public ResponseEntity<SurfaceSnapshotResponse> cutoverMatrixSubstituicaoTribunal(@PathVariable String tribunalCodigo,
                                                                                      Authentication authentication) {
        enforce(authentication, "admin_governance_substituicao_cutover_matrix_tribunal");
        return ResponseEntity.ok(facadeService.governanceCutoverMatrixTribunal(tribunalCodigo));
    }

    @GetMapping("/substituicao-nucleo-duro")
    public ResponseEntity<SurfaceSnapshotResponse> nucleoDuroSubstituicao(Authentication authentication) {
        enforce(authentication, "admin_governance_substituicao_nucleo_duro");
        return ResponseEntity.ok(facadeService.governanceNucleoDuroSubstituicao());
    }

    @GetMapping("/substituicao-nucleo-duro/tribunal/{tribunalCodigo}")
    public ResponseEntity<SurfaceSnapshotResponse> nucleoDuroSubstituicaoTribunal(@PathVariable String tribunalCodigo,
                                                                                   Authentication authentication) {
        enforce(authentication, "admin_governance_substituicao_nucleo_duro_tribunal");
        return ResponseEntity.ok(facadeService.governanceNucleoDuroTribunal(tribunalCodigo));
    }


    @GetMapping("/substituicao-malha-julgadora")
    public ResponseEntity<SurfaceSnapshotResponse> malhaJulgadoraSubstituicao(Authentication authentication) {
        enforce(authentication, "admin_governance_substituicao_malha_julgadora");
        return ResponseEntity.ok(facadeService.governanceMalhaJulgadoraSubstituicao());
    }

    @GetMapping("/substituicao-malha-julgadora/tribunal/{tribunalCodigo}")
    public ResponseEntity<SurfaceSnapshotResponse> malhaJulgadoraSubstituicaoTribunal(@PathVariable String tribunalCodigo,
                                                                                        Authentication authentication) {
        enforce(authentication, "admin_governance_substituicao_malha_julgadora_tribunal");
        return ResponseEntity.ok(facadeService.governanceMalhaJulgadoraTribunal(tribunalCodigo));
    }


    @GetMapping("/substituicao-precedentes-qualificados")
    public ResponseEntity<SurfaceSnapshotResponse> precedentesQualificadosSubstituicao(Authentication authentication) {
        enforce(authentication, "admin_governance_substituicao_precedentes_qualificados");
        return ResponseEntity.ok(facadeService.governancePrecedentesQualificadosSubstituicao());
    }

    @GetMapping("/substituicao-precedentes-qualificados/tribunal/{tribunalCodigo}")
    public ResponseEntity<SurfaceSnapshotResponse> precedentesQualificadosSubstituicaoTribunal(@PathVariable String tribunalCodigo,
                                                                                                Authentication authentication) {
        enforce(authentication, "admin_governance_substituicao_precedentes_qualificados_tribunal");
        return ResponseEntity.ok(facadeService.governancePrecedentesQualificadosTribunal(tribunalCodigo));
    }
    @GetMapping("/substituicao-tutela-coletiva")
    public ResponseEntity<SurfaceSnapshotResponse> tutelaColetivaSubstituicao(Authentication authentication) {
        enforce(authentication, "admin_governance_substituicao_tutela_coletiva");
        return ResponseEntity.ok(facadeService.governanceTutelaColetivaSubstituicao());
    }

    @GetMapping("/substituicao-tutela-coletiva/tribunal/{tribunalCodigo}")
    public ResponseEntity<SurfaceSnapshotResponse> tutelaColetivaSubstituicaoTribunal(@PathVariable String tribunalCodigo,
                                                                                       Authentication authentication) {
        enforce(authentication, "admin_governance_substituicao_tutela_coletiva_tribunal");
        return ResponseEntity.ok(facadeService.governanceTutelaColetivaTribunal(tribunalCodigo));
    }


    @GetMapping("/substituicao-pos-coletiva")
    public ResponseEntity<SurfaceSnapshotResponse> posColetivaSubstituicao(Authentication authentication) {
        enforce(authentication, "admin_governance_substituicao_pos_coletiva");
        return ResponseEntity.ok(facadeService.governancePosColetivaSubstituicao());
    }

    @GetMapping("/substituicao-pos-coletiva/tribunal/{tribunalCodigo}")
    public ResponseEntity<SurfaceSnapshotResponse> posColetivaSubstituicaoTribunal(@PathVariable String tribunalCodigo,
                                                                                    Authentication authentication) {
        enforce(authentication, "admin_governance_substituicao_pos_coletiva_tribunal");
        return ResponseEntity.ok(facadeService.governancePosColetivaTribunal(tribunalCodigo));
    }


    @GetMapping("/plataforma-sustentacao")
    public ResponseEntity<SurfaceSnapshotResponse> plataformaSustentacao(Authentication authentication) {
        enforce(authentication, "admin_governance_plataforma_sustentacao");
        return ResponseEntity.ok(facadeService.governancePlataformaSustentacao());
    }

    @GetMapping("/health-check")
    public ResponseEntity<SurfaceSnapshotResponse> healthCheck(Authentication authentication) {
        enforce(authentication, "admin_governance_health");
        return ResponseEntity.ok(facadeService.governanceHealthCheck());
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
