package com.tcc.pjb.backend.core.kernel.advisory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;

@Service
public class SettlementIntelligenceService {

    public NegotiationWindowReport analyze(Processo processo, BigDecimal requestedAmount, List<String> negotiationSignals) {
        Objects.requireNonNull(processo, "processo");
        BigDecimal base = positive(requestedAmount != null ? requestedAmount : processo.getValorCausa());
        LinkedHashSet<String> leverage = new LinkedHashSet<>();
        LinkedHashSet<String> risks = new LinkedHashSet<>();
        LinkedHashSet<String> recommendations = new LinkedHashSet<>();
        double score = 0.55d;

        if (processo.getValorCausa() != null && processo.getValorCausa().compareTo(BigDecimal.ZERO) > 0) {
            leverage.add("Há valor de causa definido para calibrar a faixa negocial.");
            score += 0.10d;
        }
        if (processo.getFaseAtual() != null) {
            switch (processo.getFaseAtual()) {
                case CONHECIMENTO -> {
                    leverage.add("A fase de conhecimento ainda favorece composição estratégica antes do adensamento do litígio.");
                    score += 0.08d;
                }
                case EXECUCAO, CUMPRIMENTO_SENTENCA -> {
                    leverage.add("A fase executiva aumenta pressão de encerramento e pode favorecer acordo resolutivo.");
                    score += 0.06d;
                }
                default -> score += 0.02d;
            }
        }
        if (processo.getStatusProcesso() == StatusProcesso.CONCLUSO || processo.getStatusProcesso() == StatusProcesso.AGUARDANDO_PARECER) {
            risks.add("O processo está concluso, o que reduz a janela negocial e aumenta risco de decisão próxima.");
            recommendations.add("Priorizar proposta objetiva e imediatamente executável, com protocolo urgente.");
            score -= 0.12d;
        }
        if (processo.getFaseAtual() == FaseProcessual.RECURSAL) {
            risks.add("A fase recursal tende a elevar rigidez estratégica e custo de reversibilidade da proposta.");
            recommendations.add("Amarrar propostas condicionadas com cláusulas de desistência recursal e quitação delimitada.");
            score -= 0.08d;
        }
        if (negotiationSignals != null) {
            for (String signal : negotiationSignals) {
                if (signal != null && !signal.isBlank()) {
                    leverage.add(signal.trim());
                }
            }
        }

        BigDecimal piso = percent(base, 0.62d);
        BigDecimal alvo = percent(base, 0.78d);
        BigDecimal teto = percent(base, 0.94d);
        if (base.compareTo(BigDecimal.ZERO) == 0) {
            piso = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            alvo = piso;
            teto = piso;
            risks.add("Não há base econômica consistente para calibragem da faixa de acordo.");
            recommendations.add("Materializar valor econômico mínimo do litígio antes da rodada negocial.");
            score -= 0.18d;
        }
        if (requestedAmount != null && requestedAmount.compareTo(teto) > 0) {
            risks.add("O valor pretendido supera a faixa de acordo sugerida para o estágio atual do caso.");
            recommendations.add("Avaliar proposta escalonada ou composição por etapas para não travar a negociação.");
            score -= 0.10d;
        }

        String status = score >= 0.82d ? "FAVORAVEL" : score >= 0.62d ? "ESTAVEL" : score >= 0.45d ? "CAUTELOSA" : "HOSTIL";
        if (recommendations.isEmpty()) {
            recommendations.add("Sustentar a negociação com narrativa de ganho de tempo, previsibilidade e executabilidade.");
        }
        return new NegotiationWindowReport(status, round(score), piso, alvo, teto, List.copyOf(leverage), List.copyOf(risks), List.copyOf(recommendations));
    }

    public NegotiationWindowReport analyze(PropostaAcordo proposta) {
        Objects.requireNonNull(proposta, "proposta");
        List<String> signals = new ArrayList<>();
        if (proposta.getStatus() != null) {
            signals.add("Status atual da proposta: " + proposta.getStatus().name());
        }
        if (proposta.getDataCriacao() != null) {
            signals.add("Há trilha temporal materializada para mensurar inércia ou urgência da negociação.");
        }
        return analyze(proposta.getProcesso(), proposta.getValorAcordo(), signals);
    }

    private BigDecimal positive(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(BigDecimal base, double fraction) {
        return base.multiply(BigDecimal.valueOf(fraction)).setScale(2, RoundingMode.HALF_UP);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
