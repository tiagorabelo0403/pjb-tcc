package com.tcc.pjb.backend.controller.juiz;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.integration.judicial.financeiro.InfojudApplicationService;
import com.tcc.pjb.backend.integration.judicial.financeiro.SisbajudApplicationService;
import com.tcc.pjb.backend.model.dto.api.ApiCommandResponse;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping(OperationalApiRoutes.JUDGE_SISTEMAS_EXTERNOS_BASE)
public class JuizSistemasExternosController {

    private static final String JUDGE_ROLES = "hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR')";

    private final SisbajudApplicationService sisbajudApplicationService;
    private final InfojudApplicationService infojudApplicationService;
    private final ApiResponseFactory apiResponseFactory;
    private final CapabilityRateLimiter rateLimiter;

    public JuizSistemasExternosController(SisbajudApplicationService sisbajudApplicationService,
                                          InfojudApplicationService infojudApplicationService,
                                          ApiResponseFactory apiResponseFactory,
                                          CapabilityRateLimiter rateLimiter) {
        this.sisbajudApplicationService = Objects.requireNonNull(sisbajudApplicationService);
        this.infojudApplicationService = Objects.requireNonNull(infojudApplicationService);
        this.apiResponseFactory = Objects.requireNonNull(apiResponseFactory);
        this.rateLimiter = Objects.requireNonNull(rateLimiter);
    }

    @PostMapping("/sisbajud/operacoes/bloqueio")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<ApiCommandResponse<?>> bloquearSisbajud(@RequestParam("processoId") Long processoId,
                                                                   @RequestParam("cpfDevedor") String cpfDevedor,
                                                                   @RequestParam("valor") BigDecimal valor,
                                                                   @RequestParam("numeroOficio") String numeroOficio,
                                                                   @RequestParam("authzTrailId") String authzTrailId,
                                                                   @RequestParam(value = "delegatedOperation", defaultValue = "false") boolean delegatedOperation,
                                                                   Authentication authentication) {
        enforce(authentication, "juiz_sisbajud_bloqueio");
        return ResponseEntity.ok(apiResponseFactory.commandOk("bloqueio SISBAJUD solicitado",
                sisbajudApplicationService.bloquear(processoId, cpfDevedor, valor, numeroOficio, authzTrailId, delegatedOperation), List.of()));
    }

    @GetMapping("/sisbajud/operacoes/{operacaoId}")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<ApiQueryResponse<?>> consultaSisbajud(@PathVariable Long operacaoId, Authentication authentication) {
        enforce(authentication, "juiz_sisbajud_consulta");
        return ResponseEntity.ok(apiResponseFactory.queryOk(sisbajudApplicationService.consulta(operacaoId), List.of()));
    }

    @GetMapping("/sisbajud/operacoes/{operacaoId}/view")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<ApiQueryResponse<?>> viewSisbajud(@PathVariable Long operacaoId, Authentication authentication) {
        enforce(authentication, "juiz_sisbajud_view");
        return ResponseEntity.ok(apiResponseFactory.queryOk(sisbajudApplicationService.view(operacaoId), List.of()));
    }

    @PostMapping("/infojud/consultas")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<ApiCommandResponse<?>> consultarInfojud(@RequestParam("processoId") Long processoId,
                                                                   @RequestParam("cpfCnpjConsultado") String cpfCnpjConsultado,
                                                                   @RequestParam("authzTrailId") String authzTrailId,
                                                                   @RequestParam(value = "delegatedOperation", defaultValue = "false") boolean delegatedOperation,
                                                                   Authentication authentication) {
        enforce(authentication, "juiz_infojud_consulta");
        return ResponseEntity.ok(apiResponseFactory.commandOk("consulta INFOJUD solicitada",
                infojudApplicationService.consultar(processoId, cpfCnpjConsultado, authzTrailId, delegatedOperation), List.of()));
    }

    @GetMapping("/infojud/consultas/{consultaId}/view")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<ApiQueryResponse<?>> viewInfojud(@PathVariable Long consultaId, Authentication authentication) {
        enforce(authentication, "juiz_infojud_view");
        return ResponseEntity.ok(apiResponseFactory.queryOk(infojudApplicationService.view(consultaId), List.of()));
    }

    @GetMapping("/infojud/consultas/{consultaId}/status")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<ApiQueryResponse<?>> statusInfojud(@PathVariable Long consultaId, Authentication authentication) {
        enforce(authentication, "juiz_infojud_status");
        return ResponseEntity.ok(apiResponseFactory.queryOk(infojudApplicationService.status(consultaId), List.of()));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
