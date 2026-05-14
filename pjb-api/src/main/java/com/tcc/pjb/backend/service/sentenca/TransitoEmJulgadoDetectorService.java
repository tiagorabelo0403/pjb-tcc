package com.tcc.pjb.backend.service.sentenca;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TransitoEmJulgadoDetectorService {

    public record TransitoInput(
            UUID processoId,
            SentencaTipo tipo,
            LocalDate dataSentenca,
            LocalDate dataUltimaPublicacao,
            boolean recursoPendente,
            boolean prazosRecursaisEsgotados,
            boolean renunciaExpressaRecurso,
            boolean fazendaPublica,
            boolean ministerioPublico,
            boolean defensoriaPublica
    ) {}

    public record TransitoResult(
            boolean transitado,
            LocalDate dataTransito,
            String fundamentacao
    ) {}

    public TransitoResult detectar(TransitoInput input) {
        if (!input.tipo().transitaEmJulgado()) {
            return new TransitoResult(false, null,
                    "Decisão do tipo " + input.tipo() + " não sujeita a trânsito em julgado.");
        }
        if (input.recursoPendente()) {
            return new TransitoResult(false, null,
                    "Recurso pendente — trânsito em julgado suspenso (art. 502, CPC).");
        }
        if (input.renunciaExpressaRecurso()) {
            return new TransitoResult(true, input.dataSentenca(),
                    "Trânsito em julgado por renúncia expressa ao recurso (art. 999, CPC).");
        }
        if (input.prazosRecursaisEsgotados()) {
            LocalDate dataTransito = calcularDataTransito(input);
            return new TransitoResult(true, dataTransito,
                    "Trânsito em julgado pelo esgotamento do prazo recursal (art. 502, CPC).");
        }
        return new TransitoResult(false, null,
                "Prazo recursal em curso — trânsito em julgado não verificado.");
    }

    private LocalDate calcularDataTransito(TransitoInput input) {
        if (input.dataUltimaPublicacao() == null) return input.dataSentenca();
        int prazo = 15;
        if (input.fazendaPublica() || input.ministerioPublico()) prazo = 60;
        else if (input.defensoriaPublica()) prazo = 30;
        return input.dataUltimaPublicacao().plusDays(prazo);
    }
}
