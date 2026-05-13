package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RecursalNationalRitesPetitioningBlueprint {

    private RecursalNationalRitesPetitioningBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> sections = new ArrayList<>();
        sections.add(RecursalFormalSectionLabels.MATRIZ_NACIONAL_RECURSOS_E_PECAS_POR_RITO);
        sections.add(RecursalFormalSectionLabels.CIVIL_EXECUCAO_JUIZADOS_RECURSAIS);
        sections.add(RecursalFormalSectionLabels.PENAL_RECURSAL_E_EXECUCAO_PENAL);
        sections.add(RecursalFormalSectionLabels.TRABALHISTA_RECURSAL_PROPRIO);
        sections.add(RecursalFormalSectionLabels.ELEITORAL_RECURSAL_ESPECIALIDADE);
        sections.add(RecursalFormalSectionLabels.MILITAR_RECURSAL_PROPRIO);
        sections.add("TRIBUTARIO_EXECUCAO_FISCAL_E_FAZENDA_PUBLICA_RECURSAL");
        sections.add("FAMILIA_SUCESSOES_INFANCIA_RECURSAL_SIGILOSA");
        sections.add("PREVIDENCIARIO_JEF_RPPS_ACIDENTARIO_RECURSAL");
        sections.add("AMBIENTAL_COLETIVO_AGRARIO_RECURSAL");
        sections.add("EMPRESARIAL_FALIMENTAR_RECUPERACIONAL_RECURSAL");
        sections.add("INTERNACIONAL_COOPERACAO_E_CORTES_SUPERIORES");
        sections.add(RecursalFormalSectionLabels.PECAS_INSTITUCIONAIS_E_CONTRADITORIO_RECURSAL);
        sections.add(RecursalFormalSectionLabels.DIFERENCIACAO_ENDERECAMENTO_PRAZO_PREPARO_E_EFEITOS);
        return List.copyOf(sections);
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("CLASSIFICAR_FAMILIA_RECURSAL", "identificar a família recursal dominante do caso em todos os ritos brasileiros, sem herdar automaticamente a semântica da petição inicial: "
                + especieResumoGeral());
        checklist.put("DIFERENCIAR_PETICAO_POR_RITO_E_ESPECIE", "tratar como peças diferentes a petição de primeiro grau, a peça recursal, a petição de embargos e a manifestação institucional; para a rota "
                + recursoPrincipal + " no contexto " + familiaRamo(request) + ", o studio deve ajustar endereçamento, órgão ad quem, fundamentos, prazo, preparo, efeitos e protocolo");
        checklist.put("APLICAR_CIVIL_EXECUCAO_E_JUIZADOS", "no eixo civil e dos juizados, distinguir apelação, agravo de instrumento, agravo interno, embargos de declaração, recursos excepcionais, agravo em recurso excepcional, recurso inominado, pedido de uniformização e manifestações de contrarrazões ou adesivo, sem colar o regime de uma espécie sobre outra");
        checklist.put("APLICAR_PENAL_E_EXECUCAO_PENAL", "no eixo penal, separar apelação, recurso em sentido estrito, embargos de declaração, carta testemunhável, agravos executórios e recursos excepcionais; o peticionamento deve reconhecer impugnação criminal, razões recursais, contrarrazões, memoriais e manifestações do Ministério Público com semântica penal própria");
        checklist.put("APLICAR_TRABALHISTA", "no processo do trabalho, separar recurso ordinário, agravo de petição, recurso de revista, agravo de instrumento, embargos no TST, agravo interno e embargos de declaração; o sistema deve recalibrar depósito, transcendência, execução e admissibilidade interna do ramo antes de abrir a peça");
        checklist.put("APLICAR_ELEITORAL", "na Justiça Eleitoral, distinguir recurso eleitoral, recurso especial eleitoral, recurso ordinário eleitoral, agravos internos ou regimentais, embargos de declaração e petições criminais eleitorais fundamentadas; o sistema não pode importar automaticamente a prática do CPP ou do CPC quando a especialidade do rito exigir lógica própria");
        checklist.put("APLICAR_MILITAR", "na Justiça Militar, distinguir apelação, recurso em sentido estrito, embargos infringentes e de nulidade, embargos de declaração, correição parcial e recurso extraordinário, preservando diferenças de organização do processo penal militar e da instância castrense");
        checklist.put("APLICAR_TRIBUTARIO_FISCAL", "no tributário, fazenda pública e execução fiscal, distinguir apelação, agravo de instrumento, embargos à execução fiscal, exceção de pré-executividade, remessa necessária, recursos excepcionais, preparo isento e acervo de CDA, garantia, penhora e cálculo");
        checklist.put("APLICAR_FAMILIA_SUCESSOES_INFANCIA", "em família, sucessões e infância, separar apelação, agravos, embargos, medidas urgentes, alimentos, curatela, inventário, estudo psicossocial e intervenção obrigatória do Ministério Público quando houver incapaz ou interesse indisponível");
        checklist.put("APLICAR_PREVIDENCIARIO", "no previdenciário comum, JEF, RPPS e acidentário, distinguir recurso inominado, pedido de uniformização, apelação, agravo, tutela de benefício e recursos excepcionais, preservando prova médica e sigilo de saúde");
        checklist.put("APLICAR_AMBIENTAL_COLETIVO_AGRARIO", "em ambiental, coletivo, urbanístico e agrário, preservar tutela de urgência, legitimidade coletiva, prova técnica, georreferenciamento, área degradada, posse, atuação do Ministério Público e retorno operacional ao órgão de origem");
        checklist.put("APLICAR_EMPRESARIAL_INSOLVENCIA", "em empresarial, falimentar e recuperação judicial, distinguir agravo de instrumento, apelação, agravo interno e recursos excepcionais, carregando plano, QGC, administrador judicial, assembleia, ativos e sigilo empresarial");
        checklist.put("APLICAR_INTERNACIONAL_CONSTITUCIONAL", "em cooperação internacional, constitucional originário e cortes superiores, separar homologação, carta rogatória, reclamação, recurso ordinário constitucional, agravo interno e embargos, sem inventar primeiro grau quando a competência nasce em tribunal");
        checklist.put("ABRIR_PECAS_POR_ATOR_JURIDICO", "advogado, Defensoria, Procuradoria, Ministério Público e demais legitimados compatíveis devem reutilizar o mesmo studio, mas com perfis vivos de peça: recurso próprio, contrarrazões, contraminuta, parecer, cota, promoção, memoriais, resposta a laudo, quesitos complementares e petição intercorrente recursal");
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alerts = new ArrayList<>();
        alerts.add("o PJB não pode tratar todos os recursos dos ritos brasileiros como se fossem apelação cível com nome diferente");
        alerts.add("a peça de embargos tem identidade própria e não pode ser montada como sucedâneo de petição inicial ou de recurso ordinário genérico");
        alerts.add("o ramo " + familiaRamo(request) + " exige diferenciação de endereçamento, admissibilidade, preparo, efeitos, sustentação, órgão julgador e pós-julgamento");
        alerts.add(descricaoRamoAtual(request));
        return List.copyOf(alerts);
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "aplicar uma matriz nacional de peticionamento recursal por rito e espécie, reaproveitando o studio base já existente, mas distinguindo a peça do ramo "
                + familiaRamo(request)
                + " em torno da rota "
                + recursoPrincipal
                + " e dos legitimados que podem peticionar, contrarrazoar, embargar, parecer ou promover atos recursais.";
    }

    private static String especieResumoGeral() {
        return "civil/juizados, penal, trabalhista, eleitoral, militar, tributário/fiscal, fazenda pública, família/sucessões/infância, previdenciário, ambiental/coletivo/agrário, empresarial/insolvência, internacional e constitucional";
    }

    private static String descricaoRamoAtual(RecursalAutomationRequest request) {
        return switch (normalizedRamo(request)) {
            case "PENAL" -> "no penal, a malha precisa separar apelação, recurso em sentido estrito, embargos, carta testemunhável e execução penal, com espaço próprio para razões, contrarrazões, parecer e memoriais";
            case "TRABALHISTA" -> "no trabalhista, a malha precisa separar recurso ordinário, agravo de petição, recurso de revista, agravo de instrumento, embargos e agravo interno, com filtros próprios de admissibilidade e execução";
            case "ELEITORAL" -> "no eleitoral, a malha precisa separar recurso eleitoral, recurso especial eleitoral, recurso ordinário eleitoral, agravos e embargos, respeitando a especialidade da Justiça Eleitoral e das petições criminais eleitorais fundamentadas";
            case "MILITAR" -> "no militar, a malha precisa separar apelação, recurso em sentido estrito, embargos infringentes e de nulidade, correição parcial e recurso extraordinário, com semântica própria da Justiça Militar";
            case "TRIBUTARIO", "EXECUCAO_FISCAL", "FAZENDA_PUBLICA", "ADMINISTRATIVO" -> "no tributário, execução fiscal e fazenda pública, a malha precisa preservar CDA, penhora, garantia, ente público, remessa necessária, cálculo e sigilo fiscal, distinguindo vara de origem e câmara fazendária";
            case "FAMILIA", "SUCESSOES", "INFANCIA_JUVENTUDE" -> "em família, sucessões e infância, a malha precisa graduar sigilo, intervenção do Ministério Público, incapazes, alimentos, curatela, inventário e medidas urgentes antes de abrir o painel recursal";
            case "PREVIDENCIARIO", "ACIDENTARIO" -> "no previdenciário, a malha precisa separar JEF, vara federal, RPPS e acidentário, preservando prova médica, CNIS, laudo, tutela e sigilo de saúde";
            case "AMBIENTAL", "URBANISTICO", "CIVIL_PUBLICA_COLETIVO", "AGRARIO" -> "em ambiental, coletivo, urbanístico e agrário, a malha precisa preservar prova técnica, área, posse, MP, tutela coletiva e urgência na subida";
            case "EMPRESARIAL", "FALIMENTAR_RECUPERACIONAL" -> "em empresarial e insolvência, a malha precisa carregar plano, QGC, administrador judicial, assembleia, ativos e credores para a câmara competente";
            case "INTERNACIONAL", "CONSTITUCIONAL" -> "em internacional e constitucional, a malha precisa separar cooperação, competência originária, reclamação, recurso ordinário constitucional e cortes superiores sem criar primeiro grau artificial";
            case "CIVEL" -> request != null && request.juizadoEspecial()
                    ? "no eixo cível dos juizados, a malha precisa diferenciar recurso inominado, pedido de uniformização, embargos e incidentes próprios das turmas recursais"
                    : "no eixo cível, a malha precisa diferenciar apelação, agravos, embargos, recursos excepcionais, adesivo, contrarrazões e execução, sem tratar tudo como peça única";
            default -> "quando o ramo não vier mapeado de forma explícita, o PJB deve cair em matriz multirramo conservadora, exigindo classificação antes do protocolo";
        };
    }

    private static String familiaRamo(RecursalAutomationRequest request) {
        String ramo = normalizedRamo(request);
        if (request != null && request.juizadoEspecial()) {
            return ramo + "/JUIZADO_ESPECIAL";
        }
        return ramo;
    }

    private static String normalizedRamo(RecursalAutomationRequest request) {
        if (request == null || request.ramoProcessual() == null || request.ramoProcessual().isBlank()) {
            return "MULTIRRAMO";
        }
        return request.ramoProcessual().trim().toUpperCase(Locale.ROOT);
    }
}
