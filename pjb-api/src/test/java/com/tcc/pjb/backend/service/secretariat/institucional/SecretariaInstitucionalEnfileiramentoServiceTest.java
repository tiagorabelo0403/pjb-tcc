package com.tcc.pjb.backend.service.secretariat.institucional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.SecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstitucionalAbrangencia;
import com.tcc.pjb.backend.model.entity.enums.MotivoEnfileiramentoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.SecretariaInstitucionalItemRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstitucionalAbrangenciaRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstituicaoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

class SecretariaInstitucionalEnfileiramentoServiceTest {

    private final UnidadeInstituicaoRepository unidadeRepository = mock(UnidadeInstituicaoRepository.class);
    private final UnidadeInstitucionalAbrangenciaRepository abrangenciaRepository = mock(UnidadeInstitucionalAbrangenciaRepository.class);
    private final SecretariaInstitucionalItemRepository itemRepository = mock(SecretariaInstitucionalItemRepository.class);
    private final SecretariaInstitucionalItemGravador gravador = mock(SecretariaInstitucionalItemGravador.class);
    private final AuditLedgerService auditService = mock(AuditLedgerService.class);
    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final SecretariaInstitucionalEnfileiramentoService service =
            new SecretariaInstitucionalEnfileiramentoService(unidadeRepository, abrangenciaRepository, itemRepository, gravador, auditService, processoRepository);

    private UnidadeInstituicao unidade(Long id, TipoUnidadeInstitucional tipo) {
        UnidadeInstituicao u = new UnidadeInstituicao();
        ReflectionTestUtils.setField(u, "id", id);
        u.setTipo(tipo);
        return u;
    }

    @Test
    void promotoriaComUnidadeSediadaNaComarcaCriaItemComPrazoEmDobro() {
        UnidadeInstituicao unidade = unidade(10L, TipoUnidadeInstitucional.PROMOTORIA);
        when(itemRepository.existeAtivoOuSemUnidadeResolvida(1L, TipoUnidadeInstitucional.PROMOTORIA)).thenReturn(false);
        when(unidadeRepository.findByTipoAndComarca(TipoUnidadeInstitucional.PROMOTORIA, "Fortaleza")).thenReturn(List.of(unidade));
        when(gravador.gravar(any())).thenAnswer(inv -> inv.getArgument(0));

        SecretariaInstitucionalItem item = service.enfileirar(1L, "Fortaleza", TipoUnidadeInstitucional.PROMOTORIA,
                MotivoEnfileiramentoInstitucional.PARTE_AUTOMATICA, 15);

        assertThat(item.getUnidadeInstitucionalId()).isEqualTo(10L);
        assertThat(item.getStatus()).isEqualTo(StatusSecretariaInstitucionalItem.PENDENTE);
        assertThat(item.isPrazoEmDobro()).isTrue();
    }

    @Test
    void comarcaSemUnidadeSediadaResolvePorAbrangenciaRegional() {
        UnidadeInstituicao regional = unidade(11L, TipoUnidadeInstitucional.NUCLEO_DEFENSORIA);
        when(itemRepository.existeAtivoOuSemUnidadeResolvida(2L, TipoUnidadeInstitucional.NUCLEO_DEFENSORIA)).thenReturn(false);
        when(unidadeRepository.findByTipoAndComarca(TipoUnidadeInstitucional.NUCLEO_DEFENSORIA, "Aquiraz")).thenReturn(List.of());
        when(unidadeRepository.findAll()).thenReturn(List.of()); // não usado neste caminho, ver Step 3 para a estratégia real
        UnidadeInstitucionalAbrangencia cobertura = new UnidadeInstitucionalAbrangencia();
        cobertura.setUnidadeInstitucionalId(11L);
        cobertura.setComarcaAtendida("Aquiraz");
        when(abrangenciaRepository.findByComarcaAtendida("Aquiraz")).thenReturn(List.of(cobertura));
        when(unidadeRepository.findById(11L)).thenReturn(Optional.of(regional));
        when(gravador.gravar(any())).thenAnswer(inv -> inv.getArgument(0));

        SecretariaInstitucionalItem item = service.enfileirar(2L, "Aquiraz", TipoUnidadeInstitucional.NUCLEO_DEFENSORIA,
                MotivoEnfileiramentoInstitucional.PARTE_AUTOMATICA, 15);

        assertThat(item.getUnidadeInstitucionalId()).isEqualTo(11L);
        assertThat(item.getStatus()).isEqualTo(StatusSecretariaInstitucionalItem.PENDENTE);
    }

