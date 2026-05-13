package com.tcc.pjb.backend.service.processual.participacao;

import java.util.List;

public record SignaturePolicy(String persona,
                              boolean certificateRequired,
                              boolean mfaRecommended,
                              boolean strongChannel,
                              List<String> admissibleModes,
                              List<String> guards,
                              String modeLabel) {
}
