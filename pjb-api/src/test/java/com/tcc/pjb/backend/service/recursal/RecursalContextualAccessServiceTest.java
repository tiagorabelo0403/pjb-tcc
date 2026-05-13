package com.tcc.pjb.backend.service.recursal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import com.tcc.pjb.backend.configs.EquipeContexto;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshSearchRequest;
import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.platform.security.rbac.RbacGrantedRoleResolver;

class RecursalContextualAccessServiceTest {

    @AfterEach
    void tearDown() {
        EquipeContexto.clear();
    }

    @Test
    void shouldScopeSearchToAdvocaciaOwnAndTeamProcesses() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        MembroEquipeRepository membroEquipeRepository = mock(MembroEquipeRepository.class);
        PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
        RecursalEffectiveSecrecyService secrecyService = mock(RecursalEffectiveSecrecyService.class);
        RbacGrantedRoleResolver roleResolver = mock(RbacGrantedRoleResolver.class);
        RecursalContextualAccessService service = new RecursalContextualAccessService(
                currentUserService,
                processoRepository,
                membroEquipeRepository,
                authorizationService,
                secrecyService,
                roleResolver
        );

        Usuario usuario = new Usuario();
        usuario.setId(9L);
        usuario.setCpf("11122233344");
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);
        when(currentUserService.getOrNull()).thenReturn(usuario);
        when(roleResolver.resolveGrantedRoles(TipoUsuario.ADVOGADO)).thenReturn(java.util.Set.of("ROLE_ADVOGADO"));
        when(processoRepository.findIdsByAdvogadoCpf(org.mockito.ArgumentMatchers.eq("11122233344"), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of(10L, 20L));

        MembroEquipe membro = new MembroEquipe();
        membro.setAtivo(true);
        Equipe equipe = new Equipe();
        equipe.setId(77L);
        membro.setEquipe(equipe);
        when(membroEquipeRepository.findByUsuario_Id(9L)).thenReturn(List.of(membro));
        when(processoRepository.findIdsByEquipeIds(org.mockito.ArgumentMatchers.eq(List.of(77L)), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of(20L, 30L));

        RecursalMeshSearchRequest scoped = service.scopeSearchRequest(new RecursalMeshSearchRequest(
                "tema 1102",
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
                20
        ));

        assertThat(scoped.processoIds()).containsExactly(10L, 20L, 30L);
    }

    @Test
    void shouldHonorActiveEquipeContextWhenScopingAdvocaciaSearch() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        MembroEquipeRepository membroEquipeRepository = mock(MembroEquipeRepository.class);
        PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
        RecursalEffectiveSecrecyService secrecyService = mock(RecursalEffectiveSecrecyService.class);
        RbacGrantedRoleResolver roleResolver = mock(RbacGrantedRoleResolver.class);
        RecursalContextualAccessService service = new RecursalContextualAccessService(
                currentUserService,
                processoRepository,
                membroEquipeRepository,
                authorizationService,
                secrecyService,
                roleResolver
        );

        Usuario usuario = new Usuario();
        usuario.setId(15L);
        usuario.setCpf("55566677788");
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);
        when(currentUserService.getOrNull()).thenReturn(usuario);
        when(roleResolver.resolveGrantedRoles(TipoUsuario.ADVOGADO)).thenReturn(java.util.Set.of("ROLE_ADVOGADO"));
        when(processoRepository.findIdsByAdvogadoCpf(org.mockito.ArgumentMatchers.eq("55566677788"), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of(100L));

        MembroEquipe membroAtivo = new MembroEquipe();
        membroAtivo.setAtivo(true);
        Equipe equipeAtiva = new Equipe();
        equipeAtiva.setId(88L);
        membroAtivo.setEquipe(equipeAtiva);
        EquipeContexto.setMembroAtivo(membroAtivo);
        when(processoRepository.findIdsByEquipeIds(org.mockito.ArgumentMatchers.eq(List.of(88L)), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of(200L));

        RecursalMeshSearchRequest scoped = service.scopeSearchRequest(new RecursalMeshSearchRequest(
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
                null,
                20
        ));

        assertThat(scoped.processoIds()).containsExactly(100L, 200L);
        verify(membroEquipeRepository, never()).findByUsuario_Id(15L);
    }

    @Test
    void shouldDenyGlobalSearchWithoutInstitutionalAuthorityOrContextualScope() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        MembroEquipeRepository membroEquipeRepository = mock(MembroEquipeRepository.class);
        PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
        RecursalEffectiveSecrecyService secrecyService = mock(RecursalEffectiveSecrecyService.class);
        RbacGrantedRoleResolver roleResolver = mock(RbacGrantedRoleResolver.class);
        RecursalContextualAccessService service = new RecursalContextualAccessService(
                currentUserService,
                processoRepository,
                membroEquipeRepository,
                authorizationService,
                secrecyService,
                roleResolver
        );

        Usuario usuario = new Usuario();
        usuario.setId(22L);
        usuario.setTipoUsuario(TipoUsuario.CIDADAO);
        usuario.setCpf("99988877766");
        when(currentUserService.getOrNull()).thenReturn(usuario);
        when(roleResolver.resolveGrantedRoles(TipoUsuario.CIDADAO)).thenReturn(java.util.Set.of("ROLE_CIDADAO"));
        when(processoRepository.findIdsByCidadaoCpf(org.mockito.ArgumentMatchers.eq("99988877766"), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.scopeSearchRequest(new RecursalMeshSearchRequest(
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
                null,
                20
        ))).isInstanceOf(AccessDeniedException.class);
    }
}
