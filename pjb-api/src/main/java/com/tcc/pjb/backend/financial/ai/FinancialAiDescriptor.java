package com.tcc.pjb.backend.financial.ai;

import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record FinancialAiDescriptor(
        String id,
        ApiVersion version,
        String summary,
        Set<String> capabilities,
        Instant builtAt
) {
    public FinancialAiDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(summary, "summary");
        Objects.requireNonNull(builtAt, "builtAt");
        capabilities = capabilities == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(capabilities));
    }

    public boolean supports(String capability) {
        return capability != null && capabilities.contains(capability);
    }
}
