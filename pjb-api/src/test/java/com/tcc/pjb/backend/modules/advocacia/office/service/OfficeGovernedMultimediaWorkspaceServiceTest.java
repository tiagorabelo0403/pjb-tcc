package com.tcc.pjb.backend.modules.advocacia.office.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessAccessView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedMultimediaWorkspaceRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.processual.peticionamento.workspace.InstitutionalMultimediaWorkspaceService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class OfficeGovernedMultimediaWorkspaceServiceTest {

    @Test
    void preview_deveEnriquecerWorkspaceMultimidiaSemPerderContextoDoEscritorio() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService = mock(OfficeProcessWorkspaceScopeService.class);
        InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService = mock(InstitutionalMultimediaWorkspaceService.class);
        OfficeGovernedMultimediaWorkspaceService service = new OfficeGovernedMultimediaWorkspaceService(
                currentUserService,
                officeProcessWorkspaceScopeService,
                institutionalMultimediaWorkspaceService
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);

        when(currentUserService.getRequired()).thenReturn(usuario);
        when(officeProcessWorkspaceScopeService.access(1001L, com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType.PETICIONAR, request))
                .thenReturn(new PjbFrontendOfficeProcessAccessView(1001L, "0001", 44L, "HYBRID", "PETICIONAR", true, true, false, 77L, "Dr. Senior", List.of(), List.of("ASSINATURA_PATRONAL_OBRIGATORIA")));
        when(institutionalMultimediaWorkspaceService.enrich(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Map.of(
                        "nextAction", "SUBMETER_PECA_INSTITUCIONAL",
                        "pieceProfile", "PECA_INSTITUCIONAL_MULTIMIDIA",
                        "multimediaEnabled", true,
                        "uploadGovernance", Map.of("maxFiles", 10)
                ));

        var view = service.preview(1001L, new FrontendOfficeGovernedMultimediaWorkspaceRequest("PETICIONAR", "INSTITUCIONAL", "PETICAO_INSTITUCIONAL", true, false, false, Map.of("anexos", List.of("peticao.pdf"))), request);

        assertThat(view.action()).isEqualTo("PETICIONAR");
        assertThat(view.queueRequired()).isTrue();
        assertThat(view.workspaceMode()).isEqualTo("HYBRID");
        assertThat(view.nextAction()).isEqualTo("SUBMETER_PECA_INSTITUCIONAL");
        assertThat(view.multimediaEnabled()).isTrue();
    }
}
