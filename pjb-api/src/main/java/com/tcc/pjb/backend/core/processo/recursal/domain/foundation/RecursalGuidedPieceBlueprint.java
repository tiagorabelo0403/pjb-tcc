package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalGuidedPieceBlueprint {

    private RecursalGuidedPieceBlueprint() {
    }

    public static boolean supported(String recurso) {
        return switch (recurso) {
            case "APELACAO",
                    "RECURSO_INOMINADO",
                    "AGRAVO_DE_INSTRUMENTO",
                    "AGRAVO_INTERNO",
                    "RECURSO_ESPECIAL",
                    "RECURSO_EXTRAORDINARIO",
                    "AGRAVO_EM_RECURSO_ESPECIAL_EXTRAORDINARIO",
                    "EMBARGOS_DECLARACAO",
                    "EMBARGOS_DIVERGENCIA" -> true;
            default -> false;
        };
    }

    public static String trackCode(String recurso) {
        return switch (recurso) {
            case "APELACAO" -> "APELACAO_GUIADA";
            case "RECURSO_INOMINADO" -> "RECURSO_INOMINADO_GUIADO";
            case "AGRAVO_DE_INSTRUMENTO" -> "AGRAVO_DE_INSTRUMENTO_GUIADO";
            case "AGRAVO_INTERNO" -> "AGRAVO_INTERNO_GUIADO";
            case "RECURSO_ESPECIAL" -> "RECURSO_ESPECIAL_GUIADO";
            case "RECURSO_EXTRAORDINARIO" -> "RECURSO_EXTRAORDINARIO_GUIADO";
            case "AGRAVO_EM_RECURSO_ESPECIAL_EXTRAORDINARIO" -> "AGRAVO_RECURSO_EXCEPCIONAL_GUIADO";
            case "EMBARGOS_DECLARACAO" -> "EMBARGOS_DECLARACAO_GUIADO";
            case "EMBARGOS_DIVERGENCIA" -> "EMBARGOS_DIVERGENCIA_GUIADO";
            default -> "PECA_RECURSAL_GUIADA";
        };
    }

    public static String title(String recurso) {
        return switch (recurso) {
            case "APELACAO" -> "Apelação guiada por espécie";
            case "RECURSO_INOMINADO" -> "Recurso inominado guiado por espécie";
            case "AGRAVO_DE_INSTRUMENTO" -> "Agravo de instrumento guiado por espécie";
            case "AGRAVO_INTERNO" -> "Agravo interno guiado por espécie";
            case "RECURSO_ESPECIAL" -> "Recurso especial guiado por espécie";
            case "RECURSO_EXTRAORDINARIO" -> "Recurso extraordinário guiado por espécie";
            case "AGRAVO_EM_RECURSO_ESPECIAL_EXTRAORDINARIO" -> "Agravo em recurso excepcional guiado por espécie";
            case "EMBARGOS_DECLARACAO" -> "Embargos de declaração guiados por espécie";
            case "EMBARGOS_DIVERGENCIA" -> "Embargos de divergência guiados por espécie";
            default -> "Peça recursal guiada";
        };
    }

    public static List<String> sections(String recurso) {
        return switch (recurso) {
            case "APELACAO" -> List.of(
                    RecursalFormalSectionLabels.PETICAO_INTERPOSICAO,
                    RecursalFormalSectionLabels.ENDERECAMENTO_A_QUO,
                    RecursalFormalSectionLabels.ENDERECAMENTO_AD_QUEM,
                    RecursalFormalSectionLabels.IDENTIFICACAO_PARTES_RECURSAIS,
                    RecursalFormalSectionLabels.SINTESE_PROCESSO,
                    RecursalFormalSectionLabels.RAZOES_RECURSAIS,
                    RecursalFormalSectionLabels.CABIMENTO,
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
                    RecursalFormalSectionLabels.TEMPESTIVIDADE,
                    RecursalFormalSectionLabels.PREPARO,
                    RecursalFormalSectionLabels.EFEITOS_RECURSAIS
            );
            case "AGRAVO_DE_INSTRUMENTO" -> List.of(
                    RecursalFormalSectionLabels.PETICAO_INTERPOSICAO,
                    RecursalFormalSectionLabels.ENDERECAMENTO_AD_QUEM,
                    RecursalFormalSectionLabels.IDENTIFICACAO_PARTES_RECURSAIS,
                    RecursalFormalSectionLabels.CABIMENTO,
                    RecursalFormalSectionLabels.TEMPESTIVIDADE,
                    RecursalFormalSectionLabels.PREPARO,
                    RecursalFormalSectionLabels.REGULARIDADE_FORMAL,
                    RecursalFormalSectionLabels.PEDIDO_EFEITO_SUSPENSIVO
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
            case "RECURSO_ESPECIAL" -> List.of(
                    RecursalFormalSectionLabels.PETICAO_INTERPOSICAO,
                    RecursalFormalSectionLabels.ENDERECAMENTO_A_QUO,
                    RecursalFormalSectionLabels.ENDERECAMENTO_AD_QUEM,
                    RecursalFormalSectionLabels.IDENTIFICACAO_PARTES_RECURSAIS,
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
                    RecursalFormalSectionLabels.VIOLACAO_CONSTITUCIONAL,
                    RecursalFormalSectionLabels.PREQUESTIONAMENTO,
                    RecursalFormalSectionLabels.REPERCUSSAO_GERAL,
                    RecursalFormalSectionLabels.TEMPESTIVIDADE,
                    RecursalFormalSectionLabels.PREPARO,
                    RecursalFormalSectionLabels.JUIZO_ADMISSIBILIDADE_TRIBUNAL_RECORRIDO
            );
            case "AGRAVO_EM_RECURSO_ESPECIAL_EXTRAORDINARIO" -> List.of(
                    RecursalFormalSectionLabels.PETICAO_INTERPOSICAO,
                    RecursalFormalSectionLabels.ENDERECAMENTO_AD_QUEM,
                    RecursalFormalSectionLabels.IDENTIFICACAO_PARTES_RECURSAIS,
                    RecursalFormalSectionLabels.IMPUGNACAO_INADMISSIBILIDADE,
                    RecursalFormalSectionLabels.TEMPESTIVIDADE,
                    RecursalFormalSectionLabels.PREPARO,
                    RecursalFormalSectionLabels.REGULARIDADE_FORMAL,
                    RecursalFormalSectionLabels.PEDIDO_SANEAMENTO_VICIO_FORMAL
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
            default -> RecursalFormalChecklistBlueprint.secoesPorRota(recurso);
        };
    }

    public static Map<String, String> checklist(String recurso) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        switch (recurso) {
            case "APELACAO" -> {
                checklist.put("ISOLAR_CAPITULO_SENTENCA", "isolar os capítulos da sentença efetivamente impugnados e evitar devolução genérica");
                checklist.put("TRAVAR_DUPLO_ENDERECAMENTO", "conferir a petição de interposição ao juízo de origem e as razões ao tribunal competente");
                checklist.put("CONSOLIDAR_RAZOES_E_PEDIDOS", "alinhar causa de pedir recursal, pedido de reforma ou invalidação e extensão devolutiva");
                checklist.put("BLINDAR_TEMPESTIVIDADE_PREPARO", "fechar a amarração de prazo, preparo e eventual dispensa antes da remessa");
                checklist.put("MAPEAR_EFEITOS", "explicitar os efeitos recursais pretendidos e os limites de retratação potencial");
            }
            case "RECURSO_INOMINADO" -> {
                checklist.put("ISOLAR_SENTENCA_JEC", "confirmar que a sentença é do microssistema dos juizados e que a rota não deve desviar para apelação clássica");
                checklist.put("TRAVAR_TURMA_RECURSAL", "endereçar a remessa ao colegiado recursal próprio e impedir distribuição em câmara recursal comum");
                checklist.put("CONSOLIDAR_RAZOES_OBJETIVAS", "montar razões claras e sintéticas para sessão da turma recursal");
                checklist.put("BLINDAR_PRAZO_JEC", "controlar a janela própria do recurso inominado sem importar automaticamente o rito de apelação");
                checklist.put("PREPARAR_SESSAO_RECURSAL", "deixar a peça apta para pauta e julgamento no colegiado recursal dos juizados");
            }
            case "AGRAVO_DE_INSTRUMENTO" -> {
                checklist.put("DELIMITAR_DECISAO_INTERLOCUTORIA", "identificar a decisão interlocutória agravada e o ponto exato de urgência ou lesividade");
                checklist.put("FORMAR_INSTRUMENTO", "selecionar e ordenar as peças obrigatórias e úteis para compreensão imediata do tribunal");
                checklist.put("IMPUGNAR_CABIMENTO", "demonstrar a hipótese legal de cabimento sem narrativa aberta ou genérica");
                checklist.put("TRAVAR_EFEITO_ATIVO_OU_SUSPENSIVO", "justificar pedido de efeito ativo ou suspensivo quando o cenário exigir tutela imediata");
                checklist.put("VALIDAR_PROTOCOLO_AUTONOMO", "garantir tempestividade autônoma e regularidade formal do instrumento antes da distribuição");
            }
            case "AGRAVO_INTERNO" -> {
                checklist.put("DELIMITAR_MONOCRATICA", "delimitar precisamente a decisão monocrática submetida ao colegiado");
                checklist.put("IMPUGNAR_FUNDAMENTOS", "atacar todos os fundamentos utilizados pelo relator sem omissões de capítulos decisórios");
                checklist.put("RECOMPOSICAO_COLEGIADA", "demonstrar por que o órgão colegiado deve revisar a decisão monocrática");
                checklist.put("FECHAR_TEMPESTIVIDADE_PREPARO", "amarrar prazo e preparo antes da autuação interna do tribunal");
                checklist.put("PREPARAR_PAUTA_INTERNA", "deixar a peça pronta para julgamento colegiado sem depender de complementação posterior");
            }
            case "RECURSO_ESPECIAL" -> {
                checklist.put("ISOLAR_LEI_FEDERAL", "delimitar a ofensa à lei federal infraconstitucional sem reabrir matéria fática incompatível");
                checklist.put("CONFIRMAR_PREQUESTIONAMENTO", "validar o pré-questionamento e o enfrentamento do tema pelo acórdão recorrido");
                checklist.put("TRAVAR_FILTRO_PRESIDENCIA", "preparar a peça para o juízo inicial de admissibilidade da presidência ou vice-presidência do tribunal recorrido");
                checklist.put("BLINDAR_FORMALIDADE_E_PREPARO", "revalidar preparo, representação e regularidade formal antes da subida");
                checklist.put("PREPARAR_SUBIDA_STJ", "deixar a peça apta para eventual remessa ao STJ sem depender de saneamento tardio");
            }
            case "RECURSO_EXTRAORDINARIO" -> {
                checklist.put("ISOLAR_OFENSA_CONSTITUCIONAL", "delimitar ofensa constitucional direta com aderência estrita ao acórdão recorrido");
                checklist.put("CONFIRMAR_PREQUESTIONAMENTO", "validar o pré-questionamento e a maturidade constitucional da tese");
                checklist.put("DEMONSTRAR_REPERCUSSAO_GERAL", "explicitar a repercussão geral antes do filtro de admissibilidade na origem");
                checklist.put("TRAVAR_FILTRO_PRESIDENCIA", "preparar a peça para o juízo inicial de admissibilidade da presidência ou vice-presidência do tribunal recorrido");
                checklist.put("PREPARAR_SUBIDA_STF", "deixar a peça apta para remessa ao STF com lastro formal íntegro");
            }
            case "AGRAVO_EM_RECURSO_ESPECIAL_EXTRAORDINARIO" -> {
                checklist.put("ISOLAR_FUNDAMENTOS_INADMISSAO", "isolar todos os fundamentos da decisão que inadmitiu o recurso excepcional");
                checklist.put("IMPUGNAR_NEGATIVA_ANALITICAMENTE", "impugnar de modo analítico cada óbice usado para negar seguimento ao recurso excepcional");
                checklist.put("PRESERVAR_VIABILIDADE_EXCEPCIONAL", "demonstrar que a tese excepcional segue viável após a negativa de admissibilidade");
                checklist.put("BLINDAR_PREPARO_E_REGULARIDADE", "revalidar preparo, representação e demais requisitos formais rígidos do agravo");
                checklist.put("PREPARAR_SUBIDA_CORTE_SUPERIOR", "formatar a peça para admissibilidade estrita e remessa à corte superior");
            }
            case "EMBARGOS_DECLARACAO" -> {
                checklist.put("TIPIFICAR_VICIO", "tipificar com precisão omissão, contradição, obscuridade ou erro material sem ampliar artificialmente o objeto");
                checklist.put("APONTAR_TRECHO_COMPROMETIDO", "vincular o vício ao trecho exato da decisão embargada");
                checklist.put("PEDIR_INTEGRACAO_UTIL", "formular pedido de integração ou correção com resultado processualmente útil");
                checklist.put("JUSTIFICAR_EFEITO_INFRINGENTE", "sustentar efeito modificativo apenas quando a integração puder alterar o resultado legitimamente");
                checklist.put("FECHAR_JANELA_CURTA", "controlar a janela recursal curta e a interrupção dos demais prazos vinculados");
            }
            case "EMBARGOS_DIVERGENCIA" -> {
                checklist.put("SELECIONAR_PARADIGMA_IDONEO", "selecionar acórdão paradigma apto e compatível com o órgão de confronto exigido");
                checklist.put("PROVAR_SIMILITUDE_FATICA", "demonstrar similitude fática e jurídica entre o acórdão embargado e o paradigma");
                checklist.put("COTEJO_ANALITICO_ESTRITO", "realizar confronto analítico minucioso sem depender de mera ementa genérica");
                checklist.put("BLINDAR_REGULARIDADE_FORMAL", "validar requisitos regimentais estritos antes da distribuição colegiada");
                checklist.put("PREPARAR_JULGAMENTO_INTERNO", "deixar a peça apta para triagem, relatoria e julgamento na corte superior");
            }
            default -> RecursalFormalChecklistBlueprint.checklistDescricaoPorCodigo(recurso).forEach(checklist::put);
        }
        return Map.copyOf(checklist);
    }
}
