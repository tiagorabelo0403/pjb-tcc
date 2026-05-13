package com.tcc.pjb.backend.core.eleitoral.domain;

public record ExtincaoEleitoralResult(Long processoId,
                                      boolean extinto,
                                      String motivoExtincao) {
    public static ExtincaoEleitoralResult semFeito(Long id) {
        return new ExtincaoEleitoralResult(id, false, "feito eleitoral não registrado");
    }
}
