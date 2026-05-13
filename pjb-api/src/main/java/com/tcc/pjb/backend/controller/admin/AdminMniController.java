package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.integration.mni.MniApplicationService;
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
@RequestMapping("/api/v1/admin/mni")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
public class AdminMniController {

    private final MniApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminMniController(MniApplicationService applicationService,
                              ApiResponseFactory apiResponseFactory) {
        this.applicationService = Objects.requireNonNull(applicationService);
        this.apiResponseFactory = Objects.requireNonNull(apiResponseFactory);
    }

    @PostMapping("/remessas/reprocess")
    public ResponseEntity<ApiCommandResponse<?>> reprocessar(@RequestParam(value = "limit", required = false) Integer limit) {
        return ResponseEntity.ok(apiResponseFactory.commandOk("reprocessamento MNI executado", applicationService.reprocessar(limit), List.of()));
    }

    @GetMapping("/remessas/{remessaId}")
    public ResponseEntity<ApiQueryResponse<?>> remessa(@PathVariable Long remessaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.remessa(remessaId), List.of()));
    }

    @GetMapping("/remessas/{remessaId}/status")
    public ResponseEntity<ApiQueryResponse<?>> remessaStatus(@PathVariable Long remessaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.remessaStatus(remessaId), List.of()));
    }

    @GetMapping("/remessas/{remessaId}/timeline")
    public ResponseEntity<ApiQueryResponse<?>> remessaTimeline(@PathVariable Long remessaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.remessaTimeline(remessaId), List.of()));
    }

    @GetMapping("/remessas/{remessaId}/health")
    public ResponseEntity<ApiQueryResponse<?>> remessaHealth(@PathVariable Long remessaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.remessaHealth(remessaId), List.of()));
    }

    @GetMapping("/remessas/{remessaId}/payload")
    public ResponseEntity<ApiQueryResponse<?>> remessaPayload(@PathVariable Long remessaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.remessaPayload(remessaId), List.of()));
    }

    @GetMapping("/remessas/{remessaId}/window")
    public ResponseEntity<ApiQueryResponse<?>> remessaWindow(@PathVariable Long remessaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.remessaWindow(remessaId), List.of()));
    }

    @GetMapping("/tribunais/{tribunalCodigo}/endpoint")
    public ResponseEntity<ApiQueryResponse<?>> endpoint(@PathVariable String tribunalCodigo) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.endpoint(tribunalCodigo), List.of()));
    }

    @GetMapping("/tribunais/{tribunalCodigo}/endpoint/health")
    public ResponseEntity<ApiQueryResponse<?>> endpointHealth(@PathVariable String tribunalCodigo) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.endpointHealth(tribunalCodigo), List.of()));
    }

    @GetMapping("/recepcoes/{recepcaoId}")
    public ResponseEntity<ApiQueryResponse<?>> recepcao(@PathVariable Long recepcaoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.recepcao(recepcaoId), List.of()));
    }

    @GetMapping("/recepcoes/{recepcaoId}/consulta")
    public ResponseEntity<ApiQueryResponse<?>> recepcaoConsulta(@PathVariable Long recepcaoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.recepcaoConsulta(recepcaoId), List.of()));
    }

    @GetMapping("/recepcoes/{recepcaoId}/status")
    public ResponseEntity<ApiQueryResponse<?>> recepcaoStatus(@PathVariable Long recepcaoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.recepcaoStatus(recepcaoId), List.of()));
    }

    @GetMapping("/recepcoes/{recepcaoId}/timeline")
    public ResponseEntity<ApiQueryResponse<?>> recepcaoTimeline(@PathVariable Long recepcaoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.recepcaoTimeline(recepcaoId), List.of()));
    }

    @GetMapping("/recepcoes/{recepcaoId}/health")
    public ResponseEntity<ApiQueryResponse<?>> recepcaoHealth(@PathVariable Long recepcaoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.recepcaoHealth(recepcaoId), List.of()));
    }

    @GetMapping("/recepcoes/{recepcaoId}/envelope")
    public ResponseEntity<ApiQueryResponse<?>> recepcaoEnvelope(@PathVariable Long recepcaoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.recepcaoEnvelope(recepcaoId), List.of()));
    }

    @GetMapping("/recepcoes/{recepcaoId}/failure")
    public ResponseEntity<ApiQueryResponse<?>> recepcaoFailure(@PathVariable Long recepcaoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.recepcaoFailure(recepcaoId), List.of()));
    }
}
