package com.tcc.pjb.backend.service.processual.movimentacao;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoAdjustmentMode;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;

@Service
public class MovimentacaoComplianceReviewService {

    public ReviewResult review(MovimentacaoProcessual movimentacao,
                               MovimentacaoAdjustmentMode mode,
                               String motivo,
                               String descricaoSubstitutiva) {
        List<String> flags = new ArrayList<>();
        int score = 100;
        if (motivo == null || motivo.isBlank() || motivo.trim().length() < 20) {
            flags.add("MOTIVO_INSUFICIENTE");
            score -= 60;
        }
        if (mode == MovimentacaoAdjustmentMode.DESCONSIDERACAO_LOGICA) {
            flags.add("AJUSTE_COM_MODO_SENSIVEL");
            score -= 15;
        }
        if (descricaoSubstitutiva != null && descricaoSubstitutiva.length() > 4000) {
            flags.add("DESCRICAO_SUBSTITUTIVA_EXTENSA");
            score -= 10;
        }
        if (movimentacao.getDataMovimentacao() == null) {
            flags.add("SEM_DATA_ORIGINAL");
            score -= 5;
        }
        String verdict = score >= 60 ? "APROVADO_AUTOMATICAMENTE" : "REJEITADO_AUTOMATICAMENTE";
        return new ReviewResult(score, verdict, List.copyOf(flags), score >= 60);
    }

    public record ReviewResult(int score,
                               String verdict,
                               List<String> flags,
                               boolean approved) {
    }
}
