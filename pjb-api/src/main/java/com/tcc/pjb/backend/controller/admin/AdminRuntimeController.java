package com.tcc.pjb.backend.controller.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.model.dto.api.ApiCommandResponse;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.platform.runtime.PjbRuntimeApplicationService;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/runtime")
@PreAuthorize("hasAnyRole('ADMIN','ADMINISTRADOR')")
public class AdminRuntimeController {

    private final PjbRuntimeApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminRuntimeController(PjbRuntimeApplicationService applicationService,
                                  ApiResponseFactory apiResponseFactory) {
        this.applicationService = applicationService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @GetMapping("/sizing")
    public ResponseEntity<ApiQueryResponse<?>> sizing() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.sizing(), List.of()));
    }

    @GetMapping("/memory-budget")
    public ResponseEntity<ApiQueryResponse<?>> memoryBudget() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.memoryBudget(), List.of()));
    }

    @GetMapping("/pressure")
    public ResponseEntity<ApiQueryResponse<?>> pressure() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.pressure(), List.of()));
    }

    @GetMapping("/drain")
    public ResponseEntity<ApiQueryResponse<?>> drain() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.drain(), List.of()));
    }

    @PostMapping("/drain/begin")
    public ResponseEntity<ApiCommandResponse<?>> beginDrain(@RequestParam(value = "reason", required = false) String reason) {
        return ResponseEntity.ok(apiResponseFactory.commandOk("runtime drain iniciado", applicationService.beginDrain(reason), List.of()));
    }

    @PostMapping("/drain/accept")
    public ResponseEntity<ApiCommandResponse<?>> acceptTraffic(@RequestParam(value = "reason", required = false) String reason) {
        return ResponseEntity.ok(apiResponseFactory.commandOk("runtime voltou a aceitar trafego", applicationService.acceptTraffic(reason), List.of()));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiQueryResponse<?>> health() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.health(), List.of()));
    }

    @GetMapping("/raw/policy")
    public ResponseEntity<ApiQueryResponse<?>> rawPolicy() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.rawPolicy(), List.of()));
    }

    @GetMapping("/raw/request")
    public ResponseEntity<ApiQueryResponse<?>> rawRequest() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.rawRequest(), List.of()));
    }
}
