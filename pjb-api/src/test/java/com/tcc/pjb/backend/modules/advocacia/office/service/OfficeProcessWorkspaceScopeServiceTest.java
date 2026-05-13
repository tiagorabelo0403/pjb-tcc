package com.tcc.pjb.backend.modules.advocacia.office.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeModeView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeWorkspaceProcessQueryRequest;
import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockHttpServletRequest;

class OfficeProcessWorkspaceScopeServiceTest {

    @Test
    void currentWorkspaceProcesses_deveOcultarRamoNaoAutorizadoESigiloSemTrust() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        OfficeWorkspaceModeService officeWorkspaceModeService = mock(OfficeWorkspaceModeService.class);
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);

        OfficeProcessWorkspaceScopeService service = new OfficeProcessWorkspaceScopeService(currentUserService, officeWorkspaceModeService, processoRepository, auditLedgerService);

        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setNome("Tiago Silva");
        usuario.setCpf("12345678901");

        Equipe equipe = new Equipe();
        equipe.setId(44L);
        equipe.setNome("Escritorio Rocha & Silva");

        Processo civil = new Processo();
        civil.setId(1001L);
        civil.setEquipe(equipe);
        civil.setNumeroUnificado("0001");
        civil.setRamoDireito(RamoDireito.CIVIL);
        civil.setNivelSigilo(NivelSigilo.PUBLICO);
        civil.setStatusProcesso(StatusProcesso.EM_ANDAMENTO);

        Processo penalSigiloso = new Processo();
        penalSigiloso.setId(1002L);
        penalSigiloso.setEquipe(equipe);
        penalSigiloso.setNumeroUnificado("0002");
        penalSigiloso.setRamoDireito(RamoDireito.PENAL);
        penalSigiloso.setNivelSigilo(NivelSigilo.SEGREDO_JUSTICA);
        penalSigiloso.setStatusProcesso(StatusProcesso.EM_ANDAMENTO);

        when(currentUserService.getRequired()).thenReturn(usuario);
        when(officeWorkspaceModeService.current(org.mockito.ArgumentMatchers.any())).thenReturn(new PjbFrontendOfficeModeView(
                "OFFICE", 44L, "Escritorio Rocha & Silva", 77L, "Dr. Senior", true, false, true, false, false, false,
                List.of(), List.of(), List.of("CIVIL"), false, 6, "ELEVADO", 8, true, 77L, "Dr. Senior"));
        when(processoRepository.searchWorkspaceVisible(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(List.of(civil, penalSigiloso)));

        var page = service.currentWorkspaceProcesses(new FrontendOfficeWorkspaceProcessQueryRequest(0, 20, null, StatusProcesso.EM_ANDAMENTO, null, false), new MockHttpServletRequest());

        assertThat(page.returnedCount()).isEqualTo(1);
        assertThat(page.items()).extracting("processoId").containsExactly(1001L);
        assertThat(page.warnings()).contains("PROCESSOS_SENSIVEIS_OCULTADOS_POR_TRUST");
    }
}
