package com.tcc.pjb.backend.core.plataforma.substituicao.parity;

import java.util.List;
import java.util.Map;

public record PjbLegacyParityReport(String status,
                                    Map<PjbLegacyParityCapability, Boolean> coverage,
                                    List<PjbLegacyParityFinding> missingFindings) {
}
