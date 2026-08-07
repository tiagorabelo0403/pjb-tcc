package com.tcc.pjb.backend.modules.custas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.modules.custas.api.CustaJudicialStorePort;
import com.tcc.pjb.backend.modules.custas.api.GruCodigoBarrasGenerator;
import com.tcc.pjb.backend.modules.custas.api.ProcessoCustaPort;
import com.tcc.pjb.backend.modules.custas.domain.CustaConsultaTimelineCommand;
import com.tcc.pjb.backend.modules.custas.domain.CustaHealthQuery;
import com.tcc.pjb.backend.modules.custas.domain.CustaIsencaoPolicy;
import com.tcc.pjb.backend.modules.custas.domain.PixPayloadGenerator;
import com.tcc.pjb.backend.modules.custas.infrastructure.persistence.CustaJudicial;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CustaJudicialApplicationServiceViewsTest {

    @Test
    void shouldExposeHealthTimelinePaymentAndPixViews() {
        ProcessoCustaPort processoPort = mock(ProcessoCustaPort.class);
        CustaJudicialStorePort custaStore = mock(CustaJudicialStorePort.class);
        CustaJudicial entity = CustaJudicial.builder()
                .id(31L)
                .processoId(19L)
                .tipo(com.tcc.pjb.backend.modules.custas.domain.TipoCusta.PREPARO_RECURSAL)
                .valor(new BigDecimal("150.00"))
                .status("PAGO")
                .linhaDigitavel("34191")
                .codigoBarras("3419")
                .pixPayload("pix-payload")
                .pixTxid("tx-31")
                .vencimento(LocalDate.now().plusDays(5))
                .pagoEm(Instant.parse("2026-04-11T11:00:00Z"))
                .valorPago(new BigDecimal("150.00"))
                .createdAt(Instant.parse("2026-04-10T10:00:00Z"))
                .build();
        when(custaStore.findById(31L)).thenReturn(Optional.of(entity));

        CustaJudicialApplicationService service = new CustaJudicialApplicationService(
                processoPort,
                custaStore,
                mock(GruCodigoBarrasGenerator.class),
                mock(PixPayloadGenerator.class),
                mock(CustaIsencaoPolicy.class),
                mock(AuditLedgerService.class),
                mock(ReadAfterWriteConsistencyPolicy.class));

        var health = service.health(new CustaHealthQuery(31L));
        var timeline = service.consultarTimeline(new CustaConsultaTimelineCommand(31L));
        var pagamento = service.pagamentoView(31L);
        var linha = service.linhaDigitavelView(31L);
        var pix = service.pixHealth(31L);

        assertThat(health.status().status()).isEqualTo("PAGO");
        assertThat(timeline.entries()).hasSize(3);
        assertThat(pagamento.valorPago()).isEqualByComparingTo("150.00");
        assertThat(linha.linhaDigitavel()).isEqualTo("34191");
        assertThat(pix.pendente()).isFalse();
    }
}
