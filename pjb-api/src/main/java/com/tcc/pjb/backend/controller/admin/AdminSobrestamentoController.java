package com.tcc.pjb.backend.controller.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.core.judicial.sobrestamento.SobrestamentoApplicationService;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/sobrestamento")
@PreAuthorize("hasAnyRole('ADMIN','ADMINISTRADOR')")
public class AdminSobrestamentoController {

    private final SobrestamentoApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminSobrestamentoController(SobrestamentoApplicationService applicationService,
                                        ApiResponseFactory apiResponseFactory) {
        this.applicationService = applicationService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/{codigoTema}/run")
    public ResponseEntity<ApiQueryResponse<?>> run(@PathVariable String codigoTema) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.sobrestar(codigoTema), List.of()));
    }

    @PostMapping("/{codigoTema}/retomada")
    public ResponseEntity<ApiQueryResponse<?>> retomada(@PathVariable String codigoTema,
                                                        @RequestParam String resultado) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.retomar(codigoTema, resultado), List.of()));
    }

    @GetMapping("/{codigoTema}/consulta")
    public ResponseEntity<ApiQueryResponse<?>> consulta(@PathVariable String codigoTema) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.consulta(codigoTema), List.of()));
    }

    @GetMapping("/{codigoTema}/status")
    public ResponseEntity<ApiQueryResponse<?>> status(@PathVariable String codigoTema) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.status(codigoTema), List.of()));
    }

    @GetMapping("/{codigoTema}/health")
    public ResponseEntity<ApiQueryResponse<?>> health(@PathVariable String codigoTema) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.health(codigoTema), List.of()));
    }

    @GetMapping("/{codigoTema}/timeline")
    public ResponseEntity<ApiQueryResponse<?>> timeline(@PathVariable String codigoTema) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.timeline(codigoTema), List.of()));
    }

    @GetMapping("/{codigoTema}/consistency")
    public ResponseEntity<ApiQueryResponse<?>> consistency(@PathVariable String codigoTema) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.consistency(codigoTema), List.of()));
    }

    @GetMapping("/{codigoTema}/budget")
    public ResponseEntity<ApiQueryResponse<?>> budget(@PathVariable String codigoTema) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.budget(codigoTema), List.of()));
    }

    @GetMapping("/{codigoTema}/projection")
    public ResponseEntity<ApiQueryResponse<?>> projection(@PathVariable String codigoTema) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.projection(codigoTema), List.of()));
    }

    @GetMapping("/{codigoTema}/projection/health")
    public ResponseEntity<ApiQueryResponse<?>> projectionHealth(@PathVariable String codigoTema) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.projectionHealth(codigoTema), List.of()));
    }

    @GetMapping("/{codigoTema}/decision")
    public ResponseEntity<ApiQueryResponse<?>> decision(@PathVariable String codigoTema,
                                                        @RequestParam String classeTpuCodigo) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.decision(codigoTema, classeTpuCodigo), List.of()));
    }

    @GetMapping("/{codigoTema}/decision/audit")
    public ResponseEntity<ApiQueryResponse<?>> decisionAudit(@PathVariable String codigoTema,
                                                             @RequestParam String classeTpuCodigo) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.decisionAudit(codigoTema, classeTpuCodigo), List.of()));
    }

    @GetMapping("/{codigoTema}/compatibilidade")
    public ResponseEntity<ApiQueryResponse<?>> compatibilidade(@PathVariable String codigoTema,
                                                               @RequestParam Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.compatibilidade(processoId, codigoTema), List.of()));
    }

    @GetMapping("/{codigoTema}/window")
    public ResponseEntity<ApiQueryResponse<?>> window(@PathVariable String codigoTema) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.window(codigoTema), List.of()));
    }

    @GetMapping("/{codigoTema}/window/health")
    public ResponseEntity<ApiQueryResponse<?>> windowHealth(@PathVariable String codigoTema,
                                                            @RequestParam(defaultValue = "APLICACAO_TESE") String resultado) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.windowHealth(codigoTema, resultado), List.of()));
    }

    @GetMapping("/{codigoTema}/window/view")
    public ResponseEntity<ApiQueryResponse<?>> windowView(@PathVariable String codigoTema) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.windowView(codigoTema), List.of()));
    }

    @GetMapping("/{codigoTema}/window/audit")
    public ResponseEntity<ApiQueryResponse<?>> windowAudit(@PathVariable String codigoTema) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.windowAudit(codigoTema), List.of()));
    }

    @GetMapping("/{codigoTema}/signal")
    public ResponseEntity<ApiQueryResponse<?>> signal(@PathVariable String codigoTema) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.signal(codigoTema), List.of()));
    }

    @GetMapping("/{codigoTema}/owner")
    public ResponseEntity<ApiQueryResponse<?>> owner(@PathVariable String codigoTema) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.owner(codigoTema), List.of()));
    }

    @GetMapping("/{codigoTema}/envelope")
    public ResponseEntity<ApiQueryResponse<?>> envelope(@PathVariable String codigoTema) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.envelope(codigoTema), List.of()));
    }

    @GetMapping("/{codigoTema}/audit")
    public ResponseEntity<ApiQueryResponse<?>> audit(@PathVariable String codigoTema,
                                                     @RequestParam(defaultValue = "STATUS") String evento) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.audit(codigoTema, evento), List.of()));
    }

    @GetMapping("/{codigoTema}/audit/entry")
    public ResponseEntity<ApiQueryResponse<?>> auditEntry(@PathVariable String codigoTema) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.auditEntry(codigoTema), List.of()));
    }

    @GetMapping("/{codigoTema}/audit/health")
    public ResponseEntity<ApiQueryResponse<?>> auditHealth(@PathVariable String codigoTema) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.auditHealth(codigoTema), List.of()));
    }

    @GetMapping("/{codigoTema}/timeline/audit")
    public ResponseEntity<ApiQueryResponse<?>> timelineAudit(@PathVariable String codigoTema) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.timelineAudit(codigoTema), List.of()));
    }

    @GetMapping("/{codigoTema}/timeline/health")
    public ResponseEntity<ApiQueryResponse<?>> timelineHealth(@PathVariable String codigoTema) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.timelineHealth(codigoTema), List.of()));
    }

    @GetMapping("/{codigoTema}/status/health")
    public ResponseEntity<ApiQueryResponse<?>> statusHealth(@PathVariable String codigoTema) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.statusHealth(codigoTema), List.of()));
    }

    @GetMapping("/{codigoTema}/health/view")
    public ResponseEntity<ApiQueryResponse<?>> healthView(@PathVariable String codigoTema) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.healthView(codigoTema), List.of()));
    }

    @GetMapping("/{codigoTema}/retomada/health")
    public ResponseEntity<ApiQueryResponse<?>> retomadaHealth(@PathVariable String codigoTema,
                                                              @RequestParam(defaultValue = "APLICACAO_TESE") String resultado) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.retomadaHealth(codigoTema, resultado), List.of()));
    }
}
