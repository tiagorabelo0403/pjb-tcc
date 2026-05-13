package com.tcc.pjb.backend.core.processo.orfandade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.encaixe.application.ProcessoEncaixeFinalApplicationService;
import com.tcc.pjb.backend.core.processo.encaixe.domain.ProcessoEncaixeFinalAggregate;
import com.tcc.pjb.backend.core.processo.hardening.application.ProcessoHardeningFinalApplicationService;
import com.tcc.pjb.backend.core.processo.hardening.domain.ProcessoHardeningAggregate;
import com.tcc.pjb.backend.core.processo.orfandade.application.ProcessoAntiOrfaoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProcessoAntiOrfaoApplicationServiceTest {

    @Test
    void deveMaterializarCoberturaAntiOrfaoComPlano() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        ProcessoEncaixeFinalApplicationService encaixe = mock(ProcessoEncaixeFinalApplicationService.class);
        ProcessoHardeningFinalApplicationService hardening = mock(ProcessoHardeningFinalApplicationService.class);
        Processo processo = Processo.builder().id(11L).numeroProcesso("00011").build();
        when(processoRepository.findById(11L)).thenReturn(Optional.of(processo));
        when(encaixe.detalhar(11L)).thenReturn(new ProcessoEncaixeFinalAggregate(11L, "00011", "READY", 96L, 0L, 0L, List.of(), List.of(), List.of(), Instant.now()));
        when(hardening.detalhar(11L)).thenReturn(new ProcessoHardeningAggregate(
                new ProcessoUnificadoIdentity(11L, "00011", "00011", "TJCE", "CE", "FORTALEZA", "1VC", "Classe", "Assunto", "Autor", "Reu", List.of()),
                "READY_FOR_PILOT",
                92L,
                0L,
                0L,
                List.of(),
                List.of(),
                List.of("MANTER"),
                List.of(),
                Instant.now()
        ));
        ProcessoAntiOrfaoApplicationService service = new ProcessoAntiOrfaoApplicationService(processoRepository, encaixe, hardening);

        var aggregate = service.detalhar(11L);

        assertThat(aggregate.totalContextos()).isGreaterThan(10);
        assertThat(aggregate.coberturaPercentual()).isEqualTo(100L);
        assertThat(aggregate.gaps()).isEmpty();
    }
}
