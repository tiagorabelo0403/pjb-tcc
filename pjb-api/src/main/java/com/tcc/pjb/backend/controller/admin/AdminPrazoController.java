package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.core.prazos.application.PrazoApplicationService;
import com.tcc.pjb.backend.core.prazos.PrazoRegime;
import com.tcc.pjb.backend.core.prazos.auditoria.domain.PrazoAuditQuery;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/admin/prazos")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
public class AdminPrazoController {

    private final PrazoApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminPrazoController(PrazoApplicationService applicationService,
                                ApiResponseFactory apiResponseFactory) {
        this.applicationService = Objects.requireNonNull(applicationService);
        this.apiResponseFactory = Objects.requireNonNull(apiResponseFactory);
    }

    @GetMapping("/health")
    public ResponseEntity<ApiQueryResponse<?>> health(@RequestParam(value = "uf", required = false) String uf,
                                                      @RequestParam(value = "comarca", required = false) String comarca) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.health(uf, comarca), List.of()));
    }

    @GetMapping("/calculo")
    public ResponseEntity<ApiQueryResponse<?>> calculo(@RequestParam("inicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
                                                       @RequestParam("quantidade") int quantidade,
                                                       @RequestParam("regime") PrazoRegime regime,
                                                       @RequestParam(value = "uf", required = false) String uf,
                                                       @RequestParam(value = "comarca", required = false) String comarca) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.calcular(inicio, quantidade, regime, uf, comarca), List.of()));
    }

    @GetMapping("/window")
    public ResponseEntity<ApiQueryResponse<?>> window(@RequestParam("inicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
                                                      @RequestParam("quantidade") int quantidade,
                                                      @RequestParam("regime") PrazoRegime regime,
                                                      @RequestParam(value = "uf", required = false) String uf,
                                                      @RequestParam(value = "comarca", required = false) String comarca) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.window(inicio, quantidade, regime, uf, comarca), List.of()));
    }

    @GetMapping("/calendario/health")
    public ResponseEntity<ApiQueryResponse<?>> calendarioHealth(@RequestParam(value = "uf", required = false) String uf,
                                                                @RequestParam(value = "comarca", required = false) String comarca) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.calendarioHealth(uf, comarca), List.of()));
    }

    @GetMapping("/regime/{regime}")
    public ResponseEntity<ApiQueryResponse<?>> regime(@PathVariable PrazoRegime regime) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.regime(regime), List.of()));
    }

    @GetMapping("/policy")
    public ResponseEntity<ApiQueryResponse<?>> policy(@RequestParam(value = "ramo", required = false) String ramo,
                                                      @RequestParam(value = "rito", required = false) String rito,
                                                      @RequestParam(value = "defensoria", defaultValue = "false") boolean defensoria,
                                                      @RequestParam(value = "ministerioPublico", defaultValue = "false") boolean ministerioPublico,
                                                      @RequestParam(value = "fazenda", defaultValue = "false") boolean fazenda) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.policy(ramo, rito, defensoria, ministerioPublico, fazenda), List.of()));
    }

    @GetMapping("/audit")
    public ResponseEntity<ApiQueryResponse<?>> audit(@RequestParam("processoId") Long processoId,
                                                     @RequestParam("eventoRef") String eventoRef,
                                                     @RequestParam(value = "quantidade", defaultValue = "0") int quantidade,
                                                     @RequestParam(value = "regime", defaultValue = "UTEIS") PrazoRegime regime,
                                                     @RequestParam(value = "inicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
                                                     @RequestParam(value = "fim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
                                                     @RequestParam(value = "uf", required = false) String uf,
                                                     @RequestParam(value = "comarca", required = false) String comarca) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.audit(new PrazoAuditQuery(processoId, eventoRef, quantidade, regime, inicio, fim, uf, comarca)), List.of()));
    }

    @GetMapping("/audit/health")
    public ResponseEntity<ApiQueryResponse<?>> auditHealth(@RequestParam("processoId") Long processoId,
                                                           @RequestParam("eventoRef") String eventoRef,
                                                           @RequestParam(value = "quantidade", defaultValue = "0") int quantidade,
                                                           @RequestParam(value = "regime", defaultValue = "UTEIS") PrazoRegime regime,
                                                           @RequestParam(value = "inicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
                                                           @RequestParam(value = "fim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
                                                           @RequestParam(value = "uf", required = false) String uf,
                                                           @RequestParam(value = "comarca", required = false) String comarca) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.auditHealth(new PrazoAuditQuery(processoId, eventoRef, quantidade, regime, inicio, fim, uf, comarca)), List.of()));
    }

    @GetMapping("/timeline")
    public ResponseEntity<ApiQueryResponse<?>> timeline(@RequestParam("processoId") Long processoId,
                                                        @RequestParam("eventoRef") String eventoRef,
                                                        @RequestParam(value = "quantidade", defaultValue = "0") int quantidade,
                                                        @RequestParam(value = "regime", defaultValue = "UTEIS") PrazoRegime regime,
                                                        @RequestParam(value = "inicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
                                                        @RequestParam(value = "fim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
                                                        @RequestParam(value = "uf", required = false) String uf,
                                                        @RequestParam(value = "comarca", required = false) String comarca) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.timeline(new PrazoAuditQuery(processoId, eventoRef, quantidade, regime, inicio, fim, uf, comarca)), List.of()));
    }
}
