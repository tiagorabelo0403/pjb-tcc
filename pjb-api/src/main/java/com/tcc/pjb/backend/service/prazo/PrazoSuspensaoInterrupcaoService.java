package com.tcc.pjb.backend.service.prazo;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PrazoSuspensaoInterrupcaoService {

    public sealed interface EventoPrazo permits
            EventoPrazo.Suspensao,
            EventoPrazo.Interrupcao,
            EventoPrazo.Retomada,
            EventoPrazo.Prorrogacao {

        record Suspensao(UUID prazoId, LocalDate dataInicio,
                String motivo) implements EventoPrazo {}

        record Interrupcao(UUID prazoId, LocalDate data,
                String ato) implements EventoPrazo {}

        record Retomada(UUID prazoId, LocalDate data,
                boolean reinicarDoZero) implements EventoPrazo {}

        record Prorrogacao(UUID prazoId, int diasAdicionais,
                String autorizadoPor) implements EventoPrazo {}
    }

    public record AjusteSnapshot(
            UUID prazoId,
            PrazoSituacao situacaoAnterior,
            PrazoSituacao situacaoAtual,
            LocalDate novoVencimento,
            String descricaoAjuste
    ) {}

    public AjusteSnapshot aplicar(EventoPrazo evento, LocalDate vencimentoAtual,
            CalendarioUteisService.CalendarioInput cal,
            CalendarioUteisService calendarioService) {
        return switch (evento) {
            case EventoPrazo.Suspensao s -> new AjusteSnapshot(
                    s.prazoId(), PrazoSituacao.ABERTO, PrazoSituacao.SUSPENSO,
                    vencimentoAtual,
                    "Prazo suspenso: " + s.motivo());
            case EventoPrazo.Interrupcao i -> new AjusteSnapshot(
                    i.prazoId(), PrazoSituacao.ABERTO, PrazoSituacao.INTERROMPIDO,
                    vencimentoAtual,
                    "Prazo interrompido pelo ato: " + i.ato() + " (recontagem do início).");
            case EventoPrazo.Retomada r -> {
                LocalDate novoV = r.reinicarDoZero()
                        ? calendarioService.proximoDiaUtil(r.data(), cal)
                        : vencimentoAtual;
                yield new AjusteSnapshot(
                        r.prazoId(), PrazoSituacao.SUSPENSO, PrazoSituacao.ABERTO,
                        novoV, "Prazo retomado em " + r.data() + ".");
            }
            case EventoPrazo.Prorrogacao p -> {
                LocalDate prorrogado = calendarioService.calcularVencimento(
                        vencimentoAtual, p.diasAdicionais(), cal);
                yield new AjusteSnapshot(
                        p.prazoId(), PrazoSituacao.ABERTO, PrazoSituacao.PRORROGADO,
                        prorrogado,
                        "Prazo prorrogado por " + p.diasAdicionais()
                                + " dias úteis. Autorizado por: " + p.autorizadoPor());
            }
        };
    }
}
