package com.tcc.pjb.backend.service.magistratura.acts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.security.persona.PersonaKey;
import com.tcc.pjb.backend.core.security.persona.UserPersona;
import com.tcc.pjb.backend.core.security.persona.UserPersonaService;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCode;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCommandRequest;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActWorkspaceResponse;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.juiz.guardrails.JuizProcessoGuardRailService;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MagistraturaJudicialActWorkbenchServiceTest {

    @Mock private CurrentUserService currentUserService;
    @Mock private UserPersonaService personaService;
    @Mock private PjbAuthorizationService authorizationService;
    @Mock private ProcessoRepository processoRepository;
    @Mock private JuizProcessoGuardRailService guardRailService;
    @Mock private MagistraturaJudicialProvidenceAutomationService providenceAutomationService;
    @Mock private MagistraturaJudicialActExecutionSupport executionSupport;

    private MagistraturaJudicialActProjectionSupport projectionSupport;
    private MagistraturaJudicialActWorkbenchService service;

    @BeforeEach
    void setUp() {
        projectionSupport = new MagistraturaJudicialActProjectionSupport();
        service = new MagistraturaJudicialActWorkbenchService(
                currentUserService,
                personaService,
                authorizationService,
                processoRepository,
                guardRailService,
                providenceAutomationService,
                projectionSupport,
                executionSupport
        );
    }

    @Test
    void workspaceDoMinistroDeveProjetarTrilhaSuperiorSemDuplicacao() {
        Usuario usuario = new Usuario();
        usuario.setId(9L);
        usuario.setNome("Ministro Teste");
        usuario.setTipoUsuario(TipoUsuario.MINISTRO);
        usuario.setUf("DF");

        UserPersona persona = new UserPersona(
                TipoUsuario.MINISTRO,
                PersonaKey.MINISTRO,
                "Ministro",
                "Excelência",
                GrauJurisdicao.SUPERIOR,
                EsferaJurisdicao.JUSTICA_FEDERAL,
                false
        );

        when(currentUserService.getRequired()).thenReturn(usuario);
        when(personaService.getRequiredPersona()).thenReturn(persona);

        MagistraturaJudicialActWorkspaceResponse response = service.workspace(null);

        assertThat(response.lane()).isEqualTo("SUPERIOR");
        assertThat(response.acts()).extracting(it -> it.code())
                .containsExactly(
                        MagistraturaJudicialActCode.DECISAO_MONOCRATICA,
                        MagistraturaJudicialActCode.INCLUSAO_PAUTA,
                        MagistraturaJudicialActCode.DECISAO_PLENARIA,
                        MagistraturaJudicialActCode.NOMEACAO_PERITO
                );
    }

    @Test
    void executeDeveDelegarParaExecutionSupportEDispatchProvidencias() {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setNome("Juiz Teste");
        usuario.setTipoUsuario(TipoUsuario.JUIZ_ESTADUAL);
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");

        UserPersona persona = new UserPersona(
                TipoUsuario.JUIZ_ESTADUAL,
                PersonaKey.JUIZ_ESTADUAL,
                "Juiz Estadual",
                "Excelência",
                GrauJurisdicao.PRIMEIRO_GRAU,
                EsferaJurisdicao.JUSTICA_ESTADUAL,
                false
        );

        Processo processo = Processo.builder()
                .id(77L)
                .numeroProcesso("0001234-56.2026.8.06.0001")
                .build();

        JuizProcessoGuardRailService.GuardRailSnapshot guard = org.mockito.Mockito.mock(JuizProcessoGuardRailService.GuardRailSnapshot.class);
        when(guard.allowed()).thenReturn(true);
        when(guard.metrics()).thenReturn(Map.of("allowed", true));
        when(guard.verdictBand()).thenReturn("LIBERADO");
        when(guard.fundamentos()).thenReturn(List.of("ok"));

        when(currentUserService.getRequired()).thenReturn(usuario);
        when(personaService.getRequiredPersona()).thenReturn(persona);
        when(processoRepository.findMagistraturaActsScopedById(77L)).thenReturn(Optional.of(processo));
        when(guardRailService.avaliar(any(Processo.class), eq(usuario), eq(persona), eq("DESPACHO"))).thenReturn(guard);
        when(executionSupport.execute(eq(processo), eq(usuario), eq(77L), eq(MagistraturaJudicialActCode.DESPACHO), any()))
                .thenReturn(Map.of("status", "ASSINADO", "processoId", 77L));
        when(providenceAutomationService.dispatch(eq(processo), eq(usuario), eq(MagistraturaJudicialActCode.DESPACHO), any(), any()))
                .thenReturn(List.of());

        MagistraturaJudicialActCommandRequest request = new MagistraturaJudicialActCommandRequest(
                "DESPACHO",
                "Intime-se.",
                "CPC",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        var response = service.execute(77L, request);

        verify(authorizationService).requireReadProcesso(processo);
        verify(executionSupport).execute(eq(processo), eq(usuario), eq(77L), eq(MagistraturaJudicialActCode.DESPACHO), any());
        assertThat(response.action()).isEqualTo(MagistraturaJudicialActCode.DESPACHO);
        assertThat(response.status()).isEqualTo("ASSINADO");
    }

    @Test
    void previewDeveProjetarRazoesEMetricasComProjectionSupportExtraido() {
        Usuario usuario = new Usuario();
        usuario.setId(12L);
        usuario.setNome("Desembargador Teste");
        usuario.setTipoUsuario(TipoUsuario.DESEMBARGADOR);
        usuario.setUf("CE");

        UserPersona persona = new UserPersona(
                TipoUsuario.DESEMBARGADOR,
                PersonaKey.DESEMBARGADOR,
                "Desembargador",
                "Excelência",
                GrauJurisdicao.SEGUNDO_GRAU,
                EsferaJurisdicao.JUSTICA_ESTADUAL,
                false
        );

        Processo processo = Processo.builder()
                .id(88L)
                .numeroProcesso("0009999-00.2026.8.06.0001")
                .build();

        JuizProcessoGuardRailService.GuardRailSnapshot guard = org.mockito.Mockito.mock(JuizProcessoGuardRailService.GuardRailSnapshot.class);
        when(guard.allowed()).thenReturn(true);
        when(guard.metrics()).thenReturn(Map.of("allowed", true));
        when(guard.verdictBand()).thenReturn("LIBERADO");
        when(guard.fundamentos()).thenReturn(List.of("colegiado apto"));

        when(currentUserService.getRequired()).thenReturn(usuario);
        when(personaService.getRequiredPersona()).thenReturn(persona);
        when(processoRepository.findMagistraturaActsScopedById(88L)).thenReturn(Optional.of(processo));
        when(guardRailService.avaliar(eq(processo), eq(usuario), eq(persona), eq("DESPACHO_RELATOR"))).thenReturn(guard);
        when(providenceAutomationService.preview(eq(processo), eq(usuario), eq(MagistraturaJudicialActCode.DESPACHO_RELATOR), any()))
                .thenReturn(List.of());

        var response = service.preview(88L, "DESPACHO_RELATOR");

        assertThat(response.allowed()).isTrue();
        assertThat(response.lane()).isEqualTo("SEGUNDO_GRAU");
        assertThat(response.nativeRoute()).contains("/api/v1/magistratura/processos/88/atos");
        assertThat(response.reasons()).anyMatch(reason -> reason.contains("DESPACHO_RELATOR"));
        assertThat(response.metrics()).containsEntry("lane", "SEGUNDO_GRAU");
    }

    @Test
    void requireProcessoDeveFalharQuandoNaoEncontrado() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setTipoUsuario(TipoUsuario.JUIZ_ESTADUAL);

        UserPersona persona = new UserPersona(
                TipoUsuario.JUIZ_ESTADUAL,
                PersonaKey.JUIZ_ESTADUAL,
                "Juiz Estadual",
                "Excelência",
                GrauJurisdicao.PRIMEIRO_GRAU,
                EsferaJurisdicao.JUSTICA_ESTADUAL,
                false
        );

        when(currentUserService.getRequired()).thenReturn(usuario);
        when(personaService.getRequiredPersona()).thenReturn(persona);
        when(processoRepository.findMagistraturaActsScopedById(999L)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.preview(999L, "DESPACHO"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
