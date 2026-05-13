package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.service.exception.enums.TipoViolacaoTeto;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralEconomicGateFactory {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final NationalProceduralRoutingMessages messages;

    public NationalProceduralEconomicGateFactory(NationalProceduralRoutingMessages messages) {
        this.messages = Objects.requireNonNull(messages);
    }

    public ProceduralEconomicGateReport build(BigDecimal valorCausa,
                                              TipoJustica tipoJustica,
                                              String ritoSugerido,
                                              String proceduralTrack,
                                              TetoProcessualService.DiagnosticoTetoProcessual teto,
                                              String actionNature,
                                              String actionFamily,
                                              boolean admiteJuizado,
                                              Collection<String> missingInputs,
                                              Collection<String> reviewChecklist,
                                              Collection<String> blockingIssues,
                                              boolean requiresEconomicValue) {
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        LinkedHashSet<String> rerouteOptions = new LinkedHashSet<>();
        LinkedHashSet<String> checklist = new LinkedHashSet<>(reviewChecklist == null ? Set.of() : reviewChecklist);
        BigDecimal valor = valorCausa == null ? ZERO : valorCausa.setScale(2, RoundingMode.HALF_UP);
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("valorCausa", valor);
        metrics.put("limiteLegal", teto.limiteLegal());
        metrics.put("valorMinimoLegal", teto.valorMinimoLegal());
        metrics.put("excedente", teto.excedente());
        metrics.put("margemRestante", teto.margemRestante());
        metrics.put("percentualExcesso", teto.percentualExcesso());
        metrics.put("quantidadeSalariosLimite", teto.quantidadeSalariosLimite());
        metrics.put("salaryReference", teto.salarioMinimoReferencia());
        String thresholdKind = teto.tipoViolacao() != null ? teto.tipoViolacao().name() : "SEM_TETO_APLICAVEL";
        String economicBand;
        if (valor.compareTo(ZERO) <= 0 && (admiteJuizado || requiresEconomicValue)) {
            economicBand = "VALOR_NAO_INFORMADO";
            reasons.add(messages.economicValueMissingReason());
        } else if (teto.bloqueante()) {
            economicBand = "EXCEDIDO";
            reasons.add(messages.economicExceededReason(teto.fundamentoLegal()));
        } else if (teto.alerta()) {
            economicBand = "PROXIMO_TETO";
            reasons.add(messages.economicNearThresholdReason());
        } else if (teto.tipoViolacao() != null) {
            economicBand = "DENTRO_FAIXA";
            reasons.add(messages.economicCompatibleReason());
        } else {
            economicBand = "SEM_TETO_APLICAVEL";
            reasons.add(messages.economicNoThresholdReason());
        }
        if (teto.sugestaoOperacional() != null && !teto.sugestaoOperacional().isBlank()) {
            reasons.add(teto.sugestaoOperacional());
        }
        if (teto.bloqueante()) {
            TipoViolacaoTeto tipoViolacao = teto.tipoViolacao();
            if (tipoViolacao == null) {
                tipoViolacao = TipoViolacaoTeto.ALCADA_JUIZADO_ESPECIAL;
            }
            switch (tipoViolacao) {
                case ALCADA_JUIZADO_ESPECIAL -> rerouteOptions.add(tipoJustica == TipoJustica.FEDERAL ? messages.rerouteFederalCommon() : messages.rerouteStateCommon());
                case ALCADA_JUIZADO_ESPECIAL_FEDERAL -> rerouteOptions.add(RamoDireito.PREVIDENCIARIO.name().equalsIgnoreCase(actionFamily) || containsAny(ritoSugerido, "PREVIDENCIARIO") ? messages.rerouteFederalPrevidenciarioCommon() : messages.rerouteFederalCommon());
                case ALCADA_FAZENDA_PUBLICA -> rerouteOptions.add(messages.rerouteFazendaComum());
                case ALCADA_TRABALHISTA_SUMARISSIMO -> rerouteOptions.add(messages.rerouteTrabalhistaOrdinario());
                default -> rerouteOptions.add(messages.rerouteGenericReview());
            }
            if (teto.competenciaSugerida() != null && !teto.competenciaSugerida().isBlank()) {
                rerouteOptions.add(messages.rerouteCompetenciaContingencia(teto.competenciaSugerida()));
            }
            if (teto.ritoSugerido() != null && !teto.ritoSugerido().isBlank()) {
                rerouteOptions.add(messages.rerouteRitoContingencia(teto.ritoSugerido()));
            }
        }
        if (missingInputs != null && !missingInputs.isEmpty()) {
            checklist.add(messages.economicInputsChecklist(missingInputs));
        }
        if (blockingIssues != null && !blockingIssues.isEmpty()) {
            checklist.addAll(blockingIssues);
        }
        return new ProceduralEconomicGateReport(
                Instant.now(),
                thresholdKind,
                teto.codigoDiagnostico(),
                economicBand,
                !teto.bloqueante(),
                teto.bloqueante(),
                teto.alerta(),
                firstNonBlank(ritoSugerido, proceduralTrack),
                teto.competenciaSugerida(),
                teto.ritoSugerido(),
                teto.anoReferencia(),
                teto.sugestaoOperacional(),
                java.util.List.copyOf(reasons),
                java.util.List.copyOf(rerouteOptions),
                java.util.List.copyOf(checklist),
                Map.copyOf(metrics)
        );
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static boolean containsAny(String value, String... keys) {
        if (value == null || value.isBlank() || keys == null) {
            return false;
        }
        String normalized = value.toUpperCase();
        for (String key : keys) {
            if (key != null && !key.isBlank() && normalized.contains(key.toUpperCase())) {
                return true;
            }
        }
        return false;
    }
}
