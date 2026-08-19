package com.tcc.pjb.backend.modules.custas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.modules.custas.api.CustaJudicialStorePort;
import com.tcc.pjb.backend.modules.custas.api.GruCodigoBarrasGenerator;
import com.tcc.pjb.backend.modules.custas.api.ProcessoCustaContexto;
import com.tcc.pjb.backend.modules.custas.api.ProcessoCustaPort;
import com.tcc.pjb.backend.modules.custas.domain.CustaIsencaoPolicy;
import com.tcc.pjb.backend.modules.custas.domain.GerarCustaJudicialCommand;
import com.tcc.pjb.backend.modules.custas.domain.IsencaoCustaResult;
import com.tcc.pjb.backend.modules.custas.domain.PixPayloadGenerator;
import com.tcc.pjb.backend.modules.custas.domain.TipoCusta;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CustaJudicialApplicationServiceIsencaoTest {

    @Test
    void shouldReturnIsencaoWithoutGeneratingGruOrPix() {
        ProcessoCustaPort processoPort = mock(ProcessoCustaPort.class);
        CustaJudicialStorePort custaStore = mock(CustaJudicialStorePort.class);
        GruCodigoBarrasGenerator gruGenerator = mock(GruCodigoBarrasGenerator.class);
        PixPayloadGenerator pixGenerator = mock(PixPayloadGenerator.class);
        CustaIsencaoPolicy isentoPolicy = mock(CustaIsencaoPolicy.class);
        when(processoPort.obterContexto(3L)).thenReturn(Optional.of(new ProcessoCustaContexto(3L, "CE", RamoDireito.CIVIL, null)));
        when(isentoPolicy.verificar(any(), any(), any())).thenReturn(IsencaoCustaResult.isento("gratuidade"));
        when(custaStore.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CustaJudicialApplicationService service = new CustaJudicialApplicationService(
                processoPort,
                custaStore,
                gruGenerator,
                pixGenerator,
                isentoPolicy,
                mock(AuditLedgerService.class),
                mock(ReadAfterWriteConsistencyPolicy.class));

        var result = service.gerarCustas(new GerarCustaJudicialCommand(3L, TipoCusta.CUSTAS_INICIAIS, new BigDecimal("50.00")));

        assertThat(result.isento()).isTrue();
        assertThat(result.motivoIsencao()).isEqualTo("gratuidade");
        verifyNoInteractions(gruGenerator, pixGenerator);
    }
}
