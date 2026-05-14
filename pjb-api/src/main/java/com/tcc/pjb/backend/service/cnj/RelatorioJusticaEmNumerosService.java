package com.tcc.pjb.backend.service.cnj;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RelatorioJusticaEmNumerosService {

    public record PeriodoRelatorio(LocalDate inicio, LocalDate fim, int ano) {}

    public record IndicadorJN(
            String nomeIndicador,
            String codigoCnj,
            double valor,
            String unidade,
            String tribunal,
            PeriodoRelatorio periodo
    ) {}

    public record RelatorioJN(
            String tribunal,
            PeriodoRelatorio periodo,
            List<IndicadorJN> indicadores,
            Map<String, Long> processosPorRito,
            long totalProcessosNovos,
            long totalProcessosBaixados,
            long totalProcessosPendentes,
            double taxaSolucao,
            double ipca,
            String observacoes
    ) {}

    public RelatorioJN montar(String tribunal, PeriodoRelatorio periodo,
            long novos, long baixados, long pendentes,
            Map<RitoProcessual, Long> porRito) {
        double taxa = novos > 0 ? (double) baixados / novos : 0.0;
        double ipca = novos > 0 ? (double) baixados / (baixados + pendentes) : 0.0;
        List<IndicadorJN> indicadores = List.of(
                new IndicadorJN("Taxa de Solução", "IPC-A", taxa, "%", tribunal, periodo),
                new IndicadorJN("Índice de Produtividade", "IPCA", ipca, "%", tribunal, periodo)
        );
        Map<String, Long> processosStrRito = new java.util.LinkedHashMap<>();
        porRito.forEach((rito, count) -> processosStrRito.put(rito.name(), count));
        return new RelatorioJN(
                tribunal, periodo, indicadores, processosStrRito,
                novos, baixados, pendentes, taxa, ipca,
                "Gerado automaticamente pelo PJB — sem ranking punitivo por magistrado."
        );
    }
}
