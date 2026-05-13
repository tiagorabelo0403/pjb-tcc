package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.time.Instant;

public record RecursalPublicPaymentTrace(
        boolean aguardandoExpedicao,
        String modalidade,
        String requisicaoPagamentoId,
        Instant expedidoEm,
        boolean pagamentoLiberado,
        Instant pagamentoLiberadoEm) {

    public static RecursalPublicPaymentTrace empty() {
        return new RecursalPublicPaymentTrace(false, null, null, null, false, null);
    }

    public RecursalPublicPaymentTrace ativar() {
        return new RecursalPublicPaymentTrace(true, modalidade, requisicaoPagamentoId, expedidoEm, false, pagamentoLiberadoEm);
    }

    public RecursalPublicPaymentTrace expedir(RecursalTransitionDetails details, String modalidadeDefault, Instant at) {
        return new RecursalPublicPaymentTrace(
                false,
                firstNonBlank(details == null ? null : details.modalidadePagamento(), modalidadeDefault, modalidade),
                firstNonBlank(details == null ? null : details.requisicaoPagamentoId(), requisicaoPagamentoId),
                at == null ? expedidoEm : at,
                false,
                pagamentoLiberadoEm
        );
    }

    public RecursalPublicPaymentTrace liberar(RecursalTransitionDetails details, Instant at) {
        return new RecursalPublicPaymentTrace(
                false,
                firstNonBlank(details == null ? null : details.modalidadePagamento(), modalidade),
                firstNonBlank(details == null ? null : details.requisicaoPagamentoId(), requisicaoPagamentoId),
                expedidoEm,
                true,
                at == null ? pagamentoLiberadoEm : at
        );
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
