package com.tcc.pjb.backend.modules.custas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.modules.custas.api.CustaJudicialStorePort;
import com.tcc.pjb.backend.modules.custas.api.GruCodigoBarrasGenerator;
import com.tcc.pjb.backend.modules.custas.api.ProcessoCustaPort;
import com.tcc.pjb.backend.modules.custas.domain.CustaConsultaCommand;
import com.tcc.pjb.backend.modules.custas.domain.CustaIsencaoPolicy;
import com.tcc.pjb.backend.modules.custas.domain.PixPayloadGenerator;
import com.tcc.pjb.backend.modules.custas.infrastructure.persistence.CustaJudicial;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CustaJudicialApplicationServiceConsultaTest {

    @Test
    void deveConsultarCustaEConstruirViewsBasicas() {
        ProcessoCustaPort processoPort = mock(ProcessoCustaPort.class);
        CustaJudicialStorePort custaStore = mock(CustaJudicialStorePort.class);
        CustaJudicial entity = CustaJudicial.builder()
                .id(10L)
                .tipo(com.tcc.pjb.backend.modules.custas.domain.TipoCusta.CUSTAS_INICIAIS)
                .valor(new BigDecimal("100.00"))
                .status("PENDENTE")
                .linhaDigitavel("001")
                .codigoBarras("237")
                .pixPayload("pix")
                .pixTxid("tx-1")
                .vencimento(LocalDate.now().plusDays(10))
                .createdAt(Instant.now())
                .build();
        when(custaStore.findById(10L)).thenReturn(Optional.of(entity));
        CustaJudicialApplicationService service = new CustaJudicialApplicationService(processoPort, custaStore, mock(GruCodigoBarrasGenerator.class), mock(PixPayloadGenerator.class), mock(CustaIsencaoPolicy.class), mock(AuditLedgerService.class), mock(ReadAfterWriteConsistencyPolicy.class));

        var consulta = service.consultar(new CustaConsultaCommand(10L));
        var view = service.view(10L);
        var pix = service.pixView(10L);

        assertThat(consulta.id()).isEqualTo(10L);
        assertThat(view.custaId()).isEqualTo(10L);
        assertThat(pix.txid()).isEqualTo("tx-1");
    }
}
