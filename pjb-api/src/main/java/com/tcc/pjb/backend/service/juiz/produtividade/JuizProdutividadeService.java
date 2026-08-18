package com.tcc.pjb.backend.service.juiz.produtividade;

import com.tcc.pjb.backend.model.dto.juiz.produtividade.JuizProdutividadeAtoResponse;
import com.tcc.pjb.backend.model.dto.juiz.produtividade.JuizProdutividadePainelResponse;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
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
public class JuizProdutividadeService {

    private final MovimentacaoProcessualRepository movimentacaoRepository;

    public JuizProdutividadeService(MovimentacaoProcessualRepository movimentacaoRepository) {
        this.movimentacaoRepository = Objects.requireNonNull(movimentacaoRepository);
    }

    @Transactional(readOnly = true)
    public JuizProdutividadePainelResponse painel(Long magistradoId, int diasJanela) {
        Instant desde = Instant.now().minus(diasJanela, ChronoUnit.DAYS);
        List<MovimentacaoProcessual> movimentacoes =
                movimentacaoRepository.findByAtor_IdAndDataMovimentacaoAfterOrderByDataMovimentacaoDesc(magistradoId, desde);

        Map<String, Integer> porTipo = new LinkedHashMap<>();
        List<JuizProdutividadeAtoResponse> itens = movimentacoes.stream()
                .map(movimentacao -> {
                    String tipo = classificar(movimentacao.getDescricao());
                    porTipo.merge(tipo, 1, Integer::sum);
                    return new JuizProdutividadeAtoResponse(
                            movimentacao.getId(),
                            movimentacao.getProcesso() == null ? null : movimentacao.getProcesso().getId(),
                            tipo,
                            movimentacao.getDataMovimentacao());
                })
                .toList();

        return new JuizProdutividadePainelResponse(magistradoId, diasJanela, itens.size(), porTipo,
                intervaloMedioHoras(movimentacoes), itens);
    }

    private String classificar(String descricao) {
        if (descricao == null) {
            return "OUTRO";
        }
        if (descricao.startsWith("Despacho judicial")) {
            return "DESPACHO";
        }
        if (descricao.startsWith("Sentença judicial")) {
            return "SENTENCA";
        }
        if (descricao.startsWith("Decisão interlocutória")) {
            return "DECISAO_INTERLOCUTORIA";
        }
        return "OUTRO";
    }

    private Double intervaloMedioHoras(List<MovimentacaoProcessual> movimentacoesDesc) {
        if (movimentacoesDesc.size() < 2) {
            return null;
        }
        long totalSegundos = 0;
        int pares = 0;
        for (int i = 0; i < movimentacoesDesc.size() - 1; i++) {
            Instant maisRecente = movimentacoesDesc.get(i).getDataMovimentacao();
            Instant anterior = movimentacoesDesc.get(i + 1).getDataMovimentacao();
            totalSegundos += Duration.between(anterior, maisRecente).getSeconds();
            pares++;
        }
        return pares == 0 ? null : (totalSegundos / 3600.0) / pares;
    }
}
