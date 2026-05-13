package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.integration.datajud.feed.DataJudApplicationService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/admin/datajud")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
public class AdminDataJudController {

    private final DataJudApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminDataJudController(DataJudApplicationService applicationService,
                                  ApiResponseFactory apiResponseFactory) {
        this.applicationService = Objects.requireNonNull(applicationService);
        this.apiResponseFactory = Objects.requireNonNull(apiResponseFactory);
    }

    @PostMapping("/tribunais/{tribunalCodigo}/run")
    public ResponseEntity<ApiCommandResponse<?>> run(@PathVariable String tribunalCodigo) {
        return ResponseEntity.ok(apiResponseFactory.commandOk("feed DataJud executado", applicationService.run(tribunalCodigo), List.of()));
    }

    @GetMapping("/tribunais/{tribunalCodigo}/checkpoint")
    public ResponseEntity<ApiQueryResponse<?>> checkpoint(@PathVariable String tribunalCodigo) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.checkpoint(tribunalCodigo), List.of()));
    }

    @GetMapping("/tribunais/{tribunalCodigo}/checkpoint/audit")
    public ResponseEntity<ApiQueryResponse<?>> checkpointAudit(@PathVariable String tribunalCodigo) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.checkpointAudit(tribunalCodigo), List.of()));
    }

    @GetMapping("/tribunais/{tribunalCodigo}/checkpoint/result")
    public ResponseEntity<ApiQueryResponse<?>> checkpointResult(@PathVariable String tribunalCodigo) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.checkpointResult(tribunalCodigo), List.of()));
    }

    @GetMapping("/tribunais/{tribunalCodigo}/health")
    public ResponseEntity<ApiQueryResponse<?>> health(@PathVariable String tribunalCodigo) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.health(tribunalCodigo), List.of()));
    }

    @GetMapping("/tribunais/{tribunalCodigo}/execution/health")
    public ResponseEntity<ApiQueryResponse<?>> executionHealth(@PathVariable String tribunalCodigo) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.executionHealth(tribunalCodigo), List.of()));
    }

    @GetMapping("/tribunais/{tribunalCodigo}/window")
    public ResponseEntity<ApiQueryResponse<?>> window(@PathVariable String tribunalCodigo) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.window(tribunalCodigo), List.of()));
    }

    @GetMapping("/tribunais/{tribunalCodigo}/tribunal-health")
    public ResponseEntity<ApiQueryResponse<?>> tribunalHealth(@PathVariable String tribunalCodigo) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.tribunalHealth(tribunalCodigo), List.of()));
    }

    @GetMapping("/tribunais/{tribunalCodigo}/execution")
    public ResponseEntity<ApiQueryResponse<?>> executionView(@PathVariable String tribunalCodigo) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.executionView(tribunalCodigo), List.of()));
    }

    @GetMapping("/tribunais/{tribunalCodigo}/audit")
    public ResponseEntity<ApiQueryResponse<?>> audit(@PathVariable String tribunalCodigo) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.audit(tribunalCodigo), List.of()));
    }
}
