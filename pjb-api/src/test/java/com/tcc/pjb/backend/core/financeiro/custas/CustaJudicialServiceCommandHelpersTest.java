package com.tcc.pjb.backend.core.financeiro.custas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.financeiro.custas.domain.RegistrarPagamentoCustaCommand;
import com.tcc.pjb.backend.model.entity.financeiro.CustaJudicial;
import com.tcc.pjb.backend.model.repository.CustaJudicialRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CustaJudicialServiceCommandHelpersTest {

    @Test
    void shouldExposePaymentCommandAndGruPixSnapshots() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        CustaJudicialRepository custaRepository = mock(CustaJudicialRepository.class);
        CustaJudicial entity = CustaJudicial.builder()
                .id(70L)
                .tipo("TAXA_JUDICIARIA")
                .valor(new BigDecimal("120.00"))
                .status("PENDENTE")
                .codigoReceita("18870")
                .linhaDigitavel("34191.79001")
                .codigoBarras("3419179001")
                .nossoNumero("NN-1")
                .pixTxid("pix-1")
                .pixPayload("000201...")
                .vencimento(LocalDate.now().plusDays(5))
                .createdAt(Instant.parse("2026-04-11T10:00:00Z"))
                .build();
        when(custaRepository.findById(70L)).thenReturn(Optional.of(entity));

        CustaJudicialService service = new CustaJudicialService(
                processoRepository,
                custaRepository,
                mock(GruCodigoBarrasGenerator.class),
                mock(PixPayloadGenerator.class),
                mock(IsentoCustaPolicy.class),
                mock(AuditLedgerService.class),
                mock(ReadAfterWriteConsistencyPolicy.class));

        var commandSnapshot = service.paymentCommandSnapshot(new RegistrarPagamentoCustaCommand(70L, new BigDecimal("120.00"), Instant.parse("2026-04-11T11:00:00Z")));
        var gruSnapshot = service.gruSnapshot(70L);
        var pixSnapshot = service.pixPayloadSnapshot(70L);
        var statusSnapshot = service.statusSnapshot(70L);
        var vencimentoSnapshot = service.vencimentoSnapshot(70L);

        assertThat(commandSnapshot.custaId()).isEqualTo(70L);
        assertThat(gruSnapshot.codigoReceita()).isEqualTo("18870");
        assertThat(pixSnapshot.txid()).isEqualTo("pix-1");
        assertThat(statusSnapshot.status()).isEqualTo("PENDENTE");
        assertThat(vencimentoSnapshot.vencida()).isFalse();
    }
}
