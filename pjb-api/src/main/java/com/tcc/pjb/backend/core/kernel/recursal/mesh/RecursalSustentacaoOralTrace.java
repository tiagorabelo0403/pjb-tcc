package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.time.Instant;

public record RecursalSustentacaoOralTrace(
        boolean solicitada,
        String pautaId,
        String sessaoId,
        Instant sessaoDesignadaEm,
        boolean realizada,
        boolean dispensada,
        String sustentante,
        int totalAdiamentos,
        String motivoAdiamento,
        Instant ultimaAtualizacaoEm) {

    public static RecursalSustentacaoOralTrace empty() {
        return new RecursalSustentacaoOralTrace(false, null, null, null, false, false, null, 0, null, null);
    }

    public RecursalSustentacaoOralTrace solicitar(RecursalTransitionDetails details, Instant at) {
        return new RecursalSustentacaoOralTrace(
                true,
                firstNonBlank(details == null ? null : details.pautaId(), pautaId),
                firstNonBlank(details == null ? null : details.sessaoId(), sessaoId),
                at == null ? sessaoDesignadaEm : at,
                false,
                false,
                sustentante,
                totalAdiamentos,
                motivoAdiamento,
                at == null ? ultimaAtualizacaoEm : at
        );
    }

    public RecursalSustentacaoOralTrace sustentar(RecursalTransitionDetails details, Instant at) {
        return new RecursalSustentacaoOralTrace(
                true,
                firstNonBlank(details == null ? null : details.pautaId(), pautaId),
                firstNonBlank(details == null ? null : details.sessaoId(), sessaoId),
                sessaoDesignadaEm,
                true,
                false,
                firstNonBlank(details == null ? null : details.sustentante(), sustentante),
                totalAdiamentos,
                motivoAdiamento,
                at == null ? ultimaAtualizacaoEm : at
        );
    }

    public RecursalSustentacaoOralTrace dispensar(RecursalTransitionDetails details, Instant at) {
        return new RecursalSustentacaoOralTrace(
                true,
                firstNonBlank(details == null ? null : details.pautaId(), pautaId),
                firstNonBlank(details == null ? null : details.sessaoId(), sessaoId),
                sessaoDesignadaEm,
                false,
                true,
                firstNonBlank(details == null ? null : details.sustentante(), sustentante),
                totalAdiamentos,
                motivoAdiamento,
                at == null ? ultimaAtualizacaoEm : at
        );
    }

    public RecursalSustentacaoOralTrace adiar(RecursalTransitionDetails details, Instant at) {
        return new RecursalSustentacaoOralTrace(
                true,
                firstNonBlank(details == null ? null : details.pautaId(), pautaId),
                firstNonBlank(details == null ? null : details.sessaoId(), sessaoId),
                sessaoDesignadaEm,
                realizada,
                dispensada,
                firstNonBlank(details == null ? null : details.sustentante(), sustentante),
                totalAdiamentos + 1,
                firstNonBlank(details == null ? null : details.motivoAdiamentoSessao(), motivoAdiamento),
                at == null ? ultimaAtualizacaoEm : at
        );
    }

    private static String firstNonBlank(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }
}
