package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.time.Instant;

public record RecursalCompetenciaTrace(
        String orgaoSuscitante,
        String orgaoSuscitado,
        boolean suscitadoRecebido,
        Instant suscitadoRecebidoEm,
        boolean competenciaDefinida,
        String juizoCompetente,
        String tribunalCompetente,
        Instant competenciaDefinidaEm,
        boolean autosRemetidosAoJuizoCompetente,
        Instant autosRemetidosEm) {

    public static RecursalCompetenciaTrace empty() {
        return new RecursalCompetenciaTrace(null, null, false, null, false, null, null, null, false, null);
    }

    public RecursalCompetenciaTrace iniciar(RecursalTransitionDetails details) {
        return new RecursalCompetenciaTrace(
                firstNonBlank(details == null ? null : details.orgaoSuscitante(), orgaoSuscitante),
                firstNonBlank(details == null ? null : details.orgaoSuscitado(), orgaoSuscitado),
                suscitadoRecebido,
                suscitadoRecebidoEm,
                competenciaDefinida,
                juizoCompetente,
                tribunalCompetente,
                competenciaDefinidaEm,
                autosRemetidosAoJuizoCompetente,
                autosRemetidosEm
        );
    }

    public RecursalCompetenciaTrace receberSuscitado(RecursalTransitionDetails details, Instant at) {
        return new RecursalCompetenciaTrace(
                firstNonBlank(details == null ? null : details.orgaoSuscitante(), orgaoSuscitante),
                firstNonBlank(details == null ? null : details.orgaoSuscitado(), orgaoSuscitado),
                true,
                at == null ? suscitadoRecebidoEm : at,
                competenciaDefinida,
                juizoCompetente,
                tribunalCompetente,
                competenciaDefinidaEm,
                autosRemetidosAoJuizoCompetente,
                autosRemetidosEm
        );
    }

    public RecursalCompetenciaTrace definir(RecursalTransitionDetails details, Instant at) {
        return new RecursalCompetenciaTrace(
                firstNonBlank(details == null ? null : details.orgaoSuscitante(), orgaoSuscitante),
                firstNonBlank(details == null ? null : details.orgaoSuscitado(), orgaoSuscitado),
                suscitadoRecebido,
                suscitadoRecebidoEm,
                true,
                firstNonBlank(details == null ? null : details.juizoCompetenteCodigo(), juizoCompetente),
                firstNonBlank(details == null ? null : details.tribunalCompetenteCodigo(), tribunalCompetente),
                at == null ? competenciaDefinidaEm : at,
                autosRemetidosAoJuizoCompetente,
                autosRemetidosEm
        );
    }

    public RecursalCompetenciaTrace remeterAoJuizoCompetente(RecursalTransitionDetails details, Instant at) {
        return new RecursalCompetenciaTrace(
                firstNonBlank(details == null ? null : details.orgaoSuscitante(), orgaoSuscitante),
                firstNonBlank(details == null ? null : details.orgaoSuscitado(), orgaoSuscitado),
                suscitadoRecebido,
                suscitadoRecebidoEm,
                competenciaDefinida,
                firstNonBlank(details == null ? null : details.juizoCompetenteCodigo(), juizoCompetente),
                firstNonBlank(details == null ? null : details.tribunalCompetenteCodigo(), tribunalCompetente),
                competenciaDefinidaEm,
                true,
                at == null ? autosRemetidosEm : at
        );
    }

    private static String firstNonBlank(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }
}
