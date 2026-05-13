package com.tcc.pjb.backend.core.kernel.advisory;

import java.math.BigDecimal;
import java.util.List;

public record NegotiationWindowReport(
        String status,
        double score,
        BigDecimal pisoSugerido,
        BigDecimal alvoSugerido,
        BigDecimal tetoSugerido,
        List<String> leveragePoints,
        List<String> risks,
        List<String> recommendations
) {
    public boolean favorable() {
        return score >= 0.70d && !"HOSTIL".equals(status);
    }
}
