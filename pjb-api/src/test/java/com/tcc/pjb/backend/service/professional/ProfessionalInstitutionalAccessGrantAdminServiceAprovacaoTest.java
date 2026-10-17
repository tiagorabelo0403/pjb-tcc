package com.tcc.pjb.backend.service.professional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.professional.ProfessionalAccessGrantType;
import com.tcc.pjb.backend.core.security.professional.ProfessionalActorClass;
import com.tcc.pjb.backend.core.security.professional.ProfessionalGrantApprovalStatus;
import com.tcc.pjb.backend.core.security.professional.ProfessionalProcessAccessVectorService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.professional.ProfessionalInstitutionalAccessGrant;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.professional.ProfessionalAccessGrantTemplateRepository;
import com.tcc.pjb.backend.model.repository.professional.ProfessionalInstitutionalAccessGrantEventRepository;
import com.tcc.pjb.backend.model.repository.professional.ProfessionalInstitutionalAccessGrantRepository;
import com.tcc.pjb.backend.service.competencia.ComarcaResolutionService;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProfessionalInstitutionalAccessGrantAdminServiceAprovacaoTest {

    private static final Instant AGORA = Instant.parse("2026-10-17T15:00:00Z");
    private static final ZoneId FORTALEZA = ZoneId.of("America/Fortaleza");


    private final ProfessionalInstitutionalAccessGrantRepository grantRepository = mock(ProfessionalInstitutionalAccessGrantRepository.class);
    private final ProfessionalProcessAccessVectorService accessVectorService = mock(ProfessionalProcessAccessVectorService.class);

    @Test
    void quemPediuOGrantNaoPodeAprovar() {
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
                mock(ComarcaResolutionService.class),
                relogio()
        );
    }

    private static Usuario usuario(Long id, TipoUsuario tipo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome("Usuario " + id);
        usuario.setTipoUsuario(tipo);
        return usuario;
    }

    private static PjbTimeService relogio() {
        PjbTimeService relogio = mock(PjbTimeService.class);
        when(relogio.nowUtc()).thenReturn(AGORA);
        when(relogio.legalZone()).thenReturn(FORTALEZA);
        return relogio;
    }
}
