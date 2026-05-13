package com.tcc.pjb.backend.core.certidao;

import java.time.Instant;
import java.util.Objects;

public record CertidaoDigital(
        String codigoVerificacao,
        TipoCertidao tipo,
        String processoId,
        String conteudoTexto,
        byte[] assinaturaRfc3161,
        String hashSha256,
        Instant emitidaEm,
        Instant validaAte
) {
    public CertidaoDigital {
        Objects.requireNonNull(codigoVerificacao, "codigoVerificacao");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(processoId, "processoId");
        Objects.requireNonNull(hashSha256, "hashSha256");
        emitidaEm = emitidaEm == null ? Instant.now() : emitidaEm;
    }
}
