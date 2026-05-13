package com.tcc.pjb.backend.service.workitem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.actor.ActorAssignmentEngine;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.dto.workitem.WorkItemDto;
import com.tcc.pjb.backend.model.dto.workitem.WorkItemGenerationRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.rito.ProcessoRitoSnapshotService;
import com.tcc.pjb.backend.service.rito.RitoPackService;
import com.tcc.pjb.backend.service.rito.model.RitoDefinition;
import com.tcc.pjb.backend.service.rito.model.RitoStage;
import com.tcc.pjb.backend.service.rito.model.WorkTemplate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProcessWorkItemAutomationServiceTest {

    @Test
    void shouldGenerateUsingResolvedRitoWhenProcessoRitoIsNull() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
        RitoPackService ritoPackService = mock(RitoPackService.class);
        WorkItemMapper mapper = mock(WorkItemMapper.class);
        PjbTimeService timeService = mock(PjbTimeService.class);
        ActorAssignmentEngine actorAssignmentEngine = mock(ActorAssignmentEngine.class);
        ProcessoRitoSnapshotService snapshotService = mock(ProcessoRitoSnapshotService.class);

        ProcessWorkItemAutomationService service = new ProcessWorkItemAutomationService(
                processoRepository,
                workItemRepository,
                ritoPackService,
                mapper,
                timeService,
                actorAssignmentEngine,
                snapshotService
        );

        Processo processo = new Processo();
        processo.setId(77L);
        processo.setRito(null);
        processo.setFaseAtual(FaseProcessual.CONHECIMENTO);

        WorkTemplate template = new WorkTemplate();
        template.setCode("PROTOCOLO_INICIAL_ADV");
        template.setTitle("Protocolar inicial");
        template.setActorRole("ADVOGADO");
        template.setType("MANIFESTACAO");

        RitoStage stage = new RitoStage();
        stage.setFase(FaseProcessual.CONHECIMENTO.name());
        stage.setWork(List.of(template));

        RitoDefinition definition = RitoDefinition.builder()
                .title("Rito comum")
                .definitionsVersion("1")
                .definitionsHash("abc")
                .metadata(Map.of())
                .stages(List.of(stage))
                .build();

        Instant now = Instant.parse("2026-03-08T12:00:00Z");
        when(timeService.nowUtc()).thenReturn(now);
        when(processoRepository.findById(77L)).thenReturn(Optional.of(processo));
        when(snapshotService.resolve(processo, null)).thenReturn(new ProcessoRitoSnapshotService.ProcessoRitoSnapshot(
                RitoProcessual.COMUM_ORDINARIO,
                "COMUM_ORDINARIO",
                "Rito Comum",
                "CIVIL",
                0.93d,
                false,
                List.of(),
                "OK",
                false
        ));
        when(ritoPackService.get(RitoProcessual.COMUM_ORDINARIO)).thenReturn(Optional.of(definition));
        when(workItemRepository.findAllByProcesso(77L)).thenReturn(List.of());
        when(workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(anyLong(), anyString(), any(WorkItemStatus.class)))
                .thenReturn(Optional.empty());
        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(invocation -> {
            WorkItem wi = invocation.getArgument(0);
            wi.setId(99L);
            return wi;
        });
        when(mapper.toDto(any(WorkItem.class))).thenReturn(WorkItemDto.builder().id(99L).build());
        when(actorAssignmentEngine.assign(any(ActorAssignmentEngine.AssignmentContext.class)))
                .thenReturn(new ActorAssignmentEngine.AssignmentResult(
                        77L,
                        RitoProcessual.COMUM_ORDINARIO,
                        FaseProcessual.CONHECIMENTO,
                        List.of(),
                        List.of(),
                        Map.of(),
                        true
                ));

        var response = service.generate(new WorkItemGenerationRequest(77L, false, FaseProcessual.CONHECIMENTO.name()));

        assertEquals(1, response.created());
        assertEquals("COMUM_ORDINARIO", response.debug().get("rito"));
        assertEquals("OK", response.debug().get("ritoStatus"));
        assertNotNull(response.createdItems());
    }
}
