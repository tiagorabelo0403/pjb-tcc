package com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice;

import java.time.LocalDate;

public record PjbJuizadoAdjuntoNucleoOptionRequest(String tribunalCode,
                                                   String comarca,
                                                   String courtUnit,
                                                   String subjectMatter,
                                                   LocalDate protocolDate,
                                                   boolean newCase,
                                                   boolean optionSelectedInPjeRegistration,
                                                   boolean optionOnlyMentionedInInitialPetition,
                                                   boolean distributionCompleted,
                                                   boolean hasAutonomousJuizadoUnit) {
}
