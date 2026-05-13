package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.integration.judicial.financeiro.SisbajudApplicationService;
import com.tcc.pjb.backend.model.dto.api.ApiCommandResponse;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/admin/sisbajud")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
public class AdminSisbajudController {

    private final SisbajudApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminSisbajudController(SisbajudApplicationService applicationService,
                                   ApiResponseFactory apiResponseFactory) {
        this.applicationService = Objects.requireNonNull(applicationService);
        this.apiResponseFactory = Objects.requireNonNull(apiResponseFactory);
    }

    @PostMapping("/operacoes/bloqueio")
    public ResponseEntity<ApiCommandResponse<?>> bloquear(@RequestParam("processoId") Long processoId,
                                                          @RequestParam("cpfDevedor") String cpfDevedor,
                                                          @RequestParam("valor") BigDecimal valor,
                                                          @RequestParam("numeroOficio") String numeroOficio,
                                                          @RequestParam("authzTrailId") String authzTrailId,
                                                          @RequestParam(value = "delegatedOperation", defaultValue = "false") boolean delegatedOperation) {
        return ResponseEntity.ok(apiResponseFactory.commandOk("bloqueio SISBAJUD solicitado", applicationService.bloquear(processoId, cpfDevedor, valor, numeroOficio, authzTrailId, delegatedOperation), List.of()));
    }

    @GetMapping("/operacoes/{operacaoId}")
    public ResponseEntity<ApiQueryResponse<?>> consulta(@PathVariable Long operacaoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.consulta(operacaoId), List.of()));
    }

    @GetMapping("/operacoes/{operacaoId}/snapshot")
    public ResponseEntity<ApiQueryResponse<?>> snapshot(@PathVariable Long operacaoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.snapshot(operacaoId), List.of()));
    }

    @GetMapping("/operacoes/{operacaoId}/retry")
    public ResponseEntity<ApiQueryResponse<?>> retry(@PathVariable Long operacaoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.retry(operacaoId), List.of()));
    }

    @GetMapping("/operacoes/{operacaoId}/audit")
    public ResponseEntity<ApiQueryResponse<?>> audit(@PathVariable Long operacaoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.audit(operacaoId), List.of()));
    }

    @GetMapping("/operacoes/{operacaoId}/view")
    public ResponseEntity<ApiQueryResponse<?>> view(@PathVariable Long operacaoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.view(operacaoId), List.of()));
    }
}
