package com.tcc.pjb.backend.core.procedural.atermacao;

import java.math.BigDecimal;
import java.util.Set;

public record PjbAtermacaoGuidedIntakeRequest(String narrative,
                                              String requestedRelief,
                                              BigDecimal estimatedValue,
                                              Set<String> documents,
                                              boolean urgent,
                                              boolean vulnerableParty,
                                              boolean representedByLawyer,
                                              boolean publicEntityDefendant) {
}
