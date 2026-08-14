package com.tcc.pjb.backend.core.security.abac;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.GovBrAssuranceExtractor;
import com.tcc.pjb.backend.core.security.abac.policy.AccessPolicy;
import com.tcc.pjb.backend.core.security.abac.policy.PolicyRegistry;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.AcaoProcessualServidor;
import com.tcc.pjb.backend.model.entity.enums.FuncaoServidorJudiciario;
import com.tcc.pjb.backend.model.entity.servidor.FuncaoServidorJudiciarioEntity;
import com.tcc.pjb.backend.model.repository.FuncaoServidorJudiciarioRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PjbAuthorizationFuncaoServidorFacadeTest {

    @Mock
    private PolicyRegistry policyRegistry;
    @Mock
    private AccessPolicy accessPolicy;
    @Mock
    private FuncaoServidorJudiciarioRepository funcaoServidorRepository;
    @Mock
    private CurrentUserService currentUserService;

    private PjbAuthorizationFuncaoServidorFacade facade;

    @BeforeEach
    void setUp() {
        when(accessPolicy.version()).thenReturn("abac-v1.0");
        PolicyRegistry.ActivePolicy activePolicy = new PolicyRegistry.ActivePolicy(accessPolicy, null, "sha256");
        when(policyRegistry.active()).thenReturn(activePolicy);

        Usuario usuario = new Usuario();
        usuario.setId(10L);
        when(currentUserService.getOptional()).thenReturn(Optional.of(usuario));

        PjbAuthorizationDecisionContextResolver contextResolver =
                new PjbAuthorizationDecisionContextResolver(currentUserService, new GovBrAssuranceExtractor());
        PjbAuthorizationTrailAssembler trailAssembler = new PjbAuthorizationTrailAssembler();
        facade = new PjbAuthorizationFuncaoServidorFacade(policyRegistry, contextResolver, funcaoServidorRepository, trailAssembler);
    }

    @Test
    void funcaoAtivaComCapacidade_permiteAcao() {
        FuncaoServidorJudiciarioEntity entidade = new FuncaoServidorJudiciarioEntity(
                10L, 5L, FuncaoServidorJudiciario.DIRETOR_SECRETARIA, LocalDate.now().minusDays(30), null, null);
        when(funcaoServidorRepository.findByUsuarioIdAndUnidadeIdAndAtivo(10L, 5L, true))
                .thenReturn(List.of(entidade));

        PjbAuthorizationEvaluation evaluation =
                facade.evaluate(5L, AcaoProcessualServidor.PROFERIR, Processo.builder().build());

        assertThat(evaluation.allowed()).isTrue();
    }

    @Test
    void funcaoAtivaSemCapacidade_negaAcao() {
        FuncaoServidorJudiciarioEntity entidade = new FuncaoServidorJudiciarioEntity(
                10L, 5L, FuncaoServidorJudiciario.ASSISTENTE_JUDICIARIO, LocalDate.now().minusDays(30), null, null);
        when(funcaoServidorRepository.findByUsuarioIdAndUnidadeIdAndAtivo(10L, 5L, true))
                .thenReturn(List.of(entidade));

        PjbAuthorizationEvaluation evaluation =
                facade.evaluate(5L, AcaoProcessualServidor.PROFERIR, Processo.builder().build());

        assertThat(evaluation.allowed()).isFalse();
    }

    @Test
    void semFuncaoAtivaNaUnidade_negaAcao() {
        when(funcaoServidorRepository.findByUsuarioIdAndUnidadeIdAndAtivo(10L, 5L, true))
                .thenReturn(List.of());

        PjbAuthorizationEvaluation evaluation =
                facade.evaluate(5L, AcaoProcessualServidor.PROFERIR, Processo.builder().build());

        assertThat(evaluation.allowed()).isFalse();
    }

    @Test
    void unidadeIdNulo_negaAcaoPorContextoInvalido() {
        PjbAuthorizationEvaluation evaluation =
                facade.evaluate(null, AcaoProcessualServidor.PROFERIR, Processo.builder().build());

        assertThat(evaluation.allowed()).isFalse();
        assertThat(evaluation.decision().reason()).isEqualTo("funcao_servidor_contexto_invalido");
    }

    @Test
    void designacaoComDataInicioFutura_naoEstaVigente_negaAcao() {
        FuncaoServidorJudiciarioEntity entidade = new FuncaoServidorJudiciarioEntity(
                10L, 5L, FuncaoServidorJudiciario.DIRETOR_SECRETARIA, LocalDate.now().plusDays(10), null, null);
        when(funcaoServidorRepository.findByUsuarioIdAndUnidadeIdAndAtivo(10L, 5L, true))
                .thenReturn(List.of(entidade));

        PjbAuthorizationEvaluation evaluation =
                facade.evaluate(5L, AcaoProcessualServidor.PROFERIR, Processo.builder().build());

        assertThat(evaluation.allowed()).isFalse();
    }
}
