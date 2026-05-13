package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.util.Objects;

public final class RecursalTribunalDetalhadoResolver {

    public RecursalTribunalDetalhado resolveOrigem(RecursalCaseContext context) {
        Objects.requireNonNull(context, "context");
        return context.tribunalDetalhadoOrigem();
    }

    public RecursalTribunalDetalhado resolveDestino(RecursalCaseContext context, RecursalTribunal tribunalDestino) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(tribunalDestino, "tribunalDestino");
        if (tribunalDestino == context.tribunalOrigem()) {
            return context.tribunalDetalhadoOrigem();
        }
        return RecursalTribunalDetalhado.fromFamily(tribunalDestino);
    }
}
