package com.tcc.pjb.backend.service.recursal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.kernel.recursal.model.CanonicalFact;
import com.tcc.pjb.backend.core.kernel.recursal.plan.RecursalPlan;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.recursal.routing.RecursalWorkItemPlannerService;
import com.tcc.pjb.backend.service.recursal.routing.WorkItemSpec;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RecursalWorkItemMaterializerServiceTest {

    @Test
    void materializeHerdaComarcaEntidadeDaJurisdicaoDoProcesso() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
        RecursalWorkItemPlannerService planner = mock(RecursalWorkItemPlannerService.class);
        PjbTimeService time = mock(PjbTimeService.class);
        when(time.endOfDayLegal(any())).thenReturn(Instant.parse("2026-01-01T23:59:59Z"));

        Comarca comarca = new Comarca("Fortaleza", "CE", "2304400", null);

        Jurisdicao jurisdicao = new Jurisdicao();
        jurisdicao.setComarcaEntidade(comarca);

        Processo processo = Processo.builder().id(1L).jurisdicao(jurisdicao).build();

        when(processoRepository.findProcessoCompletoById(1L)).thenReturn(Optional.of(processo));
        when(workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(eq(1L), any(), any()))
                .thenReturn(Optional.empty());

        WorkItemSpec spec = new WorkItemSpec("FILA", "INBOX", "Contrarrazões", "desc",
                LocalDate.of(2026, 1, 1), null, null, false);
        when(planner.plan(eq(processo), any(CanonicalFact.class), any(RecursalPlan.class)))
                .thenReturn(List.of(spec));

        RecursalWorkItemMaterializerService service =
                new RecursalWorkItemMaterializerService(processoRepository, workItemRepository, planner, time);

        service.materialize(1L, mock(CanonicalFact.class), mock(RecursalPlan.class));

        ArgumentCaptor<WorkItem> captor = ArgumentCaptor.forClass(WorkItem.class);
        verify(workItemRepository).save(captor.capture());
        WorkItem saved = captor.getValue();

        assertThat(saved.getUf()).isEqualTo("CE");
        assertThat(saved.getComarca()).isEqualTo("Fortaleza");
        assertThat(saved.getComarcaEntidade()).isSameAs(comarca);
    }
}
