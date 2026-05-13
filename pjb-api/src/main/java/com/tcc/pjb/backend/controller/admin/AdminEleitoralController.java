package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.core.eleitoral.EleitoralApplicationService;
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
@RequestMapping("/api/v1/admin/eleitoral")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
public class AdminEleitoralController {

    private final EleitoralApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminEleitoralController(EleitoralApplicationService applicationService,
                                    ApiResponseFactory apiResponseFactory) {
        this.applicationService = Objects.requireNonNull(applicationService);
        this.apiResponseFactory = Objects.requireNonNull(apiResponseFactory);
    }

    @GetMapping("/processos/{processoId}/feito")
    public ResponseEntity<ApiQueryResponse<?>> feito(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.feito(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/status")
    public ResponseEntity<ApiQueryResponse<?>> status(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.status(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/health")
    public ResponseEntity<ApiQueryResponse<?>> health(@PathVariable Long processoId,
                                                      @RequestParam("uf") String uf,
                                                      @RequestParam(value = "data", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.health(processoId, uf, data), List.of()));
    }

    @GetMapping("/processos/{processoId}/timeline")
    public ResponseEntity<ApiQueryResponse<?>> timeline(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.timeline(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/timeline/audit")
    public ResponseEntity<ApiQueryResponse<?>> timelineAudit(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.timelineAudit(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/diplomacao")
    public ResponseEntity<ApiQueryResponse<?>> diplomacao(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.diplomacao(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/diplomacao/health")
    public ResponseEntity<ApiQueryResponse<?>> diplomacaoHealth(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.diplomacaoHealth(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/diplomacao/window")
    public ResponseEntity<ApiQueryResponse<?>> diplomacaoWindow(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.diplomacaoWindow(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/diplomacao/window/audit")
    public ResponseEntity<ApiQueryResponse<?>> diplomacaoWindowAudit(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.diplomacaoWindowAudit(processoId), List.of()));
    }

    @PostMapping("/diplomacao/sync/run")
    public ResponseEntity<ApiCommandResponse<?>> diplomacaoSyncRun() {
        return ResponseEntity.ok(apiResponseFactory.commandOk("sincronização de diplomação executada", applicationService.diplomacaoSyncRun(), List.of()));
    }

    @GetMapping("/pendentes/diplomacao")
    public ResponseEntity<ApiQueryResponse<?>> pendentes(@RequestParam(value = "limit", required = false) Integer limit) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.pendentes(limit), List.of()));
    }

    @GetMapping("/processos/{processoId}/prestacao-contas")
    public ResponseEntity<ApiQueryResponse<?>> prestacaoContas(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.prestacaoContas(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/prestacao-contas/status")
    public ResponseEntity<ApiQueryResponse<?>> prestacaoContasStatus(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.prestacaoContasStatus(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/prestacao-contas/health")
    public ResponseEntity<ApiQueryResponse<?>> prestacaoContasHealth(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.prestacaoContasHealth(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/prestacao-contas/consistency")
    public ResponseEntity<ApiQueryResponse<?>> prestacaoContasConsistency(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.prestacaoContasConsistency(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/prestacao-contas/window")
    public ResponseEntity<ApiQueryResponse<?>> prestacaoContasWindow(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.prestacaoContasWindow(processoId), List.of()));
    }

    @PostMapping("/processos/{processoId}/prestacao-contas/sync")
    public ResponseEntity<ApiCommandResponse<?>> sincronizarPrestacaoContas(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.commandOk("prestação de contas sincronizada", applicationService.sincronizarPrestacaoContas(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/ownership")
    public ResponseEntity<ApiQueryResponse<?>> ownership(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.ownership(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/consistency")
    public ResponseEntity<ApiQueryResponse<?>> consistency(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.consistency(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/cargo")
    public ResponseEntity<ApiQueryResponse<?>> cargo(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.cargo(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/partido")
    public ResponseEntity<ApiQueryResponse<?>> partido(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.partido(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/zona")
    public ResponseEntity<ApiQueryResponse<?>> zona(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.zona(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/zona/processo")
    public ResponseEntity<ApiQueryResponse<?>> zonaProcesso(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.zonaProcesso(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/zona/health")
    public ResponseEntity<ApiQueryResponse<?>> zonaHealth(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.zonaHealth(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/zona/health/result")
    public ResponseEntity<ApiQueryResponse<?>> zonaHealthResult(@PathVariable Long processoId,
                                                                @RequestParam(value = "criterio", defaultValue = "zona") String criterio) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.zonaHealthResult(processoId, criterio), List.of()));
    }

    @GetMapping("/calendario")
    public ResponseEntity<ApiQueryResponse<?>> calendario(@RequestParam("uf") String uf,
                                                          @RequestParam(value = "data", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.calendario(uf, data), List.of()));
    }

    @GetMapping("/calendario/health")
    public ResponseEntity<ApiQueryResponse<?>> calendarioHealth(@RequestParam("uf") String uf,
                                                                @RequestParam(value = "data", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.calendarioHealth(uf, data), List.of()));
    }

    @GetMapping("/calendario/window")
    public ResponseEntity<ApiQueryResponse<?>> calendarioWindow(@RequestParam("uf") String uf,
                                                                @RequestParam(value = "data", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.calendarioWindow(uf, data), List.of()));
    }

    @GetMapping("/calendario/window/audit")
    public ResponseEntity<ApiQueryResponse<?>> calendarioWindowAudit(@RequestParam("uf") String uf,
                                                                     @RequestParam(value = "data", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.calendarioWindowAudit(uf, data), List.of()));
    }

    @GetMapping("/resultado/window")
    public ResponseEntity<ApiQueryResponse<?>> resultadoWindow(@RequestParam("uf") String uf,
                                                               @RequestParam("fase") String fase,
                                                               @RequestParam(value = "dataReferencia", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataReferencia) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.resultadoWindow(uf, fase, dataReferencia), List.of()));
    }

    @GetMapping("/silencio")
    public ResponseEntity<ApiQueryResponse<?>> silencio(@RequestParam("uf") String uf,
                                                        @RequestParam(value = "data", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                                                        @RequestParam(value = "tutelaUrgente", defaultValue = "false") boolean tutelaUrgente) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.silencio(uf, data, tutelaUrgente), List.of()));
    }
}
