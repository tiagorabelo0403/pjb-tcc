package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RecursalSecretariatParityBlueprint {

    private RecursalSecretariatParityBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> sections = new ArrayList<>();
        sections.add(RecursalFormalSectionLabels.ESPELHO_SECRETARIA_MULTIGRAU);
        sections.add(RecursalFormalSectionLabels.CONTINUIDADE_OPERACIONAL_SECRETARIA);
        sections.add(RecursalFormalSectionLabels.MESA_EXCECOES_SECRETARIA_DESTINO);
        sections.add(RecursalFormalSectionLabels.COBERTURA_SUBSTITUICAO_SECRETARIA_DESTINO);
        sections.add(RecursalFormalSectionLabels.CATALOGO_FORMAL_SECRETARIA_DESTINO);
        sections.add(RecursalFormalSectionLabels.SIMETRIA_SECRETARIA_INSTITUCIONAL);
        if (RecursalJurisdictionPanelBlueprint.mesmoOrgaoProlator(recursoPrincipal, request)) {
            sections.add(RecursalFormalSectionLabels.FILA_INTEGRACAO_ORGAO_PROLATOR);
        } else if (request.juizadoEspecial() && recursoPrincipal.equals("RECURSO_INOMINADO")) {
            sections.add(RecursalFormalSectionLabels.COLEGIADO_RECURSAL_PROPRIO);
            sections.add(RecursalFormalSectionLabels.PAUTA_JULGAMENTO);
            sections.add(RecursalFormalSectionLabels.PUBLICACAO_ACORDAO);
        } else {
            sections.add(RecursalFormalSectionLabels.DISTRIBUICAO_TRIBUNAL);
            sections.add(RecursalFormalSectionLabels.RELATORIA);
            sections.add(RecursalFormalSectionLabels.PAUTA_JULGAMENTO);
            sections.add(RecursalFormalSectionLabels.PUBLICACAO_ACORDAO);
        }
        if (RecursalSecondInstanceBlueprint.rotaExigeSubidaEstrita(recursoPrincipal)) {
            sections.add(RecursalFormalSectionLabels.JUIZO_ADMISSIBILIDADE_TRIBUNAL_RECORRIDO);
            sections.add(RecursalFormalSectionLabels.SUBIDA_CORTE_SUPERIOR);
        }
        return List.copyOf(sections);
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("ESPELHAR_CAPACIDADES_BASE", "replicar da secretaria de origem para " + secretariaDestino(recursoPrincipal, request)
                + " as capacidades mínimas de agenda/calendário, comunicação/intimação, mesa de exceções, cobertura/substituição, catálogo formal e contingência");
        checklist.put("ADAPTAR_CAPACIDADES_POR_GRAU", adaptacaoPorGrau(recursoPrincipal, request));
        checklist.put("PRESERVAR_SIGILO_E_CONTEXTO", "manter sigilo, urgência, partes, contatos operacionais autorizados, prevenção e vínculo integral com o processo de origem durante toda a troca de painel");
        checklist.put("REFORCAR_SECRETARIA_INSTITUCIONAL", "garantir que MP, Defensoria e Procuradoria recebam pré-pauta, comunicação de sessão, resultado, prazo pós-ato e diligência complementar também na instância recursal competente");
        checklist.put("SINCRONIZAR_DESK_E_COBERTURA", "abrir desk de exceções, cobertura e substituição no órgão competente para evitar buraco operacional entre distribuição, gabinete, pauta, sessão e publicação");
        checklist.put("FECHAR_RETORNO_POS_ATO", retornoPosAto(recursoPrincipal, request));
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alertas = new ArrayList<>();
        if (RecursalJurisdictionPanelBlueprint.mesmoOrgaoProlator(recursoPrincipal, request)) {
            alertas.add("a secretaria não some nem troca de grau artificialmente: a integração permanece no mesmo órgão prolator com a mesma malha operacional mínima");
        } else {
            alertas.add("o que existe na secretaria do primeiro grau não desaparece no recurso; deve reaparecer no órgão competente com adaptação para distribuição, gabinete, pauta, sessão, acórdão e baixa");
        }
        if (request.juizadoEspecial() && recursoPrincipal.equals("RECURSO_INOMINADO")) {
            alertas.add("no juizado especial a simetria recursal migra para a turma recursal própria, sem conversão cosmética em câmara cível clássica");
        }
        if (RecursalSecondInstanceBlueprint.rotaExigeSubidaEstrita(recursoPrincipal)) {
            alertas.add("na rota excepcional a presidência ou vice-presidência do tribunal recorrido também vira camada operacional obrigatória antes da corte superior");
        }
        alertas.add("a secretaria institucional também precisa espelhar o ciclo recursal: pré-pauta, comunicação de sessão, resultado, prazo recursal subsequente e providência pós-julgamento");
        return List.copyOf(alertas);
    }

    public static String secretariaDestino(String recursoPrincipal, RecursalAutomationRequest request) {
        if (RecursalJurisdictionPanelBlueprint.mesmoOrgaoProlator(recursoPrincipal, request)) {
            return "a própria secretaria do órgão prolator";
        }
        if (request.juizadoEspecial() && recursoPrincipal.equals("RECURSO_INOMINADO")) {
            return "a secretaria ou apoio operacional da turma recursal";
        }
        if (RecursalSecondInstanceBlueprint.rotaExigeSubidaEstrita(recursoPrincipal)) {
            return "a secretaria da presidência ou vice-presidência e, depois, a unidade competente da corte superior";
        }
        return "a secretaria do órgão fracionário recursal competente";
    }

    private static String adaptacaoPorGrau(String recursoPrincipal, RecursalAutomationRequest request) {
        if (RecursalJurisdictionPanelBlueprint.mesmoOrgaoProlator(recursoPrincipal, request)) {
            return "manter audiência, despacho integrativo, certificação, intimação e devolução no mesmo órgão prolator, sem redistribuição fictícia para outro painel";
        }
        if (request.juizadoEspecial() && recursoPrincipal.equals("RECURSO_INOMINADO")) {
            return "adaptar a malha da secretaria de primeiro grau para recebimento, distribuição interna, preparação colegiada, sessão da turma recursal, registro do resultado e publicação";
        }
        if (RecursalSecondInstanceBlueprint.rotaExigeSubidaEstrita(recursoPrincipal)) {
            return "adaptar a malha para filtro de presidência ou vice-presidência, admissibilidade excepcional, remessa documental, gabinete superior, sessão colegiada e publicação";
        }
        return "adaptar a malha para distribuição, gabinete do relator, pauta, sessão, acórdão, intimação e baixa recursal sem perder agenda, cobertura, catálogo formal ou desk de exceções";
    }

    private static String retornoPosAto(String recursoPrincipal, RecursalAutomationRequest request) {
        if (RecursalJurisdictionPanelBlueprint.mesmoOrgaoProlator(recursoPrincipal, request)) {
            return "fechar o pós-ato no mesmo órgão prolator, registrando publicação, eventual efeito modificativo e retomada da fila local";
        }
        if (RecursalSecondInstanceBlueprint.rotaExigeSubidaEstrita(recursoPrincipal)) {
            return "após o filtro e o julgamento, devolver o resultado ao painel e à secretaria compatíveis com o grau subsequente, inclusive nova janela recursal ou baixa";
        }
        return "após julgamento e publicação, devolver o resultado à origem apenas para leitura, cumprimento, esclarecimentos e nova fase processual, sem restaurar caneta recursal ao órgão anterior";
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "reforçar simetria secretarial entre origem e destino, levando para "
                + secretariaDestino(recursoPrincipal, request)
                + " o mesmo núcleo operacional do primeiro grau com adaptação ao grau, ao colegiado e ao ramo "
                + ramoHumanizado(request.ramoProcessual())
                + ".";
    }

    private static String ramoHumanizado(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "CIVEL" -> "cível";
            case "PENAL" -> "penal";
            case "TRABALHISTA" -> "trabalhista";
            case "ELEITORAL" -> "eleitoral";
            case "MILITAR" -> "militar";
            case "TRIBUTARIO" -> "tributário";
            case "FAZENDA_PUBLICA" -> "fazenda pública";
            default -> "processual pertinente";
        };
    }
}
