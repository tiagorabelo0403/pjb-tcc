package com.tcc.pjb.backend.service.prazo;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PrazoProcessualEngine {

    public record PrazoInput(
            UUID processoId,
            RitoProcessual rito,
            PrazoTipo tipo,
            String descricao,
            LocalDate dataInicio,
            int diasUteis,
            boolean intimacaoPessoal,
            boolean fazendaPublica,
            boolean ministerioPublico,
            CalendarioUteisService.CalendarioInput calendario
    ) {}

    public record PrazoSnapshot(
            UUID processoId,
            String descricao,
            PrazoTipo tipo,
            PrazoSituacao situacao,
            LocalDate dataInicio,
            LocalDate dataVencimento,
            int diasUteisTotal,
            long diasUteisRestantes,
            boolean vencido,
            boolean prorrogavel,
            String alertaNivel
    ) {}

    private final CalendarioUteisService calendario;

    public PrazoProcessualEngine(CalendarioUteisService calendario) {
        this.calendario = calendario;
    }

    public PrazoSnapshot calcular(PrazoInput input) {
        int dias = ajustarDiasPorParte(input);
        LocalDate vencimento = calendario.calcularVencimento(
                input.dataInicio(), dias, input.calendario());
        LocalDate hoje = LocalDate.now();
        boolean vencido = hoje.isAfter(vencimento);
        long restantes = vencido ? 0
                : calendario.diasUteisEntre(hoje, vencimento, input.calendario());
        PrazoSituacao situacao = vencido
                ? PrazoSituacao.ENCERRADO : PrazoSituacao.ABERTO;
        return new PrazoSnapshot(
                input.processoId(),
                input.descricao(),
                input.tipo(),
                situacao,
                input.dataInicio(),
                vencimento,
                dias,
                restantes,
                vencido,
                input.tipo().admiteProrogacao(),
                resolverAlerta(restantes, vencido)
        );
    }

    public List<PrazoSnapshot> calcularLote(List<PrazoInput> inputs) {
        return inputs.stream().map(this::calcular).toList();
    }

    private int ajustarDiasPorParte(PrazoInput input) {
        int dias = input.diasUteis();
        if (input.fazendaPublica() || input.ministerioPublico()) {
            dias = dias * 4;
        } else if (input.intimacaoPessoal()) {
            dias = dias * 2;
        }
        return dias;
    }

    private String resolverAlerta(long restantes, boolean vencido) {
        if (vencido) return "CRITICO";
        if (restantes <= 1) return "URGENTE";
        if (restantes <= 3) return "ATENCAO";
        if (restantes <= 7) return "NORMAL";
        return "OK";
    }
}
