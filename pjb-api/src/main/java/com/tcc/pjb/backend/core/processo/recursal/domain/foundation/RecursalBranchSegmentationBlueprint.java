package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalBranchSegmentationBlueprint {

    private RecursalBranchSegmentationBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                RecursalFormalSectionLabels.MALHA_RECURSAL_POR_RAMO,
                RecursalFormalSectionLabels.FILTRO_RAMO_PROCESSUAL_DESTINO,
                RecursalFormalSectionLabels.FILTRO_RITO_PROCESSUAL_DESTINO,
                RecursalFormalSectionLabels.SIGILO_GRADUADO_POR_RAMO,
                RecursalFormalSectionLabels.ESPELHO_MOVIMENTACOES_E_CORES_POR_RAMO
        );
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        String ramo = ramoAxis(request);
        String rito = ritoAxis(request, recursoPrincipal);
        checklist.put("FILTRAR_CIDADAO_POR_RAMO", "no painel do cidadão, organizar apenas processos próprios por ramo/rito/espécie e manter as últimas movimentações e cores na linguagem do ramo "
                + ramo + ": " + String.join(" | ", citizenSurfaces(ramo)));
        checklist.put("FILTRAR_REPRESENTACAO_POR_RAMO", "na lente de advogado, Defensoria, Procuradoria e Ministério Público, abrir fila recursal dedicada por ramo/rito/espécie sem competir com o painel geral: "
                + String.join(" | ", representationSurfaces(ramo)));
        checklist.put("FILTRAR_MAGISTRATURA_E_SECRETARIA_POR_RAMO", "na subida do processo, reaproveitar painéis de magistratura e secretaria conectados ao ramo " + ramo + " e ao rito " + rito + ": "
                + String.join(" | ", adjudicationSurfaces(ramo, request)));
        checklist.put("APLICAR_SIGILO_GRADUADO", "aplicar política de sigilo graduado por ramo: " + sigiloPolicy(ramo));
        checklist.put("REUSAR_MOVIMENTACOES_E_CORES", "reaproveitar última movimentação, timeline, reading-mode e legenda oficial já existentes, sem cor paralela nem timeline recursal duplicada");
        checklist.put("MANTER_ESCADA_POR_RAMO", "a escada deve nascer em degraus compatíveis com o ramo " + ramo + ", preservando cidadão, representantes, apoio institucional, secretaria e magistratura até a última instância");
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alertas = new ArrayList<>();
        String ramo = ramoAxis(request);
        alertas.add("a organização recursal deve deixar explícito o ramo " + ramo + " para cidadão, representantes, secretaria e magistratura");
        alertas.add("o filtro recursal precisa combinar ramo, rito, classe " + classificacaoRecursal(recursoPrincipal) + " e espécie " + recursoPrincipal + " sem misturar acervos de famílias jurisdicionais diferentes");
        if (ramo.equals("PENAL") || ramo.equals("ELEITORAL") || ramo.equals("MILITAR")) {
            alertas.add("o ramo " + ramo + " exige reforço de sigilo e linguagem mais contida para o degrau externo do cidadão");
        }
        if (ramo.equals("TRABALHISTA")) {
            alertas.add("no trabalhista a malha recursal deve preservar execução, mídia e dinâmica do TRT/TST sem cair em modelagem cível genérica");
        }
        if (ramo.equals("TRIBUTARIO") || ramo.equals("EXECUCAO_FISCAL") || ramo.equals("FAZENDA_PUBLICA")) {
            alertas.add("no eixo tributário/fazendário, a malha precisa preservar CDA, garantia, penhora, cálculo, remessa necessária e sigilo fiscal");
        }
        if (ramo.equals("FAMILIA") || ramo.equals("SUCESSOES") || ramo.equals("INFANCIA_JUVENTUDE")) {
            alertas.add("em família, sucessões e infância, o degrau externo deve ser mínimo e o painel recursal deve carregar MP, incapaz, estudo técnico e sigilo familiar");
        }
        if (ramo.equals("AMBIENTAL") || ramo.equals("AGRARIO") || ramo.equals("CIVIL_PUBLICA_COLETIVO")) {
            alertas.add("em ambiental, coletivo e agrário, a subida precisa preservar prova técnica, urgência coletiva, área, posse e legitimidade institucional");
        }
        return List.copyOf(alertas);
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        String ramo = ramoAxis(request);
        return "segmentar a escada recursal por ramo, rito e sigilo, organizando cidadão, advogados, Defensoria, Procuradoria, Ministério Público, secretaria e magistratura na linguagem operacional do ramo "
                + ramo
                + ", com filtros já aplicados por classe "
                + classificacaoRecursal(recursoPrincipal)
                + " e espécie "
                + recursoPrincipal
                + ".";
    }

    private static List<String> citizenSurfaces(String ramo) {
        ArrayList<String> surfaces = new ArrayList<>();
        surfaces.add(RecursalWorkbenchSurfaceCatalog.citizenOwnProcesses());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.citizenProcessOverview());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.citizenTimelineVisual());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.uiLegend());
        surfaces.addAll(RecursalWorkbenchSurfaceCatalog.ramoSurfaceSummary(ramo).values());
        return List.copyOf(surfaces);
    }

    private static List<String> representationSurfaces(String ramo) {
        ArrayList<String> surfaces = new ArrayList<>();
        surfaces.add(RecursalWorkbenchSurfaceCatalog.officeWorkspaceExecutiveDashboard());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.professionalWorkspaceExecutiveDashboard());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.defensoriaExecutiveDashboard());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.procuradoriaExecutiveDashboard());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.institutionalWorkbench());
        surfaces.addAll(RecursalWorkbenchSurfaceCatalog.ramoSurfaceSummary(ramo).values());
        return List.copyOf(surfaces);
    }

    private static List<String> adjudicationSurfaces(String ramo, RecursalAutomationRequest request) {
        ArrayList<String> surfaces = new ArrayList<>();
        surfaces.add(RecursalWorkbenchSurfaceCatalog.magistratureExecutiveDashboard());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.distribuicaoWorkbench());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.institutionalWorkbenchOperationalQueue());
        surfaces.add(RecursalWorkbenchSurfaceCatalog.painelMagistradoPrimeiroGrau(justicaAxis(request), tribunalAxis(request)));
        surfaces.add(RecursalWorkbenchSurfaceCatalog.painelColegiadoSegundoGrau(justicaAxis(request), tribunalAxis(request)));
        surfaces.addAll(RecursalWorkbenchSurfaceCatalog.ramoSurfaceSummary(ramo).values());
        return List.copyOf(surfaces);
    }

    private static String sigiloPolicy(String ramo) {
        return switch (ramo) {
            case "PENAL" -> "SIGILO_REFORCADO_POR_ACUSADO_VITIMA_TESTEMUNHA_E_ATOS_SENSIVEIS";
            case "ELEITORAL" -> "SIGILO_REFORCADO_EM_CORREGEDORIA_CADASTROS_E_MEDIDAS_SENSIVEIS";
            case "MILITAR" -> "SIGILO_REFORCADO_EM_HIERARQUIA_PLANTAO_E_ATOS_OPERACIONAIS";
            case "TRABALHISTA" -> "SIGILO_GRADUADO_COM_FOCO_EM_EXECUCAO_MIDIAS_E_DADOS_RESTRITOS";
            case "TRIBUTARIO", "EXECUCAO_FISCAL", "FAZENDA_PUBLICA", "ADMINISTRATIVO" -> "SIGILO_FISCAL_E_ADMINISTRATIVO_COM_VISIBILIDADE_EXTERNA_MINIMA";
            case "FAMILIA", "SUCESSOES", "INFANCIA_JUVENTUDE" -> "SIGILO_FAMILIAR_INFANCIA_E_INCAPAZ_COM_MP_E_DETALHE_RESTRITO";
            case "PREVIDENCIARIO", "ACIDENTARIO" -> "SIGILO_SAUDE_E_DADOS_SOCIAIS_COM_PROVA_MEDICA_PROTEGIDA";
            case "AMBIENTAL", "URBANISTICO", "CIVIL_PUBLICA_COLETIVO", "AGRARIO" -> "SIGILO_TECNICO_COLETIVO_GRADUADO_COM_URGENCIA_E_PROVA_PERICIAL";
            case "EMPRESARIAL", "FALIMENTAR_RECUPERACIONAL" -> "SIGILO_EMPRESARIAL_E_PATRIMONIAL_COM_ACESSO_DE_CREDORES_GOVERNADO";
            case "INTERNACIONAL", "CONSTITUCIONAL" -> "SIGILO_INSTITUCIONAL_DE_CORTE_SUPERIOR_COM_DOSSIER_CONTROLADO";
            default -> "SIGILO_GRADUADO_COM_LEITURA_EXTERNA_COMPATIVEL_E_DETALHE_TECNICO_AUTENTICADO";
        };
    }

    private static String classificacaoRecursal(String recursoPrincipal) {
        return recursoPrincipal.startsWith("EMBARGOS") ? "EMBARGOS" : "RECURSO";
    }

    private static String ramoAxis(RecursalAutomationRequest request) {
        if (request.ramoProcessual() == null || request.ramoProcessual().isBlank()) {
            return "CIVEL";
        }
        return request.ramoProcessual().trim().toUpperCase();
    }

    private static String ritoAxis(RecursalAutomationRequest request, String recursoPrincipal) {
        if (request.juizadoEspecial()) {
            return "JUIZADO_ESPECIAL";
        }
        if (recursoPrincipal.equals("EMBARGOS_DECLARACAO")) {
            return "INTEGRATIVO_DO_PROPRIO_ORGAO";
        }
        return switch (ramoAxis(request)) {
            case "PENAL" -> "RITO_RECURSAL_PENAL";
            case "TRABALHISTA" -> "RITO_RECURSAL_TRABALHISTA";
            case "ELEITORAL" -> "RITO_RECURSAL_ELEITORAL";
            case "MILITAR" -> "RITO_RECURSAL_MILITAR";
            case "TRIBUTARIO", "EXECUCAO_FISCAL", "FAZENDA_PUBLICA", "ADMINISTRATIVO" -> "RITO_RECURSAL_FAZENDA_PUBLICA_E_FISCAL";
            case "FAMILIA", "SUCESSOES", "INFANCIA_JUVENTUDE" -> "RITO_RECURSAL_FAMILIA_SUCESSOES_INFANCIA";
            case "PREVIDENCIARIO", "ACIDENTARIO" -> "RITO_RECURSAL_PREVIDENCIARIO";
            case "AMBIENTAL", "URBANISTICO", "CIVIL_PUBLICA_COLETIVO", "AGRARIO" -> "RITO_RECURSAL_COLETIVO_AMBIENTAL_AGRARIO";
            case "EMPRESARIAL", "FALIMENTAR_RECUPERACIONAL" -> "RITO_RECURSAL_EMPRESARIAL_INSOLVENCIA";
            case "INTERNACIONAL", "CONSTITUCIONAL" -> "RITO_RECURSAL_CORTE_SUPERIOR_ORIGINARIA";
            default -> "RITO_RECURSAL_CIVEL";
        };
    }

    private static String justicaAxis(RecursalAutomationRequest request) {
        String segmento = request.segmentoJudiciario() == null ? "" : request.segmentoJudiciario().trim().toUpperCase();
        return segmento.isBlank() ? "estadual" : segmento.toLowerCase();
    }

    private static String tribunalAxis(RecursalAutomationRequest request) {
        if (request.juizadoEspecial()) {
            return "turma-recursal";
        }
        return switch (justicaAxis(request)) {
            case "federal" -> "trf";
            case "trabalhista" -> "trt";
            case "eleitoral" -> "tre";
            case "militar" -> "tm";
            default -> "tj";
        };
    }
}
