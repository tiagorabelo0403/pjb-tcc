package com.tcc.pjb.backend.core.financeiro.custas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.financeiro.custas.domain.CustaConsultaCommand;
import com.tcc.pjb.backend.model.entity.financeiro.CustaJudicial;
import com.tcc.pjb.backend.model.repository.CustaJudicialRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CustaJudicialServiceConsultaTest {

    @Test
    void deveConsultarCustaEConstruirViewsBasicas() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        CustaJudicialRepository custaRepository = mock(CustaJudicialRepository.class);
        CustaJudicial entity = CustaJudicial.builder()
                .id(10L)
                .tipo("CUSTAS_INICIAIS")
                .valor(new BigDecimal("100.00"))
                .status("PENDENTE")
                .linhaDigitavel("001")
                .codigoBarras("237")
                .pixPayload("pix")
                .pixTxid("tx-1")
                .vencimento(LocalDate.now().plusDays(10))
                .createdAt(Instant.now())
                .build();
        when(custaRepository.findById(10L)).thenReturn(Optional.of(entity));
        CustaJudicialService service = new CustaJudicialService(processoRepository, custaRepository, mock(GruCodigoBarrasGenerator.class), mock(PixPayloadGenerator.class), mock(IsentoCustaPolicy.class), mock(AuditLedgerService.class), mock(ReadAfterWriteConsistencyPolicy.class));

        var consulta = service.consultar(new CustaConsultaCommand(10L));
        var view = service.view(10L);
        var pix = service.pixView(10L);

        assertThat(consulta.id()).isEqualTo(10L);
        assertThat(view.custaId()).isEqualTo(10L);
        assertThat(pix.txid()).isEqualTo("tx-1");
    }
}
