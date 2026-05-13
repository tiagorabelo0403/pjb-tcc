package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalGuidedPieceBlueprint;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationCandidateView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationPlaybookResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceChecklistItemView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceTrackView;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class RecursalGuidedPieceTrackFactory {

    private RecursalGuidedPieceTrackFactory() {
    }

    public static List<RecursalAutomationWorkspaceTrackView> buildTracks(String recursoPrincipal,
                                                                         RecursalAutomationResponse response,
                                                                         RecursalAutomationRequest request,
                                                                         RecursalAutomationPlaybookResponse playbook) {
        LinkedHashSet<String> recursos = new LinkedHashSet<>();
        if (RecursalGuidedPieceBlueprint.supported(recursoPrincipal)) {
            recursos.add(recursoPrincipal);
        }
        if (deveAcoplarRotaSuplementar(response, recursoPrincipal, "AGRAVO_EM_RECURSO_ESPECIAL_EXTRAORDINARIO")) {
            recursos.add("AGRAVO_EM_RECURSO_ESPECIAL_EXTRAORDINARIO");
        }
        if (deveAcoplarRotaSuplementar(response, recursoPrincipal, "EMBARGOS_DIVERGENCIA")) {
            recursos.add("EMBARGOS_DIVERGENCIA");
        }
        List<RecursalAutomationWorkspaceTrackView> tracks = new ArrayList<>();
        for (String recurso : recursos) {
            tracks.add(buildTrack(recurso, response, request, playbook));
        }
        return List.copyOf(tracks);
    }

    private static boolean deveAcoplarRotaSuplementar(RecursalAutomationResponse response,
                                                      String recursoPrincipal,
                                                      String recursoAlvo) {
        if (recursoAlvo.equals(recursoPrincipal)) {
            return false;
        }
        return response.candidatos().stream().map(RecursalAutomationCandidateView::recurso).anyMatch(recursoAlvo::equals);
    }

    private static RecursalAutomationWorkspaceTrackView buildTrack(String recurso,
                                                                   RecursalAutomationResponse response,
                                                                   RecursalAutomationRequest request,
                                                                   RecursalAutomationPlaybookResponse playbook) {
        Map<String, String> checklist = RecursalGuidedPieceBlueprint.checklist(recurso);
        List<String> alertas = buildAlerts(recurso, response, request, playbook);
        return new RecursalAutomationWorkspaceTrackView(
                RecursalGuidedPieceBlueprint.trackCode(recurso),
                RecursalGuidedPieceBlueprint.title(recurso),
                recurso,
                RecursalGuidedPieceBlueprint.sections(recurso),
                checklist.entrySet().stream()
                        .map(entry -> item(entry.getKey(), entry.getValue(), true))
                        .toList(),
                List.copyOf(alertas)
        );
    }

    private static List<String> buildAlerts(String recurso,
                                            RecursalAutomationResponse response,
                                            RecursalAutomationRequest request,
                                            RecursalAutomationPlaybookResponse playbook) {
        ArrayList<String> alertas = new ArrayList<>();
        alertas.add("peça específica guiada conectada à rota " + recurso + " sem abrir eixo paralelo fora do recursal");
        if (recurso.equals(playbook.rotaAlternativa())) {
            alertas.add("esta peça está mantida como rota subsidiária sob revisão técnica antes do protocolo");
        }
        if (request.preparoInsuficiente()) {
            alertas.add("há alerta de preparo insuficiente; blindar complementação antes de estabilizar a peça");
        }
        if (!request.preparoEfetuado() && !request.autosEletronicos()) {
            alertas.add("há risco de deserção; revalidar preparo ou hipótese de dispensa legal");
        }
        if (request.feriadoLocalAplicavel() && !request.feriadoLocalComprovado()) {
            alertas.add("feriado local aplicável ainda não comprovado; a prova deve acompanhar a interposição");
        }
        if (recurso.equals("AGRAVO_DE_INSTRUMENTO") && request.processoFisico()) {
            alertas.add("processo físico exige conferência reforçada das peças do instrumento antes da distribuição");
        }
        if (recurso.equals("RECURSO_INOMINADO") && request.juizadoEspecial()) {
            alertas.add("o cenário aponta microssistema dos juizados; a peça deve seguir para turma recursal própria, sem desvio para apelação clássica");
        }
        if (recurso.equals("RECURSO_ESPECIAL")) {
            alertas.add("recurso especial exige violação de lei federal e passará pelo filtro da presidência ou vice-presidência do tribunal recorrido antes da eventual subida ao STJ");
        }
        if (recurso.equals("RECURSO_EXTRAORDINARIO")) {
            alertas.add("recurso extraordinário exige ofensa constitucional direta, repercussão geral e filtro inicial da presidência ou vice-presidência do tribunal recorrido");
        }
        if (recurso.equals("EMBARGOS_DECLARACAO") && request.pretendeEfeitoInfringente()) {
            alertas.add("efeito infringente exige fundamentação apta e tende a demandar contraditório prévio");
        }
        if (recurso.equals("AGRAVO_EM_RECURSO_ESPECIAL_EXTRAORDINARIO") && request.inadmissaoRecursoExcepcional()) {
            alertas.add("o agravo excepcional ficou ativo porque o cenário informou inadmissão do recurso especial ou extraordinário");
        }
        if (recurso.equals("EMBARGOS_DIVERGENCIA") && request.divergenciaJurisprudencialInterna()) {
            alertas.add("divergência interna qualificada exige acórdão paradigma idôneo e cotejo analítico estrito");
        }
        if (recurso.equals("APELACAO") && response.admiteRecursoAdesivo()) {
            alertas.add(response.observacaoRecursoAdesivo());
        }
        if (request.desejaSustentacaoOral() && recursoExigeTrilhaColegiada(recurso)) {
            alertas.add("sustentação oral foi marcada como necessária; a peça já deve nascer pronta para a pauta colegiada");
        }
        return alertas;
    }

    private static boolean recursoExigeTrilhaColegiada(String recurso) {
        return switch (recurso) {
            case "APELACAO", "RECURSO_INOMINADO", "AGRAVO_INTERNO", "RECURSO_ESPECIAL", "RECURSO_EXTRAORDINARIO", "AGRAVO_EM_RECURSO_ESPECIAL_EXTRAORDINARIO", "EMBARGOS_DIVERGENCIA" -> true;
            default -> false;
        };
    }

    private static RecursalAutomationWorkspaceChecklistItemView item(String code, String description, boolean required) {
        return new RecursalAutomationWorkspaceChecklistItemView(code, description, required);
    }
}
