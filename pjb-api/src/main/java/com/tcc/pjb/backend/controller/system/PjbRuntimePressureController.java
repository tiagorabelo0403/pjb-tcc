package com.tcc.pjb.backend.controller.system;

import com.tcc.pjb.backend.platform.runtime.PjbRuntimeDrainService;
import com.tcc.pjb.backend.platform.runtime.PjbRuntimePressureService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyAuthority('PJB_RUNTIME_ADMIN', 'ROLE_ADMIN')")
public class PjbRuntimePressureController {

    private final PjbRuntimePressureService pressureService;
    private final PjbRuntimeDrainService drainService;
    private final PjbLocalRequestGuard localRequestGuard;

    public PjbRuntimePressureController(PjbRuntimePressureService pressureService,
                                        PjbRuntimeDrainService drainService,
                                        PjbLocalRequestGuard localRequestGuard) {
        this.pressureService = pressureService;
        this.drainService = drainService;
        this.localRequestGuard = localRequestGuard;
    }

    @GetMapping("/internal/runtime/pressure")
    public ResponseEntity<Map<String, Object>> pressure(HttpServletRequest request) {
        if (!localRequestGuard.isAllowed(request)) {
            return localRequestGuard.forbidden();
        }
        PjbRuntimePressureService.Snapshot pressure = pressureService.snapshot();
        PjbRuntimeDrainService.Snapshot drain = drainService.snapshot();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", drain.draining() ? "DRAINING" : (pressure.warmingUp() ? "WARMING_UP" : (pressure.ready() ? "UP" : "PRESSURED")));
        body.put("pressure", pressure);
        body.put("drain", drain);
        return ResponseEntity.ok(body);
    }
}
