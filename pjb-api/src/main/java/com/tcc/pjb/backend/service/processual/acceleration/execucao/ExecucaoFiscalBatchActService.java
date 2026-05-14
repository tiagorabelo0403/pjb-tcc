package com.tcc.pjb.backend.service.processual.acceleration.execucao;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ExecucaoFiscalBatchActService {

    public record ExecucaoBatchItem(
            UUID processoId,
            String numeroProcesso,
            String acaoSugerida,
            String fazendaId
    ) {}

    public record LoteFiscal(
            String fazendaId,
            String acaoTipo,
            int quantidade,
            List<UUID> processos,
            String descricao
    ) {}

    public List<LoteFiscal> detectarLotes(List<ExecucaoBatchItem> processos) {
        Map<String, List<ExecucaoBatchItem>> porFazendaEAcao = processos.stream()
                .collect(Collectors.groupingBy(
                        p -> p.fazendaId() + "|" + p.acaoSugerida()));

        return porFazendaEAcao.values().stream()
                .filter(grupo -> grupo.size() >= 2)
                .map(grupo -> {
                    ExecucaoBatchItem ref = grupo.get(0);
                    List<UUID> ids = grupo.stream().map(ExecucaoBatchItem::processoId).toList();
                    return new LoteFiscal(
                            ref.fazendaId(), ref.acaoSugerida(), grupo.size(), ids,
                            grupo.size() + " execuções fiscais da Fazenda " + ref.fazendaId()
                                    + " aptas para " + ref.acaoSugerida());
                })
                .sorted((a, b) -> Integer.compare(b.quantidade(), a.quantidade()))
                .toList();
    }
}
