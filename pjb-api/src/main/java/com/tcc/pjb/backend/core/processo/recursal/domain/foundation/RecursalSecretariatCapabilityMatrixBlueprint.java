package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RecursalSecretariatCapabilityMatrixBlueprint {

    private RecursalSecretariatCapabilityMatrixBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> sections = new ArrayList<>();
        sections.add(RecursalFormalSectionLabels.CAPACIDADES_NUCLEO_SECRETARIA_ORIGEM);
        sections.add(RecursalFormalSectionLabels.CAPACIDADES_SECRETARIA_DESTINO_POR_GRAU);
        sections.add(RecursalFormalSectionLabels.CAPACIDADES_SECRETARIA_INSTITUCIONAL_DESTINO);
        sections.add(RecursalFormalSectionLabels.CAPACIDADES_POS_JULGAMENTO_MULTIGRAU);
        if (RecursalSecondInstanceBlueprint.rotaExigeSubidaEstrita(recursoPrincipal)) {
            sections.add(RecursalFormalSectionLabels.CAPACIDADES_PRESIDENCIA_VICE);
            sections.add(RecursalFormalSectionLabels.CAPACIDADES_CORTE_SUPERIOR);
        }
        return List.copyOf(sections);
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("NUCLEO_MINIMO_ORIGEM", "fixar como núcleo replicável da secretaria de origem: agenda/calendário, comunicação/intimação, mesa de exceções, cobertura/substituição, catálogo formal, contingência, pós-julgamento e trilha de retorno controlado");
        checklist.put("REUSAR_SUPERFICIES_SECRETARIA_JUDICIAL", "reusar as superfícies já existentes da secretaria judicial em vez de duplicar contrato: "
                + String.join(" | ", rotasSecretariaJudicialExistentes()));
        checklist.put("REPLICAR_NO_DESTINO", "replicar esse núcleo em " + destinoPorGrau(recursoPrincipal, request)
                + ", sem downgrade operacional quando o processo subir para outra instância ou órgão colegiado");
        checklist.put("REUSAR_SUPERFICIES_COLEGIADAS", "na atuação recursal/colegiada, plugar a matriz nas superfícies já existentes de pauta, sessão, publicação, acórdão e baixa: "
                + String.join(" | ", rotasColegiadasExistentes(recursoPrincipal, request)));
        checklist.put("ADAPTAR_POR_RAMO", adaptacaoPorRamo(request));
        checklist.put("REUSAR_SUPERFICIES_RAMO", "ao adaptar por ramo, reaproveitar as rotas especializadas já presentes no projeto: "
                + String.join(" | ", rotasEspecializadasPorRamo(request)));
        checklist.put("PRESERVAR_ATE_ULTIMA_INSTANCIA", preservacaoAteUltimaInstancia(recursoPrincipal, request));
        checklist.put("ESPINHA_INSTITUCIONAL", "espelhar para MP, Defensoria e Procuradoria a mesma malha de pré-pauta, comunicação de sessão, resultado, prazo subsequente, diligência complementar e cumprimento institucional");
        checklist.put("REUSAR_SUPERFICIES_SECRETARIA_INSTITUCIONAL", "conectar a subida recursal às superfícies institucionais já existentes, sem novo endpoint satélite: "
                + String.join(" | ", rotasInstitucionaisExistentes(request)));
        checklist.put("POS_JULGAMENTO_E_NOVA_JANELA", retornoPosJulgamento(recursoPrincipal, request));
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alertas = new ArrayList<>();
        alertas.add("o núcleo secretarial do primeiro grau não pode sumir na subida recursal; ele precisa reaparecer no grau seguinte com adaptação de competência e rito");
        if (request.juizadoEspecial() && recursoPrincipal.equals("RECURSO_INOMINADO")) {
            alertas.add("no juizado a matriz não vira câmara cível genérica: ela migra para a turma recursal com sessão, resultado colegiado e publicação próprios");
        }
        if (RecursalSecondInstanceBlueprint.rotaExigeSubidaEstrita(recursoPrincipal)) {
            alertas.add("na rota excepcional a presidência ou vice-presidência também precisa ter agenda, mesa de exceções, cobertura, catálogo formal e devolução pós-filtro antes da corte superior");
            alertas.add("a corte superior não recebe apenas a peça: recebe também a continuidade operacional mínima da secretaria e da secretaria institucional até a última instância");
        }
        alertas.add("secretaria judicial e secretaria institucional devem viajar juntas no ciclo recursal: o que existe para o fórum também precisa existir para o órgão de apoio institucional do grau competente");
        alertas.add("reusar queue/governance/coverage/formal-catalog, superfícies colegiadas e institutional-support já existentes antes de admitir qualquer contrato novo no eixo recursal");
        return List.copyOf(alertas);
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "materializar matriz de capacidades da secretaria para garantir que agenda, comunicação, exceções, cobertura, catálogo formal, pós-julgamento e espelho institucional atravessem a subida recursal até "
                + destinoPorGrau(recursoPrincipal, request)
                + ", reaproveitando superfícies já existentes de secretaria judicial, colegiado e apoio institucional.";
    }

    public static String descricaoConexaoComSuperficiesExistentes(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> families = new ArrayList<>();
        families.add("secretaria judicial=" + String.join(" | ", rotasSecretariaJudicialExistentes()));
        families.add("colegiado=" + String.join(" | ", rotasColegiadasExistentes(recursoPrincipal, request)));
        families.add("institucional=" + String.join(" | ", rotasInstitucionaisExistentes(request)));
        List<String> ramoRoutes = rotasEspecializadasPorRamo(request);
        if (!ramoRoutes.isEmpty()) {
            families.add("ramo=" + String.join(" | ", ramoRoutes));
        }
        return "reutilizar superfícies já existentes no projeto e conectar a matriz multigrau sem contrato paralelo: " + String.join(" ; ", families);
    }

    private static List<String> rotasSecretariaJudicialExistentes() {
        return List.of(
                OperationalApiRoutes.secretariatQueuePanel(),
                OperationalApiRoutes.secretariatQueueAgenda(),
                OperationalApiRoutes.secretariatQueueGovernance(),
                OperationalApiRoutes.secretariatQueueCoverage(),
                OperationalApiRoutes.secretariatQueueFormalCatalog(),
                OperationalApiRoutes.secretariatOperationalSnapshot()
        );
    }

    private static List<String> rotasColegiadasExistentes(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> routes = new ArrayList<>();
        if (RecursalJurisdictionPanelBlueprint.mesmoOrgaoProlator(recursoPrincipal, request)) {
            routes.add(OperationalApiRoutes.secretariatOperationalIntimacao(0L));
            routes.add(OperationalApiRoutes.secretariatOperationalConclusao(0L));
            routes.add(OperationalApiRoutes.secretariatOperationalJuntada(0L));
            return List.copyOf(routes);
        }
        routes.add(OperationalApiRoutes.secretariatOperationalCollegiatePauta(0L));
        routes.add(OperationalApiRoutes.secretariatOperationalCollegiatePublication(0L));
        if (request.desejaSustentacaoOral()) {
            routes.add(OperationalApiRoutes.secretariatOperationalCollegiateSustentacao(0L));
        }
        routes.add(OperationalApiRoutes.secretariatOperationalCollegiateAcordao(0L));
        routes.add(OperationalApiRoutes.secretariatOperationalCollegiateBaixa(0L));
        if (request.juizadoEspecial() && recursoPrincipal.equals("RECURSO_INOMINADO")) {
            routes.add(OperationalApiRoutes.desembargadorColegiadoMalhaProcesso(0L));
        }
        if (RecursalSecondInstanceBlueprint.rotaExigeSubidaEstrita(recursoPrincipal)) {
            routes.add(OperationalApiRoutes.ministroPlenarioPauta(0L));
            routes.add(OperationalApiRoutes.ministroPlenarioDecisaoPlenaria(0L));
        }
        return List.copyOf(routes);
    }

    private static List<String> rotasInstitucionaisExistentes(RecursalAutomationRequest request) {
        String branchCode = branchCodePlaceholder(request);
        return List.of(
                OperationalApiRoutes.institutionalSupportCompetenceMatrix(branchCode),
                OperationalApiRoutes.institutionalSupportCoverage(branchCode),
                OperationalApiRoutes.institutionalSupportProcessPrePauta(branchCode, 0L)
        );
    }

    private static List<String> rotasEspecializadasPorRamo(RecursalAutomationRequest request) {
        return switch (normalize(request.ramoProcessual())) {
            case "ELEITORAL" -> List.of(
                    OperationalApiRoutes.secretariatOperationalElectoralCorregedoria(0L),
                    OperationalApiRoutes.secretariatOperationalElectoralInspecao(0L),
                    OperationalApiRoutes.secretariatOperationalElectoralPesquisa(0L)
            );
            case "TRABALHISTA" -> List.of(
                    OperationalApiRoutes.secretariatOperationalLabourMidiaRecebimento(0L),
                    OperationalApiRoutes.secretariatOperationalLabourMidiaDisponibilizacao(0L),
                    OperationalApiRoutes.secretariatOperationalLabourExecucao(0L)
            );
            case "MILITAR" -> List.of(
                    OperationalApiRoutes.secretariatOperationalMilitaryPlantao(0L),
                    OperationalApiRoutes.secretariatOperationalMilitaryBalcao(0L)
            );
            case "PENAL", "CIVEL", "CIVIL", "PROCESSUAL_CIVIL", "FAMILIA", "SUCESSOES", "INFANCIA_JUVENTUDE", "FAZENDA_PUBLICA", "ADMINISTRATIVO", "TRIBUTARIO", "EXECUCAO_FISCAL", "PREVIDENCIARIO", "ACIDENTARIO", "AMBIENTAL", "URBANISTICO", "CIVIL_PUBLICA_COLETIVO", "AGRARIO", "EMPRESARIAL", "FALIMENTAR_RECUPERACIONAL", "CONSTITUCIONAL", "INTERNACIONAL" -> List.of(
                    OperationalApiRoutes.secretariatJulgamentoProcesso(0L),
                    OperationalApiRoutes.secretariatJulgamentoStatus(0L),
                    OperationalApiRoutes.secretariatJulgamentoAcordao(0L)
            );
            default -> List.of();
        };
    }

    private static String branchCodePlaceholder(RecursalAutomationRequest request) {
        return switch (normalize(request.segmentoJudiciario())) {
            case "FEDERAL" -> "branchCode-federal";
            case "ELEITORAL" -> "branchCode-eleitoral";
            case "MILITAR" -> "branchCode-militar";
            case "TRABALHISTA" -> "branchCode-trabalhista";
            case "INTERNACIONAL" -> "branchCode-superior";
            default -> "branchCode-estadual";
        };
    }

    private static String destinoPorGrau(String recursoPrincipal, RecursalAutomationRequest request) {
        if (RecursalJurisdictionPanelBlueprint.mesmoOrgaoProlator(recursoPrincipal, request)) {
            return "o mesmo órgão prolator";
        }
        if (request.juizadoEspecial() && recursoPrincipal.equals("RECURSO_INOMINADO")) {
            return "a turma recursal própria";
        }
        if (RecursalSecondInstanceBlueprint.rotaExigeSubidaEstrita(recursoPrincipal)) {
            return "a presidência ou vice-presidência do tribunal recorrido e, depois, a corte superior competente";
        }
        return "o órgão fracionário recursal competente";
    }

    private static String adaptacaoPorRamo(RecursalAutomationRequest request) {
        return switch (normalize(request.ramoProcessual())) {
            case "PENAL" -> "no penal, preservar secretaria de sessão, comunicação com custódia/urgência, pauta colegiada, publicação do acórdão e providências pós-julgamento sem perder a malha do primeiro grau";
            case "TRABALHISTA" -> "no trabalhista, preservar pauta, publicação, secretaria de turma, retorno para cumprimento e integração com a malha executiva trabalhista";
            case "ELEITORAL" -> "no eleitoral, preservar calendário sensível, sessão colegiada, publicização adequada e retorno célere ao cumprimento no grau competente";
            case "MILITAR" -> "no militar, preservar cadeia de sigilo, competência estrita, sessão própria e retorno controlado para execução/cumprimento no órgão competente";
            case "CIVEL", "CIVIL", "PROCESSUAL_CIVIL", "FAZENDA_PUBLICA", "ADMINISTRATIVO", "TRIBUTARIO", "EXECUCAO_FISCAL", "PREVIDENCIARIO", "ACIDENTARIO" -> "no eixo cível, fazendário, fiscal e previdenciário, preservar agenda, mesa de exceções, gabinete do relator, pauta, sessão, publicação, cálculo, preparo ou isenção, cumprimento/baixa e leitura controlada na origem";
            case "FAMILIA", "SUCESSOES", "INFANCIA_JUVENTUDE" -> "em família, sucessões e infância, preservar secretaria sigilosa, MP, incapazes, estudo social, alimentos, curatela, inventário e retorno protegido ao órgão de origem";
            case "AMBIENTAL", "URBANISTICO", "CIVIL_PUBLICA_COLETIVO", "AGRARIO" -> "em ambiental, coletivo, urbanístico e agrário, preservar prova técnica, urgência coletiva, georreferenciamento, área, posse e atuação institucional na subida";
            case "EMPRESARIAL", "FALIMENTAR_RECUPERACIONAL" -> "em empresarial e insolvência, preservar QGC, assembleia, administrador judicial, ativos, credores, sigilo empresarial e atos urgentes para a câmara competente";
            case "CONSTITUCIONAL", "INTERNACIONAL" -> "em constitucional e internacional, preservar dossiê de corte superior, autoridade central, precedente, competência originária e semântica de tribunal sem inventar primeiro grau artificial";
            default -> "preservar o núcleo secretarial com adaptação para o ramo processual competente, sem perder a simetria entre origem, destino e apoio institucional";
        };
    }

    private static String preservacaoAteUltimaInstancia(String recursoPrincipal, RecursalAutomationRequest request) {
        if (RecursalJurisdictionPanelBlueprint.mesmoOrgaoProlator(recursoPrincipal, request)) {
            return "como a atuação permanece no mesmo órgão prolator, a mesma secretaria mantém integralmente o núcleo operacional e institucional sem handoff artificial";
        }
        if (RecursalSecondInstanceBlueprint.rotaExigeSubidaEstrita(recursoPrincipal)) {
            return "a cada subida, replicar o núcleo da secretaria de origem no filtro de presidência ou vice e novamente na corte superior, mantendo a mesma espinha até a última instância, com governança, cobertura e acórdão conectados em "
                    + OperationalApiRoutes.secretariatQueueGovernance()
                    + " | "
                    + OperationalApiRoutes.institutionalSupportCoverage(branchCodePlaceholder(request))
                    + " | "
                    + OperationalApiRoutes.secretariatJulgamentoAcordao(0L);
        }
        return "sempre que houver nova subida recursal, o mesmo núcleo precisa ser reaplicado no próximo grau, evitando que o sistema trate a secretaria da instância superior como uma caixa vazia, com governança, cobertura e acórdão conectados em "
                + OperationalApiRoutes.secretariatQueueGovernance()
                + " | "
                + OperationalApiRoutes.institutionalSupportCoverage(branchCodePlaceholder(request))
                + " | "
                + OperationalApiRoutes.secretariatJulgamentoAcordao(0L);
    }

    private static String retornoPosJulgamento(String recursoPrincipal, RecursalAutomationRequest request) {
        if (RecursalJurisdictionPanelBlueprint.mesmoOrgaoProlator(recursoPrincipal, request)) {
            return "fechar publicação, eventual integração/modificação e retomada da fila local no mesmo órgão, mantendo secretaria judicial e institucional sincronizadas";
        }
        if (RecursalSecondInstanceBlueprint.rotaExigeSubidaEstrita(recursoPrincipal)) {
            return "após filtro e julgamento, devolver resultado, nova janela recursal, baixa ou cumprimento para o grau competente, mantendo a malha secretarial e institucional pronta para o próximo movimento";
        }
        return "após julgamento e publicação, devolver à origem apenas leitura, apoio e cumprimento, mas preservar no grau julgador o fechamento do pós-ato, da sessão e da nova janela recursal";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
