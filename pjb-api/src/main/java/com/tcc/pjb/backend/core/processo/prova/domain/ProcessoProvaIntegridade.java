package com.tcc.pjb.backend.core.processo.prova.domain;

public record ProcessoProvaIntegridade(
        String sha256,
        String sha384,
        boolean hashPresente,
        boolean hashCorrespondeAoConteudo,
        boolean duplicidadeNoMesmoFeito,
        boolean compartilhadaEntreFeitos,
        long processosCorrelatos
) {

    public ProcessoProvaIntegridade {
        sha256 = normalize(sha256);
        sha384 = normalize(sha384);
        processosCorrelatos = Math.max(0L, processosCorrelatos);
    }

    public boolean hasIntegrityFailure() {
        return hashPresente && !hashCorrespondeAoConteudo;
    }

    public boolean hasCrossCaseReuse() {
        return compartilhadaEntreFeitos || processosCorrelatos > 0L;
    }

    public boolean hasAnyHash() {
        return !sha256.isBlank() || !sha384.isBlank();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
