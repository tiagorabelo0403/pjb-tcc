package com.tcc.pjb.backend.core.security.abac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.abac.policy.AccessPolicy;
import com.tcc.pjb.backend.core.security.abac.policy.PolicyRegistry;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.processo.PoloProcessual;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.PoloProcessualRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.oficial_justica.OficialJusticaProcessoVinculoService;
import java.util.List;
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
    private PoloProcessualRepository poloProcessualRepository;
    @Mock
    private WorkItemRepository workItemRepository;
    @Mock
    private OficialJusticaProcessoVinculoService oficialJusticaProcessoVinculoService;

    private PjbAuthorizationInstitutionalMalhaAccessFacade facade;

    @BeforeEach
    void setUp() {
        when(accessPolicy.version()).thenReturn("abac-v1.0");
        PolicyRegistry.ActivePolicy activePolicy = new PolicyRegistry.ActivePolicy(accessPolicy, null, "sha256");
        when(policyRegistry.active()).thenReturn(activePolicy);
        facade = new PjbAuthorizationInstitutionalMalhaAccessFacade(
                policyRegistry, poloProcessualRepository, workItemRepository, oficialJusticaProcessoVinculoService);
    }

    @Test
    void negaQuandoUsuarioOuProcessoAusente() {
        assertThat(facade.evaluate(null, 1L).allowed()).isFalse();
        assertThat(facade.evaluate(usuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO), null).allowed()).isFalse();
    }

    @Test
    void promotorComPoloDeMpAtivoNoProcessoEAutorizado() {
        Usuario promotor = usuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);
        PoloProcessual polo = poloAtivo();
        when(poloProcessualRepository.findByProcessoIdAndTipoPolo(7L, TipoPolo.MINISTERIO_PUBLICO))
                .thenReturn(List.of(polo));

        assertThat(facade.evaluate(promotor, 7L).allowed()).isTrue();
    }

    @Test
    void promotorSemPoloDeMpNoProcessoENegado() {
        Usuario promotor = usuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);
        when(poloProcessualRepository.findByProcessoIdAndTipoPolo(7L, TipoPolo.MINISTERIO_PUBLICO))
                .thenReturn(List.of());

        assertThat(facade.evaluate(promotor, 7L).allowed()).isFalse();
    }

    @Test
    void defensorNaoEnxergaProcessoQueSoTemPoloDeMinisterioPublico() {
        Usuario defensor = usuario(TipoUsuario.DEFENSOR_PUBLICO);
        when(poloProcessualRepository.findByProcessoIdAndTipoPolo(7L, TipoPolo.DEFENSORIA))
                .thenReturn(List.of());

        assertThat(facade.evaluate(defensor, 7L).allowed()).isFalse();
    }

    @Test
    void oficialDeJusticaComVinculoDiretoEAutorizado() {
        Usuario oficial = usuario(TipoUsuario.OFICIAL_JUSTICA);
        when(oficialJusticaProcessoVinculoService.possuiVinculoDireto(eq(7L), eq(oficial.getId()), eq(TipoUsuario.OFICIAL_JUSTICA)))
                .thenReturn(true);

        assertThat(facade.evaluate(oficial, 7L).allowed()).isTrue();
    }

    @Test
    void oficialDeJusticaSemVinculoENegado() {
        Usuario oficial = usuario(TipoUsuario.OFICIAL_JUSTICA);
        when(oficialJusticaProcessoVinculoService.possuiVinculoDireto(anyLong(), anyLong(), any()))
                .thenReturn(false);

        assertThat(facade.evaluate(oficial, 7L).allowed()).isFalse();
    }

    @Test
    void desembargadorComWorkItemAtribuidoNoProcessoEAutorizado() {
        Usuario desembargador = usuario(TipoUsuario.DESEMBARGADOR);
        when(workItemRepository.findByProcessoIdAndAssignedUserIdAndRolesAndStatusNot(
                eq(7L), eq(desembargador.getId()), eq(List.of(TipoUsuario.DESEMBARGADOR)), eq(WorkItemStatus.CANCELADO), any()))
                .thenReturn(List.of(new WorkItem()));

        assertThat(facade.evaluate(desembargador, 7L).allowed()).isTrue();
    }

    @Test
    void desembargadorSemWorkItemNoProcessoENegado() {
        Usuario desembargador = usuario(TipoUsuario.DESEMBARGADOR);
        when(workItemRepository.findByProcessoIdAndAssignedUserIdAndRolesAndStatusNot(
                anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(List.of());

        assertThat(facade.evaluate(desembargador, 7L).allowed()).isFalse();
    }

    @Test
    void delegadoSegueOMesmoPadraoDeWorkItemQueDesembargador() {
        Usuario delegado = usuario(TipoUsuario.DELEGADO_POLICIA);
        when(workItemRepository.findByProcessoIdAndAssignedUserIdAndRolesAndStatusNot(
                eq(7L), eq(delegado.getId()), eq(List.of(TipoUsuario.DELEGADO_POLICIA)), eq(WorkItemStatus.CANCELADO), any()))
                .thenReturn(List.of(new WorkItem()));

        assertThat(facade.evaluate(delegado, 7L).allowed()).isTrue();
    }

    @Test
    void papelNaoReconhecidoENegadoPorPadrao() {
        Usuario cidadao = usuario(TipoUsuario.CIDADAO);

        assertThat(facade.evaluate(cidadao, 7L).allowed()).isFalse();
    }

    @Test
    void administradorSempreAutorizadoSemConsultarRepositorios() {
        Usuario admin = usuario(TipoUsuario.ADMINISTRADOR);

        assertThat(facade.evaluate(admin, 7L).allowed()).isTrue();
    }

    private static Usuario usuario(TipoUsuario tipo) {
        Usuario usuario = new Usuario();
        usuario.setId(42L);
        usuario.setTipoUsuario(tipo);
        return usuario;
    }

    private static PoloProcessual poloAtivo() {
        PoloProcessual polo = new PoloProcessual(7L, com.tcc.pjb.backend.model.entity.enums.TipoPolo.MINISTERIO_PUBLICO,
                com.tcc.pjb.backend.model.entity.enums.TipoParte.MINISTERIO_PUBLICO, "Ministério Público", null, null,
                null, null, null, null, null, 1);
        return polo;
    }
}
