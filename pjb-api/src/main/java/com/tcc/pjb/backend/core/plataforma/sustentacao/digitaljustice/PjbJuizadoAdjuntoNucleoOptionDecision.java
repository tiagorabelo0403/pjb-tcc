package com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice;

import java.util.List;

public record PjbJuizadoAdjuntoNucleoOptionDecision(String status,
                                                    String targetLane,
                                                    boolean eligible,
                                                    boolean authorChoiceRespected,
                                                    boolean immutableAfterDistribution,
                                                    String stageCode,
                                                    List<String> reasons,
                                                    List<String> warnings,
                                                    List<String> legalBasis) {
}
