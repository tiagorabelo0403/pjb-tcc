package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.time.Instant;

public record RecursalRemessaTrace(
        boolean saidaRegistrada,
        String protocoloSaida,
        String canalRemessa,
        Instant saidaRegistradaEm,
        boolean recebimentoDestinoConfirmado,
        String protocoloRecebimentoDestino,
        Instant recebimentoDestinoEm,
        boolean remessaDevolvida,
        String motivoDevolucao,
        Instant devolvidaEm) {

    public static RecursalRemessaTrace empty() {
        return new RecursalRemessaTrace(false, null, null, null, false, null, null, false, null, null);
    }

    public RecursalRemessaTrace registrarSaida(RecursalTransitionDetails details, Instant at) {
        return new RecursalRemessaTrace(
                true,
                firstNonBlank(details == null ? null : details.protocoloSaidaAutos(), protocoloSaida),
                firstNonBlank(details == null ? null : details.canalRemessa(), canalRemessa),
                at == null ? saidaRegistradaEm : at,
                recebimentoDestinoConfirmado,
                protocoloRecebimentoDestino,
                recebimentoDestinoEm,
                remessaDevolvida,
                motivoDevolucao,
                devolvidaEm
        );
    }

    public RecursalRemessaTrace confirmarRecebimento(RecursalTransitionDetails details, Instant at) {
        return new RecursalRemessaTrace(
                saidaRegistrada,
                protocoloSaida,
                canalRemessa,
                saidaRegistradaEm,
                true,
                firstNonBlank(details == null ? null : details.protocoloRecebimentoDestino(), protocoloRecebimentoDestino),
                at == null ? recebimentoDestinoEm : at,
                false,
                null,
                null
        );
    }

    public RecursalRemessaTrace devolver(RecursalTransitionDetails details, Instant at) {
        return new RecursalRemessaTrace(
                saidaRegistrada,
                protocoloSaida,
                canalRemessa,
                saidaRegistradaEm,
                recebimentoDestinoConfirmado,
                protocoloRecebimentoDestino,
                recebimentoDestinoEm,
                true,
                firstNonBlank(details == null ? null : details.motivoDevolucaoRemessa(), motivoDevolucao),
                at == null ? devolvidaEm : at
        );
    }

    private static String firstNonBlank(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }
}
