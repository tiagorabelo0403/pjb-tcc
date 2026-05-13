package com.tcc.pjb.backend.controller.processual.malha;

import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.model.dto.processual.malha.ProcessoMalhaExecucaoAssistidaResponse;
import com.tcc.pjb.backend.model.dto.processual.malha.ProcessoMalhaFechamentoResponse;
import com.tcc.pjb.backend.model.dto.processual.malha.ProcessoMalhaOperacaoInstitucionalResponse;
import com.tcc.pjb.backend.model.dto.processual.malha.ProcessoMalhaPainelPapelResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import com.tcc.pjb.backend.service.processual.malha.ProcessoMalhaAssistidaFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processual/malha")
public class ProcessoMalhaAssistidaController {

    private final ProcessoMalhaAssistidaFacadeService processoMalhaAssistidaFacadeService;
    private final ApiResponseFactory apiResponseFactory;

    public ProcessoMalhaAssistidaController(ProcessoMalhaAssistidaFacadeService processoMalhaAssistidaFacadeService,
                                            ApiResponseFactory apiResponseFactory) {
        this.processoMalhaAssistidaFacadeService = processoMalhaAssistidaFacadeService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @GetMapping("/{processoId}/execucao-assistida")
    @PreAuthorize("@processoMalhaEndpointAuthorization.canAccess(authentication, #papel)")
    public ResponseEntity<ApiQueryResponse<ProcessoMalhaExecucaoAssistidaResponse>> execucaoAssistida(@PathVariable Long processoId,
                                                                                                       @RequestParam(required = false) String papel,
                                                                                                       @RequestParam(required = false) String ramo,
                                                                                                       @RequestHeader(value = "X-PJB-StepUp-Token", required = false) String stepUpToken,
                                                                                                       @RequestHeader(value = "X-PJB-Sigilo-Request-Id", required = false) String sigiloRequestId,
                                                                                                       @RequestHeader(value = "X-PJB-Sigilo-Password", required = false) String sigiloPassword,
                                                                                                       @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor) {
        ProcessoMalhaExecucaoAssistidaResponse response = processoMalhaAssistidaFacadeService.execucaoAssistida(processoId, papel, ramo, stepUpToken, sigiloRequestId, sigiloPassword, forwardedFor);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.fundamentos()));
    }

    @GetMapping("/{processoId}/fechamento")
    @PreAuthorize("@processoMalhaEndpointAuthorization.canAccess(authentication, #papel)")
    public ResponseEntity<ApiQueryResponse<ProcessoMalhaFechamentoResponse>> fechamento(@PathVariable Long processoId,
                                                                                         @RequestParam(required = false) String papel,
                                                                                         @RequestParam(required = false) String ramo,
                                                                                         @RequestHeader(value = "X-PJB-StepUp-Token", required = false) String stepUpToken,
                                                                                         @RequestHeader(value = "X-PJB-Sigilo-Request-Id", required = false) String sigiloRequestId,
                                                                                         @RequestHeader(value = "X-PJB-Sigilo-Password", required = false) String sigiloPassword,
                                                                                         @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor) {
        ProcessoMalhaFechamentoResponse response = processoMalhaAssistidaFacadeService.fechamento(processoId, papel, ramo, stepUpToken, sigiloRequestId, sigiloPassword, forwardedFor);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.fundamentos()));
    }

    @GetMapping("/{processoId}/painel")
    @PreAuthorize("@processoMalhaEndpointAuthorization.canAccess(authentication, #papel)")
    public ResponseEntity<ApiQueryResponse<ProcessoMalhaPainelPapelResponse>> painel(@PathVariable Long processoId,
                                                                                      @RequestParam(required = false) String papel,
                                                                                      @RequestParam(required = false) String ramo,
                                                                                      @RequestHeader(value = "X-PJB-StepUp-Token", required = false) String stepUpToken,
                                                                                      @RequestHeader(value = "X-PJB-Sigilo-Request-Id", required = false) String sigiloRequestId,
                                                                                      @RequestHeader(value = "X-PJB-Sigilo-Password", required = false) String sigiloPassword,
                                                                                      @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor) {
        ProcessoMalhaPainelPapelResponse response = processoMalhaAssistidaFacadeService.painel(processoId, papel, ramo, stepUpToken, sigiloRequestId, sigiloPassword, forwardedFor);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.fundamentos()));
    }

    @PostMapping("/{processoId}/materializar-operacao")
    @PreAuthorize("@processoMalhaEndpointAuthorization.canAccess(authentication, #papel)")
    public ResponseEntity<ApiQueryResponse<ProcessoMalhaOperacaoInstitucionalResponse>> materializarOperacao(@PathVariable Long processoId,
                                                                                                              @RequestParam(required = false) String papel,
                                                                                                              @RequestParam(required = false) String ramo,
                                                                                                              @RequestHeader(value = "X-PJB-StepUp-Token", required = false) String stepUpToken,
                                                                                                              @RequestHeader(value = "X-PJB-Sigilo-Request-Id", required = false) String sigiloRequestId,
                                                                                                              @RequestHeader(value = "X-PJB-Sigilo-Password", required = false) String sigiloPassword,
                                                                                                              @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor) {
        ProcessoMalhaOperacaoInstitucionalResponse response = processoMalhaAssistidaFacadeService.materializarOperacao(processoId, papel, ramo, stepUpToken, sigiloRequestId, sigiloPassword, forwardedFor);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.fundamentos()));
    }
}
