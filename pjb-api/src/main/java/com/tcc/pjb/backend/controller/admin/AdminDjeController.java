package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.core.dje.DjeApplicationService;
import com.tcc.pjb.backend.model.dto.api.ApiCommandResponse;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/v1/admin/dje")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
public class AdminDjeController {

    private final DjeApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminDjeController(DjeApplicationService applicationService,
                              ApiResponseFactory apiResponseFactory) {
        this.applicationService = Objects.requireNonNull(applicationService);
        this.apiResponseFactory = Objects.requireNonNull(apiResponseFactory);
    }

    @GetMapping("/metrics/publication")
    public ResponseEntity<ApiQueryResponse<?>> metrics() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.metrics(), List.of()));
    }

    @PostMapping("/lifecycle/run")
    public ResponseEntity<ApiCommandResponse<?>> lifecycleRun(@RequestParam(value = "hoje", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hoje,
                                                              @RequestParam(value = "batchSize", required = false) Integer batchSize) {
        return ResponseEntity.ok(apiResponseFactory.commandOk(
                "lifecycle do DJe executado",
                applicationService.lifecycleRun(hoje, batchSize),
                List.of()));
    }

    @GetMapping("/publicacoes/{djeId}")
    public ResponseEntity<ApiQueryResponse<?>> publication(@PathVariable Long djeId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.publication(djeId), List.of()));
    }

    @GetMapping("/publicacoes/{djeId}/status")
    public ResponseEntity<ApiQueryResponse<?>> status(@PathVariable Long djeId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.status(djeId), List.of()));
    }

    @GetMapping("/publicacoes/{djeId}/health")
    public ResponseEntity<ApiQueryResponse<?>> health(@PathVariable Long djeId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.health(djeId), List.of()));
    }

    @GetMapping("/publicacoes/{djeId}/lifecycle/health")
    public ResponseEntity<ApiQueryResponse<?>> lifecycleHealth(@PathVariable Long djeId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.lifecycleHealth(djeId), List.of()));
    }

    @GetMapping("/publicacoes/{djeId}/timeline")
    public ResponseEntity<ApiQueryResponse<?>> timeline(@PathVariable Long djeId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.timeline(djeId), List.of()));
    }

    @GetMapping("/publicacoes/{djeId}/prazo")
    public ResponseEntity<ApiQueryResponse<?>> prazo(@PathVariable Long djeId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.prazo(djeId), List.of()));
    }

    @GetMapping("/publicacoes/{djeId}/prazo/health")
    public ResponseEntity<ApiQueryResponse<?>> prazoHealth(@PathVariable Long djeId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.prazoHealth(djeId), List.of()));
    }

    @GetMapping("/publicacoes/{djeId}/deadline/window")
    public ResponseEntity<ApiQueryResponse<?>> deadlineWindow(@PathVariable Long djeId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.deadlineWindow(djeId), List.of()));
    }

    @GetMapping("/publicacoes/{djeId}/notificacao")
    public ResponseEntity<ApiQueryResponse<?>> notificacao(@PathVariable Long djeId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.notificacao(djeId), List.of()));
    }

    @GetMapping("/publicacoes/{djeId}/notificacao/audit")
    public ResponseEntity<ApiQueryResponse<?>> notificacaoAudit(@PathVariable Long djeId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.notificacaoAudit(djeId), List.of()));
    }

    @GetMapping("/publicacoes/{djeId}/falha")
    public ResponseEntity<ApiQueryResponse<?>> falha(@PathVariable Long djeId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.falha(djeId), List.of()));
    }

    @GetMapping("/publicacoes/{djeId}/consistency")
    public ResponseEntity<ApiQueryResponse<?>> consistency(@PathVariable Long djeId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.consistency(djeId), List.of()));
    }

    @GetMapping("/publicacoes/{djeId}/consistency/audit")
    public ResponseEntity<ApiQueryResponse<?>> consistencyAudit(@PathVariable Long djeId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.consistencyAudit(djeId), List.of()));
    }

    @GetMapping("/publicacoes/{djeId}/conteudo/health")
    public ResponseEntity<ApiQueryResponse<?>> conteudoHealth(@PathVariable Long djeId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.conteudoHealth(djeId), List.of()));
    }

    @GetMapping("/publicacoes/{djeId}/signal")
    public ResponseEntity<ApiQueryResponse<?>> signal(@PathVariable Long djeId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.signal(djeId), List.of()));
    }

    @GetMapping("/publicacoes/{djeId}/owner")
    public ResponseEntity<ApiQueryResponse<?>> owner(@PathVariable Long djeId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.owner(djeId), List.of()));
    }

    @GetMapping("/publicacoes/{djeId}/window")
    public ResponseEntity<ApiQueryResponse<?>> window(@PathVariable Long djeId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.window(djeId), List.of()));
    }

    @GetMapping("/publicacoes/{djeId}/audit")
    public ResponseEntity<ApiQueryResponse<?>> audit(@PathVariable Long djeId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.audit(djeId), List.of()));
    }

    @GetMapping("/processos/{processoId}/publication")
    public ResponseEntity<ApiQueryResponse<?>> processoPublication(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.processoPublication(processoId), List.of()));
    }

    @GetMapping("/tribunais/{tribunalCodigo}/publication")
    public ResponseEntity<ApiQueryResponse<?>> tribunalPublication(@PathVariable String tribunalCodigo) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.tribunalPublication(tribunalCodigo), List.of()));
    }

    @GetMapping("/tribunais/{tribunalCodigo}/health")
    public ResponseEntity<ApiQueryResponse<?>> tribunalHealth(@PathVariable String tribunalCodigo) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.tribunalHealth(tribunalCodigo), List.of()));
    }

    @GetMapping("/tribunais/{tribunalCodigo}/health/result")
    public ResponseEntity<ApiQueryResponse<?>> tribunalHealthResult(@PathVariable String tribunalCodigo,
                                                                    @RequestParam(value = "criterio", defaultValue = "publicacao") String criterio) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.tribunalHealthResult(tribunalCodigo, criterio), List.of()));
    }

    @GetMapping("/tribunais/{tribunalCodigo}/budget")
    public ResponseEntity<ApiQueryResponse<?>> budget(@PathVariable String tribunalCodigo) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.budget(tribunalCodigo), List.of()));
    }

    @GetMapping("/tribunais/{tribunalCodigo}/dispatch/health")
    public ResponseEntity<ApiQueryResponse<?>> dispatchHealth(@PathVariable String tribunalCodigo,
                                                              @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.dispatchHealth(tribunalCodigo, limit), List.of()));
    }

    @GetMapping("/tribunais/{tribunalCodigo}/dispatch/window")
    public ResponseEntity<ApiQueryResponse<?>> dispatchWindow(@PathVariable String tribunalCodigo,
                                                              @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.dispatchWindow(tribunalCodigo, limit), List.of()));
    }

    @GetMapping("/tribunais/{tribunalCodigo}/publication-window/health")
    public ResponseEntity<ApiQueryResponse<?>> publicationWindowHealth(@PathVariable String tribunalCodigo) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.publicationWindowHealth(tribunalCodigo), List.of()));
    }

    @GetMapping("/edicoes/{edicao}/metrics")
    public ResponseEntity<ApiQueryResponse<?>> edicaoMetrics(@PathVariable String edicao) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.edicaoMetrics(edicao), List.of()));
    }

    @GetMapping("/edicoes/{edicao}/health")
    public ResponseEntity<ApiQueryResponse<?>> edicaoHealth(@PathVariable String edicao) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.edicaoHealth(edicao), List.of()));
    }
}
