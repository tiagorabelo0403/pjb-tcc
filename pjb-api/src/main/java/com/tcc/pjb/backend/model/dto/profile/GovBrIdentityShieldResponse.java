package com.tcc.pjb.backend.model.dto.profile;

import java.time.LocalDateTime;
import java.util.List;

public record GovBrIdentityShieldResponse(
        String actor,
        boolean govBrHabilitado,
        boolean vinculado,
        String nivelConfianca,
        boolean stepUpRequerido,
        String endpointSugerido,
        LocalDateTime ultimoVinculoGovBr,
        LocalDateTime ultimaAutenticacaoForte,
        List<String> controlesAtivos,
        List<String> pendencias
) {
}
