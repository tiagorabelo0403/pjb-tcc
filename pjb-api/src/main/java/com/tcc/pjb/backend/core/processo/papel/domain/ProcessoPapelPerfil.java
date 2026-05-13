package com.tcc.pjb.backend.core.processo.papel.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoPapelPerfil(
        String codigo,
        String nomeExibicao,
        String painel,
        String processProfile,
        String trustFloor,
        String accentColor,
        List<String> visualizar,
        List<String> receber,
        List<String> preparar,
        List<String> aprovar,
        List<String> assinar,
        List<String> peticionar,
        List<String> certificar,
        List<String> redistribuir,
        List<String> recorrer,
        List<String> embargar,
        List<String> sugerir,
        List<String> separadores,
        List<String> guardas,
        List<String> fundamentos
) {
    public ProcessoPapelPerfil(String codigo, String nomeExibicao, String painel, String processProfile, String trustFloor, String accentColor,
                               List<String> visualizar, List<String> receber, List<String> preparar, List<String> aprovar,
                               List<String> assinar, List<String> peticionar, List<String> certificar, List<String> redistribuir,
                               List<String> recorrer, List<String> embargar, List<String> sugerir, List<String> guardas, List<String> fundamentos) {
        this(codigo, nomeExibicao, painel, processProfile, trustFloor, accentColor, visualizar, receber, preparar, aprovar, assinar, peticionar, certificar, redistribuir, recorrer, embargar, sugerir, List.of(), guardas, fundamentos);
    }

    public ProcessoPapelPerfil {
        Objects.requireNonNull(codigo);
        Objects.requireNonNull(nomeExibicao);
        painel = painel == null ? "NAO_INFORMADO" : painel;
        processProfile = processProfile == null ? "NAO_INFORMADO" : processProfile;
        trustFloor = trustFloor == null ? "NAO_INFORMADO" : trustFloor;
        accentColor = accentColor == null ? "slate" : accentColor;
        visualizar = visualizar == null ? List.of() : List.copyOf(visualizar);
        receber = receber == null ? List.of() : List.copyOf(receber);
        preparar = preparar == null ? List.of() : List.copyOf(preparar);
        aprovar = aprovar == null ? List.of() : List.copyOf(aprovar);
        assinar = assinar == null ? List.of() : List.copyOf(assinar);
        peticionar = peticionar == null ? List.of() : List.copyOf(peticionar);
        certificar = certificar == null ? List.of() : List.copyOf(certificar);
        redistribuir = redistribuir == null ? List.of() : List.copyOf(redistribuir);
        recorrer = recorrer == null ? List.of() : List.copyOf(recorrer);
        embargar = embargar == null ? List.of() : List.copyOf(embargar);
        sugerir = sugerir == null ? List.of() : List.copyOf(sugerir);
        separadores = separadores == null ? List.of() : List.copyOf(separadores);
        guardas = guardas == null ? List.of() : List.copyOf(guardas);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
