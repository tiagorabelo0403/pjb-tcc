package com.tcc.pjb.backend.service.secretariat.institucional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Instituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstitucionalAbrangencia;
import com.tcc.pjb.backend.model.entity.enums.StatusUnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoInstituicao;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.model.repository.InstituicaoRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstitucionalAbrangenciaRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstituicaoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class UnidadeInstitucionalAdminServiceTest {

    private final InstituicaoRepository instituicaoRepository = mock(InstituicaoRepository.class);
    private final UnidadeInstituicaoRepository unidadeRepository = mock(UnidadeInstituicaoRepository.class);
    private final UnidadeInstitucionalAbrangenciaRepository abrangenciaRepository = mock(UnidadeInstitucionalAbrangenciaRepository.class);
    private final SecretariaInstitucionalEnfileiramentoService enfileiramentoService = mock(SecretariaInstitucionalEnfileiramentoService.class);
    private final AuditLedgerService auditService = mock(AuditLedgerService.class);
    private final UnidadeInstitucionalAdminService service = new UnidadeInstitucionalAdminService(
            instituicaoRepository, unidadeRepository, abrangenciaRepository, enfileiramentoService, auditService);

    @Test
    void criarInstituicaoSalvaEAudita() {
        when(instituicaoRepository.save(any())).thenAnswer(inv -> {
            Instituicao i = inv.getArgument(0);
            i.setTipo(TipoInstituicao.MINISTERIO_PUBLICO);
            return i;
        });

        Instituicao criada = service.criarInstituicao(TipoInstituicao.MINISTERIO_PUBLICO, "Ministerio Publico do Ceara", "MPCE");

        assertThat(criada.getTipo()).isEqualTo(TipoInstituicao.MINISTERIO_PUBLICO);
        verify(auditService).appendSafely(org.mockito.ArgumentMatchers.eq("INSTITUICAO_CRIADA"), any());
    }

    @Test
    void criarUnidadeSalvaEAuditaSemReprocessarNaMesmaTransacao() {
        Instituicao instituicao = new Instituicao();
        instituicao.setTipo(TipoInstituicao.MINISTERIO_PUBLICO);
        when(instituicaoRepository.findById(1L)).thenReturn(Optional.of(instituicao));
        when(unidadeRepository.save(any())).thenAnswer(inv -> {
            UnidadeInstituicao u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 10L);
            return u;
        });

        UnidadeInstituicao criada = service.criarUnidade(1L, "1a Promotoria Criminal", TipoUnidadeInstitucional.PROMOTORIA, "Fortaleza", "CE");

        assertThat(criada.getId()).isEqualTo(10L);
        verify(auditService).appendSafely(org.mockito.ArgumentMatchers.eq("UNIDADE_INSTITUICAO_CRIADA"), any());
        verify(enfileiramentoService, org.mockito.Mockito.never()).reprocessarSemUnidade(any());
    }

    @Test
    void reprocessarBacklogAposCriacaoDeUnidadeChamaEnfileiramentoEAuditaSeHouverResolvidos() {
        UnidadeInstituicao unidade = new UnidadeInstituicao();
        ReflectionTestUtils.setField(unidade, "id", 10L);
        unidade.setTipo(TipoUnidadeInstitucional.PROMOTORIA);
        when(enfileiramentoService.reprocessarSemUnidade(TipoUnidadeInstitucional.PROMOTORIA)).thenReturn(2);

        service.reprocessarBacklogAposCriacaoDeUnidade(unidade);

        verify(enfileiramentoService).reprocessarSemUnidade(TipoUnidadeInstitucional.PROMOTORIA);
        ArgumentCaptor<String> detalhesCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService).appendSafely(
                org.mockito.ArgumentMatchers.eq("SECRETARIA_INSTITUCIONAL_REPROCESSAMENTO_EM_LOTE"),
                detalhesCaptor.capture());
        assertThat(detalhesCaptor.getValue())
                .contains("tipo=PROMOTORIA")
                .contains("itensResolvidos=2")
                .contains("unidade 10");
    }

    @Test
    void reprocessarBacklogAposCriacaoDeUnidadeNaoAuditaQuandoNenhumItemResolvido() {
        UnidadeInstituicao unidade = new UnidadeInstituicao();
        ReflectionTestUtils.setField(unidade, "id", 11L);
        unidade.setTipo(TipoUnidadeInstitucional.NUCLEO_DEFENSORIA);
        when(enfileiramentoService.reprocessarSemUnidade(TipoUnidadeInstitucional.NUCLEO_DEFENSORIA)).thenReturn(0);

        service.reprocessarBacklogAposCriacaoDeUnidade(unidade);

        verify(auditService, org.mockito.Mockito.never())
                .appendSafely(org.mockito.ArgumentMatchers.eq("SECRETARIA_INSTITUCIONAL_REPROCESSAMENTO_EM_LOTE"), any());
    }

    @Test
    void criarUnidadeComInstituicaoInexistenteLancaIllegalArgumentException() {
        when(instituicaoRepository.findById(99L)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.criarUnidade(99L, "X", TipoUnidadeInstitucional.FORUM, "Fortaleza", "CE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void adicionarAbrangenciaSalvaEReprocessaItensPresosDoTipoDaUnidade() {
        UnidadeInstituicao unidade = new UnidadeInstituicao();
        ReflectionTestUtils.setField(unidade, "id", 5L);
        unidade.setTipo(TipoUnidadeInstitucional.NUCLEO_DEFENSORIA);
        when(unidadeRepository.findById(5L)).thenReturn(Optional.of(unidade));
        when(abrangenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(enfileiramentoService.reprocessarSemUnidade(TipoUnidadeInstitucional.NUCLEO_DEFENSORIA)).thenReturn(1);

        UnidadeInstitucionalAbrangencia criada = service.adicionarAbrangencia(5L, "Aquiraz");

        assertThat(criada.getComarcaAtendida()).isEqualTo("Aquiraz");
        verify(enfileiramentoService).reprocessarSemUnidade(TipoUnidadeInstitucional.NUCLEO_DEFENSORIA);
    }

    @Test
    void desativarUnidadeMarcaStatusComoInativa() {
        UnidadeInstituicao unidade = new UnidadeInstituicao();
        ReflectionTestUtils.setField(unidade, "id", 6L);
        unidade.setStatusUnidade(StatusUnidadeInstitucional.ATIVA);
        when(unidadeRepository.findById(6L)).thenReturn(Optional.of(unidade));
        when(unidadeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.desativarUnidade(6L);

        assertThat(unidade.getStatusUnidade()).isEqualTo(StatusUnidadeInstitucional.INATIVA);
        verify(auditService).appendSafely(org.mockito.ArgumentMatchers.eq("UNIDADE_INSTITUICAO_DESATIVADA"), any());
    }
}
