package com.tcc.pjb.backend.service.recurso;

import com.tcc.pjb.backend.service.prazo.CalendarioUteisService;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RecursoTempestividadeGuardService {

    public record TempestividadeInput(
            UUID processoId,
            RecursoProcessualTipo tipo,
            LocalDate dataReferencia,
            boolean fazendaPublica,
            boolean ministerioPublico,
            boolean defensoriaPublica
    ) {}

    public record TempestividadeResult(
            boolean tempestivo,
            LocalDate prazoFinal,
            int diasUteisPrazo,
            int diasUteisRestantes,
            String fundamentacao
    ) {}

    private final CalendarioUteisService calendarioService;

    public RecursoTempestividadeGuardService(CalendarioUteisService calendarioService) {
        this.calendarioService = calendarioService;
    }

    public TempestividadeResult avaliar(TempestividadeInput input,
            CalendarioUteisService.CalendarioInput cal) {
        int diasBase = input.tipo().prazoBaseDias();
        int diasAjustados = ajustarPrazo(diasBase, input.fazendaPublica(),
                input.ministerioPublico(), input.defensoriaPublica());
        LocalDate prazoFinal = calendarioService.calcularVencimento(
                input.dataReferencia(), diasAjustados, cal);
        long restantes = calendarioService.diasUteisEntre(LocalDate.now(), prazoFinal, cal);
        boolean tempestivo = !LocalDate.now().isAfter(prazoFinal);
        return new TempestividadeResult(
                tempestivo,
                prazoFinal,
                diasAjustados,
                (int) Math.max(0, restantes),
                montarFundamentacao(input.tipo(), diasAjustados, tempestivo));
    }

    private int ajustarPrazo(int base, boolean fazenda, boolean mp, boolean defensoria) {
        if (fazenda || mp) return base * 4;
        if (defensoria) return base * 2;
        return base;
    }

    private String montarFundamentacao(RecursoProcessualTipo tipo, int dias, boolean tempestivo) {
        return tipo + " — prazo: " + dias + " dias úteis"
                + (tempestivo ? " — tempestivo" : " — INTEMPESTIVO: verificar justificativa");
    }
}