    @Test
    void forumNuncaTemPrazoEmDobro() {
        UnidadeInstituicao unidade = unidade(20L, TipoUnidadeInstitucional.FORUM);
        when(itemRepository.existeAtivoOuSemUnidadeResolvida(3L, TipoUnidadeInstitucional.FORUM)).thenReturn(false);
        when(unidadeRepository.findByTipoAndComarca(TipoUnidadeInstitucional.FORUM, "Fortaleza")).thenReturn(List.of(unidade));
        when(gravador.gravar(any())).thenAnswer(inv -> inv.getArgument(0));

        SecretariaInstitucionalItem item = service.enfileirar(3L, "Fortaleza", TipoUnidadeInstitucional.FORUM,
                MotivoEnfileiramentoInstitucional.DESPACHO_VISTA, 5);

        assertThat(item.isPrazoEmDobro()).isFalse();
    }

    @Test
    void procuradoriaPublicaNuncaTemPrazoEmDobro() {
        UnidadeInstituicao unidade = unidade(30L, TipoUnidadeInstitucional.PROCURADORIA_PUBLICA);
        when(itemRepository.existeAtivoOuSemUnidadeResolvida(4L, TipoUnidadeInstitucional.PROCURADORIA_PUBLICA)).thenReturn(false);
        when(unidadeRepository.findByTipoAndComarca(TipoUnidadeInstitucional.PROCURADORIA_PUBLICA, "Fortaleza")).thenReturn(List.of(unidade));
        when(gravador.gravar(any())).thenAnswer(inv -> inv.getArgument(0));

        SecretariaInstitucionalItem item = service.enfileirar(4L, "Fortaleza", TipoUnidadeInstitucional.PROCURADORIA_PUBLICA,
                MotivoEnfileiramentoInstitucional.DESPACHO_VISTA, 5);

        assertThat(item.isPrazoEmDobro()).isFalse();
    }

    @Test
    void semUnidadeSediadaNemAbrangenciaCriaItemComStatusEspecialSemLancarExcecao() {
        when(itemRepository.existeAtivoOuSemUnidadeResolvida(5L, TipoUnidadeInstitucional.NUCLEO_DEFENSORIA)).thenReturn(false);
        when(unidadeRepository.findByTipoAndComarca(TipoUnidadeInstitucional.NUCLEO_DEFENSORIA, "Comarca Sem Cobertura")).thenReturn(List.of());
        when(abrangenciaRepository.findByComarcaAtendida("Comarca Sem Cobertura")).thenReturn(List.of());
        when(gravador.gravar(any())).thenAnswer(inv -> inv.getArgument(0));

        SecretariaInstitucionalItem item = service.enfileirar(5L, "Comarca Sem Cobertura", TipoUnidadeInstitucional.NUCLEO_DEFENSORIA,
                MotivoEnfileiramentoInstitucional.PARTE_AUTOMATICA, 15);

        assertThat(item.getStatus()).isEqualTo(StatusSecretariaInstitucionalItem.SEM_UNIDADE_RESOLVIDA);
        assertThat(item.getUnidadeInstitucionalId()).isNull();
    }

    @Test
    void configuracaoAmbiguaComMaisDeUmaUnidadeSediadaTambemViraSemUnidadeResolvida() {
        UnidadeInstituicao u1 = unidade(1L, TipoUnidadeInstitucional.PROMOTORIA);
        UnidadeInstituicao u2 = unidade(2L, TipoUnidadeInstitucional.PROMOTORIA);
        when(itemRepository.existeAtivoOuSemUnidadeResolvida(6L, TipoUnidadeInstitucional.PROMOTORIA)).thenReturn(false);
        when(unidadeRepository.findByTipoAndComarca(TipoUnidadeInstitucional.PROMOTORIA, "Fortaleza")).thenReturn(List.of(u1, u2));
        when(gravador.gravar(any())).thenAnswer(inv -> inv.getArgument(0));

        SecretariaInstitucionalItem item = service.enfileirar(6L, "Fortaleza", TipoUnidadeInstitucional.PROMOTORIA,
                MotivoEnfileiramentoInstitucional.PARTE_AUTOMATICA, 15);

        assertThat(item.getStatus()).isEqualTo(StatusSecretariaInstitucionalItem.SEM_UNIDADE_RESOLVIDA);
    }

