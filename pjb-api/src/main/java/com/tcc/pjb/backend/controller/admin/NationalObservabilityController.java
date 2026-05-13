package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.admin.surface.AdminOperationalSurfaceFacadeService;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(NationalObservabilityRoutes.CANONICAL_BASE)
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
public class NationalObservabilityController {

    private final AdminOperationalSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public NationalObservabilityController(AdminOperationalSurfaceFacadeService facadeService,
                                           CapabilityRateLimiter rateLimiter) {
        this.facadeService = Objects.requireNonNull(facadeService);
        this.rateLimiter = Objects.requireNonNull(rateLimiter);
    }

    @GetMapping(NationalObservabilityRoutes.PATH_DASHBOARD)
    public ResponseEntity<SurfaceSnapshotResponse> dashboard(Authentication authentication) {
        enforce(authentication, "observability_national_dashboard");
        return ResponseEntity.ok(facadeService.observabilityDashboard());
    }

    @GetMapping(NationalObservabilityRoutes.PATH_SLA_REPORT)
    public ResponseEntity<SurfaceSnapshotResponse> slaReport(Authentication authentication) {
        enforce(authentication, "observability_national_sla");
        return ResponseEntity.ok(facadeService.observabilitySlaReport());
    }

    @GetMapping(NationalObservabilityRoutes.PATH_RUNBOOK_STATUS)
    public ResponseEntity<SurfaceSnapshotResponse> runbookStatus(Authentication authentication) {
        enforce(authentication, "observability_national_runbook");
        return ResponseEntity.ok(facadeService.observabilityRunbookStatus());
    }

    @GetMapping(NationalObservabilityRoutes.PATH_SUBSTITUICAO_READINESS)
    public ResponseEntity<SurfaceSnapshotResponse> substituicaoReadiness(Authentication authentication) {
        enforce(authentication, "observability_national_substituicao");
        return ResponseEntity.ok(facadeService.observabilitySubstituicaoReadiness());
    }


    @GetMapping(NationalObservabilityRoutes.PATH_SUBSTITUICAO_CENTRO_COMANDO)
    public ResponseEntity<SurfaceSnapshotResponse> substituicaoCentroComando(Authentication authentication) {
        enforce(authentication, "observability_national_substituicao_centro_comando");
        return ResponseEntity.ok(facadeService.observabilitySubstituicaoCentroComando());
    }


    @GetMapping(NationalObservabilityRoutes.PATH_SUBSTITUICAO_WAR_ROOM)
    public ResponseEntity<SurfaceSnapshotResponse> substituicaoWarRoom(Authentication authentication) {
        enforce(authentication, "observability_national_substituicao_war_room");
        return ResponseEntity.ok(facadeService.observabilitySubstituicaoWarRoom());
    }

    @GetMapping(NationalObservabilityRoutes.PATH_SUBSTITUICAO_WAR_ROOM_TRIBUNAL)
    public ResponseEntity<SurfaceSnapshotResponse> substituicaoWarRoomTribunal(@PathVariable String tribunalCodigo, Authentication authentication) {
        enforce(authentication, "observability_national_substituicao_war_room_tribunal");
        return ResponseEntity.ok(facadeService.observabilitySubstituicaoWarRoomTribunal(tribunalCodigo));
    }

    @GetMapping(NationalObservabilityRoutes.PATH_SUBSTITUICAO_CUTOVER_MATRIX)
    public ResponseEntity<SurfaceSnapshotResponse> substituicaoCutoverMatrix(Authentication authentication) {
        enforce(authentication, "observability_national_substituicao_cutover_matrix");
        return ResponseEntity.ok(facadeService.observabilitySubstituicaoCutoverMatrix());
    }

    @GetMapping(NationalObservabilityRoutes.PATH_SUBSTITUICAO_CUTOVER_MATRIX_TRIBUNAL)
    public ResponseEntity<SurfaceSnapshotResponse> substituicaoCutoverMatrixTribunal(@PathVariable String tribunalCodigo, Authentication authentication) {
        enforce(authentication, "observability_national_substituicao_cutover_matrix_tribunal");
        return ResponseEntity.ok(facadeService.observabilitySubstituicaoCutoverMatrixTribunal(tribunalCodigo));
    }

    @GetMapping(NationalObservabilityRoutes.PATH_SUBSTITUICAO_NUCLEO_DURO)
    public ResponseEntity<SurfaceSnapshotResponse> substituicaoNucleoDuro(Authentication authentication) {
        enforce(authentication, "observability_national_substituicao_nucleo_duro");
        return ResponseEntity.ok(facadeService.observabilitySubstituicaoNucleoDuro());
    }

