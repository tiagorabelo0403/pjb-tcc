package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.core.peticionamento.saga.PeticionamentoSagaApplicationService;
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
@RequestMapping("/api/v1/admin/peticionamento/saga")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
public class AdminPeticionamentoSagaController {

    private final PeticionamentoSagaApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminPeticionamentoSagaController(PeticionamentoSagaApplicationService applicationService,
                                             ApiResponseFactory apiResponseFactory) {
        this.applicationService = Objects.requireNonNull(applicationService);
        this.apiResponseFactory = Objects.requireNonNull(apiResponseFactory);
    }

    @PostMapping("/{rascunhoId}/validar")
    public ResponseEntity<ApiQueryResponse<?>> validar(@PathVariable Long rascunhoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.validar(rascunhoId), List.of()));
    }

    @PostMapping("/{rascunhoId}/protocolo")
    public ResponseEntity<ApiCommandResponse<?>> protocolo(@PathVariable Long rascunhoId) {
        return ResponseEntity.ok(apiResponseFactory.commandOk("protocolo da saga gerado", applicationService.gerarProtocolo(rascunhoId), List.of()));
    }

    @PostMapping("/{rascunhoId}/registrar")
    public ResponseEntity<ApiCommandResponse<?>> registrar(@PathVariable Long rascunhoId) {
        return ResponseEntity.ok(apiResponseFactory.commandOk("registro processual executado", applicationService.registrarNoProcesso(rascunhoId), List.of()));
    }

    @PostMapping("/{rascunhoId}/triagem")
    public ResponseEntity<ApiCommandResponse<?>> triagem(@PathVariable Long rascunhoId) {
        return ResponseEntity.ok(apiResponseFactory.commandOk("triagem disparada", applicationService.dispararTriagem(rascunhoId), List.of()));
    }

    @PostMapping("/{rascunhoId}/notificar")
    public ResponseEntity<ApiCommandResponse<?>> notificar(@PathVariable Long rascunhoId) {
        return ResponseEntity.ok(apiResponseFactory.commandOk("notificação de partes executada", applicationService.notificarPartes(rascunhoId), List.of()));
    }

    @PostMapping("/{rascunhoId}/compensar")
    public ResponseEntity<ApiCommandResponse<?>> compensar(@PathVariable Long rascunhoId) {
        return ResponseEntity.ok(apiResponseFactory.commandOk("compensação da saga executada", applicationService.compensar(rascunhoId), List.of()));
    }

    @GetMapping("/{rascunhoId}/health")
    public ResponseEntity<ApiQueryResponse<?>> health(@PathVariable Long rascunhoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.health(rascunhoId), List.of()));
    }

    @GetMapping("/{rascunhoId}/timeline")
    public ResponseEntity<ApiQueryResponse<?>> timeline(@PathVariable Long rascunhoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.timeline(rascunhoId), List.of()));
    }

    @GetMapping("/{rascunhoId}/execution/health")
    public ResponseEntity<ApiQueryResponse<?>> executionHealth(@PathVariable Long rascunhoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.executionHealth(rascunhoId), List.of()));
    }

    @GetMapping("/{rascunhoId}/audit")
    public ResponseEntity<ApiQueryResponse<?>> audit(@PathVariable Long rascunhoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.audit(rascunhoId), List.of()));
    }

    @GetMapping("/{rascunhoId}/steps/{etapa}")
    public ResponseEntity<ApiQueryResponse<?>> step(@PathVariable Long rascunhoId,
                                                    @PathVariable String etapa) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.step(rascunhoId, etapa), List.of()));
    }

    @GetMapping("/{rascunhoId}/compensation")
    public ResponseEntity<ApiQueryResponse<?>> compensation(@PathVariable Long rascunhoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.compensation(rascunhoId), List.of()));
    }
}
