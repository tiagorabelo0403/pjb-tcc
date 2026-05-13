package com.tcc.pjb.backend.service.innovation;

import com.tcc.pjb.backend.model.dto.innovation.PjbMigrationHygienePreviewRequest;
import com.tcc.pjb.backend.model.dto.innovation.PjbMigrationHygienePreviewResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PjbMigrationHygieneService {

    public PjbMigrationHygienePreviewResponse preview(PjbMigrationHygienePreviewRequest request) {
        PjbMigrationHygienePreviewRequest safe = request == null
                ? new PjbMigrationHygienePreviewRequest("DESCONHECIDO", false, false, false, false, false, false, true, false, false, 0, false)
                : request;
        ArrayList<String> blockers = new ArrayList<>();
        ArrayList<String> sanitationActions = new ArrayList<>();
        ArrayList<String> automationOpportunities = new ArrayList<>();

        if (safe.pendingSignatures()) {
            blockers.add("assinaturas pendentes");
            sanitationActions.add("forçar regularização de assinatura antes da migração");
        }
        if (safe.hearingScheduled()) {
            blockers.add("audiência agendada");
            sanitationActions.add("reprogramar ou congelar a agenda antes da mudança de trilha");
        }
        if (safe.judgmentScheduled()) {
            blockers.add("julgamento agendado");
            sanitationActions.add("sincronizar agenda colegiada antes da redistribuição");
        }
        if (safe.openDeadlines()) {
            blockers.add("prazo em aberto");
            sanitationActions.add("encerrar ou registrar a transição com ciência processual");
        }
        if (safe.pendingTribunalAppeals()) {
            blockers.add("recurso pendente no tribunal");
            sanitationActions.add("vincular o feito ao trilho colegiado antes da migração de origem");
        }
        if (safe.missingNationalIds()) {
            blockers.add("parte sem CPF ou CNPJ consistente");
            sanitationActions.add("saneamento cadastral obrigatório das partes");
        }
        if (!safe.tpuClassificationConsistent()) {
            blockers.add("classificação TPU inconsistente");
            sanitationActions.add("normalização de classe, assunto e rito antes da conversão");
        }
        if (safe.suspended()) {
            sanitationActions.add("validar se a suspensão deve permanecer após a migração");
        }
        if (safe.archived()) {
            sanitationActions.add("verificar se o acervo será apenas espelhado ou reaberto");
        }
        if (safe.mediaCount() > 0) {
            automationOpportunities.add("indexar mídia processual e eventos relevantes automaticamente");
        }
        if (safe.collegiateCase()) {
            automationOpportunities.add("pré-vincular a trilha de gabinete, pauta e acórdão");
        }
        automationOpportunities.add("gerar certidão de prontidão e checklist de saneamento");
        automationOpportunities.add("apontar automaticamente a mesa operacional de destino");

        int score = 100 - blockers.size() * 18 - sanitationActions.size() * 4;
        String readiness;
        if (!blockers.isEmpty()) {
            readiness = "BLOCKED";
        } else if (score < 85 || !sanitationActions.isEmpty()) {
            readiness = "READY_WITH_ATTENTION";
        } else {
            readiness = "READY";
        }
        String suggestedJourney = safe.collegiateCase() ? "TRIBUNAL_COLLEGIATE_SECRETARIAT" : "FIRST_INSTANCE_SECRETARIAT";
        return new PjbMigrationHygienePreviewResponse(
                normalizeSystem(safe.sourceSystem()),
                readiness,
                List.copyOf(blockers),
                List.copyOf(sanitationActions),
                List.copyOf(automationOpportunities),
                suggestedJourney,
                Math.max(0, Math.min(100, score))
        );
    }

    private String normalizeSystem(String value) {
        String text = Objects.toString(value, "DESCONHECIDO").trim();
        if (text.isEmpty()) {
            return "DESCONHECIDO";
        }
        return text.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}
