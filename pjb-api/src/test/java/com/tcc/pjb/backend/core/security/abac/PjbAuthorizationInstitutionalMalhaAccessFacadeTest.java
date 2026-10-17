package com.tcc.pjb.backend.core.security.abac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.abac.policy.AccessPolicy;
import com.tcc.pjb.backend.core.security.abac.policy.PolicyRegistry;
import com.tcc.pjb.backend.core.security.access.ProcessoVinculoResolver;
import com.tcc.pjb.backend.core.security.access.VinculoProcessualDoUsuario;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PjbAuthorizationInstitutionalMalhaAccessFacadeTest {

    @Mock
    private PolicyRegistry policyRegistry;
    @Mock
    private AccessPolicy accessPolicy;
    @Mock
    private ProcessoVinculoResolver vinculoResolver;

    private PjbAuthorizationInstitutionalMalhaAccessFacade facade;

    @BeforeEach
    void setUp() {
        when(accessPolicy.version()).thenReturn("abac-v1.0");
        PolicyRegistry.ActivePolicy activePolicy = new PolicyRegistry.ActivePolicy(accessPolicy, null, "sha256");
        when(policyRegistry.active()).thenReturn(activePolicy);
        facade = new PjbAuthorizationInstitutionalMalhaAccessFacade(policyRegistry, vinculoResolver);
    }

    @Test
    void negaQuandoUsuarioOuProcessoAusente() {
        assertThat(facade.evaluate(null, 1L).allowed()).isFalse();
        assertThat(facade.evaluate(usuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO), null).allowed()).isFalse();
    }

    @Test
    void autorizaComOVinculoInstitucionalDoResolverENomeiaOVinculoNoMotivo() {
        Usuario promotor = usuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);
        when(vinculoResolver.vinculoInstitucional(promotor, 7L)).thenReturn(Optional.of(VinculoProcessualDoUsuario.ORGAO_NO_POLO));

        AuthzDecision decisao = facade.evaluate(promotor, 7L);

        assertThat(decisao.allowed()).isTrue();
        assertThat(decisao.reason()).isEqualTo("malha_institucional_vinculo_orgao_no_polo");
    }

    @Test
    void negaSemVinculoInstitucional() {
        Usuario desembargador = usuario(TipoUsuario.DESEMBARGADOR);
        when(vinculoResolver.vinculoInstitucional(desembargador, 7L)).thenReturn(Optional.empty());

        AuthzDecision decisao = facade.evaluate(desembargador, 7L);

        assertThat(decisao.allowed()).isFalse();
        assertThat(decisao.reason()).isEqualTo("malha_institucional_sem_vinculo_no_processo");
    }

    @Test
    void administradorSempreAutorizadoSemConsultarVinculo() {
        Usuario admin = usuario(TipoUsuario.ADMINISTRADOR);

        assertThat(facade.evaluate(admin, 7L).allowed()).isTrue();
        verifyNoInteractions(vinculoResolver);
    }

    private static Usuario usuario(TipoUsuario tipo) {
        Usuario usuario = new Usuario();
        usuario.setId(42L);
        usuario.setTipoUsuario(tipo);
        return usuario;
    }
}
