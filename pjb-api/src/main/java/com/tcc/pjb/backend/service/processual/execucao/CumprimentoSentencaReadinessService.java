package com.tcc.pjb.backend.service.processual.execucao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CumprimentoSentencaReadinessService {

    public record CumprimentoInput(
            UUID processoId,
            BigDecimal valorCondenacao,
            boolean liquidacaoRealizada,
            boolean transitadoEmJulgado,
            boolean integerimaNoticias,
            boolean devedorIntimado,
            boolean prazoVoluntarioPago,
            boolean garantiaOferecida,
            boolean multa10PercentAplicada
    ) {}

    public record CumprimentoSnapshot(
            UUID processoId,
            boolean aptaParaCumprimento,
            BigDecimal multaDevida,
            List<String> etapas,
            List<String> pendencias,
            String proximaProvidencia
    ) {}

    public CumprimentoSnapshot avaliar(CumprimentoInput input) {
        List<String> etapas = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();
        if (!input.transitadoEmJulgado()) {
            pendencias.add("Aguardar trânsito em julgado.");
        }
        if (!input.liquidacaoRealizada() && input.valorCondenacao() == null) {
            pendencias.add("Liquidação de sentença necessária antes do cumprimento.");
        }
        if (input.transitadoEmJulgado() && !input.devedorIntimado()) {
            etapas.add("Intimar devedor para pagamento voluntário em 15 dias (CPC, art. 523).");
        }
        BigDecimal multa = BigDecimal.ZERO;
        if (input.devedorIntimado() && !input.prazoVoluntarioPago()) {
            multa = input.valorCondenacao() != null
                    ? input.valorCondenacao().multiply(new BigDecimal("0.10"))
                    : BigDecimal.ZERO;
            etapas.add("Aplicar multa de 10% + honorários de 10% (CPC, art. 523, §1º).");
        }
        if (!input.garantiaOferecida() && input.prazoVoluntarioPago()) {
            etapas.add("Verificar necessidade de penhora e avaliação.");
        }
        boolean apta = pendencias.isEmpty();
        String proxima = !apta ? pendencias.get(0)
                : etapas.isEmpty() ? "Cumprimento em curso." : etapas.get(0);
        return new CumprimentoSnapshot(input.processoId(), apta, multa,
                List.copyOf(etapas), List.copyOf(pendencias), proxima);
    }
}
