package com.tcc.pjb.backend.core.certidao;

import java.util.Objects;

public record CertidaoRequest(
        TipoCertidao tipo,
        String processoId,
        String solicitanteCpfCnpj,
        String solicitanteNome,
        String finalidade
) {
    public CertidaoRequest {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(processoId, "processoId");
        Objects.requireNonNull(solicitanteCpfCnpj, "solicitanteCpfCnpj");
    }
}
