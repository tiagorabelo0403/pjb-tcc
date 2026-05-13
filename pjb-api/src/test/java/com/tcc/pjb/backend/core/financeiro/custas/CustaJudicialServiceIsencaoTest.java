package com.tcc.pjb.backend.core.financeiro.custas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.financeiro.custas.domain.GerarCustaJudicialCommand;
import com.tcc.pjb.backend.core.financeiro.custas.domain.IsencaoCustaResult;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.CustaJudicialRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CustaJudicialServiceIsencaoTest {

    @Test
    void shouldReturnIsencaoWithoutGeneratingGruOrPix() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        CustaJudicialRepository custaRepository = mock(CustaJudicialRepository.class);
        GruCodigoBarrasGenerator gruGenerator = mock(GruCodigoBarrasGenerator.class);
        PixPayloadGenerator pixGenerator = mock(PixPayloadGenerator.class);
        IsentoCustaPolicy isentoPolicy = mock(IsentoCustaPolicy.class);
        when(processoRepository.findById(3L)).thenReturn(Optional.of(Processo.builder().id(3L).uf("CE").build()));
        when(isentoPolicy.verificar(any(), any())).thenReturn(IsencaoCustaResult.isento("gratuidade"));
        when(custaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CustaJudicialService service = new CustaJudicialService(
                processoRepository,
                custaRepository,
                gruGenerator,
                pixGenerator,
                isentoPolicy,
                mock(AuditLedgerService.class),
                mock(ReadAfterWriteConsistencyPolicy.class));

        var result = service.gerarCustas(new GerarCustaJudicialCommand(3L, "CUSTAS_INICIAIS", new BigDecimal("50.00")));

        assertThat(result.isento()).isTrue();
        assertThat(result.motivoIsencao()).isEqualTo("gratuidade");
        verifyNoInteractions(gruGenerator, pixGenerator);
    }
}
