package com.tcc.pjb.backend.core.processo.posse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.posse.application.ProcessoPosseTrabalhoApplicationService;
import com.tcc.pjb.backend.core.processo.trabalho.application.ProcessoTrabalhoApplicationService;
import com.tcc.pjb.backend.core.processo.trabalho.domain.ProcessoTrabalhoAggregate;
import com.tcc.pjb.backend.core.processo.trabalho.domain.ProcessoTrabalhoIdentity;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessoPosseTrabalhoApplicationServiceTest {

    @Mock
    private WorkItemRepository workItemRepository;

    @Mock
    private ProcessoTrabalhoApplicationService processoTrabalhoApplicationService;

    @Test
    void deveGerarTrilhaImutavelDePosseTransitória() {
        when(processoTrabalhoApplicationService.detalhar(55L)).thenReturn(new ProcessoTrabalhoAggregate(
                new ProcessoTrabalhoIdentity(55L, "0003", "PENAL", "COMUM", "CONHECIMENTO", "EM_ANDAMENTO", "TJCE", "3a Vara", List.of("PENAL")),
                1, 1, 0, 0, 0, 1, "CONTROLADA", List.of(), List.of(), List.of(), Instant.now()
        ));
        WorkItem item = WorkItem.builder()
                .id(1L)
                .processo(Processo.builder().id(55L).build())
                .titulo("Vista ao MP")
                .queueCode("TRIAGEM")
                .inboxKey("PROMOTORIA")
                .assignedRole(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO)
                .status(WorkItemStatus.PENDENTE)
                .blocking(true)
                .createdAt(Instant.now().minusSeconds(600))
                .updatedAt(Instant.now().minusSeconds(60))
                .build();
        when(workItemRepository.findAllByProcesso(55L)).thenReturn(List.of(item));

        ProcessoPosseTrabalhoApplicationService service = new ProcessoPosseTrabalhoApplicationService(workItemRepository, processoTrabalhoApplicationService);
        var aggregate = service.detalhar(55L);

        assertThat(aggregate.totalItems()).isEqualTo(1);
        assertThat(aggregate.items()).singleElement().extracting("immutableTrailHash").isNotNull();
        assertThat(aggregate.alerts()).contains("EXISTE_ITEM_BLOQUEANTE_COM_POSSE_SENSIVEL");
    }
}
