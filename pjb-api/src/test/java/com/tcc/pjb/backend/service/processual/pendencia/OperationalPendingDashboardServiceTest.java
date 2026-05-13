package com.tcc.pjb.backend.service.processual.pendencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class OperationalPendingDashboardServiceTest {

    @Test
    void shouldMergeUserAndRoleInbox() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        WorkItemRepository workItemRepository = Mockito.mock(WorkItemRepository.class);
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setTipoUsuario(TipoUsuario.SERVIDOR_FORUM);
        when(currentUserService.getRequired()).thenReturn(usuario);
        Processo processo = new Processo();
        processo.setId(9L);
        processo.setNumeroProcesso("0009");
        WorkItem userItem = WorkItem.builder().id(11L).processo(processo).titulo("Usuário").type(WorkItemType.INTIMACAO).status(WorkItemStatus.PENDENTE).dueAt(Instant.now().plusSeconds(60)).build();
        WorkItem roleItem = WorkItem.builder().id(12L).processo(processo).titulo("Fila").type(WorkItemType.CITACAO).status(WorkItemStatus.PENDENTE).dueAt(Instant.now().plusSeconds(120)).build();
        when(workItemRepository.inboxByUser(1L, PageRequest.of(0, 40))).thenReturn(new PageImpl<>(List.of(userItem)));
        when(workItemRepository.inboxByRoleAndTerritory(TipoUsuario.SERVIDOR_FORUM, null, null, PageRequest.of(0, 40))).thenReturn(new PageImpl<>(List.of(roleItem)));
        OperationalPendingDashboardService service = new OperationalPendingDashboardService(currentUserService, workItemRepository);
        var response = service.dashboard(null);
        assertEquals(2, response.totalNaAmostra());
        assertEquals(1L, response.porTipo().get("INTIMACAO"));
        assertEquals(1L, response.porTipo().get("CITACAO"));
    }
}
