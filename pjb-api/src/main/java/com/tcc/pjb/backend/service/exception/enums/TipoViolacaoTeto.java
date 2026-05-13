package com.tcc.pjb.backend.service.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TipoViolacaoTeto {

    ALCADA_JUIZADO_ESPECIAL(
            "Violacao de Alcada do Juizado Especial",
            "O valor da causa excede a alcada economica do Juizado Especial.",
            "TETO-JEC"
    ),

    ALCADA_JUIZADO_ESPECIAL_FEDERAL(
            "Violacao de Alcada do Juizado Especial Federal",
            "O valor da causa excede a alcada economica do Juizado Especial Federal.",
            "TETO-JEF"
    ),

    ALCADA_FAZENDA_PUBLICA(
            "Violacao de Alcada Fazendaria",
            "O valor da causa excede a alcada economica dos Juizados Especiais da Fazenda Publica.",
            "TETO-JEFP"
    ),

    ALCADA_TRABALHISTA_SUMARISSIMO(
            "Violacao de Alcada do Rito Sumarissimo Trabalhista",
            "O valor da causa excede a alcada economica do rito sumarissimo trabalhista.",
            "TETO-TRAB-SUM"
    ),

    COMPETENCIA_ECONOMICA_INCOMPATIVEL(
            "Competencia Economica Incompativel",
            "O valor da causa nao e compativel com a competencia economica selecionada.",
            "TETO-COMP"
    ),

    VALOR_CAUSA_SUBDIMENSIONADO(
            "Valor da Causa Subdimensionado",
            "O valor informado parece abaixo do minimo economico esperado para a via eleita.",
            "TETO-SUB"
    ),

    ALERTA_PROXIMIDADE_LIMITE(
            "Proximidade do Limite Economico",
            "O valor da causa esta muito proximo do teto economico aplicavel e exige conferencia reforcada.",
            "TETO-ALERTA"
    ),

    RITO_INCOMPATIVEL(
            "Incompatibilidade Procedimental",
            "O rito escolhido nao suporta a complexidade, a materia ou o valor da causa.",
            "TETO-RITO"
    ),

    CLAUSULA_PETREA(
            "Violacao de Direito Indisponivel",
            "A tentativa fere direitos fundamentais ou clausulas petreas.",
            "TETO-CONST"
    ),

    SUMULA_VINCULANTE(
            "Violacao de Precedente Qualificado",
            "A acao contraria sumula vinculante ou precedente qualificado aplicavel.",
            "TETO-PREC"
    ),

    INDISPONIBILIDADE_ORCAMENTARIA(
            "Teto Orcamentario",
            "A operacao excede limite de aprovacao sem previsao orcamentaria adequada.",
            "TETO-ORC"
    );

    private final String tituloJuridico;
    private final String descricaoPadrao;
    private final String codigo;
}
