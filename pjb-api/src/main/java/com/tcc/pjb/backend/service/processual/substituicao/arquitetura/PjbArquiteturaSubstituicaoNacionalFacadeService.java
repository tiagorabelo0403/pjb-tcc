package com.tcc.pjb.backend.service.processual.substituicao.arquitetura;

import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbArquiteturaSubstituicaoNacionalApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoCapacidade;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoNacionalAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoPilar;
import com.tcc.pjb.backend.model.dto.processual.substituicao.arquitetura.PjbArquiteturaSubstituicaoCapacidadeResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.arquitetura.PjbArquiteturaSubstituicaoNacionalResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.arquitetura.PjbArquiteturaSubstituicaoPilarResponse;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PjbArquiteturaSubstituicaoNacionalFacadeService {

    private final PjbArquiteturaSubstituicaoNacionalApplicationService applicationService;

    public PjbArquiteturaSubstituicaoNacionalFacadeService(PjbArquiteturaSubstituicaoNacionalApplicationService applicationService) {
        this.applicationService = Objects.requireNonNull(applicationService);
    }

    public PjbArquiteturaSubstituicaoNacionalResponse avaliar() {
        PjbArquiteturaSubstituicaoNacionalAggregate aggregate = applicationService.avaliar();
        return new PjbArquiteturaSubstituicaoNacionalResponse(
                aggregate.scoreGeral(),
                aggregate.prontoParaSubstituicaoImediata(),
                aggregate.buildGateAprovado(),
                aggregate.totalProcessos(),
                aggregate.totalWorkItemsPendentes(),
                aggregate.totalWorkItemsExpirados(),
                aggregate.totalTribunaisCatalogados(),
                aggregate.totalRitosCatalogados(),
                aggregate.pilares().stream().map(this::mapPilar).toList(),
                aggregate.conclusaoTecnica(),
                aggregate.fundamentos(),
                aggregate.geradoEm()
        );
    }

    private PjbArquiteturaSubstituicaoPilarResponse mapPilar(PjbArquiteturaSubstituicaoPilar pilar) {
        return new PjbArquiteturaSubstituicaoPilarResponse(
                pilar.codigo(),
                pilar.titulo(),
                pilar.status().name(),
                pilar.score(),
                pilar.pronto(),
                pilar.capacidades().stream().map(this::mapCapacidade).toList(),
                pilar.proximasAcoes()
        );
    }

    private PjbArquiteturaSubstituicaoCapacidadeResponse mapCapacidade(PjbArquiteturaSubstituicaoCapacidade capacidade) {
        return new PjbArquiteturaSubstituicaoCapacidadeResponse(
                capacidade.codigo(),
                capacidade.titulo(),
                capacidade.status().name(),
                capacidade.score(),
                capacidade.conclusao(),
                capacidade.evidencias(),
                capacidade.pendencias()
        );
    }
}
