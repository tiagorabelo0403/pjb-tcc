package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalFormalChecklistBlueprint {

    private RecursalFormalChecklistBlueprint() {
    }

    public static List<String> secoesPorRota(String rota) {
        return switch (rota) {
            case "APELACAO" -> List.of(
                    RecursalFormalSectionLabels.PETICAO_INTERPOSICAO,
                    RecursalFormalSectionLabels.ENDERECAMENTO_A_QUO,
                    RecursalFormalSectionLabels.ENDERECAMENTO_AD_QUEM,
                    RecursalFormalSectionLabels.IDENTIFICACAO_PARTES_RECURSAIS,
                    RecursalFormalSectionLabels.SINTESE_PROCESSO,
                    RecursalFormalSectionLabels.RAZOES_RECURSAIS,
                    RecursalFormalSectionLabels.CABIMENTO,
                    RecursalFormalSectionLabels.LEGITIMIDADE,
                    RecursalFormalSectionLabels.INTERESSE_RECURSAL,
                    RecursalFormalSectionLabels.TEMPESTIVIDADE,
                    RecursalFormalSectionLabels.PREPARO,
                    RecursalFormalSectionLabels.EFEITOS_RECURSAIS
            );
            case "RECURSO_INOMINADO" -> List.of(
                    RecursalFormalSectionLabels.PETICAO_INTERPOSICAO,
                    RecursalFormalSectionLabels.ENDERECAMENTO_A_QUO,
                    RecursalFormalSectionLabels.COLEGIADO_RECURSAL_PROPRIO,
                    RecursalFormalSectionLabels.IDENTIFICACAO_PARTES_RECURSAIS,
                    RecursalFormalSectionLabels.SINTESE_PROCESSO,
                    RecursalFormalSectionLabels.RAZOES_RECURSAIS,
                    RecursalFormalSectionLabels.CABIMENTO,
                    RecursalFormalSectionLabels.LEGITIMIDADE,
                    RecursalFormalSectionLabels.INTERESSE_RECURSAL,
                    RecursalFormalSectionLabels.TEMPESTIVIDADE,
                    RecursalFormalSectionLabels.PREPARO,
                    RecursalFormalSectionLabels.EFEITOS_RECURSAIS
            );
            case "AGRAVO_INTERNO" -> List.of(
                    RecursalFormalSectionLabels.PETICAO_INTERPOSICAO,
                    RecursalFormalSectionLabels.ENDERECAMENTO_AD_QUEM,
                    RecursalFormalSectionLabels.IDENTIFICACAO_PARTES_RECURSAIS,
                    RecursalFormalSectionLabels.DECISAO_MONOCRATICA_RELATOR,
                    RecursalFormalSectionLabels.IMPUGNACAO_ESPECIFICA_RECURSO_PRINCIPAL,
                    RecursalFormalSectionLabels.TEMPESTIVIDADE,
                    RecursalFormalSectionLabels.PREPARO
            );
            case "AGRAVO_DE_INSTRUMENTO", "AGRAVO_EM_RECURSO_ESPECIAL_EXTRAORDINARIO" -> List.of(
                    RecursalFormalSectionLabels.PETICAO_INTERPOSICAO,
                    RecursalFormalSectionLabels.ENDERECAMENTO_AD_QUEM,
                    RecursalFormalSectionLabels.IDENTIFICACAO_PARTES_RECURSAIS,
                    RecursalFormalSectionLabels.CABIMENTO,
                    RecursalFormalSectionLabels.TEMPESTIVIDADE,
                    RecursalFormalSectionLabels.PREPARO,
                    RecursalFormalSectionLabels.REGULARIDADE_FORMAL,
                    RecursalFormalSectionLabels.PEDIDO_SANEAMENTO_VICIO_FORMAL
            );
            case "RECURSO_ESPECIAL" -> List.of(
                    RecursalFormalSectionLabels.PETICAO_INTERPOSICAO,
                    RecursalFormalSectionLabels.ENDERECAMENTO_A_QUO,
                    RecursalFormalSectionLabels.ENDERECAMENTO_AD_QUEM,
                    RecursalFormalSectionLabels.IDENTIFICACAO_PARTES_RECURSAIS,
                    RecursalFormalSectionLabels.CABIMENTO,
                    RecursalFormalSectionLabels.VIOLACAO_LEI_FEDERAL,
                    RecursalFormalSectionLabels.PREQUESTIONAMENTO,
                    RecursalFormalSectionLabels.TEMPESTIVIDADE,
                    RecursalFormalSectionLabels.PREPARO,
                    RecursalFormalSectionLabels.JUIZO_ADMISSIBILIDADE_TRIBUNAL_RECORRIDO
            );
            case "RECURSO_EXTRAORDINARIO" -> List.of(
                    RecursalFormalSectionLabels.PETICAO_INTERPOSICAO,
                    RecursalFormalSectionLabels.ENDERECAMENTO_A_QUO,
                    RecursalFormalSectionLabels.ENDERECAMENTO_AD_QUEM,
                    RecursalFormalSectionLabels.IDENTIFICACAO_PARTES_RECURSAIS,
                    RecursalFormalSectionLabels.CABIMENTO,
                    RecursalFormalSectionLabels.VIOLACAO_CONSTITUCIONAL,
                    RecursalFormalSectionLabels.PREQUESTIONAMENTO,
                    RecursalFormalSectionLabels.REPERCUSSAO_GERAL,
                    RecursalFormalSectionLabels.TEMPESTIVIDADE,
                    RecursalFormalSectionLabels.PREPARO,
                    RecursalFormalSectionLabels.JUIZO_ADMISSIBILIDADE_TRIBUNAL_RECORRIDO
            );
            case "EMBARGOS_DECLARACAO" -> List.of(
                    RecursalFormalSectionLabels.PETICAO_INTERPOSICAO,
                    RecursalFormalSectionLabels.IDENTIFICACAO_PARTES_RECURSAIS,
                    RecursalFormalSectionLabels.FUNDAMENTOS_EMBARGOS,
                    RecursalFormalSectionLabels.TEMPESTIVIDADE,
                    RecursalFormalSectionLabels.PEDIDO_EFEITO_INFRINGENTE
            );
            case "EMBARGOS_DIVERGENCIA" -> List.of(
                    RecursalFormalSectionLabels.PETICAO_INTERPOSICAO,
                    RecursalFormalSectionLabels.ENDERECAMENTO_AD_QUEM,
                    RecursalFormalSectionLabels.IDENTIFICACAO_PARTES_RECURSAIS,
                    RecursalFormalSectionLabels.DEMONSTRACAO_DIVERGENCIA,
                    RecursalFormalSectionLabels.CONFRONTO_ANALITICO_DIVERGENCIA,
                    RecursalFormalSectionLabels.ACORDAO_PARADIGMA,
                    RecursalFormalSectionLabels.REGULARIDADE_FORMAL,
                    RecursalFormalSectionLabels.TEMPESTIVIDADE
            );
            case "CONTRARRAZOES" -> List.of(
                    RecursalFormalSectionLabels.IDENTIFICACAO_PARTES_RECURSAIS,
                    RecursalFormalSectionLabels.CONTRARRAZOES,
                    RecursalFormalSectionLabels.IMPUGNACAO_ESPECIFICA_RECURSO_PRINCIPAL,
                    RecursalFormalSectionLabels.PEDIDO_NAO_CONHECIMENTO,
                    RecursalFormalSectionLabels.PEDIDO_NAO_PROVIMENTO
            );
            default -> RecursalFormalSectionLabels.defaultOrder();
        };
    }

    public static Map<String, String> checklistDescricaoPorCodigo(String rota) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        for (String secao : secoesPorRota(rota)) {
            checklist.put(secao, descricao(secao));
        }
        return Map.copyOf(checklist);
    }

    public static List<String> checklistCodigos(String rota) {
        return List.copyOf(new ArrayList<>(checklistDescricaoPorCodigo(rota).keySet()));
    }

    private static String descricao(String codigo) {
        return switch (codigo) {
            case RecursalFormalSectionLabels.PETICAO_INTERPOSICAO -> "abrir a peça com a petição de interposição quando a espécie exigir dupla camada formal";
            case RecursalFormalSectionLabels.ENDERECAMENTO_A_QUO -> "endereçar corretamente ao juízo de origem quando a espécie assim exigir";
            case RecursalFormalSectionLabels.ENDERECAMENTO_AD_QUEM -> "endereçar corretamente ao órgão julgador competente";
            case RecursalFormalSectionLabels.IDENTIFICACAO_PARTES_RECURSAIS -> "identificar recorrente, recorrido e o processo-base com nomenclatura recursal correta";
            case RecursalFormalSectionLabels.SINTESE_PROCESSO -> "consolidar a síntese objetiva do processo e do capítulo impugnado";
            case RecursalFormalSectionLabels.RAZOES_RECURSAIS -> "estruturar as razões recursais com causa de pedir recursal clara";
            case RecursalFormalSectionLabels.CABIMENTO -> "demonstrar o cabimento adequado ao pronunciamento judicial atacado";
            case RecursalFormalSectionLabels.LEGITIMIDADE -> "validar legitimidade recursal da parte, terceiro ou ministerio público quando couber";
            case RecursalFormalSectionLabels.INTERESSE_RECURSAL -> "demonstrar utilidade prática e sucumbência no capítulo impugnado";
            case RecursalFormalSectionLabels.TEMPESTIVIDADE -> "amarrar a tempestividade em dias úteis com recesso, feriado e marco inicial corretos";
            case RecursalFormalSectionLabels.PREPARO -> "validar preparo, dispensa ou necessidade de complementação";
            case RecursalFormalSectionLabels.EFEITOS_RECURSAIS -> "mapear os efeitos recursais pretendidos e prováveis";
            case RecursalFormalSectionLabels.DECISAO_MONOCRATICA_RELATOR -> "delimitar a decisão monocrática do relator como objeto do agravo interno";
            case RecursalFormalSectionLabels.IMPUGNACAO_ESPECIFICA_RECURSO_PRINCIPAL -> "impugnar especificamente os fundamentos do recurso ou da decisão atacada";
            case RecursalFormalSectionLabels.REGULARIDADE_FORMAL -> "conferir requisitos formais rígidos e documentação de suporte";
            case RecursalFormalSectionLabels.PEDIDO_SANEAMENTO_VICIO_FORMAL -> "pedir saneamento quando o cenário comportar correção de vício formal";
            case RecursalFormalSectionLabels.FUNDAMENTOS_EMBARGOS -> "delimitar omissão, contradição, obscuridade ou erro material de forma específica";
            case RecursalFormalSectionLabels.PEDIDO_EFEITO_INFRINGENTE -> "explicitar por que a integração pode gerar efeito modificativo";
            case RecursalFormalSectionLabels.DEMONSTRACAO_DIVERGENCIA -> "demonstrar a divergência relevante com aderência temática";
            case RecursalFormalSectionLabels.CONFRONTO_ANALITICO_DIVERGENCIA -> "comparar analiticamente o acórdão recorrido e o paradigma";
            case RecursalFormalSectionLabels.ACORDAO_PARADIGMA -> "anexar e identificar o acórdão paradigma idôneo";
            case RecursalFormalSectionLabels.CONTRARRAZOES -> "estruturar a resposta recursal sem abrir mão da impugnação técnica";
            case RecursalFormalSectionLabels.PEDIDO_NAO_CONHECIMENTO -> "delimitar pedido de não conhecimento quando houver defeito de admissibilidade";
            case RecursalFormalSectionLabels.PEDIDO_NAO_PROVIMENTO -> "delimitar pedido de não provimento do recurso principal";
            case RecursalFormalSectionLabels.COLEGIADO_RECURSAL_PROPRIO -> "indicar a turma recursal ou colegiado recursal próprio sem desviar para câmara de tribunal comum";
            case RecursalFormalSectionLabels.JUIZO_ADMISSIBILIDADE_TRIBUNAL_RECORRIDO -> "amarrar o filtro de presidência ou vice-presidência do tribunal recorrido antes da subida";
            case RecursalFormalSectionLabels.SUBIDA_CORTE_SUPERIOR -> "preparar a remessa à corte superior com trilha rígida de admissibilidade e vinculação documental";
            case RecursalFormalSectionLabels.VIOLACAO_LEI_FEDERAL -> "delimitar violação de lei federal infraconstitucional com aderência estrita ao acórdão recorrido";
            case RecursalFormalSectionLabels.VIOLACAO_CONSTITUCIONAL -> "delimitar ofensa constitucional direta sem depender de reexame probatório incompatível";
            case RecursalFormalSectionLabels.PREQUESTIONAMENTO -> "confirmar pré-questionamento e enfrentamento do tema no acórdão recorrido";
            case RecursalFormalSectionLabels.REPERCUSSAO_GERAL -> "explicitar a repercussão geral quando a rota excepcional exigir controle constitucional";
            default -> "validar a seção formal " + codigo + " na peça ou trilha correspondente";
        };
    }
}
