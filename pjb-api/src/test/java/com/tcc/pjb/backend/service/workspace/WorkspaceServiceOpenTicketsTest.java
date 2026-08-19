package com.tcc.pjb.backend.service.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.persona.PersonaKey;
import com.tcc.pjb.backend.core.security.persona.UserPersona;
import com.tcc.pjb.backend.core.security.persona.UserPersonaService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.modules.suporte.service.SupportTicketService;
import com.tcc.pjb.backend.service.catalog.IdeaCatalogService;
import com.tcc.pjb.backend.service.workitem.WorkItemService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

class WorkspaceServiceOpenTicketsTest {

    @Test
    void meExpoeOpenTicketsCountEQuickActionDeSuporte() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        UserPersonaService personaService = mock(UserPersonaService.class);
        IdeaCatalogService ideaCatalogService = mock(IdeaCatalogService.class);
        WorkItemService workItemService = mock(WorkItemService.class);
        SupportTicketService supportTicketService = mock(SupportTicketService.class);

        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setTipoUsuario(TipoUsuario.CIDADAO);
        when(currentUserService.getRequired()).thenReturn(usuario);
        when(personaService.getRequiredPersona()).thenReturn(
                new UserPersona(TipoUsuario.CIDADAO, PersonaKey.CIDADAO, "Cidadão", "Sr(a).", null, null, false));
        when(ideaCatalogService.getByRole(any())).thenReturn(java.util.List.of());
        when(workItemService.inbox(0, 5)).thenReturn(Page.empty());
        when(supportTicketService.countAbertosPorUsuario(7L)).thenReturn(3L);

        WorkspaceService service = new WorkspaceService(
                currentUserService, personaService, ideaCatalogService, workItemService, supportTicketService);

        var resposta = service.me();

        assertThat(resposta.getOpenTicketsCount()).isEqualTo(3L);
        assertThat(resposta.getQuickActions()).contains("Abrir chamado de suporte");
    }
}
