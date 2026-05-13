package com.tcc.pjb.backend.core.security;

import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceDecisionSnapshot;
import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceHealthQuery;
import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceHealthResult;
import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceLevelView;
import com.tcc.pjb.backend.core.security.domain.GovBrAssurancePolicyView;
import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceQuery;
import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceResult;
import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceTimelineEntry;
import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceTimelineResult;
import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceWindowView;
import com.tcc.pjb.backend.core.security.domain.GovBrSensitiveActAssessment;
import com.tcc.pjb.backend.core.security.domain.GovBrSensitiveActAssessmentResult;
import com.tcc.pjb.backend.core.security.domain.GovBrSensitiveActView;
import com.tcc.pjb.backend.core.security.domain.GovBrStepUpDecisionView;
import com.tcc.pjb.backend.core.security.domain.GovBrStepUpQuery;
import com.tcc.pjb.backend.core.security.domain.GovBrStepUpRequirement;
import com.tcc.pjb.backend.core.security.domain.GovBrStepUpResult;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class GovBrAssurancePolicy {

    private static final String NIVEL_MINIMO_ATO_SENSIVEL = "ouro";
    private static final String NIVEL_MINIMO_ATO_NORMAL = "prata";
    private static final BigDecimal CORTE_VALOR_SENSIVEL = new BigDecimal("50000");

    public GovBrAssuranceResult evaluate(GovBrAssuranceQuery query) {
        String nivelAtual = query == null ? null : query.nivelAtual();
        boolean atoSensivel = query != null && query.atoSensivel();
        String nivelRequerido = atoSensivel ? NIVEL_MINIMO_ATO_SENSIVEL : NIVEL_MINIMO_ATO_NORMAL;
        boolean atendido = atoNormatizadoAtendido(nivelAtual, atoSensivel);
        return new GovBrAssuranceResult(nivelAtual, nivelRequerido, atendido, !atendido);
    }

    public GovBrSensitiveActAssessmentResult assess(GovBrSensitiveActAssessment assessment) {
        boolean atoSensivel = assessment != null && assessment.atoSensivel();
        String nivelAtual = assessment == null ? null : assessment.nivelAtual();
        String nivelRequerido = atoSensivel ? NIVEL_MINIMO_ATO_SENSIVEL : NIVEL_MINIMO_ATO_NORMAL;
        boolean permitido = atoNormatizadoAtendido(nivelAtual, atoSensivel);
        return new GovBrSensitiveActAssessmentResult(assessment == null ? null : assessment.tipoAto(), permitido, !permitido, nivelRequerido);
    }

    public GovBrStepUpRequirement requirement(String nivelAtual, boolean atoSensivel) {
        String nivelRequerido = atoSensivel ? NIVEL_MINIMO_ATO_SENSIVEL : NIVEL_MINIMO_ATO_NORMAL;
        return new GovBrStepUpRequirement(nivelAtual, nivelRequerido, exigeStepUp(nivelAtual, atoSensivel));
    }

    public GovBrAssuranceLevelView levelView(String nivelAtual) {
        int ordem = nivelOrdem(nivelAtual);
        return new GovBrAssuranceLevelView(nivelAtual, ordem, ordem > 0);
    }

    public GovBrAssuranceDecisionSnapshot snapshot(String nivelAtual, boolean atoSensivel) {
        boolean permitido = atoNormatizadoAtendido(nivelAtual, atoSensivel);
        return new GovBrAssuranceDecisionSnapshot(nivelAtual, atoSensivel, permitido, !permitido, Instant.now());
    }

    public GovBrAssuranceHealthResult health(GovBrAssuranceHealthQuery query) {
        String nivelAtual = query == null ? null : query.nivelAtual();
        return new GovBrAssuranceHealthResult(true, atoNormatizadoAtendido(nivelAtual, true), atoNormatizadoAtendido(nivelAtual, false), Instant.now());
    }

    public GovBrAssuranceWindowView windowView(String nivelAtual, boolean atoSensivel) {
        String nivelRequerido = atoSensivel ? NIVEL_MINIMO_ATO_SENSIVEL : NIVEL_MINIMO_ATO_NORMAL;
        return new GovBrAssuranceWindowView(nivelAtual, nivelRequerido, atoNormatizadoAtendido(nivelAtual, atoSensivel), Instant.now());
    }

    public GovBrAssurancePolicyView policyView() {
        return new GovBrAssurancePolicyView(NIVEL_MINIMO_ATO_NORMAL, NIVEL_MINIMO_ATO_SENSIVEL, Instant.now());
    }

    public GovBrSensitiveActView sensitiveActView(GovBrSensitiveActAssessment assessment) {
        boolean sensivel = assessment != null && assessment.atoSensivel();
        return new GovBrSensitiveActView(assessment == null ? null : assessment.tipoAto(), sensivel, sensivel ? NIVEL_MINIMO_ATO_SENSIVEL : NIVEL_MINIMO_ATO_NORMAL);
    }

    public GovBrStepUpDecisionView stepUpDecisionView(String nivelAtual, boolean atoSensivel) {
        String nivelRequerido = atoSensivel ? NIVEL_MINIMO_ATO_SENSIVEL : NIVEL_MINIMO_ATO_NORMAL;
        return new GovBrStepUpDecisionView(nivelAtual, nivelRequerido, exigeStepUp(nivelAtual, atoSensivel));
    }

    public GovBrStepUpResult stepUp(GovBrStepUpQuery query) {
        String nivelAtual = query == null ? null : query.nivelAtual();
        boolean atoSensivel = query != null && query.atoSensivel();
        String nivelRequerido = atoSensivel ? NIVEL_MINIMO_ATO_SENSIVEL : NIVEL_MINIMO_ATO_NORMAL;
        return new GovBrStepUpResult(nivelAtual, nivelRequerido, exigeStepUp(nivelAtual, atoSensivel));
    }

    public GovBrAssuranceTimelineResult timeline(String nivelAtual, boolean atoSensivel) {
        boolean permitido = atoNormatizadoAtendido(nivelAtual, atoSensivel);
        Instant now = Instant.now();
        return new GovBrAssuranceTimelineResult(List.of(
                new GovBrAssuranceTimelineEntry("received", nivelAtual, permitido, now),
                new GovBrAssuranceTimelineEntry("evaluated", nivelAtual, permitido, now)
        ));
    }

    public boolean atoNormatizadoAtendido(String nivelAtual, boolean atoSensivel) {
        if (nivelAtual == null || nivelAtual.isBlank()) {
            return false;
        }
        String requerido = atoSensivel ? NIVEL_MINIMO_ATO_SENSIVEL : NIVEL_MINIMO_ATO_NORMAL;
        return nivelOrdem(nivelAtual) >= nivelOrdem(requerido);
    }

    public boolean exigeStepUp(String nivelAtual, boolean atoSensivel) {
        return !atoNormatizadoAtendido(nivelAtual, atoSensivel);
    }

    public boolean isSensitiveCitizenAct(Processo processo, String justificativa) {
        if (processo == null) {
            return containsSensitiveMarker(justificativa);
        }
        boolean infancia = processo.getRamoDireito() == RamoDireito.INFANCIA_JUVENTUDE;
        boolean incapaz = containsSensitiveMarker(join(
                processo.getClasseProcessual(),
                processo.getAssunto(),
                processo.getObjetoProcessual(),
                processo.getPedidoPrincipal(),
                justificativa
        ));
        boolean fazenda = containsTreasuryMarker(join(
                processo.getClasseProcessual(),
                processo.getAssunto(),
                processo.getObjetoProcessual(),
                justificativa
        ));
        boolean acordoElevado = hasHighValueSettlement(processo, justificativa);
        return infancia || incapaz || fazenda || acordoElevado;
    }

    private boolean hasHighValueSettlement(Processo processo, String justificativa) {
        BigDecimal valor = processo.getValorCausa();
        if (valor == null || valor.compareTo(CORTE_VALOR_SENSIVEL) < 0) {
            return false;
        }
        String corpus = join(processo.getJanelaAcordoResumo(), processo.getObjetoProcessual(), processo.getPedidoPrincipal(), justificativa);
        return containsAny(normalizeText(corpus), "acordo", "transacao", "transacao judicial", "homologacao", "composicao");
    }

    private boolean containsSensitiveMarker(String value) {
        String normalized = normalizeText(value);
        return containsAny(normalized,
                "incapaz",
                "curatela",
                "interdicao",
                "interdicao civil",
                "tutela",
                "guarda",
                "curador",
                "curadoria",
                "menor",
                "adolescente",
                "crianca"
        );
    }

    private boolean containsTreasuryMarker(String value) {
        String normalized = normalizeText(value);
        return containsAny(normalized,
                "fazenda publica",
                "execucao fiscal",
                "execucao contra fazenda",
                "precatorio",
                "rpv",
                "ente publico",
                "procuradoria",
                "tributario"
        );
    }

    private boolean containsAny(String value, String... markers) {
        if (value == null || value.isBlank() || markers == null) {
            return false;
        }
        for (String marker : markers) {
            if (marker != null && !marker.isBlank() && value.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private String join(String... values) {
        StringBuilder out = new StringBuilder();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    if (out.length() > 0) {
                        out.append(' ');
                    }
                    out.append(value.trim());
                }
            }
        }
        return out.toString();
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        return Objects.toString(normalized, "");
    }

    private int nivelOrdem(String nivel) {
        String normalized = normalizeText(nivel);
        return switch (normalized) {
            case "ouro", "gold", "loa3", "3" -> 3;
            case "prata", "silver", "loa2", "2" -> 2;
            case "bronze", "loa1", "1" -> 1;
            default -> 0;
        };
    }
}
