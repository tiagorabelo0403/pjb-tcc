package com.tcc.pjb.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.mapper.OrgaoJudiciarioMapper;
import com.tcc.pjb.backend.model.dto.OrgaoJudiciarioRequest;
import com.tcc.pjb.backend.model.dto.OrgaoJudiciarioResponse;
import com.tcc.pjb.backend.model.entity.OrgaoJudiciario;
import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import com.tcc.pjb.backend.model.repository.OrgaoJudiciarioRepository;
import com.tcc.pjb.backend.service.competencia.ComarcaResolutionService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OrgaoJudiciarioServiceComarcaTest {

    @Test
    void resolveEAplicaComarcaDoCatalogoAoCriar() {
        OrgaoJudiciarioRepository repository = mock(OrgaoJudiciarioRepository.class);
        OrgaoJudiciarioMapper mapper = mock(OrgaoJudiciarioMapper.class);
        ComarcaResolutionService resolutionService = mock(ComarcaResolutionService.class);
        OrgaoJudiciarioService service = new OrgaoJudiciarioService(repository, mapper, resolutionService);

        OrgaoJudiciarioRequest dto = new OrgaoJudiciarioRequest();
        OrgaoJudiciario entidade = new OrgaoJudiciario("Forum de Fortaleza", "TJCE-FOR", "Forum", "Estadual", "Fortaleza", "CE");
        Comarca comarcaEsperada = mock(Comarca.class);

        when(repository.findBySigla(any())).thenReturn(Optional.empty());
        when(mapper.requestParaEntidade(dto)).thenReturn(entidade);
        when(resolutionService.resolver("Fortaleza", "CE")).thenReturn(Optional.of(comarcaEsperada));
        when(repository.save(entidade)).thenReturn(entidade);
        when(mapper.entidadeParaResponse(entidade)).thenReturn(new OrgaoJudiciarioResponse());

        service.criarOrgaoJudiciario(dto);

        assertThat(entidade.getComarcaEntidade()).isSameAs(comarcaEsperada);
    }

    @Test
    void naoResolveQuandoComarcaEmBrancoENaoLancaExcecao() {
        OrgaoJudiciarioRepository repository = mock(OrgaoJudiciarioRepository.class);
        OrgaoJudiciarioMapper mapper = mock(OrgaoJudiciarioMapper.class);
        ComarcaResolutionService resolutionService = mock(ComarcaResolutionService.class);
        OrgaoJudiciarioService service = new OrgaoJudiciarioService(repository, mapper, resolutionService);

        OrgaoJudiciarioRequest dto = new OrgaoJudiciarioRequest();
        OrgaoJudiciario entidade = new OrgaoJudiciario("Tribunal Superior X", "TSX", "Tribunal", "Federal", null, null);

        when(repository.findBySigla(any())).thenReturn(Optional.empty());
        when(mapper.requestParaEntidade(dto)).thenReturn(entidade);
        when(repository.save(entidade)).thenReturn(entidade);
        when(mapper.entidadeParaResponse(entidade)).thenReturn(new OrgaoJudiciarioResponse());

        service.criarOrgaoJudiciario(dto);

        assertThat(entidade.getComarcaEntidade()).isNull();
    }

    @Test
    void mantemComarcaEntidadeNulaQuandoResolverNaoAchaCandidata() {
        OrgaoJudiciarioRepository repository = mock(OrgaoJudiciarioRepository.class);
        OrgaoJudiciarioMapper mapper = mock(OrgaoJudiciarioMapper.class);
        ComarcaResolutionService resolutionService = mock(ComarcaResolutionService.class);
        OrgaoJudiciarioService service = new OrgaoJudiciarioService(repository, mapper, resolutionService);

        OrgaoJudiciarioRequest dto = new OrgaoJudiciarioRequest();
        OrgaoJudiciario entidade = new OrgaoJudiciario("Forum Desconhecido", "FOR-DESC", "Forum", "Estadual", "Comarca Inexistente", "CE");

        when(repository.findBySigla(any())).thenReturn(Optional.empty());
        when(mapper.requestParaEntidade(dto)).thenReturn(entidade);
        when(resolutionService.resolver("Comarca Inexistente", "CE")).thenReturn(Optional.empty());
        when(repository.save(entidade)).thenReturn(entidade);
        when(mapper.entidadeParaResponse(entidade)).thenReturn(new OrgaoJudiciarioResponse());

        service.criarOrgaoJudiciario(dto);

        assertThat(entidade.getComarcaEntidade()).isNull();
    }
}
