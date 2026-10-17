package com.tcc.pjb.backend.core.security.abac.policy;

import com.tcc.pjb.backend.core.security.abac.AuthzDecision;
import com.tcc.pjb.backend.core.security.access.ProcessoVinculoResolver;
import com.tcc.pjb.backend.core.security.access.VinculoProcessualDoUsuario;
import com.tcc.pjb.backend.core.security.sigilo.service.SigiloAccessService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;


class AbacV1PolicyTest {

    private final ProcessoVinculoResolver resolver = Mockito.mock(ProcessoVinculoResolver.class);
    private final AbacV1Policy policy = new AbacV1Policy(resolver, Mockito.mock(SigiloAccessService.class));

    @Test
    void publicProcess_allowsAuthenticated() {
        Usuario u = usuario(1L, TipoUsuario.ADVOGADO, "CE", true);
        Processo p = Processo.builder().id(10L).nivelSigilo(NivelSigilo.PUBLICO).build();

        AuthzDecision d = policy.canReadProcesso(u, p, null);
        assertTrue(d.allowed());
        assertEquals(AbacV1Policy.VERSION, d.policyVersion());
        assertEquals("publico", d.reason());
        verifyNoInteractions(resolver);
    }

    @Test
    void secretProcess_magistradoSemVinculoENegado() {
        Usuario juiz = usuario(1L, TipoUsuario.JUIZ, "CE", true);
        Processo p = sigiloso();
        semVinculo();

        AuthzDecision d = policy.canReadProcesso(juiz, p, "need-to-know");
        assertFalse(d.allowed());
        assertEquals("sem_vinculo_com_o_processo_sigiloso", d.reason());
    }

    @Test
    void secretProcess_magistradoComProcessoConclusoLe() {
        Usuario juiz = usuario(1L, TipoUsuario.JUIZ, "CE", true);
        Processo p = sigiloso();
        comVinculo(VinculoProcessualDoUsuario.PROCESSO_CONCLUSO);

        AuthzDecision d = policy.canReadProcesso(juiz, p, null);
        assertTrue(d.allowed());
        assertEquals("vinculo_processo_concluso", d.reason());
    }

    @Test
    void secretProcess_adminNaoLeAutosSigilosos() {
        Usuario admin = usuario(2L, TipoUsuario.ADMINISTRADOR, "CE", true);
        semVinculo();

        AuthzDecision d = policy.canReadProcesso(admin, sigiloso(), "need-to-know");
        assertFalse(d.allowed());
        assertEquals("admin_nao_le_autos_sigilosos", d.reason());
    }

    @Test
    void secretProcess_advogadoComProcuracaoLeESemVinculoNemCredencialENegado() {
        Usuario advogado = usuario(8L, TipoUsuario.ADVOGADO, "CE", true);
        comVinculo(VinculoProcessualDoUsuario.ADVOGADO_COM_PROCURACAO);
        AuthzDecision comProcuracao = policy.canReadProcesso(advogado, sigiloso(), "need-to-know");
        assertTrue(comProcuracao.allowed());
        assertEquals("vinculo_advogado_com_procuracao", comProcuracao.reason());

        semVinculo();
        AuthzDecision d = policy.canReadProcesso(advogado, sigiloso(), "need-to-know");
        assertFalse(d.allowed());
        assertEquals("advogado_sem_vinculo_e_sem_credencial", d.reason());
    }

    @Test
    void votos_peritoNomeadoNaoLeVotosDoColegiado() {
        Usuario perito = usuario(9L, TipoUsuario.PERITO, "CE", true);
        comVinculo(VinculoProcessualDoUsuario.PERITO_NOMEADO);

        assertFalse(policy.canReadVotosColegiados(perito, publico(), null).allowed());
    }

    @Test
    void votos_delegadoComTarefaEAssessorDesignadoNaoLeemVotos() {
        Usuario delegado = usuario(4L, TipoUsuario.DELEGADO_POLICIA, "CE", true);
        comVinculo(VinculoProcessualDoUsuario.TAREFA_ATRIBUIDA);
        assertFalse(policy.canReadVotosColegiados(delegado, sigiloso(), null).allowed());

        Usuario assessor = usuario(5L, TipoUsuario.ASSESSOR_JUDICIAL, "CE", true);
        comVinculo(VinculoProcessualDoUsuario.DESIGNACAO_FORMAL);
        assertFalse(policy.canReadVotosColegiados(assessor, sigiloso(), null).allowed());
    }

    @Test
    void votos_servidorComFuncaoNaUnidadeLeVotosDoProcessoSigiloso() {
        Usuario servidor = usuario(7L, TipoUsuario.SERVIDOR, "CE", true);
        comVinculo(VinculoProcessualDoUsuario.FUNCAO_NA_UNIDADE);

        AuthzDecision d = policy.canReadVotosColegiados(servidor, sigiloso(), null);
        assertTrue(d.allowed());
        assertEquals("vinculo_funcao_na_unidade_votos", d.reason());
    }

    @Test
    void votos_magistradoDesignadoLeVotosDoProcessoSigiloso() {
        Usuario desembargador = usuario(6L, TipoUsuario.DESEMBARGADOR, "CE", true);
        comVinculo(VinculoProcessualDoUsuario.DESIGNACAO_FORMAL);

        AuthzDecision d = policy.canReadVotosColegiados(desembargador, sigiloso(), null);
        assertTrue(d.allowed());
        assertEquals("vinculo_designacao_formal_votos", d.reason());
    }

    @Test
    void votos_institucionalSemVinculoLeSoEmProcessoPublico() {
        Usuario juiz = usuario(1L, TipoUsuario.JUIZ, "CE", true);
        semVinculo();

        AuthzDecision emPublico = policy.canReadVotosColegiados(juiz, publico(), null);
        assertTrue(emPublico.allowed());
        assertEquals("institucional_votos", emPublico.reason());
        assertFalse(policy.canReadVotosColegiados(juiz, sigiloso(), null).allowed());
    }

    @Test
    void votos_parteLeVotosDoProprioProcessoSigiloso() {
        Usuario cidadao = usuario(3L, TipoUsuario.CIDADAO, "CE", true);
        comVinculo(VinculoProcessualDoUsuario.PARTE);

        AuthzDecision d = policy.canReadVotosColegiados(cidadao, sigiloso(), null);
        assertTrue(d.allowed());
        assertEquals("vinculo_parte_votos", d.reason());
    }

    private void semVinculo() {
        doReturn(Optional.empty()).when(resolver).vinculo(any(), any(), any());
    }

    private void comVinculo(VinculoProcessualDoUsuario vinculo) {
        doAnswer(invocacao -> invocacao.<Set<VinculoProcessualDoUsuario>>getArgument(2).contains(vinculo) ? Optional.of(vinculo) : Optional.empty())
                .when(resolver).vinculo(any(), any(), any());
    }

    private static Processo sigiloso() {
        return Processo.builder().id(11L).nivelSigilo(NivelSigilo.SEGREDO_JUSTICA).build();
    }

    private static Processo publico() {
        return Processo.builder().id(12L).nivelSigilo(NivelSigilo.PUBLICO).build();
    }

    private static Usuario usuario(Long id, TipoUsuario tipo, String uf, boolean ativo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNome("Teste");
        u.setEmail("t" + id + "@exemplo.com");
        u.setCpf("0000000000" + (id % 10));
        u.setSenha("x");
        u.setAtivo(ativo);
        u.setUf(uf);
        u.setTipoUsuario(tipo);
        u.syncPerfilETipoUsuario();
        return u;
    }
}
