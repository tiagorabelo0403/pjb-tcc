package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RecursalJurisdictionPanelBlueprint {

    private RecursalJurisdictionPanelBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> sections = new ArrayList<>();
        sections.add(RecursalFormalSectionLabels.PAINEL_JURISDICIONAL_DESTINO);
        sections.add(RecursalFormalSectionLabels.ORGAO_JULGADOR_COMPETENTE);
        sections.add(RecursalFormalSectionLabels.VINCULO_PROCESSO_ORIGEM);
        sections.add(RecursalFormalSectionLabels.VISIBILIDADE_ORIGEM);
        if (mesmoOrgaoProlator(recursoPrincipal, request)) {
            sections.add(RecursalFormalSectionLabels.FILA_INTEGRACAO_ORGAO_PROLATOR);
        } else if (request.juizadoEspecial() && recursoPrincipal.equals("RECURSO_INOMINADO")) {
            sections.add(RecursalFormalSectionLabels.COLEGIADO_RECURSAL_PROPRIO);
        } else {
            sections.add(RecursalFormalSectionLabels.DISTRIBUICAO_TRIBUNAL);
            sections.add(RecursalFormalSectionLabels.RELATORIA);
        }
        if (rotaExigeCorteSuperior(recursoPrincipal)) {
            sections.add(RecursalFormalSectionLabels.JUIZO_ADMISSIBILIDADE_TRIBUNAL_RECORRIDO);
            sections.add(RecursalFormalSectionLabels.SUBIDA_CORTE_SUPERIOR);
        }
        return List.copyOf(sections);
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("QUALIFICAR_ORGAO_PROLATOR", "qualificar se a reação permanece no mesmo órgão prolator ou se há efetiva subida para órgão recursal distinto");
        checklist.put("DEFINIR_PAINEL_DESTINO", "direcionar a atuação decisória para " + painelDestino(recursoPrincipal, request));
        checklist.put("TRAVAR_ORGAO_COMPETENTE", "fixar a competência julgadora em " + orgaoJulgador(recursoPrincipal, request) + " e evitar despacho recursal fora da malha competente");
        checklist.put("PRESERVAR_VINCULO_ORIGEM", "manter vínculo íntegro com o processo originário, inclusive número de origem, partes, sigilo, histórico e trilha de publicações");
        checklist.put("SEGREGAR_ESCRITA_DECISORIA", descricaoSegregacaoDecisoria(recursoPrincipal, request));
        if (mesmoOrgaoProlator(recursoPrincipal, request)) {
            checklist.put("ABRIR_FILA_DE_INTEGRACAO", "abrir fila de integração no mesmo órgão prolator, sem redistribuição externa para novo juiz ou nova câmara");
        } else if (request.juizadoEspecial() && recursoPrincipal.equals("RECURSO_INOMINADO")) {
            checklist.put("REMETER_TURMA_RECURSAL", "remeter o caso para a turma recursal competente, preservando o microssistema dos juizados sem conversão artificial em apelação");
        } else {
            checklist.put("REMETER_PARA_DISTRIBUICAO_COMPETENTE", "remeter o caso para distribuição, prevenção ou relatoria conforme a malha recursal competente");
        }
        if (rotaExigeCorteSuperior(recursoPrincipal)) {
            checklist.put("TRAVAR_FILTRO_PRESIDENCIA", "controlar a fase de presidência ou vice-presidência do tribunal recorrido antes da remessa à corte superior");
        }
        if (requerVisaoOrigemSomenteConsulta(recursoPrincipal, request)) {
            checklist.put("MANTER_CONSULTA_ORIGEM", "preservar consulta e esclarecimentos no painel de origem, mas retirar a caneta decisória recursal do órgão prolator originário");
        }
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alertas = new ArrayList<>();
        if (mesmoOrgaoProlator(recursoPrincipal, request)) {
            alertas.add("nem todo embargo ou incidente recursal sobe de instância; neste cenário a integração permanece no mesmo órgão prolator");
        } else {
            alertas.add("a atuação decisória sai do painel jurisdicional de origem e passa ao painel recursal competente, preservando apenas leitura, apoio e esclarecimentos no órgão anterior");
        }
        alertas.add("destino jurisdicional provável: " + painelDestino(recursoPrincipal, request));
        alertas.add("órgão julgador provável: " + orgaoJulgador(recursoPrincipal, request));
        if (rotaExigeCorteSuperior(recursoPrincipal)) {
            alertas.add("há controle rígido de admissibilidade e eventual remessa para corte superior; a presidência ou vice-presidência do tribunal também integra a malha decisória");
        }
        if (request.juizadoEspecial()) {
            alertas.add("em microssistema dos juizados a malha competente tende a ser turma recursal ou colegiado recursal próprio, não câmara cível clássica");
        }
        return List.copyOf(alertas);
    }

    public static String painelDestino(String recursoPrincipal, RecursalAutomationRequest request) {
        if (mesmoOrgaoProlator(recursoPrincipal, request)) {
            return switch (normalizar(request.pronunciamentoJudicial())) {
                case "ACORDAO", "DECISAO_MONOCRATICA" -> "painel de integração do órgão prolator no tribunal";
                default -> "painel do órgão prolator de origem";
            };
        }
        if (request.juizadoEspecial()) {
            return "painel da turma recursal competente";
        }
        return switch (recursoPrincipal) {
            case "RECURSO_ESPECIAL", "RECURSO_EXTRAORDINARIO", "AGRAVO_EM_RECURSO_ESPECIAL_EXTRAORDINARIO", "EMBARGOS_DIVERGENCIA" -> "painel recursal excepcional ou superior";
            case "AGRAVO_INTERNO" -> "painel colegiado interno do tribunal";
            default -> "painel recursal do tribunal competente";
        };
    }

    public static String orgaoJulgador(String recursoPrincipal, RecursalAutomationRequest request) {
        if (mesmoOrgaoProlator(recursoPrincipal, request)) {
            return switch (normalizar(request.pronunciamentoJudicial())) {
                case "ACORDAO" -> "o mesmo órgão colegiado prolator do acórdão";
                case "DECISAO_MONOCRATICA" -> "o mesmo relator ou o colegiado interno competente, conforme o remédio cabível";
                default -> "o mesmo juiz prolator da decisão embargada";
            };
        }
        if (request.juizadoEspecial()) {
            return "turma recursal competente";
        }
        String segmento = segmentoHumanizado(request.segmentoJudiciario());
        String ramo = ramoHumanizado(request.ramoProcessual());
        return switch (recursoPrincipal) {
            case "RECURSO_ESPECIAL" -> "Superior Tribunal de Justiça, após o filtro do tribunal recorrido";
            case "RECURSO_EXTRAORDINARIO" -> "Supremo Tribunal Federal, após o filtro do tribunal recorrido";
            case "AGRAVO_EM_RECURSO_ESPECIAL_EXTRAORDINARIO" -> "tribunal superior competente após a negativa de admissibilidade na origem";
            case "EMBARGOS_DIVERGENCIA" -> "órgão interno competente da corte superior para uniformização da divergência";
            case "AGRAVO_INTERNO" -> "órgão colegiado do próprio tribunal responsável pelo controle da decisão monocrática";
            default -> segmento.isBlank() && ramo.isBlank()
                    ? "câmara, turma ou órgão fracionário recursal competente" 
                    : "órgão fracionário recursal competente da Justiça " + segmento + (ramo.isBlank() ? "" : " em matéria " + ramo);
        };
    }

    public static boolean mesmoOrgaoProlator(String recursoPrincipal, RecursalAutomationRequest request) {
        return recursoPrincipal.equals("EMBARGOS_DECLARACAO")
                || (recursoPrincipal.equals("AGRAVO_INTERNO") && normalizar(request.pronunciamentoJudicial()).equals("DECISAO_MONOCRATICA"));
    }

    private static boolean rotaExigeCorteSuperior(String recursoPrincipal) {
        return switch (recursoPrincipal) {
            case "RECURSO_ESPECIAL", "RECURSO_EXTRAORDINARIO", "AGRAVO_EM_RECURSO_ESPECIAL_EXTRAORDINARIO", "EMBARGOS_DIVERGENCIA" -> true;
            default -> false;
        };
    }

    private static boolean requerVisaoOrigemSomenteConsulta(String recursoPrincipal, RecursalAutomationRequest request) {
        return !mesmoOrgaoProlator(recursoPrincipal, request);
    }

    private static String descricaoSegregacaoDecisoria(String recursoPrincipal, RecursalAutomationRequest request) {
        if (mesmoOrgaoProlator(recursoPrincipal, request)) {
            return "manter a competência decisória no próprio órgão prolator e impedir redistribuição artificial para painel recursal de outro magistrado";
        }
        return "desabilitar nova atuação decisória recursal no painel de origem e transferir a caneta jurisdicional para o órgão ad quem ou colegiado competente";
    }

    private static String segmentoHumanizado(String value) {
        String normalized = normalizar(value);
        return switch (normalized) {
            case "ESTADUAL" -> "estadual";
            case "FEDERAL" -> "federal";
            case "TRABALHISTA" -> "do trabalho";
            case "ELEITORAL" -> "eleitoral";
            case "MILITAR" -> "militar";
            default -> "";
        };
    }

    private static String ramoHumanizado(String value) {
        String normalized = normalizar(value);
        return switch (normalized) {
            case "CIVEL", "CIVIL", "PROCESSUAL_CIVIL" -> "cível";
            case "PENAL", "PROCESSUAL_PENAL", "EXECUCAO_PENAL" -> "penal";
            case "TRIBUTARIO", "EXECUCAO_FISCAL" -> "tributária/fiscal";
            case "EMPRESARIAL", "FALIMENTAR_RECUPERACIONAL" -> "empresarial e insolvência";
            case "FAMILIA", "SUCESSOES" -> "família e sucessões";
            case "FAZENDA_PUBLICA", "ADMINISTRATIVO", "SERVIDOR_PUBLICO", "LICITACOES_CONTRATOS", "IMPROBIDADE_ADMINISTRATIVA" -> "fazenda pública";
            case "INFANCIA", "INFANCIA_JUVENTUDE" -> "infância e juventude";
            case "PREVIDENCIARIO", "ACIDENTARIO" -> "previdenciária";
            case "TRABALHISTA", "PROCESSUAL_TRABALHISTA" -> "trabalhista";
            case "ELEITORAL", "PROCESSUAL_ELEITORAL" -> "eleitoral";
            case "MILITAR" -> "militar";
            case "AMBIENTAL", "URBANISTICO", "CIVIL_PUBLICA_COLETIVO" -> "ambiental/coletiva";
            case "AGRARIO" -> "agrária";
            case "INTERNACIONAL" -> "internacional";
            case "CONSTITUCIONAL" -> "constitucional";
            default -> normalized.isBlank() ? "" : normalized.toLowerCase(Locale.ROOT).replace('_', ' ');
        };
    }

    private static String normalizar(String value) {
        return value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
    }
}
