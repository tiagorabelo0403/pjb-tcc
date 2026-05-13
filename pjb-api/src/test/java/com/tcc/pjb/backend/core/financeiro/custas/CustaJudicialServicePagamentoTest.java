package com.tcc.pjb.backend.core.financeiro.custas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.financeiro.custas.domain.GruResult;
import com.tcc.pjb.backend.core.financeiro.custas.domain.PixResult;
import com.tcc.pjb.backend.core.financeiro.custas.domain.RegistrarPagamentoCustaCommand;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.financeiro.CustaJudicial;
import com.tcc.pjb.backend.model.repository.CustaJudicialRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CustaJudicialServicePagamentoTest {

    @Test
    void shouldRegisterPaymentAndExposeHealth() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        CustaJudicialRepository custaRepository = mock(CustaJudicialRepository.class);
        CustaJudicial entity = CustaJudicial.builder().id(5L).status("PENDENTE").vencimento(LocalDate.now().plusDays(1)).build();
        when(custaRepository.findById(5L)).thenReturn(Optional.of(entity));
        when(custaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CustaJudicialService service = new CustaJudicialService(
                processoRepository,
                custaRepository,
                (tipo, valor, uf) -> new GruResult("18830", "linha", "barra", "nosso"),
                (valor, processoId, tipo) -> new PixResult("payload", "tx"),
                (p, tipo) -> com.tcc.pjb.backend.core.financeiro.custas.domain.IsencaoCustaResult.naoIsento(),
                mock(AuditLedgerService.class),
                new ReadAfterWriteConsistencyPolicy()
        );
        var pago = service.registrarPagamento(new RegistrarPagamentoCustaCommand(5L, new BigDecimal("10.00"), Instant.parse("2026-04-12T10:00:00Z")));
        assertThat(pago.quitada()).isTrue();
        assertThat(service.pagamentoView(5L).status()).isEqualTo("PAGO");
        assertThat(service.health(new com.tcc.pjb.backend.core.financeiro.custas.domain.CustaHealthQuery(5L)).status().status()).isEqualTo("PAGO");
    }
}
