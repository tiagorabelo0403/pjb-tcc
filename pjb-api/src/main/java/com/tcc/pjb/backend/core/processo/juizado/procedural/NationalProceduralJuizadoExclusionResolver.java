package com.tcc.pjb.backend.core.processo.juizado.procedural;

import com.tcc.pjb.backend.core.procedural.NationalProceduralActionProfile;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralJuizadoExclusionResolver {

    private static final String[] NATUREZAS = {"MANDADO_SEGURANCA", "HABEAS_CORPUS", "IMPROBIDADE_ADMINISTRATIVA",
            "DESAPROPRIACAO", "ACAO_CIVIL_PUBLICA", "ACAO_POPULAR", "DEMARCACAO", "INSOLVENCIA_EMPRESARIAL", "EXECUCAO_FISCAL", "TRIBUNAL_DO_JURI",
            "PROCESSO_PENAL_MILITAR", "AIJE", "AIME", "REGISTRO_CANDIDATURA"};
    private static final String[] FAMILIAS = {"TRABALHISTA", "ELEITORAL", "MILITAR", "PENAL"};
    static final List<String> NATUREZAS_EXCLUIDAS = List.of(NATUREZAS);
    static final List<String> FAMILIAS_EXCLUIDAS = List.of(FAMILIAS);

    private final NationalProceduralJuizadoDecisionMessages messages;

    public NationalProceduralJuizadoExclusionResolver(NationalProceduralJuizadoDecisionMessages messages) {
        this.messages = Objects.requireNonNull(messages);
    }

    Optional<NationalProceduralJuizadoDecision> resolve(NationalProceduralJuizadoDecisionContext context) {
        Objects.requireNonNull(context);
        NationalProceduralActionProfile actionProfile = Objects.requireNonNull(context.actionProfile());
        if (NationalProceduralJuizadoDecisionSupport.containsAny(actionProfile.actionNature(), NATUREZAS)) {
            LinkedHashSet<String> alerts = new LinkedHashSet<>();
            LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();
            alerts.add(messages.excludedBySpecialNatureAlert());
            reviewChecklist.add(messages.excludedBySpecialNatureChecklist());
            return Optional.of(NationalProceduralJuizadoDecisionSupport.decision(false, null, null, null, alerts, reviewChecklist, 0.92d, true));
        }
        if (NationalProceduralJuizadoDecisionSupport.containsAny(actionProfile.actionFamily(), FAMILIAS)
                && !"INFRACAO_MENOR_POTENCIAL".equals(actionProfile.actionNature())) {
            return Optional.of(NationalProceduralJuizadoDecisionSupport.decision(false, null, null, null, null, null, 0.94d, false));
        }
        return Optional.empty();
    }
}
