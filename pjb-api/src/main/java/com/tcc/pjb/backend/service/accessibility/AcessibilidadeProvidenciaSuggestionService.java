package com.tcc.pjb.backend.service.accessibility;

import com.tcc.pjb.backend.service.processual.chip.ProcessoChipTipo;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AcessibilidadeProvidenciaSuggestionService {

    public record ProvidenciaAcessibilidade(
            String descricao,
            String fundamentoLegal,
            boolean urgente
    ) {}

    private final AcessibilidadeProcessualChipService chipService;

    public AcessibilidadeProvidenciaSuggestionService(AcessibilidadeProcessualChipService chipService) {
        this.chipService = chipService;
    }

    public List<ProvidenciaAcessibilidade> sugerir(UUID processoId,
            java.util.Collection<ProcessoChipTipo> chips) {
        return chipService.identificar(chips).stream()
                .map(n -> new ProvidenciaAcessibilidade(
                        n.providenciaSugerida(),
                        resolverFundamento(n.chip()),
                        n.obrigatoria()))
                .toList();
    }

    private String resolverFundamento(ProcessoChipTipo chip) {
        return switch (chip) {
            case LIBRAS -> "Lei 10.436/2002 e Decreto 5.626/2005";
            case INTERPRETE_GUIA -> "Lei 13.146/2015 (Estatuto da Pessoa com Deficiência)";
            case PESSOA_COM_DEFICIENCIA -> "Lei 13.146/2015 e Resolução CNJ 230/2016";
            default -> "Resolução CNJ 230/2016 — acesso à justiça para pessoas vulneráveis";
        };
    }
}
