package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalCitizenPanelBlueprint {

    private RecursalCitizenPanelBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> sections = new ArrayList<>();
        sections.add(RecursalFormalSectionLabels.PAINEL_CIDADAO_PROCESSOS_PROPRIOS);
        sections.add(RecursalFormalSectionLabels.FILTRO_ENVOLVIMENTO_CIDADAO);
        sections.add(RecursalFormalSectionLabels.ULTIMAS_MOVIMENTACOES_RECURSAIS_CIDADAO);
        sections.add(RecursalFormalSectionLabels.CORES_PROCESSUAIS_RECURSAIS_CIDADAO);
        return List.copyOf(sections);
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("LIMITAR_PROCESSOS_PROPRIOS", "no painel do cidadão só publicar autos em que ele figure como autor, réu, interessado ou terceiro legitimado com vínculo real de acesso pessoal: "
                + String.join(" | ", rotasProcessosProprios()));
        checklist.put("FILTRAR_ESCADA_RECURSAL_CIDADAO", "a escada do cidadão deve filtrar somente processos próprios já em recurso ou embargos, sem misturar acervo de terceiros nem busca pública genérica: "
                + filtroRecursal(recursoPrincipal, request));
        checklist.put("MOSTRAR_OVERVIEW_RECURSAL", "cada processo próprio deve abrir overview autenticado com fase, rito, instâncias e vínculo recursal preservado: "
                + String.join(" | ", rotasOverviewCidadao()));
        checklist.put("MOSTRAR_ULTIMAS_MOVIMENTACOES", "mostrar ao cidadão as últimas movimentações compatíveis com sigilo, inclusive timeline visual e espelho de eventos do processo: "
                + String.join(" | ", rotasMovimentacao()));
        checklist.put("REUSAR_CORES_PROCESSUAIS_EXISTENTES", "reaproveitar a cor processual já calculada no workspace pessoal, com legenda oficial e sem criar paleta paralela: "
                + String.join(" | ", rotasCores()));
        checklist.put("PRESERVAR_SIGILO_EXTERNO", "quando o cidadão não puder ver a íntegra, manter somente resumo, cor processual, fase, últimas movimentações públicas/autenticadas permitidas e próximos marcos operacionais");
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alertas = new ArrayList<>();
        alertas.add("o painel do cidadão não pode listar processos alheios; a filtragem deve ser por vínculo real de parte ou envolvido legitimado");
        alertas.add("a visão recursal do cidadão precisa trazer últimas movimentações e cor processual já existente para explicar o degrau atual sem linguagem técnica excessiva");
        if (classificacaoRecursal(recursoPrincipal).equals("EMBARGOS")) {
            alertas.add("quando a rota for de embargos, o cidadão deve ver correção/integração do julgamento no mesmo órgão competente, sem sugerir redistribuição artificial");
        } else {
            alertas.add("quando a rota for recurso, o cidadão deve ver a subida por instância, o órgão de destino e a última movimentação relevante da trilha recursal");
        }
        return List.copyOf(alertas);
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "publicar a escada recursal do cidadão apenas para processos próprios, com overview autenticado, últimas movimentações compatíveis com sigilo e reaproveitamento das cores processuais já existentes, filtrando tudo por "
                + filtroRecursal(recursoPrincipal, request)
                + ".";
    }

    private static List<String> rotasProcessosProprios() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.citizenOwnProcesses(),
                RecursalWorkbenchSurfaceCatalog.citizenFolderProcesses(),
                RecursalWorkbenchSurfaceCatalog.personalOwnProcesses()
        );
    }

    private static List<String> rotasOverviewCidadao() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.citizenProcessOverview(),
                RecursalWorkbenchSurfaceCatalog.personalProcessOverview(),
                RecursalWorkbenchSurfaceCatalog.citizenInstancias(),
                RecursalWorkbenchSurfaceCatalog.citizenJulgamentos()
        );
    }

    private static List<String> rotasMovimentacao() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.citizenTimelineVisual(),
                RecursalWorkbenchSurfaceCatalog.citizenEventMirror(),
                RecursalWorkbenchSurfaceCatalog.publicProcessTimeline()
        );
    }

    private static List<String> rotasCores() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.uiLegend(),
                RecursalWorkbenchSurfaceCatalog.citizenTimelineVisual(),
                RecursalWorkbenchSurfaceCatalog.personalProcessOverview()
        );
    }

    private static String filtroRecursal(String recursoPrincipal, RecursalAutomationRequest request) {
        String ramo = blank(request.ramoProcessual()) ? "RAMO_NAO_MAPEADO" : request.ramoProcessual().trim().toUpperCase();
        String rito = request.juizadoEspecial() ? "JUIZADO_ESPECIAL" : "RITO_ORDINARIO_DO_RAMO";
        return "ramo=" + ramo + ", rito=" + rito + ", classe=" + classificacaoRecursal(recursoPrincipal) + ", especie=" + recursoPrincipal + ", ownership=PROCESSOS_PROPRIOS";
    }

    private static String classificacaoRecursal(String recursoPrincipal) {
        return recursoPrincipal.startsWith("EMBARGOS") ? "EMBARGOS" : "RECURSO";
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
