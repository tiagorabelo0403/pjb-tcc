package com.tcc.pjb.backend.service.analytics.celerity;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.processual.celerity.CeleridadeNivel;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RelatorioCeleridadeService {

    public record RelatorioInput(
            UUID orgaoId,
            String periodoInicio,
            String periodoFim
    ) {}

    public record RelatorioCeleridade(
            UUID orgaoId,
            String periodo,
            Map<RitoProcessual, Duration> tempsMediosPorRito,
            Map<CeleridadeNivel, Long> distribuicaoNivel,
            int totalProcessosAnalisados,
            String observacao
    ) {}

    public RelatorioCeleridade gerar(RelatorioInput input) {
        return new RelatorioCeleridade(
                input.orgaoId(),
                input.periodoInicio() + " a " + input.periodoFim(),
                Map.of(),
                Map.of(),
                0,
                "Relatório gerado — dados por rito sem ranking punitivo. Foco em gargalos sistêmicos.");
    }
}
