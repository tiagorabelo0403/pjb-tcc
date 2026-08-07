package com.tcc.pjb.backend.service.institutional.produtividade;

import com.tcc.pjb.backend.model.dto.institutional.produtividade.InstitutionalProdutividadeItemResponse;
import com.tcc.pjb.backend.model.dto.institutional.produtividade.InstitutionalProdutividadePainelResponse;
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
public class InstitutionalProdutividadeService {

    private static final List<Map.Entry<String, String>> CLASSIFICACAO = List.of(
            Map.entry("Despacho judicial", "DESPACHO"),
            Map.entry("Sentença judicial", "SENTENCA"),
            Map.entry("Decisão interlocutória", "DECISAO_INTERLOCUTORIA"),
            Map.entry("Manifestação do Ministério Público", "MANIFESTACAO_MP"),
            Map.entry("Parecer do Ministério Público", "PARECER_MP"),
            Map.entry("Petição da Defensoria Pública", "PETICAO_DEFENSORIA"),
            Map.entry("Requerimento de gratuidade da Defensoria Pública", "GRATUIDADE_DEFENSORIA"),
            Map.entry("Defesa da Defensoria Pública", "DEFESA_DEFENSORIA"),
            Map.entry("Habeas Corpus impetrado pela Defensoria Pública", "HABEAS_CORPUS_DEFENSORIA"),
            Map.entry("Assistência judiciária gratuita solicitada pela Defensoria Pública", "AJG_DEFENSORIA"),
            Map.entry("Contestação da Procuradoria", "CONTESTACAO_PROCURADORIA"),
            Map.entry("Parecer da Procuradoria", "PARECER_PROCURADORIA")
    );

    private final MovimentacaoProcessualRepository movimentacaoRepository;

    public InstitutionalProdutividadeService(MovimentacaoProcessualRepository movimentacaoRepository) {
        this.movimentacaoRepository = Objects.requireNonNull(movimentacaoRepository);
    }

    @Transactional(readOnly = true)
    public InstitutionalProdutividadePainelResponse painel(Long atorId, int diasJanela) {
        Instant desde = Instant.now().minus(diasJanela, ChronoUnit.DAYS);
        List<MovimentacaoProcessual> movimentacoes =
                movimentacaoRepository.findByAtor_IdAndDataMovimentacaoAfterOrderByDataMovimentacaoDesc(atorId, desde);

        Map<String, Integer> porTipo = new LinkedHashMap<>();
        List<InstitutionalProdutividadeItemResponse> itens = movimentacoes.stream()
                .map(movimentacao -> {
                    String tipo = classificar(movimentacao.getDescricao());
                    porTipo.merge(tipo, 1, Integer::sum);
                    return new InstitutionalProdutividadeItemResponse(
                            movimentacao.getId(),
                            movimentacao.getProcesso() == null ? null : movimentacao.getProcesso().getId(),
                            tipo,
                            movimentacao.getDataMovimentacao());
                })
                .toList();

        return new InstitutionalProdutividadePainelResponse(atorId, diasJanela, itens.size(), porTipo,
                intervaloMedioHoras(movimentacoes), itens);
    }

    private String classificar(String descricao) {
        if (descricao == null) {
            return "OUTRO";
        }
        for (Map.Entry<String, String> entrada : CLASSIFICACAO) {
            if (descricao.startsWith(entrada.getKey())) {
                return entrada.getValue();
            }
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
