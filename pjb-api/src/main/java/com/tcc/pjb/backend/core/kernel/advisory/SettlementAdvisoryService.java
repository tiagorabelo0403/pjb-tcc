package com.tcc.pjb.backend.core.kernel.advisory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.procedural.ProceduralRitoNames;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;

@Service
public class SettlementAdvisoryService {

    private final SettlementIntelligenceService settlementIntelligenceService;

    public SettlementAdvisoryService(SettlementIntelligenceService settlementIntelligenceService) {
        this.settlementIntelligenceService = Objects.requireNonNull(settlementIntelligenceService);
    }

    public SettlementAdvisoryReport analyze(Processo processo,
                                            String ritoName,
                                            BigDecimal requestedAmount,
                                            List<String> negotiationSignals,
                                            ProcessIntegrityRadarReport integrityRadar) {
        Objects.requireNonNull(processo, "processo");
        NegotiationWindowReport window = settlementIntelligenceService.analyze(processo, requestedAmount, negotiationSignals);
        LinkedHashSet<String> conditionalClauses = new LinkedHashSet<>();
        LinkedHashSet<String> safeguards = new LinkedHashSet<>();
        LinkedHashSet<String> nextMoves = new LinkedHashSet<>(window.recommendations());
        double score = window.score();
        boolean executable = window.favorable();

        conditionalClauses.add("Cláusula de inadimplemento com vencimento antecipado do saldo remanescente.");
        conditionalClauses.add("Multa de mora e correção automática em caso de atraso injustificado.");
        safeguards.add("Prever quitação delimitada por objeto, partes e período litigioso.");
        safeguards.add("Amarrar forma de pagamento, conta de destino e gatilho documental de comprovação.");

        if (processo.getFaseAtual() == FaseProcessual.RECURSAL) {
            conditionalClauses.add("Condição de desistência recursal ou renúncia delimitada, conforme o polo e a estratégia do caso.");
            safeguards.add("Vincular eficácia do acordo à regularização de custas, preparo e atos recursais pendentes.");
            nextMoves.add("Negociar cláusula recursal expressa para evitar controvérsia sobre extinção do recurso ou do cumprimento.");
            score -= 0.05d;
        }
        if (processo.getFaseAtual() == FaseProcessual.EXECUCAO || processo.getFaseAtual() == FaseProcessual.CUMPRIMENTO_SENTENCA || processo.getFaseAtual() == FaseProcessual.PENHORA) {
            conditionalClauses.add("Gatilho de retomada imediata da execução em caso de mora superior ao prazo de tolerância contratual.");
            safeguards.add("Prever garantia mínima, parcelamento curto e identificação do bem/ativo em reforço executivo.");
            nextMoves.add("Levar cronograma curto e executável para reduzir risco de frustração da fase executiva.");
            score += 0.04d;
        }
        if (ProceduralRitoNames.isOneOf(ritoName, "JUIZADO_ESPECIAL", "JUIZADO_ESPECIAL_CIVEL", "JUIZADO_ESPECIAL_FAZENDA_PUBLICA")) {
            safeguards.add("Simplificar cláusulas e manter executabilidade imediata compatível com ambiente conciliatório do juizado.");
            nextMoves.add("Preparar proposta objetiva e de baixa fricção para audiência de conciliação ou tratativa preliminar.");
            score += 0.03d;
        }
        if (integrityRadar != null && integrityRadar.blocking()) {
            executable = false;
            nextMoves.addAll(integrityRadar.nextActions());
            safeguards.add("Suspender fechamento do acordo até remoção de riscos bloqueantes de nulidade, prazo ou defeito procedimental.");
            score -= 0.10d;
        }
        if (requestedAmount != null && window.tetoSugerido() != null && requestedAmount.compareTo(window.tetoSugerido()) > 0) {
            executable = false;
            nextMoves.add("Reescalonar proposta em faixas condicionadas, com entrada realista e marcos objetivos de adimplemento.");
            score -= 0.06d;
        }
        String status = !executable ? "REVIEW_NEGOTIATION_STRUCTURE" : score >= 0.82d ? "EXECUTABLE_STRONG" : score >= 0.68d ? "EXECUTABLE_STABLE" : "EXECUTABLE_WITH_GUARDS";
        return new SettlementAdvisoryReport(
                status,
                round(Math.max(0.0d, Math.min(1.0d, score))),
                executable,
                window,
                List.copyOf(conditionalClauses),
                List.copyOf(safeguards),
                List.copyOf(nextMoves),
                PayloadMaps.ofEntries(
                        "processoId", processo.getId(),
                        "ritoName", ritoName,
                        "phase", processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null,
                        "integrityStatus", integrityRadar != null ? integrityRadar.status() : null,
                        "requestedAmount", requestedAmount
                )
        );
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
