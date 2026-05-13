package com.tcc.pjb.backend.service.oficial_justica;

record OficialJusticaAgendaTerritorialHint(
        String address,
        String bairro,
        String cityUf,
        String microterritorio,
        String source,
        Double confidence
) {

    OficialJusticaAgendaTerritorialHint {
        address = normalize(address);
        bairro = normalize(bairro);
        cityUf = normalize(cityUf);
        microterritorio = normalize(microterritorio);
        source = normalize(source);
        confidence = confidence == null ? null : Math.max(0D, Math.min(1D, confidence));
    }

    boolean hasMicroterritorio() {
        return !microterritorio.isBlank();
    }

    double confidenceOrZero() {
        return confidence == null ? 0D : confidence;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
