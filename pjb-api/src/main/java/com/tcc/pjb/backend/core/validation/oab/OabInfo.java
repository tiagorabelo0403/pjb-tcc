package com.tcc.pjb.backend.core.validation.oab;


public record OabInfo(
        String canonicalDisplay,
        String normalized,
        String uf,
        String numero,
        String sufixo
) {
}
