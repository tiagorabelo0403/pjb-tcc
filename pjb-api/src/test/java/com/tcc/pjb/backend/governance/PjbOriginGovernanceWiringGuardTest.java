package com.tcc.pjb.backend.governance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbOriginGovernanceWiringGuardTest {

    @Test
    void security_config_keeps_origin_governance_after_body_hash_and_before_idempotency() throws Exception {
        Path source = Path.of("src/main/java/com/tcc/pjb/backend/configs/SecurityConfig.java");
        String java = Files.readString(source);
        assertTrue(java.contains("http.addFilterAfter(originGovernanceFilter, RequestBodyHashFilter.class);"),
                "ApiRequestOriginGovernanceFilter deve permanecer encadeado apos o body hash para validar a origem com payload canonico.");
        assertTrue(java.contains("http.addFilterAfter(pjbIdempotencyFilter, ApiRequestOriginGovernanceFilter.class);"),
                "PjbIdempotencyFilter deve permanecer apos a governanca de origem para nao aceitar replay antes da atestacao da borda.");
    }
}
