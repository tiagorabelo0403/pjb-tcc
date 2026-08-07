package com.tcc.pjb.backend.modules.custas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.modules.custas.domain.GruResult;
import com.tcc.pjb.backend.modules.custas.domain.PixResult;
import com.tcc.pjb.backend.modules.custas.domain.RegistrarPagamentoCustaCommand;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.modules.custas.infrastructure.persistence.CustaJudicial;
import com.tcc.pjb.backend.modules.custas.infrastructure.persistence.CustaJudicialRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
                (p, tipo) -> com.tcc.pjb.backend.modules.custas.domain.IsencaoCustaResult.naoIsento(),
                mock(AuditLedgerService.class),
                new ReadAfterWriteConsistencyPolicy(Clock.fixed(Instant.now(), ZoneOffset.UTC), Duration.ofSeconds(5))
        );
        var pago = service.registrarPagamento(new RegistrarPagamentoCustaCommand(5L, new BigDecimal("10.00"), Instant.parse("2026-04-12T10:00:00Z")));
        assertThat(pago.quitada()).isTrue();
        assertThat(service.pagamentoView(5L).status()).isEqualTo("PAGO");
        assertThat(service.health(new com.tcc.pjb.backend.modules.custas.domain.CustaHealthQuery(5L)).status().status()).isEqualTo("PAGO");
    }
}
