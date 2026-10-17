package com.tcc.pjb.backend.service.professional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.professional.ProfessionalAccessBasis;
import com.tcc.pjb.backend.core.security.professional.ProfessionalAccessGrantType;
import com.tcc.pjb.backend.core.security.professional.ProfessionalActorClass;
import com.tcc.pjb.backend.core.security.professional.ProfessionalGrantApprovalStatus;
import com.tcc.pjb.backend.core.security.professional.ProfessionalProcessAccessVectorService;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantBatchIssueRequest;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantBatchItemRequest;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantBatchOperationResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.professional.ProfessionalInstitutionalAccessGrant;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.professional.ProfessionalAccessGrantTemplateRepository;
import com.tcc.pjb.backend.model.repository.professional.ProfessionalInstitutionalAccessGrantEventRepository;
import com.tcc.pjb.backend.model.repository.professional.ProfessionalInstitutionalAccessGrantRepository;
import com.tcc.pjb.backend.service.competencia.ComarcaResolutionService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProfessionalInstitutionalAccessGrantAdminServiceAprovacaoTest {

    private final ProfessionalInstitutionalAccessGrantRepository grantRepository = mock(ProfessionalInstitutionalAccessGrantRepository.class);
    private final ProfessionalProcessAccessVectorService accessVectorService = mock(ProfessionalProcessAccessVectorService.class);

    @Test
    void magistradoQuePediuOGrantNaoPodeAprovar() {
        Usuario juiz = usuario(1L, TipoUsuario.JUIZ);
        grantPendente(usuario(2L, TipoUsuario.JUIZ), 1L);

        assertThatThrownBy(() -> service(juiz).approve(99L, null)).isInstanceOf(AccessDeniedPjbException.class);
        verify(grantRepository, never()).save(any());
    }

    @Test
    void quemRecebeOGrantNaoPodeAprovar() {
        Usuario juiz = usuario(1L, TipoUsuario.JUIZ);
        grantPendente(juiz, 3L);

        assertThatThrownBy(() -> service(juiz).approve(99L, null)).isInstanceOf(AccessDeniedPjbException.class);
        verify(grantRepository, never()).save(any());
    }

    @Test
    void loteDoAdminComAutoAprovacaoSaiAprovado() {
        Usuario admin = usuario(1L, TipoUsuario.ADMINISTRADOR);
        ProfessionalInstitutionalAccessGrant[] salvo = new ProfessionalInstitutionalAccessGrant[1];

        ProfessionalGrantBatchOperationResponse resposta = lote(admin, ProfessionalActorClass.MAGISTRATURA, salvo);

        assertThat(resposta.operation()).isEqualTo("BATCH_REQUEST_AND_APPROVE");
        assertThat(resposta.failedCount()).isZero();
        assertThat(salvo[0].getApprovalStatus()).isEqualTo(ProfessionalGrantApprovalStatus.APPROVED);
    }

    @Test
    void loteDeDefensorComAutoAprovacaoFicaPendenteDeOutroOperadorSemContarComoFalha() {
        Usuario defensor = usuario(3L, TipoUsuario.DEFENSOR_PUBLICO);
        ProfessionalInstitutionalAccessGrant[] salvo = new ProfessionalInstitutionalAccessGrant[1];

        ProfessionalGrantBatchOperationResponse resposta = lote(defensor, ProfessionalActorClass.DEFENSORIA, salvo);

        assertThat(resposta.operation()).isEqualTo("BATCH_REQUEST");
        assertThat(resposta.failedCount()).isZero();
        assertThat(resposta.succeededCount()).isEqualTo(1);
        assertThat(salvo[0].getApprovalStatus()).isEqualTo(ProfessionalGrantApprovalStatus.PENDING);
    }

    private ProfessionalGrantBatchOperationResponse lote(Usuario operador, ProfessionalActorClass classeDoOperador,
                                                        ProfessionalInstitutionalAccessGrant[] salvo) {
        Usuario alvo = usuario(2L, TipoUsuario.DEFENSOR_PUBLICO);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        ProfessionalInstitutionalAccessGrantEventRepository eventRepository = mock(ProfessionalInstitutionalAccessGrantEventRepository.class);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(alvo));
        when(accessVectorService.actorClass(operador)).thenReturn(classeDoOperador);
        when(accessVectorService.actorClass(alvo)).thenReturn(ProfessionalActorClass.DEFENSORIA);
        when(grantRepository.save(any())).thenAnswer(invocacao -> {
            ProfessionalInstitutionalAccessGrant grant = invocacao.getArgument(0);
            grant.setId(99L);
            salvo[0] = grant;
            return grant;
        });
        when(grantRepository.findById(99L)).thenAnswer(invocacao -> Optional.ofNullable(salvo[0]));
        when(eventRepository.findTop100ByGrant_IdOrderByCreatedAtDesc(99L)).thenReturn(List.of());
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.getRequired()).thenReturn(operador);
        ProfessionalInstitutionalAccessGrantAdminService service = new ProfessionalInstitutionalAccessGrantAdminService(
                currentUserService, usuarioRepository, mock(ProcessoRepository.class), grantRepository, eventRepository,
                mock(ProfessionalAccessGrantTemplateRepository.class), accessVectorService, mock(AuditLedgerService.class),
                mock(ComarcaResolutionService.class));
        ProfessionalGrantBatchItemRequest item = new ProfessionalGrantBatchItemRequest(2L, ProfessionalActorClass.DEFENSORIA,
                ProfessionalAccessGrantType.LOTACAO_UNIDADE, ProfessionalAccessBasis.DEFENSORIA_DESIGNACAO_FORMAL, null, "CE", "Fortaleza",
                null, null, null, null, null, null, null, "lotação", null, null, null);
        return service.issueBatch(new ProfessionalGrantBatchIssueRequest("Lote", true, List.of(item)));
    }

    private ProfessionalInstitutionalAccessGrant grantPendente(Usuario alvo, Long solicitanteId) {
        ProfessionalInstitutionalAccessGrant grant = new ProfessionalInstitutionalAccessGrant();
        grant.setId(99L);
        grant.setUsuario(alvo);
        grant.setActorClass(ProfessionalActorClass.MAGISTRATURA);
        grant.setGrantType(ProfessionalAccessGrantType.SUBSTITUICAO);
        grant.setApprovalStatus(ProfessionalGrantApprovalStatus.PENDING);
        grant.setRequestedByUserId(solicitanteId);
        when(grantRepository.findById(99L)).thenReturn(Optional.of(grant));
        return grant;
    }

    private ProfessionalInstitutionalAccessGrantAdminService service(Usuario actor) {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.getRequired()).thenReturn(actor);
        when(accessVectorService.actorClass(actor)).thenReturn(ProfessionalActorClass.MAGISTRATURA);
        return new ProfessionalInstitutionalAccessGrantAdminService(
                currentUserService,
                mock(UsuarioRepository.class),
                mock(ProcessoRepository.class),
                grantRepository,
                mock(ProfessionalInstitutionalAccessGrantEventRepository.class),
                mock(ProfessionalAccessGrantTemplateRepository.class),
                accessVectorService,
                mock(AuditLedgerService.class),
                mock(ComarcaResolutionService.class)
        );
    }

    private static Usuario usuario(Long id, TipoUsuario tipo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome("Usuario " + id);
        usuario.setTipoUsuario(tipo);
        return usuario;
    }
}
