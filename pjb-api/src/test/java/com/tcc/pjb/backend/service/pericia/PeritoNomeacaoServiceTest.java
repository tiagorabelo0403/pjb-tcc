package com.tcc.pjb.backend.service.pericia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.pericia.PeritoNomeacaoRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.PeritoNomeacaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.service.profile.PerfilOnboardingService;
import com.tcc.pjb.backend.service.profile.PerfilRealtimeTopicService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PeritoNomeacaoServiceTest {

    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final PeritoNomeacaoRepository nomeacaoRepository = mock(PeritoNomeacaoRepository.class);
    private final WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
    private final AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
    private final OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
    private final PerfilRealtimeTopicService realtimeTopicService = mock(PerfilRealtimeTopicService.class);
    private final PerfilOnboardingService perfilOnboardingService = mock(PerfilOnboardingService.class);

    private PeritoNomeacaoService service;

    @BeforeEach
    void setUp() {
        service = new PeritoNomeacaoService(
                processoRepository, usuarioRepository, nomeacaoRepository,
                workItemRepository, currentUserService, authorizationService,
                auditLedgerService, outboxPublisher, realtimeTopicService,
                perfilOnboardingService);
    }

    @Test
    void deveLancarValidacaoQuandoRequisicaoNula() {
        assertThatThrownBy(() -> service.nomear(1L, null))
                .isInstanceOf(ErroDeValidacaoException.class);
    }

    @Test
    void deveLancarValidacaoQuandoPeritoIdNulo() {
        PeritoNomeacaoRequest req = new PeritoNomeacaoRequest(null, null);

        assertThatThrownBy(() -> service.nomear(1L, req))
                .isInstanceOf(ErroDeValidacaoException.class);
    }

    @Test
    void deveLancarAccessDeniedQuandoAtorNaoPodeNomear() {
        Usuario ator = usuarioComTipo(TipoUsuario.ADVOGADO);
        when(currentUserService.getRequired()).thenReturn(ator);

        PeritoNomeacaoRequest req = new PeritoNomeacaoRequest(99L, null);

        assertThatThrownBy(() -> service.nomear(1L, req))
                .isInstanceOf(AccessDeniedPjbException.class);
    }

    @Test
    void deveLancarRecursoNaoEncontradoQuandoProcessoInexistente() {
        Usuario ator = usuarioComTipo(TipoUsuario.JUIZ);
        when(currentUserService.getRequired()).thenReturn(ator);
        when(processoRepository.findById(99L)).thenReturn(Optional.empty());

        PeritoNomeacaoRequest req = new PeritoNomeacaoRequest(10L, null);

        assertThatThrownBy(() -> service.nomear(99L, req))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Processo");
    }

    @Test
    void deveLancarRecursoNaoEncontradoAoListarProcessoInexistente() {
        Usuario ator = usuarioComTipo(TipoUsuario.SERVIDOR_FORUM);
        when(currentUserService.getRequired()).thenReturn(ator);
        when(processoRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listar(77L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Processo");
    }

    @Test
    void deveRetornarFalsoQuandoProcessoIdOuPeritoIdNulos() {
        assertThat(service.isPeritoNomeado(null, null)).isFalse();
        assertThat(service.isPeritoNomeado(1L, null)).isFalse();
        assertThat(service.isPeritoNomeado(null, 1L)).isFalse();
    }

    private Usuario usuarioComTipo(TipoUsuario tipo) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setTipoUsuario(tipo);
        return u;
    }
}
