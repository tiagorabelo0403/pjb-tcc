package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaProdutividadeItemResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaProdutividadePainelResponse;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaEncerramentoTipo;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorEncerramento;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorEncerramentoRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OficialJusticaProdutividadeService {

    private final DiligenciaOperadorEncerramentoRepository encerramentoRepository;

    public OficialJusticaProdutividadeService(DiligenciaOperadorEncerramentoRepository encerramentoRepository) {
        this.encerramentoRepository = Objects.requireNonNull(encerramentoRepository);
    }

    @Transactional(readOnly = true)
    public OficialJusticaProdutividadePainelResponse painel(Long oficialId, int diasJanela) {
        Instant desde = Instant.now().minus(diasJanela, ChronoUnit.DAYS);
        List<DiligenciaOperadorEncerramento> encerramentos = encerramentoRepository
                .findByOperatorUserIdAndCanalAndCreatedAtAfterOrderByCreatedAtDesc(oficialId, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, desde);

        Map<String, Integer> porOutcome = new LinkedHashMap<>();
        List<OficialJusticaProdutividadeItemResponse> itens = encerramentos.stream()
                .map(encerramento -> {
                    porOutcome.merge(encerramento.getOutcome().name(), 1, Integer::sum);
                    return new OficialJusticaProdutividadeItemResponse(
                            encerramento.getId(),
                            encerramento.getProcessoId(),
                            encerramento.getProcessoNumero(),
                            encerramento.getOutcome().name(),
                            encerramento.getCreatedAt());
                })
                .toList();

        Double taxaSucesso = itens.isEmpty() ? null
                : porOutcome.getOrDefault(DiligenciaEncerramentoTipo.CUMPRIMENTO_POSITIVO.name(), 0) / (double) itens.size();

        return new OficialJusticaProdutividadePainelResponse(oficialId, diasJanela, itens.size(), porOutcome,
                taxaSucesso, intervaloMedioHoras(encerramentos), itens);
    }

    private Double intervaloMedioHoras(List<DiligenciaOperadorEncerramento> encerramentosDesc) {
        if (encerramentosDesc.size() < 2) {
            return null;
        }
        long totalSegundos = 0;
        int pares = 0;
        for (int i = 0; i < encerramentosDesc.size() - 1; i++) {
            Instant maisRecente = encerramentosDesc.get(i).getCreatedAt();
            Instant anterior = encerramentosDesc.get(i + 1).getCreatedAt();
            totalSegundos += Duration.between(anterior, maisRecente).getSeconds();
            pares++;
        }
        return pares == 0 ? null : (totalSegundos / 3600.0) / pares;
    }
}
