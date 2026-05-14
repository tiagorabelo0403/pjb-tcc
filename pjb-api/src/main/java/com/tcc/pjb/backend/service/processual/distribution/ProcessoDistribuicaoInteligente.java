package com.tcc.pjb.backend.service.processual.distribution;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.institutional.InstituicaoJudicial;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProcessoDistribuicaoInteligente {

    public record DistribuicaoRequest(
            RitoProcessual ritoSolicitado,
            MateriaJurisdicao materia,
            BigDecimal valorCausa,
            String municipio,
            String uf,
            boolean temPrevencao,
            UUID orgaoPrevento
    ) {}

    public record DistribuicaoResponse(
            RitoProcessual ritoDefinitivo,
            NaturezaProcessualResolver natureza,
            Optional<InstituicaoJudicial> orgaoJulgador,
            String foro,
            boolean distribuicaoManual,
            List<String> observacoes
    ) {}

    private final TribunalConfigurationRegistry registry;
    private final CompetenciaJudicialMatrix matrix;

    public ProcessoDistribuicaoInteligente(
            TribunalConfigurationRegistry registry,
            CompetenciaJudicialMatrix matrix) {
        this.registry = registry;
        this.matrix = matrix;
    }

    public DistribuicaoResponse distribuir(DistribuicaoRequest request) {
        List<String> observacoes = new ArrayList<>();
        NaturezaProcessualResolver natureza = NaturezaProcessualResolver
                .resolverPorRito(request.ritoSolicitado());

        if (request.temPrevencao() && request.orgaoPrevento() != null) {
            Optional<InstituicaoJudicial> prevento = registry.buscarPorId(request.orgaoPrevento());
            if (prevento.isPresent() && matrix.eCompetente(
                    prevento.get(), request.ritoSolicitado(), request.valorCausa())) {
                observacoes.add("Distribuído por prevenção ao órgão " + prevento.get().sigla());
                return new DistribuicaoResponse(request.ritoSolicitado(), natureza,
                        prevento, request.municipio(), false, observacoes);
            }
            observacoes.add("Órgão prevento não é competente — redistribuição necessária");
        }

        CompetenciaJudicialMatrix.ResolucaoCompetencia resolucao = matrix.resolver(
                request.ritoSolicitado(), natureza, request.materia(),
                request.valorCausa(), request.uf());

        observacoes.add(resolucao.justificativa());

        if (resolucao.orgaoSugerido().isPresent()) {
            InstituicaoJudicial orgao = resolucao.orgaoSugerido().get();
            if (orgao.eJecItinerante()) {
                observacoes.add("JEC Itinerante — verificar calendário de sessões");
            }
            if (orgao.eVaraUnica()) {
                observacoes.add("Vara única — servidor pode acumular múltiplas funções");
            }
        }

        return new DistribuicaoResponse(
                request.ritoSolicitado(),
                natureza,
                resolucao.orgaoSugerido(),
                request.municipio(),
                resolucao.requerDistribuicaoManual(),
                observacoes
        );
    }
}
