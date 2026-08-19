package com.tcc.pjb.backend.modules.custas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.modules.custas.api.CustaJudicialStorePort;
import com.tcc.pjb.backend.modules.custas.api.ProcessoCustaContexto;
import com.tcc.pjb.backend.modules.custas.api.ProcessoCustaPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.modules.custas.domain.GruResult;
import com.tcc.pjb.backend.modules.custas.domain.PixResult;

class CustaJudicialApplicationServiceTest {

    @Test
    void deveGerarGruEPixQuandoNaoHaIsencao() {
        ProcessoCustaPort processoPort = mock(ProcessoCustaPort.class);
        CustaJudicialStorePort custaStore = mock(CustaJudicialStorePort.class);
        when(processoPort.obterContexto(99L)).thenReturn(Optional.of(new ProcessoCustaContexto(99L, "CE", RamoDireito.CIVIL, null)));
        when(custaStore.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CustaJudicialApplicationService service = new CustaJudicialApplicationService(
                processoPort,
                custaStore,
                (tipo, valor, uf) -> new GruResult("18830", "linha", "barra", "nosso"),
                (valor, processoId, tipo) -> new PixResult("payload", "tx123"),
                (ramoDireito, rito, tipo) -> com.tcc.pjb.backend.modules.custas.domain.IsencaoCustaResult.naoIsento(),
                mock(AuditLedgerService.class),
                new ReadAfterWriteConsistencyPolicy(Clock.fixed(Instant.now(), ZoneOffset.UTC), Duration.ofSeconds(5))
        );
        var result = service.gerarCustas(99L, com.tcc.pjb.backend.modules.custas.domain.TipoCusta.CUSTAS_INICIAIS, new BigDecimal("100.00"));
        assertThat(result.isento()).isFalse();
        assertThat(result.linhaDigitavel()).isEqualTo("linha");
        assertThat(result.pixPayload()).isEqualTo("payload");
    }

    @Test
    void deveMarcarIsencaoQuandoPolicyIndicar() {
        ProcessoCustaPort processoPort = mock(ProcessoCustaPort.class);
        CustaJudicialStorePort custaStore = mock(CustaJudicialStorePort.class);
        when(processoPort.obterContexto(100L)).thenReturn(Optional.of(new ProcessoCustaContexto(100L, "CE", RamoDireito.INFANCIA_JUVENTUDE, null)));
        when(custaStore.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CustaJudicialApplicationService service = new CustaJudicialApplicationService(
                processoPort,
                custaStore,
                (tipo, valor, uf) -> new GruResult("18830", "linha", "barra", "nosso"),
                (valor, processoId, tipo) -> new PixResult("payload", "tx123"),
                (ramoDireito, rito, tipo) -> com.tcc.pjb.backend.modules.custas.domain.IsencaoCustaResult.isento("gratuidade"),
                mock(AuditLedgerService.class),
                new ReadAfterWriteConsistencyPolicy(Clock.fixed(Instant.now(), ZoneOffset.UTC), Duration.ofSeconds(5))
        );
        var result = service.gerarCustas(100L, com.tcc.pjb.backend.modules.custas.domain.TipoCusta.CUSTAS_INICIAIS, new BigDecimal("100.00"));
        assertThat(result.isento()).isTrue();
        assertThat(result.motivoIsencao()).isEqualTo("gratuidade");
    }
}
