package com.tcc.pjb.backend.service.prazo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PrazoIntempestividadeGuardService {

    public record IntempestividadeInput(
            LocalDate dataAto,
            LocalDate dataVencimentoPrazo,
            boolean fazendaPublica,
            boolean ministerioPublico,
            String justificativaJuntada
    ) {}

    public record IntempestividadeResult(
            boolean tempestivo,
            long diasAtraso,
            List<String> fundamentacao,
            boolean admiteJustificativa
    ) {}

    private final CalendarioUteisService calendarioService;

    public PrazoIntempestividadeGuardService(CalendarioUteisService calendarioService) {
        this.calendarioService = calendarioService;
    }

    public IntempestividadeResult avaliar(IntempestividadeInput input) {
        List<String> fundamentacao = new ArrayList<>();
        boolean tempestivo = !input.dataAto().isAfter(input.dataVencimentoPrazo());
        long diasAtraso = 0;
        if (!tempestivo) {
            fundamentacao.add("Ato praticado após vencimento do prazo (CPC, art. 218).");
            if (input.fazendaPublica()) {
                fundamentacao.add("Prazo em quádruplo para Fazenda Pública verificado (CPC, art. 183).");
            }
            if (input.ministerioPublico()) {
                fundamentacao.add("Prazo em quádruplo para Ministério Público verificado (CPC, art. 180).");
            }
            diasAtraso = input.dataVencimentoPrazo().until(input.dataAto()).getDays();
        }
        boolean admiteJustificativa = !tempestivo && input.justificativaJuntada() != null
                && !input.justificativaJuntada().isBlank();
        return new IntempestividadeResult(tempestivo, diasAtraso, List.copyOf(fundamentacao),
                admiteJustificativa);
    }
}
