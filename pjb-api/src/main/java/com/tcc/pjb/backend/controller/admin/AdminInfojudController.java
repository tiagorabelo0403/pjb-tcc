package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.integration.judicial.financeiro.InfojudApplicationService;
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
@RequestMapping("/api/v1/admin/infojud")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
public class AdminInfojudController {

    private final InfojudApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminInfojudController(InfojudApplicationService applicationService,
                                  ApiResponseFactory apiResponseFactory) {
        this.applicationService = Objects.requireNonNull(applicationService);
        this.apiResponseFactory = Objects.requireNonNull(apiResponseFactory);
    }

    @PostMapping("/consultas")
    public ResponseEntity<ApiCommandResponse<?>> consultar(@RequestParam("processoId") Long processoId,
                                                           @RequestParam("cpfCnpjConsultado") String cpfCnpjConsultado,
                                                           @RequestParam("authzTrailId") String authzTrailId,
                                                           @RequestParam(value = "delegatedOperation", defaultValue = "false") boolean delegatedOperation) {
        return ResponseEntity.ok(apiResponseFactory.commandOk("consulta INFOJUD solicitada", applicationService.consultar(processoId, cpfCnpjConsultado, authzTrailId, delegatedOperation), List.of()));
    }

    @GetMapping("/consultas/{consultaId}/snapshot")
    public ResponseEntity<ApiQueryResponse<?>> snapshot(@PathVariable Long consultaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.snapshot(consultaId), List.of()));
    }

    @GetMapping("/consultas/{consultaId}/view")
    public ResponseEntity<ApiQueryResponse<?>> view(@PathVariable Long consultaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.view(consultaId), List.of()));
    }

    @GetMapping("/consultas/{consultaId}/audit")
    public ResponseEntity<ApiQueryResponse<?>> audit(@PathVariable Long consultaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.audit(consultaId), List.of()));
    }

    @GetMapping("/consultas/{consultaId}/status")
    public ResponseEntity<ApiQueryResponse<?>> status(@PathVariable Long consultaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.status(consultaId), List.of()));
    }

    @GetMapping("/consultas/{consultaId}/health")
    public ResponseEntity<ApiQueryResponse<?>> health(@PathVariable Long consultaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.health(consultaId), List.of()));
    }

    @GetMapping("/consultas/{consultaId}/timeline")
    public ResponseEntity<ApiQueryResponse<?>> timeline(@PathVariable Long consultaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.timeline(consultaId), List.of()));
    }

    @GetMapping("/consultas/{consultaId}/owner")
    public ResponseEntity<ApiQueryResponse<?>> owner(@PathVariable Long consultaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.owner(consultaId), List.of()));
    }

    @GetMapping("/consultas/{consultaId}/window")
    public ResponseEntity<ApiQueryResponse<?>> window(@PathVariable Long consultaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.window(consultaId), List.of()));
    }

    @GetMapping("/consultas/{consultaId}/consistency")
    public ResponseEntity<ApiQueryResponse<?>> consistency(@PathVariable Long consultaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.consistency(consultaId), List.of()));
    }

    @GetMapping("/processos/{processoId}/consultas")
    public ResponseEntity<ApiQueryResponse<?>> processo(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.processo(processoId), List.of()));
    }

    @GetMapping("/retry-summary")
    public ResponseEntity<ApiQueryResponse<?>> retrySummary(@RequestParam(value = "limit", defaultValue = "20") int limit) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.retrySummary(limit), List.of()));
    }
}
