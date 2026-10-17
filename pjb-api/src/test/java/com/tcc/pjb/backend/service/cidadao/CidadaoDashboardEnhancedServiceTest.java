package com.tcc.pjb.backend.service.cidadao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.recursal.RecursalEffectiveSecrecyService;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CidadaoDashboardEnhancedServiceTest {

    private static final String NUMERO = "0001234-56.2026.8.06.0001";

    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
    private final RecursalEffectiveSecrecyService secrecyService = mock(RecursalEffectiveSecrecyService.class);
    private final PerfilDashboardContextFactory contextFactory = mock(PerfilDashboardContextFactory.class);
    private final Processo processo = new Processo();
    private CidadaoDashboardEnhancedService service;

    @BeforeEach
    void setUp() {
        Usuario cidadao = new Usuario();
        cidadao.setId(7L);
        cidadao.setTipoUsuario(TipoUsuario.CIDADAO);
        PerfilDashboardContext context = mock(PerfilDashboardContext.class);
        when(context.usuario()).thenReturn(cidadao);
        when(contextFactory.build()).thenReturn(context);
        service = new CidadaoDashboardEnhancedService(
                contextFactory, mock(PainelServiceCommons.class), processoRepository,
                mock(WorkItemRepository.class), authorizationService, secrecyService);
        processo.setId(42L);
        processo.setNumeroProcesso(NUMERO);
        processo.setTribunal("TJCE");
        when(processoRepository.findProcessoCompletoById(42L)).thenReturn(Optional.of(processo));
        when(secrecyService.effectiveSecrecyForProcesso(42L)).thenReturn(NivelSigilo.SEGREDO_JUSTICA);
    }

    private Function<Long, Map<String, Object>> leitura(String nome) {
        return switch (nome) {
            case "timeline" -> service::timelineVisualProcesso;
            case "orientacao" -> service::orientacaoAudiencia;
            case "resumo" -> service::exportarResumoProcesso;
            default -> throw new IllegalArgumentException(nome);
        };
    }

    @ParameterizedTest
    @ValueSource(strings = {"timeline", "orientacao", "resumo"})
    void parteDoProcessoLeComGuardaDeParteESigiloEfetivo(String nome) {
        Map<String, Object> out = leitura(nome).apply(42L);

        assertThat(out).containsEntry("tribunal", "TJCE");
        verify(authorizationService).requireReadProcessoAsCidadaoParte(processo);
        verify(authorizationService).requireReadProcessoAtSecrecy(processo, NivelSigilo.SEGREDO_JUSTICA);
    }

    @ParameterizedTest
    @ValueSource(strings = {"timeline", "orientacao", "resumo"})
    void cidadaoQueNaoEParteRecebeAcessoNegadoSemConsultarSigilo(String nome) {
        doThrow(new AccessDeniedPjbException("Cidadão não é parte do processo."))
                .when(authorizationService).requireReadProcessoAsCidadaoParte(processo);

        assertThatThrownBy(() -> leitura(nome).apply(42L)).isInstanceOf(AccessDeniedPjbException.class);
        verifyNoInteractions(secrecyService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"timeline", "orientacao", "resumo"})
    void parteSemConfirmacaoExigidaPeloSigiloRecebeAcessoNegado(String nome) {
        doThrow(new AccessDeniedPjbException("Acesso negado ao processo"))
                .when(authorizationService).requireReadProcessoAtSecrecy(processo, NivelSigilo.SEGREDO_JUSTICA);

        assertThatThrownBy(() -> leitura(nome).apply(42L)).isInstanceOf(AccessDeniedPjbException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"timeline", "orientacao", "resumo"})
    void usuarioSemPerfilDeCidadaoEBarradoAntesDeCarregarOProcesso(String nome) {
        doThrow(new AccessDeniedPjbException("Acesso negado ao perfil informado."))
                .when(authorizationService).requireRole(any(), eq("ROLE_CIDADAO"));

        assertThatThrownBy(() -> leitura(nome).apply(42L)).isInstanceOf(AccessDeniedPjbException.class);
        verify(processoRepository, never()).findProcessoCompletoById(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"timeline", "orientacao", "resumo"})
    void processoInexistenteRespondeNaoEncontradoSemAvaliarAcesso(String nome) {
        assertThatThrownBy(() -> leitura(nome).apply(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
        verify(authorizationService, never()).requireReadProcessoAsCidadaoParte(any());
    }
}
