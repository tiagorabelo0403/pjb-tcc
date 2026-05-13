package com.tcc.pjb.backend.core.security.abac.policy;

import com.tcc.pjb.backend.core.security.abac.AuthzDecision;
import com.tcc.pjb.backend.core.security.sigilo.service.SigiloAccessService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.pericia.PeritoNomeacaoStatus;
import com.tcc.pjb.backend.model.repository.PeritoNomeacaoRepository;
import com.tcc.pjb.backend.service.oficial_justica.OficialJusticaProcessoVinculoService;
import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


class AbacV1PolicyTest {

    @Test
    void publicProcess_allowsAuthenticated() {
        var repo = Mockito.mock(LaianeProcuracaoRepository.class);
        var sigilo = Mockito.mock(SigiloAccessService.class);
        var peritoRepo = Mockito.mock(PeritoNomeacaoRepository.class);
        var policy = new AbacV1Policy(repo, sigilo, peritoRepo, Mockito.mock(OficialJusticaProcessoVinculoService.class));

        Usuario u = usuario(1L, TipoUsuario.ADVOGADO, "CE", true);
        Processo p = Processo.builder().id(10L).nivelSigilo(NivelSigilo.PUBLICO).build();

        AuthzDecision d = policy.canReadProcesso(u, p, null);
        assertTrue(d.allowed());
        assertEquals(AbacV1Policy.VERSION, d.policyVersion());
        assertEquals("publico", d.reason());
    }

    @Test
    void secretProcess_institutional_allowsEvenWithoutJustification() {
        var repo = Mockito.mock(LaianeProcuracaoRepository.class);
        var sigilo = Mockito.mock(SigiloAccessService.class);
        var peritoRepo = Mockito.mock(PeritoNomeacaoRepository.class);
        var policy = new AbacV1Policy(repo, sigilo, peritoRepo, Mockito.mock(OficialJusticaProcessoVinculoService.class));

        Usuario juiz = usuario(1L, TipoUsuario.JUIZ, "CE", true);
        Processo p = Processo.builder().id(10L).nivelSigilo(NivelSigilo.SEGREDO_JUSTICA).build();

        AuthzDecision d = policy.canReadProcesso(juiz, p, null);
        assertTrue(d.allowed());
        assertEquals("institucional", d.reason());
    }

    @Test
    void secretProcess_lawyerAllowsOwnerOrProcuracao_andDeniesWithoutLink() {
        var repo = Mockito.mock(LaianeProcuracaoRepository.class);
        var sigilo = Mockito.mock(SigiloAccessService.class);
        var peritoRepo = Mockito.mock(PeritoNomeacaoRepository.class);
        var policy = new AbacV1Policy(repo, sigilo, peritoRepo, Mockito.mock(OficialJusticaProcessoVinculoService.class));

        Usuario advogado = usuario(7L, TipoUsuario.ADVOGADO, "CE", true);
        Processo procOwner = Processo.builder().id(10L)
                .nivelSigilo(NivelSigilo.SEGREDO_JUSTICA)
                .usuario(advogado)
                .build();

        assertTrue(policy.canReadProcesso(advogado, procOwner, "need-to-know").allowed());

        
        Usuario adv2 = usuario(8L, TipoUsuario.ADVOGADO, "CE", true);
        Processo proc2 = Processo.builder().id(11L)
                .nivelSigilo(NivelSigilo.SEGREDO_JUSTICA)
                .usuario(usuario(99L, TipoUsuario.ADVOGADO, "CE", true))
                .build();

        when(repo.existsByAdvogadoIdAndProcessoIdAndStatus(eq(8L), eq(11L), eq(LaianeProcuracaoStatus.ATIVA)))
                .thenReturn(true);

        assertTrue(policy.canReadProcesso(adv2, proc2, "need-to-know").allowed());

        
        when(repo.existsByAdvogadoIdAndProcessoIdAndStatus(eq(8L), eq(11L), eq(LaianeProcuracaoStatus.ATIVA)))
                .thenReturn(false);

        AuthzDecision d = policy.canReadProcesso(adv2, proc2, "need-to-know");
        assertFalse(d.allowed());
        assertEquals("advogado_sem_vinculo_e_sem_credencial", d.reason());

        
        Usuario perito = usuario(9L, TipoUsuario.PERITO, "CE", true);
        when(peritoRepo.existsByProcessoIdAndPeritoIdAndStatus(eq(11L), eq(9L), eq(PeritoNomeacaoStatus.NOMEADO)))
                .thenReturn(true);
        assertTrue(policy.canReadProcesso(perito, proc2, "need-to-know").allowed());
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
