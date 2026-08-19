package com.tcc.pjb.backend.modules.custas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.modules.custas.api.CustaJudicialStorePort;
import com.tcc.pjb.backend.modules.custas.api.GruCodigoBarrasGenerator;
import com.tcc.pjb.backend.modules.custas.api.ProcessoCustaPort;
import com.tcc.pjb.backend.modules.custas.domain.CustaIsencaoPolicy;
import com.tcc.pjb.backend.modules.custas.domain.PixPayloadGenerator;
import com.tcc.pjb.backend.modules.custas.infrastructure.persistence.CustaJudicial;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CustaJudicialApplicationServiceSnapshotsTest {

    @Test
    void shouldExposeStatusVencimentoGruAndPixSnapshots() {
        ProcessoCustaPort processoPort = mock(ProcessoCustaPort.class);
        CustaJudicialStorePort custaStore = mock(CustaJudicialStorePort.class);
        CustaJudicial entity = CustaJudicial.builder()
                .id(33L)
                .tipo(com.tcc.pjb.backend.modules.custas.domain.TipoCusta.CUSTAS_INICIAIS)
                .status("PAGO")
                .valor(BigDecimal.TEN)
                .codigoReceita("1880")
                .linhaDigitavel("123456")
                .codigoBarras("7890")
                .nossoNumero("NN-1")
                .pixTxid("TX-1")
                .pixPayload("payload")
                .vencimento(LocalDate.now().plusDays(5))
                .pagoEm(Instant.parse("2026-04-11T12:00:00Z"))
                .valorPago(BigDecimal.TEN)
                .createdAt(Instant.parse("2026-04-10T12:00:00Z"))
                .build();
        when(custaStore.findById(33L)).thenReturn(Optional.of(entity));
        CustaJudicialApplicationService service = new CustaJudicialApplicationService(
                processoPort,
                custaStore,
                mock(GruCodigoBarrasGenerator.class),
                mock(PixPayloadGenerator.class),
                mock(CustaIsencaoPolicy.class),
                mock(AuditLedgerService.class),
                mock(ReadAfterWriteConsistencyPolicy.class));

        var status = service.statusSnapshot(33L);
        var vencimento = service.vencimentoSnapshot(33L);
        var gru = service.gruSnapshot(33L);
        var pix = service.pixPayloadSnapshot(33L);
        var payment = service.paymentAuditSnapshot(33L);

        assertThat(status.status()).isEqualTo("PAGO");
        assertThat(vencimento.vencida()).isFalse();
        assertThat(gru.codigoReceita()).isEqualTo("1880");
        assertThat(pix.txid()).isEqualTo("TX-1");
        assertThat(payment.status()).isEqualTo("PAGO");
    }
}
