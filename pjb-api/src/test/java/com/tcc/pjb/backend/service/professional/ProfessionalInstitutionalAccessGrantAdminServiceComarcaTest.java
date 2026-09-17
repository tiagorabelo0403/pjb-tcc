package com.tcc.pjb.backend.service.professional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.professional.ProfessionalAccessBasis;
import com.tcc.pjb.backend.core.security.professional.ProfessionalAccessGrantType;
import com.tcc.pjb.backend.core.security.professional.ProfessionalActorClass;
import com.tcc.pjb.backend.core.security.professional.ProfessionalProcessAccessVectorService;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantIssueRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.competencia.Comarca;
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
import org.mockito.ArgumentCaptor;

class ProfessionalInstitutionalAccessGrantAdminServiceComarcaTest {

    @Test
    void resolveEAplicaComarcaDoCatalogoAoEmitirGrant() {
        Comarca comarcaEsperada = mock(Comarca.class);
        ProfessionalInstitutionalAccessGrantRepository grantRepository = mock(ProfessionalInstitutionalAccessGrantRepository.class);
        ProfessionalInstitutionalAccessGrantEventRepository eventRepository = mock(ProfessionalInstitutionalAccessGrantEventRepository.class);
        ComarcaResolutionService comarcaResolutionService = mock(ComarcaResolutionService.class);
        ProfessionalProcessAccessVectorService accessVectorService = mock(ProfessionalProcessAccessVectorService.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

        Usuario actor = usuario(1L, TipoUsuario.ADMINISTRADOR);
        Usuario target = usuario(2L, TipoUsuario.DEFENSOR);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(target));
        when(accessVectorService.actorClass(actor)).thenReturn(ProfessionalActorClass.MAGISTRATURA);
        when(accessVectorService.actorClass(target)).thenReturn(ProfessionalActorClass.DEFENSORIA);
        when(comarcaResolutionService.resolver("Fortaleza", "CE")).thenReturn(Optional.of(comarcaEsperada));
        stubSaveAndFind(grantRepository, eventRepository);

        ProfessionalInstitutionalAccessGrantAdminService service = service(
                actor, usuarioRepository, grantRepository, eventRepository, accessVectorService, comarcaResolutionService);

        service.issue(issueRequest(2L, "CE", "Fortaleza"));

        ArgumentCaptor<ProfessionalInstitutionalAccessGrant> captor = ArgumentCaptor.forClass(ProfessionalInstitutionalAccessGrant.class);
        org.mockito.Mockito.verify(grantRepository).save(captor.capture());
        assertThat(captor.getValue().getComarcaEntidade()).isSameAs(comarcaEsperada);
    }

    @Test
    void naoResolveQuandoComarcaEmBrancoENaoLancaExcecao() {
        ProfessionalInstitutionalAccessGrantRepository grantRepository = mock(ProfessionalInstitutionalAccessGrantRepository.class);
        ProfessionalInstitutionalAccessGrantEventRepository eventRepository = mock(ProfessionalInstitutionalAccessGrantEventRepository.class);
        ComarcaResolutionService comarcaResolutionService = mock(ComarcaResolutionService.class);
        ProfessionalProcessAccessVectorService accessVectorService = mock(ProfessionalProcessAccessVectorService.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

        Usuario actor = usuario(3L, TipoUsuario.ADMINISTRADOR);
        Usuario target = usuario(4L, TipoUsuario.DEFENSOR);
        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(target));
        when(accessVectorService.actorClass(actor)).thenReturn(ProfessionalActorClass.MAGISTRATURA);
        when(accessVectorService.actorClass(target)).thenReturn(ProfessionalActorClass.DEFENSORIA);
        stubSaveAndFind(grantRepository, eventRepository);

        ProfessionalInstitutionalAccessGrantAdminService service = service(
                actor, usuarioRepository, grantRepository, eventRepository, accessVectorService, comarcaResolutionService);

        service.issue(issueRequest(4L, null, null));

        ArgumentCaptor<ProfessionalInstitutionalAccessGrant> captor = ArgumentCaptor.forClass(ProfessionalInstitutionalAccessGrant.class);
        org.mockito.Mockito.verify(grantRepository).save(captor.capture());
        assertThat(captor.getValue().getComarcaEntidade()).isNull();
    }

