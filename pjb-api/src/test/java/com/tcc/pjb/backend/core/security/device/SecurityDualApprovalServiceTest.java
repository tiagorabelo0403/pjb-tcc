package com.tcc.pjb.backend.core.security.device;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.security.SecurityDualApprovalRequest;
import com.tcc.pjb.backend.model.entity.security.SecurityDualApprovalStatus;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.model.repository.security.SecurityDualApprovalRequestRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SecurityDualApprovalServiceTest {

    private final SecurityDualApprovalRequestRepository repo = mock(SecurityDualApprovalRequestRepository.class);
    private final SecurityDualApprovalService service = new SecurityDualApprovalService(repo, mock(MembroEquipeRepository.class));

    private static Usuario usuario(long id, TipoUsuario tipo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setTipoUsuario(tipo);
        return usuario;
    }

    private static SecurityDualApprovalRequest pedidoDeDescongelamento(SecurityDualApprovalStatus status) {
        SecurityDualApprovalRequest r = new SecurityDualApprovalRequest();
        r.setId(77L);
        r.setAction(DescongelamentoContaService.ACAO);
        r.setRequester(usuario(1L, TipoUsuario.ADMINISTRADOR));
        r.setStatus(status);
        r.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        return r;
    }

    @Test
    void novoPedidoComAMesmaChaveReaproveitaALinhaJaEncerradaEmVezDeColidirNaChaveUnica() {
        for (SecurityDualApprovalStatus encerrado : new SecurityDualApprovalStatus[] {
                SecurityDualApprovalStatus.EXPIRED, SecurityDualApprovalStatus.APPROVED, SecurityDualApprovalStatus.REJECTED}) {
            SecurityDualApprovalRequest anterior = pedidoDeDescongelamento(encerrado);
            anterior.setApprovedBy(usuario(2L, TipoUsuario.ADMINISTRADOR));
            anterior.setRejectedBy(usuario(3L, TipoUsuario.ADMINISTRADOR));
            when(repo.findByRequestKey(anyString())).thenReturn(Optional.of(anterior));
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SecurityDualApprovalRequest novo = service.request(usuario(1L, TipoUsuario.ADMINISTRADOR), 10L, null,
                    DescongelamentoContaService.ACAO, "POST", "/x", "panic-unfreeze", "a".repeat(64), 900);

            assertThat(novo).isSameAs(anterior);
            assertThat(novo.getStatus()).isEqualTo(SecurityDualApprovalStatus.PENDING);
            assertThat(novo.getApprovedBy()).isNull();
            assertThat(novo.getRejectedBy()).isNull();
            assertThat(novo.getExpiresAt()).isAfter(LocalDateTime.now());
        }
    }

    @Test
    void quemNaoEhAdministradorNaoLeAprovaNemRejeitaPedidoDeDescongelamento() {
        SecurityDualApprovalRequest pedido = pedidoDeDescongelamento(SecurityDualApprovalStatus.PENDING);
        when(repo.findById(77L)).thenReturn(Optional.of(pedido));
        Usuario servidor = usuario(9L, TipoUsuario.SERVIDOR);

        assertThatThrownBy(() -> service.getRequiredFor(77L, servidor)).isInstanceOf(AccessDeniedPjbException.class);
        assertThatThrownBy(() -> service.approve(77L, servidor)).isInstanceOf(AccessDeniedPjbException.class);
        assertThatThrownBy(() -> service.reject(77L, servidor)).isInstanceOf(AccessDeniedPjbException.class);

        assertThat(pedido.getStatus()).isEqualTo(SecurityDualApprovalStatus.PENDING);
        verify(repo, never()).save(any());
    }

    @Test
    void outroAdministradorAprovaPedidoDeDescongelamento() {
        SecurityDualApprovalRequest pedido = pedidoDeDescongelamento(SecurityDualApprovalStatus.PENDING);
        when(repo.findById(77L)).thenReturn(Optional.of(pedido));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SecurityDualApprovalRequest aprovado = service.approve(77L, usuario(2L, TipoUsuario.ADMINISTRADOR));

        assertThat(aprovado.getStatus()).isEqualTo(SecurityDualApprovalStatus.APPROVED);
    }

    @Test
    void consumoDaAprovacaoSoAconteceUmaVez() {
        when(repo.transitionStatus(77L, SecurityDualApprovalStatus.APPROVED, SecurityDualApprovalStatus.EXPIRED)).thenReturn(1, 0);

        assertThat(service.consumirAprovacao(77L)).isTrue();
        assertThat(service.consumirAprovacao(77L)).isFalse();
    }
}
