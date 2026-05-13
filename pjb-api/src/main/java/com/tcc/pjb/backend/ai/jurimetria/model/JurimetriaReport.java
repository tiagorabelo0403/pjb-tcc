package com.tcc.pjb.backend.ai.jurimetria.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

@Getter
@Builder(toBuilder = true)
public class JurimetriaReport implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String tese;
    private final String tribunal;
    private final String classe;
    private final String assunto;
    private final Instant geradoEm;

    @Singular
    private final List<Indicador> indicadores;

    @Singular
    private final List<String> observacoes;

    private final String explicacao;

    @Getter
    @Builder(toBuilder = true)
    public static class Indicador implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
        private final String nome;
        private final Double valor;
        private final String unidade;
    }
}
