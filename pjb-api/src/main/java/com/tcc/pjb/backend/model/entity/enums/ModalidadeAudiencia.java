package com.tcc.pjb.backend.model.entity.enums;

import java.util.Arrays;
import java.util.Optional;

public enum ModalidadeAudiencia {

    

    CONCILIACAO(
            CategoriaAudiencia.CONSENSUAL,
            true,
            true,
            "Audiência voltada à composição amigável entre as partes"
    ),

    MEDIACAO(
            CategoriaAudiencia.CONSENSUAL,
            true,
            true,
            "Audiência com mediador imparcial, foco em diálogo estruturado"
    ),

    TENTATIVA_DE_ACORDO(
            CategoriaAudiencia.CONSENSUAL,
            true,
            true,
            "Tentativa pontual de acordo em qualquer fase do processo"
    ),

    HOMOLOGACAO_DE_ACORDO(
            CategoriaAudiencia.CONSENSUAL,
            true,
            false,
            "Audiência destinada apenas à homologação do acordo"
    ),

    

    INSTRUCAO(
            CategoriaAudiencia.INSTRUTORIA,
            false,
            false,
            "Produção de provas orais e documentais"
    ),

    INSTRUCAO_E_JULGAMENTO(
            CategoriaAudiencia.INSTRUTORIA,
            false,
            false,
            "Instrução seguida de julgamento imediato"
    ),

    OITIVA_DE_TESTEMUNHAS(
            CategoriaAudiencia.INSTRUTORIA,
            false,
            false,
            "Colheita de depoimentos testemunhais"
    ),

    OITIVA_DAS_PARTES(
            CategoriaAudiencia.INSTRUTORIA,
            false,
            false,
            "Depoimento pessoal das partes"
    ),

    INTERROGATORIO(
            CategoriaAudiencia.INSTRUTORIA,
            false,
            false,
            "Interrogatório do réu (especialmente no penal)"
    ),

    ESCLARECIMENTOS_PERICIAIS(
            CategoriaAudiencia.INSTRUTORIA,
            false,
            false,
            "Esclarecimentos técnicos prestados por perito"
    ),

    

    SANEAMENTO(
            CategoriaAudiencia.PROCESSUAL,
            false,
            false,
            "Organização do processo e fixação de pontos controvertidos"
    ),

    PRELIMINAR(
            CategoriaAudiencia.PROCESSUAL,
            false,
            false,
            "Discussão de questões preliminares"
    ),

    UNA(
            CategoriaAudiencia.PROCESSUAL,
            true,
            false,
            "Audiência una (comum no rito trabalhista)"
    ),

    JUSTIFICACAO(
            CategoriaAudiencia.PROCESSUAL,
            false,
            false,
            "Audiência para apresentação de justificativas formais"
    ),

    RETRATACAO(
            CategoriaAudiencia.PROCESSUAL,
            true,
            false,
            "Retratação da parte, comum em crimes de menor potencial"
    ),

    

    CUSTODIA(
            CategoriaAudiencia.ESPECIAL,
            false,
            false,
            "Audiência de custódia (controle da prisão)"
    ),

    SUSTENTACAO_ORAL(
            CategoriaAudiencia.ESPECIAL,
            false,
            false,
            "Sustentação oral em tribunais"
    ),

    TECNICA(
            CategoriaAudiencia.ESPECIAL,
            false,
            false,
            "Audiência técnica (engenharia, saúde, contábil etc.)"
    ),

    ADMINISTRATIVA(
            CategoriaAudiencia.ESPECIAL,
            false,
            false,
            "Audiência em processo administrativo"
    ),

    PUBLICA(
            CategoriaAudiencia.ESPECIAL,
            true,
            false,
            "Audiência pública (ex.: ambiental, urbanística)"
    ),

    

    VIRTUAL(
            CategoriaAudiencia.MODERNA,
            true,
            true,
            "Audiência realizada por videoconferência"
    ),

    HIBRIDA(
            CategoriaAudiencia.MODERNA,
            true,
            true,
            "Parte presencial, parte remota"
    ),

    ASSINCRONA(
            CategoriaAudiencia.MODERNA,
            true,
            true,
            "Manifestações registradas em janelas de tempo (sem sessão ao vivo)"
    );

    

    private final CategoriaAudiencia categoria;
    private final boolean permiteAcordo;
    private final boolean recomendavelIA;
    private final String descricao;

    ModalidadeAudiencia(
            CategoriaAudiencia categoria,
            boolean permiteAcordo,
            boolean recomendavelIA,
            String descricao
    ) {
        this.categoria = categoria;
        this.permiteAcordo = permiteAcordo;
        this.recomendavelIA = recomendavelIA;
        this.descricao = descricao;
    }

    public CategoriaAudiencia getCategoria() {
        return categoria;
    }

    public boolean permiteAcordo() {
        return permiteAcordo;
    }

    public boolean recomendavelIA() {
        return recomendavelIA;
    }

    public String getDescricao() {
        return descricao;
    }

    

    public boolean isConsensual() {
        return this.categoria == CategoriaAudiencia.CONSENSUAL;
    }

    public boolean isInstrutoria() {
        return this.categoria == CategoriaAudiencia.INSTRUTORIA;
    }

    public boolean isDigital() {
        return this.categoria == CategoriaAudiencia.MODERNA;
    }

    public static Optional<ModalidadeAudiencia> fromName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        String key = name.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(v -> v.name().equals(key))
                .findFirst();
    }
}
