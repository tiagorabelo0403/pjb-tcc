package com.tcc.pjb.backend.core.processo.juizado.procedural;

import com.tcc.pjb.backend.core.procedural.NationalProceduralActionProfile;
import com.tcc.pjb.backend.core.procedural.NationalProceduralPartyProfile;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralJuizadoTrackClassifier {

    NationalProceduralJuizadoTrackLane classify(NationalProceduralJuizadoDecisionContext context) {
        Objects.requireNonNull(context);
        NationalProceduralActionProfile actionProfile = Objects.requireNonNull(context.actionProfile());
        NationalProceduralPartyProfile partyProfile = context.partyProfile();
        if ((partyProfile != null && partyProfile.federal()) || equalsIgnoreCase(context.competence().tipoJusticaSugerida(), "FEDERAL")) {
            return NationalProceduralJuizadoTrackLane.FEDERAL;
        }
        if (partyProfile != null && (partyProfile.state() || partyProfile.municipal())) {
            return NationalProceduralJuizadoTrackLane.FAZENDA;
        }
        if (NationalProceduralJuizadoDecisionSupport.containsAny(actionProfile.actionFamily(), "CIVIL_CONSUMO", "CIVIL_GERAL", "CIVIL_OBRIGACOES", "CIVIL_CREDITOS")
                || NationalProceduralJuizadoDecisionSupport.containsAny(actionProfile.actionNature(), "OBRIGACAO_DE_FAZER", "COBRANCA_REPETICAO", "INDENIZATORIA")) {
            return NationalProceduralJuizadoTrackLane.CIVEL;
        }
        if ("INFRACAO_MENOR_POTENCIAL".equals(actionProfile.actionNature())) {
            return NationalProceduralJuizadoTrackLane.CRIMINAL;
        }
        return NationalProceduralJuizadoTrackLane.NONE;
    }

    private static boolean equalsIgnoreCase(String value, String expected) {
        return value != null && expected != null && value.equalsIgnoreCase(expected);
    }
}
