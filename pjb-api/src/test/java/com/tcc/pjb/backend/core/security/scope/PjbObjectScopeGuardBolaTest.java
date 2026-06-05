package com.tcc.pjb.backend.core.security.scope;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.BoletimOcorrenciaDigitalRepository;
import com.tcc.pjb.backend.model.repository.InqueritoPolicialDigitalRepository;
import com.tcc.pjb.backend.model.repository.LotacaoInstituicaoRepository;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatInstitutionalVisibilityService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PjbObjectScopeGuardBolaTest {

    private PjbObjectScopeGuardImpl guard(SecretariatInstitutionalVisibilityService vis,
                                          CurrentUserService cur,
                                          AuditLedgerService audit) {
        return new PjbObjectScopeGuardImpl(
                vis,
                cur,
                audit,
                mock(LotacaoInstituicaoRepository.class),
                mock(DelegaciaInstitucionalScopeService.class),
                mock(BoletimOcorrenciaDigitalRepository.class),
                mock(InqueritoPolicialDigitalRepository.class)
        );
    }

    @Test
    void servidorForaDeComarcaNaoPodeVerWorkItem() {
        var vis = mock(SecretariatInstitutionalVisibilityService.class);
        when(vis.requireDeskAccess(42L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "processo fora da comarca institucional da secretaria"));

        AcessoForaDeEscopoException ex = assertThrows(AcessoForaDeEscopoException.class,
                () -> guard(vis, mock(CurrentUserService.class), mock(AuditLedgerService.class))
                        .requireAccess(TipoObjetoProtegido.WORK_ITEM, 42L, AcaoEscopo.LER));

        assertEquals(MotivoNegacaoEscopo.FORA_DA_COMARCA, ex.getMotivo());
    }

    @Test
    void capturaDeWorkItemAtribuidoAOutroRetornaMotivoDuplicidade() {
        var vis = mock(SecretariatInstitutionalVisibilityService.class);
        var cur = mock(CurrentUserService.class);

        Usuario outro = new Usuario();
        outro.setId(99L);
        WorkItem wi = WorkItem.builder().assignedUser(outro).build();
        when(vis.requireDeskAccess(1L)).thenReturn(wi);

        Usuario eu = new Usuario();
        eu.setId(1L);
        when(cur.getRequired()).thenReturn(eu);

        AcessoForaDeEscopoException ex = assertThrows(AcessoForaDeEscopoException.class,
                () -> guard(vis, cur, mock(AuditLedgerService.class))
                        .requireAccess(TipoObjetoProtegido.WORK_ITEM, 1L, AcaoEscopo.CAPTURAR));

        assertEquals(MotivoNegacaoEscopo.WORKITEM_DE_OUTRO_USUARIO, ex.getMotivo());
    }

    @Test
    void territorioVerificadoAntesDoAssignedUserNaCaptura() {
        var vis = mock(SecretariatInstitutionalVisibilityService.class);
        var cur = mock(CurrentUserService.class);
        when(vis.requireDeskAccess(5L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "processo fora da comarca institucional da secretaria"));

        AcessoForaDeEscopoException ex = assertThrows(AcessoForaDeEscopoException.class,
                () -> guard(vis, cur, mock(AuditLedgerService.class))
                        .requireAccess(TipoObjetoProtegido.WORK_ITEM, 5L, AcaoEscopo.CAPTURAR));

        assertEquals(MotivoNegacaoEscopo.FORA_DA_COMARCA, ex.getMotivo());
        verify(cur, never()).getRequired();
    }

    @Test
    void usuarioDoEscopoCorretoPodemLerWorkItem() {
        var vis = mock(SecretariatInstitutionalVisibilityService.class);
        when(vis.requireDeskAccess(10L)).thenReturn(WorkItem.builder().build());

        assertDoesNotThrow(() ->
                guard(vis, mock(CurrentUserService.class), mock(AuditLedgerService.class))
                        .requireAccess(TipoObjetoProtegido.WORK_ITEM, 10L, AcaoEscopo.LER));
    }

    @Test
    void perfilInsuficienteRetornaMotivoCorreto() {
        var vis = mock(SecretariatInstitutionalVisibilityService.class);
        when(vis.requireDeskAccess(3L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "perfil não autorizado para operar malha de secretaria"));

        AcessoForaDeEscopoException ex = assertThrows(AcessoForaDeEscopoException.class,
                () -> guard(vis, mock(CurrentUserService.class), mock(AuditLedgerService.class))
                        .requireAccess(TipoObjetoProtegido.WORK_ITEM, 3L, AcaoEscopo.LER));

        assertEquals(MotivoNegacaoEscopo.PAPEL_INSUFICIENTE, ex.getMotivo());
    }

    @Test
    void negacaoDeAcessoEhAuditada() {
        var vis = mock(SecretariatInstitutionalVisibilityService.class);
        var audit = mock(AuditLedgerService.class);
        when(vis.requireDeskAccess(7L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "processo fora da comarca institucional da secretaria"));

        assertThrows(AcessoForaDeEscopoException.class,
                () -> guard(vis, mock(CurrentUserService.class), audit)
                        .requireAccess(TipoObjetoProtegido.WORK_ITEM, 7L, AcaoEscopo.LER));

        verify(audit, atLeastOnce()).appendSafely(anyString(), anyString(), anyString());
    }

    @Test
    void capturaDoProprioWorkItemEhPermitida() {
        var vis = mock(SecretariatInstitutionalVisibilityService.class);
        var cur = mock(CurrentUserService.class);

        Usuario eu = new Usuario();
        eu.setId(1L);
        WorkItem wi = WorkItem.builder().assignedUser(eu).build();
        when(vis.requireDeskAccess(10L)).thenReturn(wi);
        when(cur.getRequired()).thenReturn(eu);

        assertDoesNotThrow(() ->
                guard(vis, cur, mock(AuditLedgerService.class))
                        .requireAccess(TipoObjetoProtegido.WORK_ITEM, 10L, AcaoEscopo.CAPTURAR));
    }

    @Test
    void capturaDeWorkItemSemDonoPorUsuarioDoEscopoEhPermitida() {
        var vis = mock(SecretariatInstitutionalVisibilityService.class);
        var cur = mock(CurrentUserService.class);

        WorkItem wi = WorkItem.builder().build();
        when(vis.requireDeskAccess(10L)).thenReturn(wi);

        Usuario eu = new Usuario();
        eu.setId(1L);
        when(cur.getRequired()).thenReturn(eu);

        assertDoesNotThrow(() ->
                guard(vis, cur, mock(AuditLedgerService.class))
                        .requireAccess(TipoObjetoProtegido.WORK_ITEM, 10L, AcaoEscopo.CAPTURAR));
    }

    @Test
    void movimentacaoDeWorkItemAtribuidoAOutroEhNegada() {
        var vis = mock(SecretariatInstitutionalVisibilityService.class);
        var cur = mock(CurrentUserService.class);

        Usuario outro = new Usuario();
        outro.setId(77L);
        WorkItem wi = WorkItem.builder().assignedUser(outro).build();
        when(vis.requireDeskAccess(20L)).thenReturn(wi);

        Usuario eu = new Usuario();
        eu.setId(1L);
        when(cur.getRequired()).thenReturn(eu);

        AcessoForaDeEscopoException ex = assertThrows(AcessoForaDeEscopoException.class,
                () -> guard(vis, cur, mock(AuditLedgerService.class))
                        .requireAccess(TipoObjetoProtegido.WORK_ITEM, 20L, AcaoEscopo.MOVIMENTAR));

        assertEquals(MotivoNegacaoEscopo.WORKITEM_DE_OUTRO_USUARIO, ex.getMotivo());
    }
}
