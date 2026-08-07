package com.tcc.pjb.backend.modules.custas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.modules.custas.infrastructure.persistence.CustaJudicialRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.modules.custas.domain.GruResult;
import com.tcc.pjb.backend.modules.custas.domain.PixResult;

class CustaJudicialServiceTest {

    @Test
    void deveGerarGruEPixQuandoNaoHaIsencao() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        CustaJudicialRepository custaRepository = mock(CustaJudicialRepository.class);
        Processo processo = Processo.builder().id(99L).uf("CE").ramoDireito(RamoDireito.CIVIL).build();
        when(processoRepository.findById(99L)).thenReturn(Optional.of(processo));
        when(custaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CustaJudicialService service = new CustaJudicialService(
                processoRepository,
                custaRepository,
                (tipo, valor, uf) -> new GruResult("18830", "linha", "barra", "nosso"),
                (valor, processoId, tipo) -> new PixResult("payload", "tx123"),
                (p, tipo) -> com.tcc.pjb.backend.modules.custas.domain.IsencaoCustaResult.naoIsento(),
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
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        CustaJudicialRepository custaRepository = mock(CustaJudicialRepository.class);
        Processo processo = Processo.builder().id(100L).uf("CE").ramoDireito(RamoDireito.INFANCIA_JUVENTUDE).build();
        when(processoRepository.findById(100L)).thenReturn(Optional.of(processo));
        when(custaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CustaJudicialService service = new CustaJudicialService(
                processoRepository,
                custaRepository,
                (tipo, valor, uf) -> new GruResult("18830", "linha", "barra", "nosso"),
                (valor, processoId, tipo) -> new PixResult("payload", "tx123"),
                (p, tipo) -> com.tcc.pjb.backend.modules.custas.domain.IsencaoCustaResult.isento("gratuidade"),
                mock(AuditLedgerService.class),
                new ReadAfterWriteConsistencyPolicy(Clock.fixed(Instant.now(), ZoneOffset.UTC), Duration.ofSeconds(5))
        );
        var result = service.gerarCustas(100L, com.tcc.pjb.backend.modules.custas.domain.TipoCusta.CUSTAS_INICIAIS, new BigDecimal("100.00"));
        assertThat(result.isento()).isTrue();
        assertThat(result.motivoIsencao()).isEqualTo("gratuidade");
    }
}
