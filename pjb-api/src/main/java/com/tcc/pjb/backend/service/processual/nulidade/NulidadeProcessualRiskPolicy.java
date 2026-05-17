package com.tcc.pjb.backend.service.processual.nulidade;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public final class NulidadeProcessualRiskPolicy {

    public NulidadeProcessualRiskPolicy() {}

    public record NulidadeInput(
            boolean intimacaoValida,
            boolean parteComRepresentante,
            boolean sigiloRespeitado,
            boolean prazoCorreto,
            boolean competenciaConfirmada,
            boolean citacaoValida
    ) {}

    public record RiscoNulidade(
            String tipo,
            String descricao,
            boolean critico
    ) {}

    public List<RiscoNulidade> avaliar(NulidadeInput input) {
        List<RiscoNulidade> riscos = new ArrayList<>();
        if (!input.intimacaoValida()) {
            riscos.add(new RiscoNulidade("INTIMACAO_INVALIDA",
                    "Intimação inválida — ato futuro pode ser anulado.", true));
        }
        if (!input.parteComRepresentante()) {
            riscos.add(new RiscoNulidade("SEM_REPRESENTACAO",
                    "Parte sem representante válido — nulidade absoluta possível.", true));
        }
        if (!input.sigiloRespeitado()) {
            riscos.add(new RiscoNulidade("SIGILO_VIOLADO",
                    "Sigilo processual não respeitado — comprometimento da imparcialidade.", true));
        }
        if (!input.prazoCorreto()) {
            riscos.add(new RiscoNulidade("PRAZO_INCORRETO",
                    "Prazo incorreto — risco de preclusão indevida.", false));
        }
        if (!input.competenciaConfirmada()) {
            riscos.add(new RiscoNulidade("COMPETENCIA_DUVIDOSA",
                    "Competência não confirmada — risco de nulidade relativa.", false));
        }
        if (!input.citacaoValida()) {
            riscos.add(new RiscoNulidade("CITACAO_INVALIDA",
                    "Citação inválida — ato de comunicação pode ser nulo.", true));
        }
        return riscos;
    }
}
