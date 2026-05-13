package com.tcc.pjb.backend.model.dto.processual.comunicacao.flow;

import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

public record NationalCommunicationCanonicalActResolveRequest(
        Long processoId,
        RamoDireito ramoDireito,
        GrauJurisdicao grauJurisdicao,
        FaseProcessual faseProcessual,
        String classeProcessual,
        String assunto,
        String objetoProcessual,
        String pedidoPrincipal,
        String uf,
        String comarca,
        String foro,
        Boolean presencaIncapaz,
        Boolean interesseCriancaAdolescente,
        Boolean reuPresoOuCustodiado,
        Boolean periciaNecessaria,
        Boolean estudoPsicossocialNecessario,
        Boolean derivacaoCejusc,
        Boolean cooperacaoJudicial,
        Boolean fazendaPublicaNoPolo,
        Boolean demandaColetiva,
        Boolean falenciaOuRecuperacao,
        Boolean curadoriaEspecial,
        Boolean conselhoTutelarNecessario,
        Boolean orgaoTecnicoConveniadoNecessario,
        Boolean cartorioExtrajudicialNecessario,
        Boolean contadoriaJudicialNecessaria,
        Boolean audienciaDesignada) {
}
