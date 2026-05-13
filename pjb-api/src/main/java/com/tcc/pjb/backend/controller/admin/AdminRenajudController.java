package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.integration.judicial.financeiro.RenajudApplicationService;
import com.tcc.pjb.backend.model.dto.api.ApiCommandResponse;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
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
@RequestMapping("/api/v1/admin/renajud")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
public class AdminRenajudController {

    private final RenajudApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminRenajudController(RenajudApplicationService applicationService,
                                  ApiResponseFactory apiResponseFactory) {
        this.applicationService = Objects.requireNonNull(applicationService);
        this.apiResponseFactory = Objects.requireNonNull(apiResponseFactory);
    }

    @PostMapping("/restricoes")
    public ResponseEntity<ApiCommandResponse<?>> restringir(@RequestParam("processoId") Long processoId,
                                                            @RequestParam("placa") String placa,
                                                            @RequestParam(value = "renavam", required = false) String renavam,
                                                            @RequestParam("tipo") String tipo,
                                                            @RequestParam("authzTrailId") String authzTrailId) {
        return ResponseEntity.ok(apiResponseFactory.commandOk("restricao RENAJUD solicitada", applicationService.restringir(processoId, placa, renavam, tipo, authzTrailId), List.of()));
    }

    @GetMapping("/restricoes/{restricaoId}/snapshot")
    public ResponseEntity<ApiQueryResponse<?>> snapshot(@PathVariable Long restricaoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.snapshot(restricaoId), List.of()));
    }

    @GetMapping("/restricoes/{restricaoId}/view")
    public ResponseEntity<ApiQueryResponse<?>> view(@PathVariable Long restricaoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.view(restricaoId), List.of()));
    }

    @GetMapping("/restricoes/{restricaoId}/audit")
    public ResponseEntity<ApiQueryResponse<?>> audit(@PathVariable Long restricaoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.audit(restricaoId), List.of()));
    }

    @GetMapping("/restricoes/{restricaoId}/status")
    public ResponseEntity<ApiQueryResponse<?>> status(@PathVariable Long restricaoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.status(restricaoId), List.of()));
    }

    @GetMapping("/restricoes/{restricaoId}/health")
    public ResponseEntity<ApiQueryResponse<?>> health(@PathVariable Long restricaoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.health(restricaoId), List.of()));
    }

    @GetMapping("/restricoes/{restricaoId}/timeline")
    public ResponseEntity<ApiQueryResponse<?>> timeline(@PathVariable Long restricaoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.timeline(restricaoId), List.of()));
    }

    @GetMapping("/restricoes/{restricaoId}/owner")
    public ResponseEntity<ApiQueryResponse<?>> owner(@PathVariable Long restricaoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.owner(restricaoId), List.of()));
    }

    @GetMapping("/restricoes/{restricaoId}/window")
    public ResponseEntity<ApiQueryResponse<?>> window(@PathVariable Long restricaoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.window(restricaoId), List.of()));
    }

    @GetMapping("/restricoes/{restricaoId}/consistency")
    public ResponseEntity<ApiQueryResponse<?>> consistency(@PathVariable Long restricaoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.consistency(restricaoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/restricoes")
    public ResponseEntity<ApiQueryResponse<?>> processo(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.processo(processoId), List.of()));
    }

    @GetMapping("/retry-summary")
    public ResponseEntity<ApiQueryResponse<?>> retrySummary(@RequestParam(value = "limit", defaultValue = "20") int limit) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.retrySummary(limit), List.of()));
    }
}
