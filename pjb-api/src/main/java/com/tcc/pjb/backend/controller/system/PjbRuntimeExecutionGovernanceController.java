package com.tcc.pjb.backend.controller.system;

import com.tcc.pjb.backend.platform.runtime.PjbRuntimeGuardrailsService;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalPressureTracker;
import com.tcc.pjb.backend.platform.runtime.PjbRuntimePressureService;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyAuthority('PJB_RUNTIME_ADMIN', 'ROLE_ADMIN')")
public class PjbRuntimeExecutionGovernanceController {

    private final PjbExecutionOrchestrator executionOrchestrator;
    private final PjbRuntimePressureService runtimePressureService;
    private final PjbRuntimeGuardrailsService runtimeGuardrailsService;
    private final PjbTransactionalPressureTracker transactionalPressureTracker;
    private final PjbLocalRequestGuard localRequestGuard;

    public PjbRuntimeExecutionGovernanceController(PjbExecutionOrchestrator executionOrchestrator,
                                                   PjbRuntimePressureService runtimePressureService,
                                                   PjbRuntimeGuardrailsService runtimeGuardrailsService,
                                                   PjbTransactionalPressureTracker transactionalPressureTracker,
                                                   PjbLocalRequestGuard localRequestGuard) {
        this.executionOrchestrator = executionOrchestrator;
        this.runtimePressureService = runtimePressureService;
        this.runtimeGuardrailsService = runtimeGuardrailsService;
        this.transactionalPressureTracker = transactionalPressureTracker;
        this.localRequestGuard = localRequestGuard;
    }

    @GetMapping("/internal/runtime/execution-governance")
    public ResponseEntity<Map<String, Object>> snapshot(HttpServletRequest request) {
        if (!localRequestGuard.isAllowed(request)) {
            return localRequestGuard.forbidden();
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("execution", executionOrchestrator.snapshot());
        body.put("pressure", runtimePressureService.snapshot());
        body.put("transactions", transactionalPressureTracker.snapshot());
        body.put("guardrails", runtimeGuardrailsService.snapshot());
        return ResponseEntity.ok(body);
    }
}