    @Test
    void mantemComarcaEntidadeNulaQuandoResolverNaoAchaCandidata() {
        ProfessionalInstitutionalAccessGrantRepository grantRepository = mock(ProfessionalInstitutionalAccessGrantRepository.class);
        ProfessionalInstitutionalAccessGrantEventRepository eventRepository = mock(ProfessionalInstitutionalAccessGrantEventRepository.class);
        ComarcaResolutionService comarcaResolutionService = mock(ComarcaResolutionService.class);
        ProfessionalProcessAccessVectorService accessVectorService = mock(ProfessionalProcessAccessVectorService.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

        Usuario actor = usuario(5L, TipoUsuario.ADMINISTRADOR);
        Usuario target = usuario(6L, TipoUsuario.DEFENSOR);
        when(usuarioRepository.findById(6L)).thenReturn(Optional.of(target));
        when(accessVectorService.actorClass(actor)).thenReturn(ProfessionalActorClass.MAGISTRATURA);
        when(accessVectorService.actorClass(target)).thenReturn(ProfessionalActorClass.DEFENSORIA);
        when(comarcaResolutionService.resolver("Comarca Inexistente", "CE")).thenReturn(Optional.empty());
        stubSaveAndFind(grantRepository, eventRepository);

        ProfessionalInstitutionalAccessGrantAdminService service = service(
                actor, usuarioRepository, grantRepository, eventRepository, accessVectorService, comarcaResolutionService);

        service.issue(issueRequest(6L, "CE", "Comarca Inexistente"));

        ArgumentCaptor<ProfessionalInstitutionalAccessGrant> captor = ArgumentCaptor.forClass(ProfessionalInstitutionalAccessGrant.class);
        org.mockito.Mockito.verify(grantRepository).save(captor.capture());
        assertThat(captor.getValue().getComarcaEntidade()).isNull();
    }

    private void stubSaveAndFind(ProfessionalInstitutionalAccessGrantRepository grantRepository,
                                 ProfessionalInstitutionalAccessGrantEventRepository eventRepository) {
        ProfessionalInstitutionalAccessGrant[] savedHolder = new ProfessionalInstitutionalAccessGrant[1];
        when(grantRepository.save(any())).thenAnswer(inv -> {
            ProfessionalInstitutionalAccessGrant g = inv.getArgument(0);
            g.setId(99L);
            savedHolder[0] = g;
            return g;
        });
        when(grantRepository.findById(99L)).thenAnswer(inv -> Optional.ofNullable(savedHolder[0]));
        when(eventRepository.findTop100ByGrant_IdOrderByCreatedAtDesc(99L)).thenReturn(List.of());
    }

    private ProfessionalInstitutionalAccessGrantAdminService service(Usuario actor,
                                                                      UsuarioRepository usuarioRepository,
                                                                      ProfessionalInstitutionalAccessGrantRepository grantRepository,
                                                                      ProfessionalInstitutionalAccessGrantEventRepository eventRepository,
                                                                      ProfessionalProcessAccessVectorService accessVectorService,
                                                                      ComarcaResolutionService comarcaResolutionService) {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.getRequired()).thenReturn(actor);
        return new ProfessionalInstitutionalAccessGrantAdminService(
                currentUserService,
                usuarioRepository,
                mock(ProcessoRepository.class),
                grantRepository,
                eventRepository,
                mock(ProfessionalAccessGrantTemplateRepository.class),
                accessVectorService,
                mock(AuditLedgerService.class),
                comarcaResolutionService
        );
    }

    private ProfessionalGrantIssueRequest issueRequest(Long targetUserId, String uf, String comarca) {
        return new ProfessionalGrantIssueRequest(
                targetUserId,
                ProfessionalActorClass.DEFENSORIA,
                ProfessionalAccessGrantType.LOTACAO_UNIDADE,
                ProfessionalAccessBasis.DEFENSORIA_DESIGNACAO_FORMAL,
                null,
                uf,
                comarca,
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
    }

    private Usuario usuario(Long id, TipoUsuario tipoUsuario) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome("Usuario " + id);
        usuario.setTipoUsuario(tipoUsuario);
        usuario.setEmail(id + "@tribunal.gov.br");
        return usuario;
    }
}
