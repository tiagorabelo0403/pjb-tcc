package com.tcc.pjb.backend.core.judicial.sobrestamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.judicial.TemaRepercussaoGeral;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.SobrestamentoTemaRepository;
import com.tcc.pjb.backend.model.repository.TemaRepercussaoGeralRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.platform.runtime.execution.PjbTransactionalExecutionSupport;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

class SobrestamentoTemaServiceTest {

    @Test
    void shouldApplySuspensionForCompatibleProcesses() {
        Processo processo = Processo.builder().id(1L).classeTpuCodigo("123").statusProcesso(StatusProcesso.EM_ANDAMENTO).build();
        TemaRepercussaoGeral tema = new TemaRepercussaoGeral();
        tema.setId(9L);
        tema.setCodigo("Tema 9");
        tema.setProcessosSobrestados(0);
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        when(processoRepository.findAllForPrazoScan(org.mockito.ArgumentMatchers.anyCollection(), org.mockito.ArgumentMatchers.any(PageRequest.class))).thenReturn(new SliceImpl<>(List.of(processo)));
        TemaRepercussaoGeralRepository temaRepository = mock(TemaRepercussaoGeralRepository.class);
        when(temaRepository.findByCodigoIgnoreCase("Tema 9")).thenReturn(Optional.of(tema));
        SobrestamentoTemaRepository sobrestamentoRepository = mock(SobrestamentoTemaRepository.class);
        when(sobrestamentoRepository.existsByProcessoIdAndTemaId(1L, 9L)).thenReturn(false);
        SobrestamentoTemaService service = new SobrestamentoTemaService(processoRepository, temaRepository, sobrestamentoRepository, mock(AuditLedgerService.class), new SimpleMeterRegistry(), new ReadAfterWriteConsistencyPolicy(), mock(PjbTransactionalExecutionSupport.class));
        service.sobrestamentoBatch("Tema 9");
        assertThat(processo.getStatusProcesso()).isEqualTo(StatusProcesso.SUSPENSO_TEMA_REPERCUSSAO);
        assertThat(tema.getProcessosSobrestados()).isEqualTo(1);
    }
}
