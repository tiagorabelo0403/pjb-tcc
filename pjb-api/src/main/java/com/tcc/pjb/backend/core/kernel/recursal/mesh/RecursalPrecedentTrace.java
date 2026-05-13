package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.time.Instant;

public record RecursalPrecedentTrace(
        boolean sobrestamentoAtivo,
        String precedenteCodigo,
        String precedenteTribunal,
        String precedenteTema,
        boolean aplicado,
        boolean distinguido,
        String fundamentoDistincao,
        Instant retomadoEm,
        Instant resolvidoEm) {

    public static RecursalPrecedentTrace empty() {
        return new RecursalPrecedentTrace(false, null, null, null, false, false, null, null, null);
    }

    public RecursalPrecedentTrace sobrestar(RecursalTransitionDetails details) {
        return new RecursalPrecedentTrace(
                true,
                firstNonBlank(details == null ? null : details.precedenteCodigo(), precedenteCodigo),
                firstNonBlank(details == null ? null : details.precedenteTribunal(), precedenteTribunal),
                firstNonBlank(details == null ? null : details.precedenteTema(), precedenteTema),
                false,
                false,
                null,
                null,
                null
        );
    }

    public RecursalPrecedentTrace retomar(RecursalTransitionDetails details, Instant at) {
        return new RecursalPrecedentTrace(
                false,
                firstNonBlank(details == null ? null : details.precedenteCodigo(), precedenteCodigo),
                firstNonBlank(details == null ? null : details.precedenteTribunal(), precedenteTribunal),
                firstNonBlank(details == null ? null : details.precedenteTema(), precedenteTema),
                aplicado,
                distinguido,
                fundamentoDistincao,
                at == null ? retomadoEm : at,
                resolvidoEm
        );
    }

    public RecursalPrecedentTrace aplicar(RecursalTransitionDetails details, Instant at) {
        return new RecursalPrecedentTrace(
                false,
                firstNonBlank(details == null ? null : details.precedenteCodigo(), precedenteCodigo),
                firstNonBlank(details == null ? null : details.precedenteTribunal(), precedenteTribunal),
                firstNonBlank(details == null ? null : details.precedenteTema(), precedenteTema),
                true,
                false,
                null,
                retomadoEm,
                at == null ? resolvidoEm : at
        );
    }

    public RecursalPrecedentTrace distinguir(RecursalTransitionDetails details, Instant at) {
        return new RecursalPrecedentTrace(
                false,
                firstNonBlank(details == null ? null : details.precedenteCodigo(), precedenteCodigo),
                firstNonBlank(details == null ? null : details.precedenteTribunal(), precedenteTribunal),
                firstNonBlank(details == null ? null : details.precedenteTema(), precedenteTema),
                false,
                true,
                firstNonBlank(details == null ? null : details.fundamentoDistincao(), fundamentoDistincao),
                retomadoEm,
                at == null ? resolvidoEm : at
        );
    }

    private static String firstNonBlank(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }
}
