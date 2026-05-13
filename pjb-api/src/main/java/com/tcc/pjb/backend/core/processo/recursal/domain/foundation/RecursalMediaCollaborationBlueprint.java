package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalMediaCollaborationBlueprint {

    private RecursalMediaCollaborationBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                RecursalFormalSectionLabels.COLABORACAO_MULTIMIDIA_VIDEOCONFERENCIA,
                RecursalFormalSectionLabels.COLABORACAO_MULTIMIDIA_GRAVACAO_E_MIDIAS,
                RecursalFormalSectionLabels.COLABORACAO_MULTIMIDIA_VISUALIZADOR_E_AUTENTICIDADE
        );
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("CONECTAR_AUDIENCIAS_E_SESSOES_REMOTAS", "ligar audiências, sessões e comunicação remota do recursal às superfícies de videoconferência e agenda: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.processualPautaAudiencia(),
                RecursalWorkbenchSurfaceCatalog.calendarWorkspace(),
                RecursalWorkbenchSurfaceCatalog.recursalVideoconferencia(),
                RecursalWorkbenchSurfaceCatalog.advogadoSessoesJulgamento())));
        checklist.put("ORQUESTRAR_GRAVACAO_E_REPOSITORIO_MIDIAS", "preservar gravação, upload, indexação e replay de mídias e atos audiovisuais do recursal: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.recursalMidiasRepositorio(),
                RecursalWorkbenchSurfaceCatalog.recursalMidiasGravacaoAudiencia(),
                RecursalWorkbenchSurfaceCatalog.recursalMidiasVisualizacao())));
        checklist.put("PUBLICAR_VISUALIZADOR_E_AUTENTICIDADE", "expor visualizador documental, autenticidade e validação de artefatos assinados sem abrir cadeia documental paralela fora do PJB: "
                + String.join(" | ", List.of(
                RecursalWorkbenchSurfaceCatalog.recursalDocumentViewer(),
                RecursalWorkbenchSurfaceCatalog.recursalDocumentAuthenticity(),
                RecursalWorkbenchSurfaceCatalog.recursalDocumentSignatureEvidence(),
                RecursalWorkbenchSurfaceCatalog.certidaoAutenticidadeProfissional())));
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        return List.of(
                "mídias, videoconferência e gravação não podem ficar como satélite desconectado do processo ativo e da agenda recursal",
                "visualizador e autenticidade precisam usar a mesma cadeia documental soberana do PJB, sem segundo pipeline opaco"
        );
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "ativar colaboração multimídia e documental recursal com videoconferência, gravação, mídias, visualizador e autenticidade conectados ao processo ativo.";
    }
}
