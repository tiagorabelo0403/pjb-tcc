package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecursalExpeditionAndProceduralActsBlueprint {

    private RecursalExpeditionAndProceduralActsBlueprint() {
    }

    public static List<String> secoes(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> sections = new ArrayList<>();
        sections.add(RecursalFormalSectionLabels.MALOTES_E_ENCAMINHAMENTOS_RECURSAIS);
        sections.add(RecursalFormalSectionLabels.PETICIONAMENTO_RECURSO_EMBARGOS);
        sections.add(RecursalFormalSectionLabels.ATOS_OPERACIONAIS_RECURSAIS);
        sections.add(RecursalFormalSectionLabels.INTIMACOES_CITACOES_CHAMAMENTOS_RECURSAIS);
        sections.add(RecursalFormalSectionLabels.AUXILIARES_JUSTICA_RECURSAL);
        return List.copyOf(sections);
    }

    public static Map<String, String> checklist(String recursoPrincipal, RecursalAutomationRequest request) {
        LinkedHashMap<String, String> checklist = new LinkedHashMap<>();
        checklist.put("ORQUESTRAR_MALOTES_ENCAMINHAMENTOS", "quando o processo entrar em recurso ou embargos, reaproveitar a porta formal de malotes, encaminhamentos, autuação e redistribuição controlada, sem trilha paralela: "
                + String.join(" | ", rotasMaloteEEncaminhamento()));
        checklist.put("ABRIR_PETICIONAMENTO_RECURSAL_CORRETO", "o peticionamento deve nascer com forma própria para recurso ou embargos, usando a espinha existente de studio, jornada inteligente e wizard de protocolo, sem tratar embargos como simples reaproveitamento de inicial: "
                + String.join(" | ", rotasPeticionamento()));
        checklist.put("MATERIALIZAR_ATOS_RECURSAIS_E_EMBARGOS", "o grau recursal também deve materializar atos equivalentes aos da primeira instância, mas na linguagem correta do órgão: despacho, decisão monocrática, voto, vista, destaque, pauta, sessão ou audiência, acórdão e publicação: "
                + String.join(" | ", rotasAtosRecursais()));
        checklist.put("ORQUESTRAR_INTIMACOES_E_CHAMAMENTOS", "intimação, juntada, conclusão, comunicação para terceiros e chamamento processual devem permanecer na malha da secretaria e dos painéis existentes, inclusive quando o rito exigir ciência de terceiros ou devolução controlada");
        checklist.put("ACIONAR_AUXILIARES_DA_JUSTICA", "quando o cenário recursal ou de embargos exigir diligência, laudo, resposta a quesitos, ciência de intimação ou ato externo, reaproveitar as superfícies existentes de perito, oficial de justiça, pauta de audiência e balcão processual: "
                + String.join(" | ", rotasAuxiliaresDaJustica()));
        checklist.put("VEDAR_DUPLICACAO_DE_FLUXO", "o mesmo processo não pode abrir malote, petição, intimação ou ato de auxiliar em eixo satélite; tudo deve permanecer ligado à secretaria, workbench e escada recursal já consolidados");
        return Map.copyOf(checklist);
    }

    public static List<String> alertas(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alertas = new ArrayList<>();
        alertas.add("malote e encaminhamento precisam acompanhar o degrau recursal para evitar que o processo suba sem secretaria, sem protocolo formal ou sem trilha de devolução ao órgão competente");
        alertas.add("o peticionamento de recurso e o peticionamento de embargos não podem ser tratados como cópia da petição inicial; cada um deve abrir a forma própria dentro do studio e do protocolo já existentes");
        alertas.add("intimação, citação de terceiros, chamamento de perito, quesitos, diligência de oficial e pauta de audiência/sessão precisam continuar aparecendo no recursal quando o rito ou o órgão exigir");
        if (recursoPrincipal.startsWith("EMBARGOS")) {
            alertas.add("embargos permanecem no órgão prolator adequado, mas ainda podem exigir intimação, juntada, vista, publicação e comunicação multicanal na mesma espinha recursal");
        }
        if (request.desejaSustentacaoOral()) {
            alertas.add("havendo sustentação oral, a malha de pauta, sessão, audiência tecnológica e intimação das partes deve permanecer conectada ao processo recursal sem ruptura de degrau");
        }
        return List.copyOf(alertas);
    }

    public static String descricaoExecutiva(String recursoPrincipal, RecursalAutomationRequest request) {
        return "conectar malotes, peticionamento próprio de recurso/embargos, atos recursais, intimações e chamamento de auxiliares da Justiça à mesma espinha já existente do PJB sob "
                + filtroRecursal(recursoPrincipal, request)
                + ".";
    }

    private static List<String> rotasMaloteEEncaminhamento() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.forumDistributionResolve(),
                RecursalWorkbenchSurfaceCatalog.secretariatOperationalSnapshot(),
                RecursalWorkbenchSurfaceCatalog.secretariatOperationalConclusao(),
                RecursalWorkbenchSurfaceCatalog.secretariatOperationalJuntada()
        );
    }

    private static List<String> rotasPeticionamento() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.peticionamentoSessaoInicial(),
                RecursalWorkbenchSurfaceCatalog.peticionamentoStudioWorkspace(),
                RecursalWorkbenchSurfaceCatalog.peticionamentoWizardProtocoloSimples(),
                RecursalWorkbenchSurfaceCatalog.peticionamentoJourneyInteligente(),
                RecursalWorkbenchSurfaceCatalog.processualParticipacaoSubmissoes()
        );
    }

    private static List<String> rotasAtosRecursais() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.magistraturaWorkspace(),
                RecursalWorkbenchSurfaceCatalog.magistraturaPreview(),
                RecursalWorkbenchSurfaceCatalog.malhaColegiada(0L),
                RecursalWorkbenchSurfaceCatalog.votoColegiado(0L),
                RecursalWorkbenchSurfaceCatalog.acordaoColegiado(0L),
                RecursalWorkbenchSurfaceCatalog.processualPautaAudiencia()
        );
    }

    private static List<String> rotasAuxiliaresDaJustica() {
        return List.of(
                RecursalWorkbenchSurfaceCatalog.secretariatOperationalIntimacao(),
                RecursalWorkbenchSurfaceCatalog.peritoPainel(),
                RecursalWorkbenchSurfaceCatalog.peritoNomeacoes(),
                RecursalWorkbenchSurfaceCatalog.peritoOperacionalSnapshot(),
                RecursalWorkbenchSurfaceCatalog.peritoOperacionalLaudo(),
                RecursalWorkbenchSurfaceCatalog.oficialJusticaAgendaOperacional(),
                RecursalWorkbenchSurfaceCatalog.oficialJusticaProcessWorkbench(),
                RecursalWorkbenchSurfaceCatalog.oficialJusticaCienteIntimacao(),
                RecursalWorkbenchSurfaceCatalog.oficialJusticaOficios(),
                RecursalWorkbenchSurfaceCatalog.oficialJusticaOficiosResposta()
        );
    }

    private static String filtroRecursal(String recursoPrincipal, RecursalAutomationRequest request) {
        String ramo = request.ramoProcessual() == null || request.ramoProcessual().isBlank()
                ? "RAMO_NAO_MAPEADO"
                : request.ramoProcessual().trim().toUpperCase();
        String segmento = request.segmentoJudiciario() == null || request.segmentoJudiciario().isBlank()
                ? "SEGMENTO_NAO_MAPEADO"
                : request.segmentoJudiciario().trim().toUpperCase();
        String rito = request.juizadoEspecial() ? "JUIZADO_ESPECIAL" : "RITO_ORDINARIO_DO_RAMO";
        return "segmento=" + segmento + ", ramo=" + ramo + ", rito=" + rito + ", classe="
                + (recursoPrincipal.startsWith("EMBARGOS") ? "EMBARGOS" : "RECURSO") + ", especie=" + recursoPrincipal;
    }
}
