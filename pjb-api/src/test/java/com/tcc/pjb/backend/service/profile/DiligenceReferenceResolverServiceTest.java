package com.tcc.pjb.backend.service.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DiligenceReferenceResolverServiceTest {

    @Test
    void resolveMandadoOficialParaWorkItemReal() {
        WorkItemRepository repository = Mockito.mock(WorkItemRepository.class);
        DiligenceReferenceResolverService service = new DiligenceReferenceResolverService(repository);
        Processo processo = new Processo();
        processo.setId(700L);
        processo.setNumeroProcesso("0004321-77.2026.8.06.0001");
        WorkItem item = WorkItem.builder()
                .id(99L)
                .processo(processo)
                .templateCode("MANDADO:99")
                .type(WorkItemType.EXPEDICAO)
                .titulo("Mandado de intimação")
                .status(WorkItemStatus.PENDENTE)
                .dueAt(Instant.parse("2026-03-15T12:00:00Z"))
                .uf("CE")
                .comarca("Fortaleza")
                .build();
        when(repository.findById(99L)).thenReturn(Optional.of(item));

        var response = service.describe(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "99");

        assertThat(response.vinculada()).isTrue();
        assertThat(response.workItemId()).isEqualTo(99L);
        assertThat(response.processoNumero()).isEqualTo("0004321-77.2026.8.06.0001");
        assertThat(response.workItemType()).isEqualTo("EXPEDICAO");
    }
}
