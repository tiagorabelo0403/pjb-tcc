package com.tcc.pjb.backend.modules.advocacia.office.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeModeView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeWorkspaceCreateRequest;
import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.EquipeRepository;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.AdvOfficeWorkspacePreferenceRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.AdvOfficeWorkspaceProfileRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.AdvOfficeWorkspacePresenceRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficePolicyRepository;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OfficeWorkspaceCreationServiceTest {

    @Test
    void createOwnOffice_deveCriarEscritorioProprioComCatalogoAmploDeRamos() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        EquipeRepository equipeRepository = mock(EquipeRepository.class);
        MembroEquipeRepository membroEquipeRepository = mock(MembroEquipeRepository.class);
        EquipeOfficePolicyRepository policyRepository = mock(EquipeOfficePolicyRepository.class);
        AdvOfficeWorkspacePreferenceRepository preferenceRepository = mock(AdvOfficeWorkspacePreferenceRepository.class);
        AdvOfficeWorkspaceProfileRepository profileRepository = mock(AdvOfficeWorkspaceProfileRepository.class);
        AdvOfficeWorkspacePresenceRepository presenceRepository = mock(AdvOfficeWorkspacePresenceRepository.class);
        OfficeWorkspaceModeService officeWorkspaceModeService = mock(OfficeWorkspaceModeService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);

        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setNome("Tiago Silva");
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);

        when(currentUserService.getRequired()).thenReturn(usuario);
        when(equipeRepository.existsByNomeIgnoreCase("Escritorio Tiago Silva")) .thenReturn(false);
        when(equipeRepository.save(any(Equipe.class))).thenAnswer(invocation -> {
            Equipe equipe = invocation.getArgument(0);
            equipe.setId(44L);
            return equipe;
        });
        when(membroEquipeRepository.save(any(MembroEquipe.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(policyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(preferenceRepository.findByUsuarioId(10L)).thenReturn(java.util.Optional.empty());
        when(preferenceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(officeWorkspaceModeService.update(any())).thenReturn(new PjbFrontendOfficeModeView("HYBRID", 44L, "Escritorio Tiago Silva", 10L, "Tiago Silva", true, true, true, false, false, false, java.util.List.of(), java.util.List.of("Documentos assinados em modo escritorio saem com o certificado do patrono Tiago Silva."), java.util.List.of("CIVIL", "PROCESSUAL_CIVIL", "EXECUCAO_FISCAL"), true, 10, "PATRONO", 10, false, 10L, "Tiago Silva"));

        OfficeWorkspaceCreationService service = new OfficeWorkspaceCreationService(currentUserService, equipeRepository, membroEquipeRepository, policyRepository, preferenceRepository, profileRepository, presenceRepository, officeWorkspaceModeService, auditLedgerService);

        var view = service.createOwnOffice(new FrontendOfficeWorkspaceCreateRequest("Escritorio Tiago Silva", "HYBRID", true, true, true, Set.of(RamoDireito.CIVIL, RamoDireito.PROCESSUAL_CIVIL)));

        assertThat(view.mode()).isEqualTo("HYBRID");
        assertThat(view.activeEquipeId()).isEqualTo(44L);
    }
}
