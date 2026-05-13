package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalSecondInstanceBlueprint;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceChecklistItemView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceTrackView;
import java.util.ArrayList;
import java.util.List;

public final class RecursalTribunalTrackFactory {

    private RecursalTribunalTrackFactory() {
    }

    public static RecursalAutomationWorkspaceTrackView buildDetailedTrack(String recursoPrincipal,
                                                                          RecursalAutomationRequest request) {
        return new RecursalAutomationWorkspaceTrackView(
                "TRILHA_SEGUNDA_INSTANCIA_TRIBUNAL",
                titulo(recursoPrincipal, request),
                alvo(recursoPrincipal, request),
                RecursalSecondInstanceBlueprint.secoes(recursoPrincipal, request),
                buildChecklist(recursoPrincipal, request),
                buildAlerts(recursoPrincipal, request)
        );
    }

    private static String titulo(String recursoPrincipal, RecursalAutomationRequest request) {
        if (request.juizadoEspecial() && recursoPrincipal.equals("RECURSO_INOMINADO")) {
            return "Trilha colegiada da turma recursal";
        }
        if (RecursalSecondInstanceBlueprint.rotaExigeSubidaEstrita(recursoPrincipal)) {
            return "Trilha excepcional e corte superior";
        }
        return "Trilha operacional detalhada do tribunal";
    }

    private static String alvo(String recursoPrincipal, RecursalAutomationRequest request) {
        if (request.juizadoEspecial() && recursoPrincipal.equals("RECURSO_INOMINADO")) {
            return "TURMA_RECURSAL";
        }
        if (RecursalSecondInstanceBlueprint.rotaExigeSubidaEstrita(recursoPrincipal)) {
            return "CORTE_SUPERIOR";
        }
        return "TRIBUNAL";
    }

    private static List<RecursalAutomationWorkspaceChecklistItemView> buildChecklist(String recursoPrincipal,
                                                                                      RecursalAutomationRequest request) {
        ArrayList<RecursalAutomationWorkspaceChecklistItemView> items = new ArrayList<>();
        items.add(item("RECEBIMENTO_RECURSAL", descricaoRecebimento(recursoPrincipal, request), true));
        items.add(item("TRIAGEM_DISTRIBUICAO", descricaoTriagem(recursoPrincipal, request), true));
        items.add(item("DISTRIBUICAO_RELATORIA", descricaoRelatoria(recursoPrincipal, request), true));
        items.add(item("PREPARO_GABINETE", "monitorar análise inicial do gabinete e pendências que possam travar a pauta", true));
        items.add(item("LIBERACAO_PAUTA", "acompanhar inclusão em pauta, intimação da sessão e janela mínima de preparação", true));
        if (request.desejaSustentacaoOral()) {
            items.add(item("SUSTENTACAO_ORAL", "reservar sustentação oral, conferir regras regimentais e preparar atuação em sessão", true));
        }
        items.add(item("JULGAMENTO_COLEGIADO", "acompanhar julgamento, proclamação do resultado e eventual modulação interna do colegiado", true));
        items.add(item("PUBLICACAO_ACORDAO", "monitorar publicação do acórdão, marco recursal seguinte e gatilhos de nova reação", true));
        if (RecursalSecondInstanceBlueprint.rotaExigeSubidaEstrita(recursoPrincipal)) {
            items.add(item("FILTRO_PRESIDENCIA_VICE", "controlar o juízo inicial de admissibilidade na presidência ou vice-presidência do tribunal recorrido", true));
            items.add(item("SUBIDA_CORTE_SUPERIOR", "preservar a trilha pronta para remessa, eventual agravo e processamento na corte superior", true));
        }
        return List.copyOf(items);
    }

    private static String descricaoRecebimento(String recursoPrincipal, RecursalAutomationRequest request) {
        if (request.juizadoEspecial() && recursoPrincipal.equals("RECURSO_INOMINADO")) {
            return "confirmar recebimento do recurso inominado, autuação no microssistema dos juizados e vinculação ao processo de origem";
        }
        return "confirmar recebimento do recurso, autuação correta e vinculação ao processo de origem";
    }

    private static String descricaoTriagem(String recursoPrincipal, RecursalAutomationRequest request) {
        if (request.juizadoEspecial() && recursoPrincipal.equals("RECURSO_INOMINADO")) {
            return "validar classe do recurso inominado, turma recursal competente e prevenção interna do colegiado próprio";
        }
        return "validar prevenção, classe recursal, órgão fracionário e regras de distribuição antes do sorteio";
    }

    private static String descricaoRelatoria(String recursoPrincipal, RecursalAutomationRequest request) {
        if (request.juizadoEspecial() && recursoPrincipal.equals("RECURSO_INOMINADO")) {
            return "acompanhar distribuição interna, conclusão ao juiz relator e estabilização da competência na turma recursal";
        }
        return "acompanhar sorteio, conclusão à relatoria e estabilização da competência interna";
    }

    private static List<String> buildAlerts(String recursoPrincipal, RecursalAutomationRequest request) {
        ArrayList<String> alertas = new ArrayList<>();
        if (request.juizadoEspecial() && recursoPrincipal.equals("RECURSO_INOMINADO")) {
            alertas.add("microssistema dos juizados pede turma recursal própria e evita desvio para câmara recursal clássica");
        } else {
            alertas.add("a trilha colegiada deve preservar distribuição, relatoria, pauta e julgamento no órgão fracionário competente");
        }
        if (request.desejaSustentacaoOral()) {
            alertas.add("sustentação oral exige sincronização fina de pauta, regras regimentais e janela de preparação");
        }
        if (RecursalSecondInstanceBlueprint.rotaExigeSubidaEstrita(recursoPrincipal)) {
            alertas.add("há filtro de presidência ou vice-presidência do tribunal recorrido antes da subida à corte superior");
            alertas.add("o percurso excepcional deve manter regularidade formal e integridade documental até a corte superior");
        }
        return List.copyOf(alertas);
    }

    private static RecursalAutomationWorkspaceChecklistItemView item(String code, String description, boolean required) {
        return new RecursalAutomationWorkspaceChecklistItemView(code, description, required);
    }
}
