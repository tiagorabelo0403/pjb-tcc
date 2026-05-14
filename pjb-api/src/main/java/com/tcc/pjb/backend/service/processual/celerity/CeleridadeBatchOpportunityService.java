package com.tcc.pjb.backend.service.processual.celerity;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CeleridadeBatchOpportunityService {

    public record OportunidadeLote(
            ProcessoAceleracaoAcaoTipo acao,
            RitoProcessual rito,
            int quantidade,
            String mensagem
    ) {}

    public record ProcessoLoteItem(
            UUID processoId,
            RitoProcessual rito,
            ProcessoGargaloTipo gargalo,
            ProcessoAceleracaoAcaoTipo acaoSugerida
    ) {}

    public List<OportunidadeLote> detectarOportunidades(List<ProcessoLoteItem> processos) {
        Map<ProcessoAceleracaoAcaoTipo, List<ProcessoLoteItem>> porAcao = processos.stream()
                .collect(Collectors.groupingBy(ProcessoLoteItem::acaoSugerida));

        List<OportunidadeLote> oportunidades = new ArrayList<>();

        porAcao.forEach((acao, itens) -> {
            if (itens.size() < 2) return;

            Map<RitoProcessual, Long> porRito = itens.stream()
                    .collect(Collectors.groupingBy(ProcessoLoteItem::rito, Collectors.counting()));

            porRito.forEach((rito, count) -> {
                if (count < 2) return;
                oportunidades.add(new OportunidadeLote(
                        acao, rito, count.intValue(),
                        JudicialIndependenceSafeAccelerationPolicy
                                .mensagemAceleracaoSegura(acao, count.intValue())));
            });
        });

        oportunidades.sort((a, b) -> Integer.compare(b.quantidade(), a.quantidade()));
        return oportunidades;
    }

    public OportunidadeLote maiorOportunidade(List<ProcessoLoteItem> processos) {
        return detectarOportunidades(processos).stream().findFirst().orElse(null);
    }
}
