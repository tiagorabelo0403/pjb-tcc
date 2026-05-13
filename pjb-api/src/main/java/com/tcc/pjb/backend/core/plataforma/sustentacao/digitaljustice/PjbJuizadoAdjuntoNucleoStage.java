package com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice;

import java.time.LocalDate;
import java.util.List;

public record PjbJuizadoAdjuntoNucleoStage(String stageCode,
                                           LocalDate newCaseStartDate,
                                           LocalDate redistributionStartDate,
                                           LocalDate redistributionEndDate,
                                           List<String> coveredCourtUnits) {

    public boolean covers(String courtUnit) {
        String normalizedCourtUnit = PjbJuizadoAdjuntoText.normalize(courtUnit);
        return coveredCourtUnits.stream()
                .map(PjbJuizadoAdjuntoText::normalize)
                .anyMatch(unit -> unit.equals(normalizedCourtUnit) || unit.contains(normalizedCourtUnit) || normalizedCourtUnit.contains(unit));
    }
}
