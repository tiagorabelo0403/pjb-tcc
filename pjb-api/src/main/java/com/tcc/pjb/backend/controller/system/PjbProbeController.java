package com.tcc.pjb.backend.controller.system;

import com.tcc.pjb.backend.core.observability.systemhealth.PjbFunctionalAvailabilityService;
import com.tcc.pjb.backend.core.observability.systemhealth.PjbFunctionalDomain;
import com.tcc.pjb.backend.platform.runtime.PjbRuntimeReadinessService;
import com.tcc.pjb.backend.platform.runtime.domain.PjbRuntimeReadinessView;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("permitAll()")
public class PjbProbeController {

    private final PjbFunctionalAvailabilityService availabilityService;
    private final PjbRuntimeReadinessService runtimeReadinessService;

    public PjbProbeController(PjbFunctionalAvailabilityService availabilityService,
                              PjbRuntimeReadinessService runtimeReadinessService) {
        this.availabilityService = availabilityService;
        this.runtimeReadinessService = runtimeReadinessService;
    }

    @GetMapping("/startupz")
    public ResponseEntity<Map<String, Object>> startupz() {
        boolean functionalUp = true;
        for (PjbFunctionalDomain domain : PjbFunctionalDomain.values()) {
            if (!availabilityService.readiness(domain).available()) {
                functionalUp = false;
                break;
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", functionalUp ? "UP" : "STARTING");
        body.put("timestamp", Instant.now().toString());
        body.put("functionalAvailability", availabilityService.exportReadiness());
        return ResponseEntity.status(functionalUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @GetMapping("/livez")
    public Map<String, Object> livez() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "pjb-backend-core");
        body.put("timestamp", Instant.now().toString());
        return body;
    }

    @GetMapping("/readyz")
    public ResponseEntity<Map<String, Object>> readyz() {
        PjbRuntimeReadinessView readiness = runtimeReadinessService.snapshot();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", readiness.status());
        body.put("timestamp", readiness.generatedAt().toString());
        body.put("functionalAvailability", availabilityService.exportReadiness());
        body.put("runtimePressure", readiness.runtimePressure());
        body.put("runtimeDrain", readiness.runtimeDrain());
        body.put("livePressure", readiness.livePressure());
        body.put("kafkaPressure", readiness.kafkaPressure());
        body.put("transactions", readiness.transactions());
        return ResponseEntity.status(readiness.up() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
