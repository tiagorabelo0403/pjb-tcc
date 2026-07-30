package com.tcc.pjb.backend.service.processual.recursal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.service.advogado.AdvogadoCockpitService;
import com.tcc.pjb.backend.service.dashboard.PerfilPainelSupportService;
import com.tcc.pjb.backend.service.defensor.DefensorPublicoPainelService;
import com.tcc.pjb.backend.service.mp.MinisterioPublicoPainelService;
import com.tcc.pjb.backend.service.procuradoria.ProcuradoriaOperacionalService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecursalPeticionamentoPerfilRouterTest {

    private PerfilPainelSupportService perfilPainelSupportService;
    private AdvogadoCockpitService advogadoCockpitService;
    private DefensorPublicoPainelService defensorPublicoPainelService;
    private MinisterioPublicoPainelService ministerioPublicoPainelService;
    private ProcuradoriaOperacionalService procuradoriaOperacionalService;
    private RecursalPeticionamentoFacadeService recursalPeticionamentoFacadeService;
    private RecursalPeticionamentoPerfilRouter router;

    @BeforeEach
    void setUp() {
        perfilPainelSupportService = mock(PerfilPainelSupportService.class);
        advogadoCockpitService = mock(AdvogadoCockpitService.class);
        defensorPublicoPainelService = mock(DefensorPublicoPainelService.class);
        ministerioPublicoPainelService = mock(MinisterioPublicoPainelService.class);
        procuradoriaOperacionalService = mock(ProcuradoriaOperacionalService.class);
        recursalPeticionamentoFacadeService = mock(RecursalPeticionamentoFacadeService.class);
        router = new RecursalPeticionamentoPerfilRouter(
                perfilPainelSupportService,
                advogadoCockpitService,
                defensorPublicoPainelService,
                ministerioPublicoPainelService,
                procuradoriaOperacionalService,
                recursalPeticionamentoFacadeService
        );
    }

    @Test
    void resolverPerfilAtivo_advogado_retornaAdvocaciaComRateLimitLawyer() {
        givenCurrentUser(TipoUsuario.ADVOGADO);

        RecursalPeticionamentoPerfilRouter.Perfil perfil = router.resolverPerfilAtivo();

        assertThat(perfil).isEqualTo(RecursalPeticionamentoPerfilRouter.Perfil.ADVOCACIA);
        assertThat(perfil.rateLimitDomain()).isEqualTo(CapabilityRateLimitDomain.LAWYER);
        assertThat(perfil.scopeToken()).isEqualTo("advocacia");
    }

    @Test
    void resolverPerfilAtivo_defensorPublico_retornaDefensoriaComRateLimitInstitucional() {
        givenCurrentUser(TipoUsuario.DEFENSOR_PUBLICO);

        RecursalPeticionamentoPerfilRouter.Perfil perfil = router.resolverPerfilAtivo();

        assertThat(perfil).isEqualTo(RecursalPeticionamentoPerfilRouter.Perfil.DEFENSORIA);
        assertThat(perfil.rateLimitDomain()).isEqualTo(CapabilityRateLimitDomain.INSTITUCIONAL);
    }

    @Test
    void resolverPerfilAtivo_membroMp_retornaMinisterioPublico() {
        givenCurrentUser(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);

        RecursalPeticionamentoPerfilRouter.Perfil perfil = router.resolverPerfilAtivo();

        assertThat(perfil).isEqualTo(RecursalPeticionamentoPerfilRouter.Perfil.MINISTERIO_PUBLICO);
    }

    @Test
    void resolverPerfilAtivo_procurador_retornaProcuradoria() {
        givenCurrentUser(TipoUsuario.PROCURADOR);

        RecursalPeticionamentoPerfilRouter.Perfil perfil = router.resolverPerfilAtivo();

        assertThat(perfil).isEqualTo(RecursalPeticionamentoPerfilRouter.Perfil.PROCURADORIA);
    }

    @Test
    void resolverPerfilAtivo_procuradorGeralRepublica_retornaMinisterioPublicoPorClassificacaoDoEnum() {
        givenCurrentUser(TipoUsuario.PROCURADOR_GERAL_REPUBLICA);

        RecursalPeticionamentoPerfilRouter.Perfil perfil = router.resolverPerfilAtivo();

        assertThat(perfil).isEqualTo(RecursalPeticionamentoPerfilRouter.Perfil.MINISTERIO_PUBLICO);
    }

    @Test
    void resolverPerfilAtivo_cidadao_retornaCidadaoComRateLimitCitizen() {
        givenCurrentUser(TipoUsuario.CIDADAO);

        RecursalPeticionamentoPerfilRouter.Perfil perfil = router.resolverPerfilAtivo();

        assertThat(perfil).isEqualTo(RecursalPeticionamentoPerfilRouter.Perfil.CIDADAO);
        assertThat(perfil.rateLimitDomain()).isEqualTo(CapabilityRateLimitDomain.CITIZEN);
        assertThat(perfil.scopeToken()).isEqualTo("cidadao");
    }

    @Test
    void resolverPerfilAtivo_perfilSemHabilitacaoRecursal_lancaIllegalArgument() {
        givenCurrentUser(TipoUsuario.JUIZ);

        assertThatThrownBy(() -> router.resolverPerfilAtivo())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JUIZ");
    }

    @Test
    void resolverPerfilAtivo_usuarioSemTipo_lancaIllegalArgument() {
        Usuario usuario = new Usuario();
        when(perfilPainelSupportService.currentUser()).thenReturn(usuario);

        assertThatThrownBy(() -> router.resolverPerfilAtivo())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("perfil ativo");
    }

    @Test
    void interporRecurso_advocacia_delegaAoAdvogadoCockpit() {
        Map<String, Object> esperado = Map.of("status", "RECURSO_INTERPOSTO", "perfil", "ADVOCACIA");
        when(advogadoCockpitService.interprorRecurso(7L, "APELACAO", "razoes", "fundamentacao", true, false, "obs"))
                .thenReturn(esperado);

        Map<String, Object> resultado = router.interporRecurso(
                RecursalPeticionamentoPerfilRouter.Perfil.ADVOCACIA,
                7L, "APELACAO", "razoes", "fundamentacao", true, false, "obs");

        assertThat(resultado).isEqualTo(esperado);
        verifyNoInteractions(defensorPublicoPainelService, ministerioPublicoPainelService, procuradoriaOperacionalService);
    }

    @Test
    void interporRecurso_defensoria_delegaAoDefensor() {
        Map<String, Object> esperado = Map.of("status", "RECURSO_INTERPOSTO", "perfil", "DEFENSORIA");
        when(defensorPublicoPainelService.interporRecurso(9L, "APELACAO", "r", "f", false, true, null))
                .thenReturn(esperado);

        Map<String, Object> resultado = router.interporRecurso(
                RecursalPeticionamentoPerfilRouter.Perfil.DEFENSORIA,
                9L, "APELACAO", "r", "f", false, true, null);

        assertThat(resultado).isEqualTo(esperado);
        verifyNoInteractions(advogadoCockpitService, ministerioPublicoPainelService, procuradoriaOperacionalService);
    }

    @Test
    void interporRecurso_ministerioPublico_delegaAoMp() {
        Map<String, Object> esperado = Map.of("status", "RECURSO_INTERPOSTO", "perfil", "MINISTERIO_PUBLICO");
        when(ministerioPublicoPainelService.interporRecurso(11L, "RESP", "r", "f", true, true, "obs"))
                .thenReturn(esperado);

        Map<String, Object> resultado = router.interporRecurso(
                RecursalPeticionamentoPerfilRouter.Perfil.MINISTERIO_PUBLICO,
                11L, "RESP", "r", "f", true, true, "obs");

        assertThat(resultado).isEqualTo(esperado);
        verifyNoInteractions(advogadoCockpitService, defensorPublicoPainelService, procuradoriaOperacionalService);
    }

    @Test
    void interporRecurso_procuradoria_delegaAoProcuradoria() {
        Map<String, Object> esperado = Map.of("status", "RECURSO_INTERPOSTO", "perfil", "PROCURADORIA");
        when(procuradoriaOperacionalService.interporRecurso(13L, "EMBARGOS_DECLARACAO", "r", "f", false, false, null))
                .thenReturn(esperado);

        Map<String, Object> resultado = router.interporRecurso(
                RecursalPeticionamentoPerfilRouter.Perfil.PROCURADORIA,
                13L, "EMBARGOS_DECLARACAO", "r", "f", false, false, null);

        assertThat(resultado).isEqualTo(esperado);
        verifyNoInteractions(advogadoCockpitService, defensorPublicoPainelService, ministerioPublicoPainelService);
    }

    @Test
    void interporRecurso_cidadao_delegaAFachadaUnica() {
        Map<String, Object> esperado = Map.of("status", "RECURSO_INTERPOSTO", "perfil", "CIDADAO");
        when(recursalPeticionamentoFacadeService.interporRecurso(15L, "EMBARGOS_DECLARACAO", "r", "f", false, false, null))
                .thenReturn(esperado);

        Map<String, Object> resultado = router.interporRecurso(
                RecursalPeticionamentoPerfilRouter.Perfil.CIDADAO,
                15L, "EMBARGOS_DECLARACAO", "r", "f", false, false, null);

        assertThat(resultado).isEqualTo(esperado);
        verifyNoInteractions(advogadoCockpitService, defensorPublicoPainelService, ministerioPublicoPainelService, procuradoriaOperacionalService);
    }

    private void givenCurrentUser(TipoUsuario tipoUsuario) {
        Usuario usuario = new Usuario();
        usuario.setTipoUsuario(tipoUsuario);
        when(perfilPainelSupportService.currentUser()).thenReturn(usuario);
    }
}