    @GetMapping(NationalObservabilityRoutes.PATH_SUBSTITUICAO_NUCLEO_DURO_TRIBUNAL)
    public ResponseEntity<SurfaceSnapshotResponse> substituicaoNucleoDuroTribunal(@PathVariable String tribunalCodigo, Authentication authentication) {
        enforce(authentication, "observability_national_substituicao_nucleo_duro_tribunal");
        return ResponseEntity.ok(facadeService.observabilitySubstituicaoNucleoDuroTribunal(tribunalCodigo));
    }


    @GetMapping(NationalObservabilityRoutes.PATH_SUBSTITUICAO_MALHA_JULGADORA)
    public ResponseEntity<SurfaceSnapshotResponse> substituicaoMalhaJulgadora(Authentication authentication) {
        enforce(authentication, "observability_national_substituicao_malha_julgadora");
        return ResponseEntity.ok(facadeService.observabilitySubstituicaoMalhaJulgadora());
    }

    @GetMapping(NationalObservabilityRoutes.PATH_SUBSTITUICAO_MALHA_JULGADORA_TRIBUNAL)
    public ResponseEntity<SurfaceSnapshotResponse> substituicaoMalhaJulgadoraTribunal(@PathVariable String tribunalCodigo, Authentication authentication) {
        enforce(authentication, "observability_national_substituicao_malha_julgadora_tribunal");
        return ResponseEntity.ok(facadeService.observabilitySubstituicaoMalhaJulgadoraTribunal(tribunalCodigo));
    }


    @GetMapping(NationalObservabilityRoutes.PATH_SUBSTITUICAO_PRECEDENTES_QUALIFICADOS)
    public ResponseEntity<SurfaceSnapshotResponse> substituicaoPrecedentesQualificados(Authentication authentication) {
        enforce(authentication, "observability_national_substituicao_precedentes_qualificados");
        return ResponseEntity.ok(facadeService.observabilitySubstituicaoPrecedentesQualificados());
    }

    @GetMapping(NationalObservabilityRoutes.PATH_SUBSTITUICAO_PRECEDENTES_QUALIFICADOS_TRIBUNAL)
    public ResponseEntity<SurfaceSnapshotResponse> substituicaoPrecedentesQualificadosTribunal(@PathVariable String tribunalCodigo, Authentication authentication) {
        enforce(authentication, "observability_national_substituicao_precedentes_qualificados_tribunal");
        return ResponseEntity.ok(facadeService.observabilitySubstituicaoPrecedentesQualificadosTribunal(tribunalCodigo));
    }
    @GetMapping(NationalObservabilityRoutes.PATH_SUBSTITUICAO_TUTELA_COLETIVA)
    public ResponseEntity<SurfaceSnapshotResponse> substituicaoTutelaColetiva(Authentication authentication) {
        enforce(authentication, "observability_national_substituicao_tutela_coletiva");
        return ResponseEntity.ok(facadeService.observabilitySubstituicaoTutelaColetiva());
    }

    @GetMapping(NationalObservabilityRoutes.PATH_SUBSTITUICAO_TUTELA_COLETIVA_TRIBUNAL)
    public ResponseEntity<SurfaceSnapshotResponse> substituicaoTutelaColetivaTribunal(@PathVariable String tribunalCodigo, Authentication authentication) {
        enforce(authentication, "observability_national_substituicao_tutela_coletiva_tribunal");
        return ResponseEntity.ok(facadeService.observabilitySubstituicaoTutelaColetivaTribunal(tribunalCodigo));
    }


    @GetMapping(NationalObservabilityRoutes.PATH_SUBSTITUICAO_POS_COLETIVA)
    public ResponseEntity<SurfaceSnapshotResponse> substituicaoPosColetiva(Authentication authentication) {
        enforce(authentication, "observability_national_substituicao_pos_coletiva");
        return ResponseEntity.ok(facadeService.observabilitySubstituicaoPosColetiva());
    }

    @GetMapping(NationalObservabilityRoutes.PATH_SUBSTITUICAO_POS_COLETIVA_TRIBUNAL)
    public ResponseEntity<SurfaceSnapshotResponse> substituicaoPosColetivaTribunal(@PathVariable String tribunalCodigo, Authentication authentication) {
        enforce(authentication, "observability_national_substituicao_pos_coletiva_tribunal");
        return ResponseEntity.ok(facadeService.observabilitySubstituicaoPosColetivaTribunal(tribunalCodigo));
    }


    @GetMapping(NationalObservabilityRoutes.PATH_PLATAFORMA_SUSTENTACAO)
    public ResponseEntity<SurfaceSnapshotResponse> plataformaSustentacao(Authentication authentication) {
        enforce(authentication, "observability_national_plataforma_sustentacao");
        return ResponseEntity.ok(facadeService.observabilityPlataformaSustentacao());
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