    @Test
    void jaExistindoItemPendenteNaoDuplica() {
        when(itemRepository.existeAtivoOuSemUnidadeResolvida(7L, TipoUnidadeInstitucional.PROMOTORIA)).thenReturn(true);

        SecretariaInstitucionalItem item = service.enfileirar(7L, "Fortaleza", TipoUnidadeInstitucional.PROMOTORIA,
                MotivoEnfileiramentoInstitucional.PARTE_AUTOMATICA, 15);

        assertThat(item).isNull();
        verify(gravador, never()).gravar(any());
        verify(unidadeRepository, never()).findByTipoAndComarca(any(), any());
    }

    @Test
    void reprocessarSemUnidadeResolveItemAgoraQueUnidadeFoiCadastrada() {
        SecretariaInstitucionalItem preso = new SecretariaInstitucionalItem();
        ReflectionTestUtils.setField(preso, "id", 8L);
        preso.setProcessoId(80L);
        preso.setTipoInstituicaoAlvo(TipoUnidadeInstitucional.PROMOTORIA);
        preso.setStatus(StatusSecretariaInstitucionalItem.SEM_UNIDADE_RESOLVIDA);
        when(itemRepository.findByStatus(StatusSecretariaInstitucionalItem.SEM_UNIDADE_RESOLVIDA)).thenReturn(List.of(preso));
        Processo processo = new Processo();
        processo.setId(80L);
        processo.setComarca("Fortaleza");
        when(processoRepository.findById(80L)).thenReturn(Optional.of(processo));
        UnidadeInstituicao unidadeNova = unidade(99L, TipoUnidadeInstitucional.PROMOTORIA);
        when(unidadeRepository.findByTipoAndComarca(TipoUnidadeInstitucional.PROMOTORIA, "Fortaleza")).thenReturn(List.of(unidadeNova));
        when(gravador.gravar(any())).thenAnswer(inv -> inv.getArgument(0));

        int resolvidos = service.reprocessarSemUnidade(TipoUnidadeInstitucional.PROMOTORIA);

        assertThat(resolvidos).isEqualTo(1);
        assertThat(preso.getUnidadeInstitucionalId()).isEqualTo(99L);
        assertThat(preso.getStatus()).isEqualTo(StatusSecretariaInstitucionalItem.PENDENTE);
    }

    @Test
    void reprocessarSemUnidadeContinuaPresoSeAindaNaoHouverCobertura() {
        SecretariaInstitucionalItem preso = new SecretariaInstitucionalItem();
        ReflectionTestUtils.setField(preso, "id", 9L);
        preso.setProcessoId(90L);
        preso.setTipoInstituicaoAlvo(TipoUnidadeInstitucional.NUCLEO_DEFENSORIA);
        preso.setStatus(StatusSecretariaInstitucionalItem.SEM_UNIDADE_RESOLVIDA);
        when(itemRepository.findByStatus(StatusSecretariaInstitucionalItem.SEM_UNIDADE_RESOLVIDA)).thenReturn(List.of(preso));
        Processo processo = new Processo();
        processo.setId(90L);
        processo.setComarca("Comarca Ainda Sem Cobertura");
        when(processoRepository.findById(90L)).thenReturn(Optional.of(processo));
        when(unidadeRepository.findByTipoAndComarca(TipoUnidadeInstitucional.NUCLEO_DEFENSORIA, "Comarca Ainda Sem Cobertura")).thenReturn(List.of());
        when(abrangenciaRepository.findByComarcaAtendida("Comarca Ainda Sem Cobertura")).thenReturn(List.of());

        int resolvidos = service.reprocessarSemUnidade(TipoUnidadeInstitucional.NUCLEO_DEFENSORIA);

        assertThat(resolvidos).isEqualTo(0);
        assertThat(preso.getStatus()).isEqualTo(StatusSecretariaInstitucionalItem.SEM_UNIDADE_RESOLVIDA);
        verify(gravador, never()).gravar(any());
    }

