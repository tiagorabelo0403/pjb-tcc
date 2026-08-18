package com.tcc.pjb.backend.modules.custas.web;

import com.tcc.pjb.backend.modules.custas.application.CustasApplicationService;
import com.tcc.pjb.backend.modules.custas.domain.TipoCusta;
import com.tcc.pjb.backend.model.dto.api.ApiCommandResponse;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.math.BigDecimal;
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
@RequestMapping("/api/v1/admin/custas")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
public class AdminCustasController {

    private final CustasApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminCustasController(CustasApplicationService applicationService,
                                 ApiResponseFactory apiResponseFactory) {
        this.applicationService = Objects.requireNonNull(applicationService);
        this.apiResponseFactory = Objects.requireNonNull(apiResponseFactory);
    }

    @PostMapping("/gerar")
    public ResponseEntity<ApiCommandResponse<?>> gerar(@RequestParam("processoId") Long processoId,
                                                       @RequestParam("tipo") TipoCusta tipo,
                                                       @RequestParam("valor") BigDecimal valor) {
        return ResponseEntity.ok(apiResponseFactory.commandOk("custa gerada", applicationService.gerar(processoId, tipo, valor), List.of()));
    }

    @GetMapping("/{custaId}")
    public ResponseEntity<ApiQueryResponse<?>> consulta(@PathVariable Long custaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.consulta(custaId), List.of()));
    }

    @GetMapping("/{custaId}/health")
    public ResponseEntity<ApiQueryResponse<?>> health(@PathVariable Long custaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.health(custaId), List.of()));
    }

    @GetMapping("/{custaId}/pagamento")
    public ResponseEntity<ApiQueryResponse<?>> pagamento(@PathVariable Long custaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.pagamento(custaId), List.of()));
    }

    @GetMapping("/{custaId}/linha-digitavel")
    public ResponseEntity<ApiQueryResponse<?>> linhaDigitavel(@PathVariable Long custaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.linhaDigitavel(custaId), List.of()));
    }

    @GetMapping("/{custaId}/pix/health")
    public ResponseEntity<ApiQueryResponse<?>> pixHealth(@PathVariable Long custaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.pixHealth(custaId), List.of()));
    }

    @GetMapping("/{custaId}/timeline")
    public ResponseEntity<ApiQueryResponse<?>> timeline(@PathVariable Long custaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.timeline(custaId), List.of()));
    }

    @GetMapping("/{custaId}/status")
    public ResponseEntity<ApiQueryResponse<?>> status(@PathVariable Long custaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.status(custaId), List.of()));
    }

    @GetMapping("/{custaId}/vencimento")
    public ResponseEntity<ApiQueryResponse<?>> vencimento(@PathVariable Long custaId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.vencimento(custaId), List.of()));
    }
}
