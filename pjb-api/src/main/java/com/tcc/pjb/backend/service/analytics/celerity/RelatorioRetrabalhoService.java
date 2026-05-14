package com.tcc.pjb.backend.service.analytics.celerity;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RelatorioRetrabalhoService {

    public record PadraoRetrabalhoAgregado(
            String tipo,
            long quantidade,
            double percentualDoTotal,
            String prevencaoSugerida
    ) {}

    public record RelatorioRetrabalho(
            UUID orgaoId,
            String periodo,
            List<PadraoRetrabalhoAgregado> padroes,
            int totalProcessosComRetrabalho,
            String observacao
    ) {}

    public RelatorioRetrabalho gerar(UUID orgaoId, String periodo,
            List<PadraoRetrabalhoAgregado> padroes, int total) {
        return new RelatorioRetrabalho(orgaoId, periodo, padroes, total,
                "Relatório de retrabalho — identifica padrões sistêmicos, não falha individual.");
    }
}
