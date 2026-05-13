package com.tcc.pjb.backend.controller.processual.guard;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.api.ApiCommandResponse;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.model.dto.processual.runtime.homologation.ProcessualHomologationGateStatusResponse;
import com.tcc.pjb.backend.model.dto.processual.runtime.guard.ProcessualOperationGuardRequest;
import com.tcc.pjb.backend.model.dto.processual.runtime.guard.ProcessualOperationGuardResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import com.tcc.pjb.backend.service.processual.runtime.guard.ProcessualOperationGuardService;

@RestController
@RequestMapping("/api/v1/processual/runtime")
public class ProcessualOperationGuardController {

    private final ProcessualOperationGuardService service;
    private final ApiResponseFactory apiResponseFactory;

    public ProcessualOperationGuardController(ProcessualOperationGuardService service,
                                             ApiResponseFactory apiResponseFactory) {
        this.service = service;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/guard")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiCommandResponse<ProcessualOperationGuardResponse>> guard(@Valid @RequestBody ProcessualOperationGuardRequest request) {
        ProcessualOperationGuardResponse response = service.guard(request);
        return ResponseEntity.ok(apiResponseFactory.commandAccepted("Envelope processual protegido por idempotência e resiliência.", response, response.warnings()));
    }

    @GetMapping("/processos/{processoId}/homologacao-bloqueios")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiQueryResponse<ProcessualHomologationGateStatusResponse>> homologacaoBloqueios(@PathVariable Long processoId,
                                                                                                            @RequestParam(defaultValue = "HOMOLOGAR") String operationCode) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(service.avaliarHomologacao(processoId, operationCode), java.util.List.of()));
    }
}
