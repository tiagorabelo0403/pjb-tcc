package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.core.criminal.custodia.CustodiaApplicationService;
import com.tcc.pjb.backend.model.dto.api.ApiCommandResponse;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.Instant;
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
@RequestMapping("/api/v1/admin/custodia")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
public class AdminCustodiaController {

    private final CustodiaApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminCustodiaController(CustodiaApplicationService applicationService,
                                   ApiResponseFactory apiResponseFactory) {
        this.applicationService = Objects.requireNonNull(applicationService);
        this.apiResponseFactory = Objects.requireNonNull(apiResponseFactory);
    }

    @PostMapping("/registrar-prisao")
    public ResponseEntity<ApiCommandResponse<?>> registrarPrisao(@RequestParam("processoId") Long processoId,
                                                                 @RequestParam("presoNome") String presoNome,
                                                                 @RequestParam(value = "presoCpf", required = false) String presoCpf,
                                                                 @RequestParam(value = "dataPrisao", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataPrisao) {
        return ResponseEntity.ok(apiResponseFactory.commandOk("prisão registrada", applicationService.registrarPrisao(processoId, presoNome, presoCpf, dataPrisao), List.of()));
    }

    @PostMapping("/{custodiaId}/concluir")
    public ResponseEntity<ApiCommandResponse<?>> concluir(@PathVariable Long custodiaId,
                                                          @RequestParam("resultado") String resultado,
                                                          @RequestParam(value = "medidasCautelares", required = false) List<String> medidasCautelares) {
        return ResponseEntity.ok(apiResponseFactory.commandOk("audiência de custódia concluída", applicationService.concluir(custodiaId, resultado, medidasCautelares), List.of()));
    }

    @GetMapping("/{custodiaId}")
    public ResponseEntity<ApiQueryResponse<?>> consulta(@PathVariable Long custodiaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.consulta(custodiaId), List.of()));
    }

    @GetMapping("/{custodiaId}/prazo")
    public ResponseEntity<ApiQueryResponse<?>> prazo(@PathVariable Long custodiaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.prazo(custodiaId), List.of()));
    }

    @GetMapping("/{custodiaId}/timeline")
    public ResponseEntity<ApiQueryResponse<?>> timeline(@PathVariable Long custodiaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.timeline(custodiaId), List.of()));
    }

    @GetMapping("/{custodiaId}/resultado")
    public ResponseEntity<ApiQueryResponse<?>> resultado(@PathVariable Long custodiaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.resultado(custodiaId), List.of()));
    }

    @GetMapping("/{custodiaId}/auditoria")
    public ResponseEntity<ApiQueryResponse<?>> auditoria(@PathVariable Long custodiaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.auditoria(custodiaId), List.of()));
    }

    @GetMapping("/{custodiaId}/andamento")
    public ResponseEntity<ApiQueryResponse<?>> andamento(@PathVariable Long custodiaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.andamento(custodiaId), List.of()));
    }

    @GetMapping("/processos/{processoId}/medidas")
    public ResponseEntity<ApiQueryResponse<?>> medidas(@PathVariable Long processoId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.medidas(processoId), List.of()));
    }
}
