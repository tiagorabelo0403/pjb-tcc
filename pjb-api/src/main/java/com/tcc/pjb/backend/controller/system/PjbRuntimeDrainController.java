package com.tcc.pjb.backend.controller.system;

import com.tcc.pjb.backend.platform.runtime.PjbRuntimeDrainService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyAuthority('PJB_RUNTIME_ADMIN', 'ROLE_ADMIN')")
public class PjbRuntimeDrainController {

    private final PjbRuntimeDrainService drainService;
    private final PjbLocalRequestGuard localRequestGuard;

    public PjbRuntimeDrainController(PjbRuntimeDrainService drainService,
                                     PjbLocalRequestGuard localRequestGuard) {
        this.drainService = drainService;
        this.localRequestGuard = localRequestGuard;
    }

    @PostMapping("/internal/runtime/drain")
    public ResponseEntity<Map<String, Object>> beginDrain(HttpServletRequest request,
                                                          @RequestParam(name = "reason", required = false) String reason) {
        if (!localRequestGuard.isAllowed(request)) {
            return localRequestGuard.forbidden();
        }
        boolean changed = drainService.beginDrain(reason == null || reason.isBlank() ? "internal-drain" : reason.trim());
        PjbRuntimeDrainService.Snapshot snapshot = drainService.snapshot();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", snapshot.draining() ? "DRAINING" : "UP");
        body.put("changed", changed);
        body.put("drain", snapshot);
        return ResponseEntity.ok(body);
    }
}
