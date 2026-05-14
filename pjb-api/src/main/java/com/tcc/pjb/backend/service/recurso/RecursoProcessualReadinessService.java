package com.tcc.pjb.backend.service.recurso;

import com.tcc.pjb.backend.service.prazo.CalendarioUteisService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RecursoProcessualReadinessService {

    public record RecursoReadinessInput(
            UUID processoId,
            RecursoAdmissibilidadeService.AdmissibilidadeInput admissibilidade,
            RecursoTempestividadeGuardService.TempestividadeInput tempestividade,
            RecursoDesertaoRadarService.DesertaoInput desertao,
            CalendarioUteisService.CalendarioInput calendario
    ) {}

    public record RecursoReadinessResult(
            boolean pronto,
            boolean admissivel,
            boolean tempestivo,
            boolean semRiscoDesercao,
            List<String> pendencias,
            String diagnostico
    ) {}

    private final RecursoAdmissibilidadeService admissibilidadeService;
    private final RecursoTempestividadeGuardService tempestividadeService;
    private final RecursoDesertaoRadarService desertaoService;

    public RecursoProcessualReadinessService(
            RecursoAdmissibilidadeService admissibilidadeService,
            RecursoTempestividadeGuardService tempestividadeService,
            RecursoDesertaoRadarService desertaoService) {
        this.admissibilidadeService = admissibilidadeService;
        this.tempestividadeService = tempestividadeService;
        this.desertaoService = desertaoService;
    }

    public RecursoReadinessResult avaliar(RecursoReadinessInput input) {
        var admissao = admissibilidadeService.avaliar(input.admissibilidade());
        var tempestividade = tempestividadeService.avaliar(input.tempestividade(), input.calendario());
        var desertao = desertaoService.analisar(input.desertao());

        List<String> pendencias = new ArrayList<>();
        if (!admissao.admissivel()) pendencias.addAll(admissao.obstaculos());
        if (!tempestividade.tempestivo()) pendencias.add("Recurso intempestivo — prazo esgotado.");
        if (desertao.emRiscoDeDesercao()) pendencias.add(desertao.diagnostico());

        boolean pronto = pendencias.isEmpty();
        String diagnostico = pronto
                ? "Recurso apto: admissível, tempestivo e sem risco de deserção."
                : "Recurso com " + pendencias.size() + " pendência(s) crítica(s).";

        return new RecursoReadinessResult(
                pronto,
                admissao.admissivel(),
                tempestividade.tempestivo(),
                !desertao.emRiscoDeDesercao(),
                List.copyOf(pendencias),
                diagnostico);
    }
}
