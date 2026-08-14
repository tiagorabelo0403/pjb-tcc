package com.tcc.pjb.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.mapper.JurisdicaoMapper;
import com.tcc.pjb.backend.model.dto.JurisdicaoRequest;
import com.tcc.pjb.backend.model.dto.JurisdicaoResponse;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import com.tcc.pjb.backend.model.entity.competencia.Tribunal;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.NaturezaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.TipoJurisdicao;
import com.tcc.pjb.backend.model.repository.JurisdicaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.modules.auditoria.AuditoriaInteligenteService;
import com.tcc.pjb.backend.service.competencia.ComarcaResolutionService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JurisdicaoServiceTest {

    private JurisdicaoRepository jurisdicaoRepository;
    private ProcessoRepository processoRepository;
    private ComarcaResolutionService comarcaResolutionService;
    private JurisdicaoMapper jurisdicaoMapper;
    private AuditoriaInteligenteService auditoriaService;
    private JurisdicaoService service;

    @BeforeEach
    void setUp() {
        jurisdicaoRepository = mock(JurisdicaoRepository.class);
        processoRepository = mock(ProcessoRepository.class);
        comarcaResolutionService = mock(ComarcaResolutionService.class);
        jurisdicaoMapper = mock(JurisdicaoMapper.class);
        auditoriaService = mock(AuditoriaInteligenteService.class);
        service = new JurisdicaoService(jurisdicaoRepository, processoRepository, comarcaResolutionService, jurisdicaoMapper, auditoriaService);
    }

    @Test
    void criar_resolveComarcaNoCatalogoQuandoNomeEUfInformados() {
        JurisdicaoRequest dto = new JurisdicaoRequest();
        dto.setCodigo("TJCE-FOR-1");
        dto.setSigla("TJCE");
        dto.setNome("1ª Vara Cível de Fortaleza");
        dto.setTipo(TipoJurisdicao.ESTADUAL);
        dto.setNatureza(NaturezaJurisdicao.CONTENCIOSA);
        dto.setGrau(GrauJurisdicao.PRIMEIRO_GRAU);
        dto.setEsfera(EsferaJurisdicao.JUSTICA_ESTADUAL);
        dto.setMateria(MateriaJurisdicao.CIVIL);
        dto.setComarca("Fortaleza");
        dto.setEstado("CE");

        Jurisdicao entidadeMapeada = new Jurisdicao();
        entidadeMapeada.setSigla("TJCE");
        when(jurisdicaoMapper.toEntity(dto)).thenReturn(entidadeMapeada);
        when(jurisdicaoRepository.save(any(Jurisdicao.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jurisdicaoMapper.toResponse(any(Jurisdicao.class))).thenReturn(new JurisdicaoResponse());

        Tribunal tribunal = new Tribunal("TJCE", "Tribunal de Justiça do Ceará", TipoJustica.ESTADUAL, GrauJurisdicao.PRIMEIRO_GRAU, "CE");
        Comarca comarca = new Comarca("Fortaleza", "CE", "2304400", "Fórum Clóvis Beviláqua", tribunal);
        when(comarcaResolutionService.resolver("Fortaleza", "CE")).thenReturn(Optional.of(comarca));

        service.criar(dto);

        assertThat(entidadeMapeada.getComarcaEntidade()).isSameAs(comarca);
        assertThat(entidadeMapeada.getUf()).isEqualTo("CE");
        assertThat(entidadeMapeada.getCidade()).isEqualTo("Fortaleza");
    }

    @Test
    void criar_usaFallbackStringQuandoComarcaNaoEstaNoCatalogo() {
        JurisdicaoRequest dto = new JurisdicaoRequest();
        dto.setCodigo("TJAC-RB-1");
        dto.setSigla("TJAC");
        dto.setNome("1ª Vara Cível de Rio Branco");
        dto.setTipo(TipoJurisdicao.ESTADUAL);
        dto.setNatureza(NaturezaJurisdicao.CONTENCIOSA);
        dto.setGrau(GrauJurisdicao.PRIMEIRO_GRAU);
        dto.setEsfera(EsferaJurisdicao.JUSTICA_ESTADUAL);
        dto.setMateria(MateriaJurisdicao.CIVIL);
        dto.setComarca("Rio Branco");
        dto.setEstado("AC");

        Jurisdicao entidadeMapeada = new Jurisdicao();
        entidadeMapeada.setSigla("TJAC");
        entidadeMapeada.setComarca(dto.getComarca());
        entidadeMapeada.setEstado(dto.getEstado());
        when(jurisdicaoMapper.toEntity(dto)).thenReturn(entidadeMapeada);
        when(jurisdicaoRepository.save(any(Jurisdicao.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jurisdicaoMapper.toResponse(any(Jurisdicao.class))).thenReturn(new JurisdicaoResponse());
        when(comarcaResolutionService.resolver("Rio Branco", "AC")).thenReturn(Optional.empty());

        service.criar(dto);

        assertThat(entidadeMapeada.getComarcaEntidade()).isNull();
        assertThat(entidadeMapeada.getUf()).isEqualTo("AC");
        assertThat(entidadeMapeada.getCidade()).isEqualTo("Rio Branco");
    }

    @Test
    void criar_naoTentaResolverComarcaQuandoDadosTerritoriaisAusentes() {
        JurisdicaoRequest dto = new JurisdicaoRequest();
        dto.setCodigo("STJ-1");
        dto.setSigla("STJ");
        dto.setNome("Superior Tribunal de Justiça");
        dto.setTipo(TipoJurisdicao.FEDERAL);
        dto.setNatureza(NaturezaJurisdicao.CONTENCIOSA);
        dto.setGrau(GrauJurisdicao.SUPERIOR);
        dto.setEsfera(EsferaJurisdicao.JUSTICA_FEDERAL);
        dto.setMateria(MateriaJurisdicao.CIVIL);

        Jurisdicao entidadeMapeada = new Jurisdicao();
        entidadeMapeada.setSigla("STJ");
        when(jurisdicaoMapper.toEntity(dto)).thenReturn(entidadeMapeada);
        when(jurisdicaoRepository.save(any(Jurisdicao.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jurisdicaoMapper.toResponse(any(Jurisdicao.class))).thenReturn(new JurisdicaoResponse());

        service.criar(dto);

        assertThat(entidadeMapeada.getComarcaEntidade()).isNull();
        org.mockito.Mockito.verifyNoInteractions(comarcaResolutionService);
    }
}
