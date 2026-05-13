package com.tcc.pjb.backend.core.processo.execucao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.execucao.application.ProcessoExecucaoApplicationService;
import com.tcc.pjb.backend.core.transito.ExecutionClosureGovernanceResolver;
import com.tcc.pjb.backend.core.transito.ExecutionEnforcementResolver;
import com.tcc.pjb.backend.core.transito.ExecutionIncidentResolver;
import com.tcc.pjb.backend.core.transito.ExecutionSatisfactionResolver;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessoExecucaoApplicationServiceTest {

    @Mock
    private ProcessoRepository processoRepository;

    @Mock
    private WorkItemRepository workItemRepository;

    @Test
    void deveMontarTrilhasExecutivasComMandadosECustodiaQuandoCabivel() {
        Processo processo = Processo.builder()
                .id(88L)
                .numeroProcesso("0000202-20.2026.8.06.0001")
                .tribunalCodigoRoteado("TJCE")
                .ramoDireito(RamoDireito.PENAL)
                .rito(RitoProcessual.EXECUCAO_PENAL)
                .faseAtual(FaseProcessual.EXECUCAO)
                .statusProcesso(StatusProcesso.CUMPRIMENTO_SENTENCA)
                .valorCausa(BigDecimal.valueOf(150000))
                .build();
        when(processoRepository.findById(88L)).thenReturn(Optional.of(processo));
        when(workItemRepository.findAllByProcesso(88L)).thenReturn(List.of());

        ProcessoExecucaoApplicationService service = new ProcessoExecucaoApplicationService(
                processoRepository,
                workItemRepository,
                new ExecutionIncidentResolver(),
                new ExecutionEnforcementResolver(),
                new ExecutionSatisfactionResolver(),
                new ExecutionClosureGovernanceResolver()
        );

        var aggregate = service.detalhar(88L);

        assertThat(aggregate.processoExecutivo()).isTrue();
        assertThat(aggregate.trilhas()).anyMatch(item -> item.codigo().equals("TRILHA_CUSTODIA_E_APRESENTACAO"));
        assertThat(aggregate.totalMandados()).isGreaterThan(0);
    }
}
