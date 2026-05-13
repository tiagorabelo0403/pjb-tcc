package com.tcc.pjb.backend.core.procedural;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;

@Component
public class NationalProceduralReviewEconomicRequirementResolver {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final NationalProceduralReviewMessages messages;

    public NationalProceduralReviewEconomicRequirementResolver(NationalProceduralReviewMessages messages) {
        this.messages = Objects.requireNonNull(messages);
    }

    NationalProceduralReviewInputSlice assess(Map<String, Object> payload,
                                              NationalProceduralJuizadoDecision juizadoDecision,
                                              NationalProceduralActionProfile actionProfile) {
        Map<String, Object> safePayload = payload == null ? Map.of() : payload;
        LinkedHashSet<String> missingInputs = new LinkedHashSet<>();
        LinkedHashSet<String> blockingIssues = new LinkedHashSet<>();
        BigDecimal valorCausaInformado = NationalProceduralRoutingSupport.decimal(safePayload.get("valorCausa"));
        if ((valorCausaInformado == null || valorCausaInformado.compareTo(ZERO) <= 0)
                && (juizadoDecision.admiteJuizado() || requiresEconomicValue(actionProfile.actionNature()))) {
            missingInputs.add("valorCausa");
            blockingIssues.add(messages.missingValorBlocking());
        }
        return new NationalProceduralReviewInputSlice(List.copyOf(missingInputs), List.copyOf(blockingIssues));
    }

    private static boolean requiresEconomicValue(String actionNature) {
        return NationalProceduralRoutingSupport.containsAny(
                actionNature,
                "OBRIGACAO_DE_FAZER",
                "COBRANCA_REPETICAO",
                "INDENIZATORIA",
                "PREVIDENCIARIO",
                "RECLAMACAO_TRABALHISTA",
                "FAZENDA_PUBLICA",
                "EXECUCAO_FISCAL"
        );
    }
}
