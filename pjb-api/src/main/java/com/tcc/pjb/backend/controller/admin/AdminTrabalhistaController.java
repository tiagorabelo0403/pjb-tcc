package com.tcc.pjb.backend.controller.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.core.financeiro.trabalhista.TrabalhistaApplicationService;
import com.tcc.pjb.backend.model.dto.api.ApiCommandResponse;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/trabalhista")
@PreAuthorize("hasAnyRole('ADMIN','ADMINISTRADOR')")
public class AdminTrabalhistaController {

    private final TrabalhistaApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminTrabalhistaController(TrabalhistaApplicationService applicationService,
                                      ApiResponseFactory apiResponseFactory) {
        this.applicationService = applicationService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/processos/{processoId}/gru")
    public ResponseEntity<ApiCommandResponse<?>> gerarGru(@PathVariable Long processoId,
                                                          @RequestParam String tipo,
                                                          @RequestParam BigDecimal valor) {
        return ResponseEntity.ok(apiResponseFactory.commandOk("gru trabalhista gerada", applicationService.gerarGru(processoId, tipo, valor), List.of()));
    }

    @PostMapping("/processos/{processoId}/deposito")
    public ResponseEntity<ApiCommandResponse<?>> registrarDeposito(@PathVariable Long processoId,
                                                                   @RequestParam String instancia,
                                                                   @RequestParam BigDecimal valorDepositado,
                                                                   @RequestParam(value = "comprovanteHash", required = false) String comprovanteHash) {
        return ResponseEntity.ok(apiResponseFactory.commandOk("deposito recursal registrado", applicationService.registrarDeposito(processoId, instancia, valorDepositado, comprovanteHash), List.of()));
    }

    @PostMapping("/processos/{processoId}/acordo/homologar")
    public ResponseEntity<ApiCommandResponse<?>> homologarAcordo(@PathVariable Long processoId,
                                                                 @RequestParam String resumo) {
        return ResponseEntity.ok(apiResponseFactory.commandOk("acordo trabalhista homologado", applicationService.homologarAcordo(processoId, resumo), List.of()));
    }

    @GetMapping("/processos/{processoId}")
    public ResponseEntity<ApiQueryResponse<?>> processo(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.processo(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/status")
    public ResponseEntity<ApiQueryResponse<?>> fluxoStatus(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.fluxoStatus(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/timeline")
    public ResponseEntity<ApiQueryResponse<?>> timeline(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.timeline(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/timeline/audit")
    public ResponseEntity<ApiQueryResponse<?>> timelineAudit(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.timelineAudit(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/timeline/window")
    public ResponseEntity<ApiQueryResponse<?>> timelineWindow(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.timelineWindow(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/execucao")
    public ResponseEntity<ApiQueryResponse<?>> execucao(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.execucao(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/execucao/health")
    public ResponseEntity<ApiQueryResponse<?>> execucaoHealth(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.execucaoHealth(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/ownership")
    public ResponseEntity<ApiQueryResponse<?>> ownership(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.ownership(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/acordo/health")
    public ResponseEntity<ApiQueryResponse<?>> acordoHealth(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.acordoHealthView(processoId), List.of()));
    }

    @GetMapping("/processos/{processoId}/health")
    public ResponseEntity<ApiQueryResponse<?>> processoHealth(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.processOverview(processoId), List.of()));
    }

    @GetMapping("/grus/{gruId}")
    public ResponseEntity<ApiQueryResponse<?>> gru(@PathVariable Long gruId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.gru(gruId), List.of()));
    }

    @GetMapping("/grus/{gruId}/view")
    public ResponseEntity<ApiQueryResponse<?>> gruView(@PathVariable Long gruId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.gruView(gruId), List.of()));
    }

    @GetMapping("/grus/{gruId}/health")
    public ResponseEntity<ApiQueryResponse<?>> gruHealth(@PathVariable Long gruId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.gruHealth(gruId), List.of()));
    }

    @GetMapping("/grus/{gruId}/consistency")
    public ResponseEntity<ApiQueryResponse<?>> gruConsistency(@PathVariable Long gruId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.gruConsistency(gruId), List.of()));
    }

    @GetMapping("/grus/{gruId}/consistency/audit")
    public ResponseEntity<ApiQueryResponse<?>> gruConsistencyAudit(@PathVariable Long gruId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.gruConsistencyAudit(gruId), List.of()));
    }

    @GetMapping("/depositos/{depositoId}/health")
    public ResponseEntity<ApiQueryResponse<?>> depositoHealth(@PathVariable Long depositoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.depositoHealth(depositoId), List.of()));
    }

    @GetMapping("/depositos/{depositoId}/consistency")
    public ResponseEntity<ApiQueryResponse<?>> depositoConsistency(@PathVariable Long depositoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.depositoConsistency(depositoId), List.of()));
    }

    @GetMapping("/depositos/{depositoId}/window")
    public ResponseEntity<ApiQueryResponse<?>> depositoWindow(@PathVariable Long depositoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.depositoWindow(depositoId), List.of()));
    }
}
