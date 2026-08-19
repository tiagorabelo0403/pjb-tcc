package com.tcc.pjb.backend.controller.juiz;

import com.tcc.pjb.backend.core.criminal.custodia.CustodiaApplicationService;
import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.model.dto.api.ApiCommandResponse;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping(OperationalApiRoutes.JUDGE_CUSTODIA_BASE)
public class JuizCustodiaController {

    private static final String JUDGE_ROLES = "hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR')";

    private final CustodiaApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;
    private final CapabilityRateLimiter rateLimiter;

    public JuizCustodiaController(CustodiaApplicationService applicationService,
                                  ApiResponseFactory apiResponseFactory,
                                  CapabilityRateLimiter rateLimiter) {
        this.applicationService = Objects.requireNonNull(applicationService);
        this.apiResponseFactory = Objects.requireNonNull(apiResponseFactory);
        this.rateLimiter = Objects.requireNonNull(rateLimiter);
    }

    @GetMapping("/pendentes")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<ApiQueryResponse<?>> pendentes(Authentication authentication) {
        enforce(authentication, "juiz_custodia_pendentes");
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.pendentes(), List.of()));
    }

    @PostMapping("/registrar-prisao")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<ApiCommandResponse<?>> registrarPrisao(@RequestParam("processoId") Long processoId,
                                                                  @RequestParam("presoNome") String presoNome,
                                                                  @RequestParam(value = "presoCpf", required = false) String presoCpf,
                                                                  @RequestParam(value = "dataPrisao", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataPrisao,
                                                                  Authentication authentication) {
        enforce(authentication, "juiz_custodia_registrar_prisao");
        return ResponseEntity.ok(apiResponseFactory.commandOk("prisão registrada", applicationService.registrarPrisao(processoId, presoNome, presoCpf, dataPrisao), List.of()));
    }

    @PostMapping("/{custodiaId}/concluir")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<ApiCommandResponse<?>> concluir(@PathVariable Long custodiaId,
                                                           @RequestParam("resultado") String resultado,
                                                           @RequestParam(value = "medidasCautelares", required = false) List<String> medidasCautelares,
                                                           Authentication authentication) {
        enforce(authentication, "juiz_custodia_concluir");
        return ResponseEntity.ok(apiResponseFactory.commandOk("audiência de custódia concluída", applicationService.concluir(custodiaId, resultado, medidasCautelares), List.of()));
    }

    @GetMapping("/{custodiaId}")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<ApiQueryResponse<?>> consulta(@PathVariable Long custodiaId, Authentication authentication) {
        enforce(authentication, "juiz_custodia_consulta");
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.consulta(custodiaId), List.of()));
    }

    @GetMapping("/{custodiaId}/prazo")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<ApiQueryResponse<?>> prazo(@PathVariable Long custodiaId, Authentication authentication) {
        enforce(authentication, "juiz_custodia_prazo");
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.prazo(custodiaId), List.of()));
    }

    @GetMapping("/{custodiaId}/timeline")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<ApiQueryResponse<?>> timeline(@PathVariable Long custodiaId, Authentication authentication) {
        enforce(authentication, "juiz_custodia_timeline");
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.timeline(custodiaId), List.of()));
    }

    @GetMapping("/processos/{processoId}/medidas")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<ApiQueryResponse<?>> medidas(@PathVariable Long processoId, Authentication authentication) {
        enforce(authentication, "juiz_custodia_medidas");
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.medidas(processoId), List.of()));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