    @Test
    void perdaDeCorridaNoGravadorNaoLancaExcecaoERetornaNullSemAuditar() {
        UnidadeInstituicao unidade = unidade(40L, TipoUnidadeInstitucional.PROMOTORIA);
        when(itemRepository.existeAtivoOuSemUnidadeResolvida(10L, TipoUnidadeInstitucional.PROMOTORIA)).thenReturn(false);
        when(unidadeRepository.findByTipoAndComarca(TipoUnidadeInstitucional.PROMOTORIA, "Fortaleza")).thenReturn(List.of(unidade));
        when(gravador.gravar(any())).thenThrow(new DataIntegrityViolationException("uq_secretaria_inst_item_ativo_por_processo_tipo"));

        SecretariaInstitucionalItem item = service.enfileirar(10L, "Fortaleza", TipoUnidadeInstitucional.PROMOTORIA,
                MotivoEnfileiramentoInstitucional.PARTE_AUTOMATICA, 15);

        assertThat(item).isNull();
        verify(auditService, never()).appendSafely(any(), any());
    }

    @Test
    void reprocessarSemUnidadeRevertMutacaoEmMemoriaQuandoGravadorConflita() {
        SecretariaInstitucionalItem preso = new SecretariaInstitucionalItem();
        ReflectionTestUtils.setField(preso, "id", 11L);
        preso.setProcessoId(110L);
        preso.setTipoInstituicaoAlvo(TipoUnidadeInstitucional.PROMOTORIA);
        preso.setStatus(StatusSecretariaInstitucionalItem.SEM_UNIDADE_RESOLVIDA);
        preso.setUnidadeInstitucionalId(null);
        when(itemRepository.findByStatus(StatusSecretariaInstitucionalItem.SEM_UNIDADE_RESOLVIDA)).thenReturn(List.of(preso));
        Processo processo = new Processo();
        processo.setId(110L);
        processo.setComarca("Fortaleza");
        when(processoRepository.findById(110L)).thenReturn(Optional.of(processo));
        UnidadeInstituicao unidadeNova = unidade(101L, TipoUnidadeInstitucional.PROMOTORIA);
        when(unidadeRepository.findByTipoAndComarca(TipoUnidadeInstitucional.PROMOTORIA, "Fortaleza")).thenReturn(List.of(unidadeNova));
        when(gravador.gravar(any())).thenThrow(new DataIntegrityViolationException("uq_secretaria_inst_item_ativo_por_processo_tipo"));

        int resolvidos = service.reprocessarSemUnidade(TipoUnidadeInstitucional.PROMOTORIA);

        assertThat(resolvidos).isEqualTo(0);
        assertThat(preso.getUnidadeInstitucionalId())
                .as("mutacao em memoria precisa ser revertida — senao a entidade gerenciada fica suja na transacao externa")
                .isNull();
        assertThat(preso.getStatus()).isEqualTo(StatusSecretariaInstitucionalItem.SEM_UNIDADE_RESOLVIDA);
    }

    @Test
    void itemSemUnidadeResolvidaJaExistenteImpedeNovoEnfileiramentoDuplicado() {
        when(itemRepository.existeAtivoOuSemUnidadeResolvida(12L, TipoUnidadeInstitucional.NUCLEO_DEFENSORIA)).thenReturn(true);

        SecretariaInstitucionalItem item = service.enfileirar(12L, "Comarca Sem Cobertura", TipoUnidadeInstitucional.NUCLEO_DEFENSORIA,
                MotivoEnfileiramentoInstitucional.PARTE_AUTOMATICA, 15);

        assertThat(item).isNull();
        verify(gravador, never()).gravar(any());
        verify(unidadeRepository, never()).findByTipoAndComarca(any(), any());
    }
}
