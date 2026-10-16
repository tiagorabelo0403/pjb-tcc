package com.tcc.pjb.backend.core.security.device;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.security.SecurityDualApprovalRequest;
import com.tcc.pjb.backend.model.entity.security.UserSecurityProfile;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.security.UserSecurityProfileRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DescongelamentoContaServiceTest {

    private static final long CONGELADO = 50L;
    private static final long APROVACAO = 900L;

    private final SecurityDualApprovalService dualApprovalService = mock(SecurityDualApprovalService.class);
    private final UserSecurityProfileRepository profileRepo = mock(UserSecurityProfileRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final SecurityAlertService alertService = mock(SecurityAlertService.class);
    private final AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
    private final DescongelamentoContaService service = new DescongelamentoContaService(
            dualApprovalService, profileRepo, usuarioRepository, alertService, auditLedgerService);

    private final Usuario solicitante = usuario(1L, TipoUsuario.ADMINISTRADOR);
    private final Usuario aprovador = usuario(2L, TipoUsuario.ADMINISTRADOR);
    private final Usuario titular = usuario(CONGELADO, TipoUsuario.ADVOGADO);
    private UserSecurityProfile perfil;

    private static Usuario usuario(long id, TipoUsuario tipo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setTipoUsuario(tipo);
        return usuario;
    }

    @BeforeEach
    void contaCongeladaPorPanico() {
        perfil = new UserSecurityProfile();
        perfil.setFrozenAt(LocalDateTime.now().minusHours(1));
        perfil.setFreezeReason("celular roubado");
        when(profileRepo.findByUsuarioId(CONGELADO)).thenReturn(Optional.of(perfil));
        when(usuarioRepository.findById(CONGELADO)).thenReturn(Optional.of(titular));
    }

    private void aprovacaoValidaDadaPor(Usuario quemAprovou) {
        SecurityDualApprovalRequest aprovacao = new SecurityDualApprovalRequest();
        aprovacao.setApprovedBy(quemAprovou);
        when(dualApprovalService.isApprovedFor(solicitante, APROVACAO, DescongelamentoContaService.hashDaAcao(CONGELADO, perfil.getFrozenAt()), null)).thenReturn(true);
        when(dualApprovalService.getRequired(APROVACAO)).thenReturn(aprovacao);
        when(dualApprovalService.consumirAprovacao(APROVACAO)).thenReturn(true);
    }

    @Test
    void solicitacaoDeAdministradorAbrePedidoDeDuplaAprovacaoAmarradoAoEpisodioDeCongelamentoEAudita() {
        SecurityDualApprovalRequest pedido = new SecurityDualApprovalRequest();
        pedido.setId(APROVACAO);
        when(dualApprovalService.request(any(), any(), any(), any(), any(), any(), any(), any(), anyInt())).thenReturn(pedido);

        service.solicitar(solicitante, 10L, CONGELADO, "/api/v1/admin/security/contas-congeladas/50/descongelamento/solicitacao");

        verify(dualApprovalService).request(eq(solicitante), eq(10L), isNull(), eq(DescongelamentoContaService.ACAO), eq("POST"),
                anyString(), eq("panic-unfreeze"), eq(DescongelamentoContaService.hashDaAcao(CONGELADO, perfil.getFrozenAt())), anyInt());
        verify(auditLedgerService).appendSafely(eq("ACCOUNT_UNFREEZE_REQUESTED"), eq("SECURITY"), eq("50"), eq("solicitante=1 aprovacao=900"));
    }

    @Test
    void aprovacaoDeOutroEpisodioDeCongelamentoNaoDescongelaONovo() {
        aprovacaoValidaDadaPor(aprovador);
        perfil.setFrozenAt(LocalDateTime.now().minusMinutes(2));

        assertThatThrownBy(() -> service.executar(solicitante, APROVACAO, CONGELADO))
                .isInstanceOf(AccessDeniedPjbException.class);
        assertThat(perfil.isFrozenNow()).isTrue();
    }

    @Test
    void aprovadorQueFoiCongeladoDepoisDeAprovarNaoDescongelaNinguem() {
        aprovacaoValidaDadaPor(aprovador);
        UserSecurityProfile aprovadorCongelado = new UserSecurityProfile();
        aprovadorCongelado.setFrozenAt(LocalDateTime.now().minusMinutes(1));
        when(profileRepo.findByUsuarioId(2L)).thenReturn(Optional.of(aprovadorCongelado));

        assertThatThrownBy(() -> service.executar(solicitante, APROVACAO, CONGELADO))
                .isInstanceOf(AccessDeniedPjbException.class);
        assertThat(perfil.isFrozenNow()).isTrue();
        verify(dualApprovalService, never()).consumirAprovacao(any());
    }

    @Test
    void aprovacaoJaUtilizadaPorExecucaoConcorrenteNaoDescongelaDeNovo() {
        aprovacaoValidaDadaPor(aprovador);
        when(dualApprovalService.consumirAprovacao(APROVACAO)).thenReturn(false);

        assertThatThrownBy(() -> service.executar(solicitante, APROVACAO, CONGELADO))
                .isInstanceOf(AccessDeniedPjbException.class);
        assertThat(perfil.isFrozenNow()).isTrue();
        verify(auditLedgerService, never()).appendSafely(eq("ACCOUNT_UNFROZEN"), any(), any(), any());
    }

    @Test
    void quemNaoEhAdministradorNaoPedeDescongelamento() {
        assertThatThrownBy(() -> service.solicitar(usuario(3L, TipoUsuario.SERVIDOR), 10L, CONGELADO, "/x"))
                .isInstanceOf(AccessDeniedPjbException.class);
        verify(dualApprovalService, never()).request(any(), any(), any(), any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void administradorNaoPedeDescongelamentoDaPropriaConta() {
        assertThatThrownBy(() -> service.solicitar(solicitante, 10L, 1L, "/x"))
                .isInstanceOf(AccessDeniedPjbException.class);
    }

    @Test
    void contaQueNaoEstaCongeladaNaoGeraPedido() {
        perfil.setFrozenAt(null);

        assertThatThrownBy(() -> service.solicitar(solicitante, 10L, CONGELADO, "/x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void semAprovacaoValidaAContaContinuaCongelada() {
        assertThatThrownBy(() -> service.executar(solicitante, APROVACAO, CONGELADO))
                .isInstanceOf(AccessDeniedPjbException.class);

        assertThat(perfil.isFrozenNow()).isTrue();
        verify(profileRepo, never()).save(any());
    }

    @Test
    void aprovacaoDadaPorQuemNaoEhAdministradorNaoDescongela() {
        aprovacaoValidaDadaPor(usuario(4L, TipoUsuario.SERVIDOR));

        assertThatThrownBy(() -> service.executar(solicitante, APROVACAO, CONGELADO))
                .isInstanceOf(AccessDeniedPjbException.class);

        assertThat(perfil.isFrozenNow()).isTrue();
    }

    @Test
    void duplaAprovacaoDeAdministradoresDescongelaEncerraAAprovacaoEAudita() {
        aprovacaoValidaDadaPor(aprovador);

        service.executar(solicitante, APROVACAO, CONGELADO);

        assertThat(perfil.isFrozenNow()).isFalse();
        assertThat(perfil.getFreezeReason()).isNull();
        verify(profileRepo).save(perfil);
        verify(dualApprovalService).consumirAprovacao(APROVACAO);
        verify(alertService).create(eq(titular), eq("ACCOUNT_UNFROZEN"), anyString(), anyString(), isNull(), anyInt());
        verify(auditLedgerService).appendSafely(eq("ACCOUNT_UNFROZEN"), eq("SECURITY"), eq("50"),
                eq("solicitante=1 aprovador=2 aprovacao=900"));
    }
}
