package com.tcc.pjb.backend.core.financeiro.trabalhista;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.financeiro.custas.GruCodigoBarrasGenerator;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.DepositoRecursalRepository;
import com.tcc.pjb.backend.model.repository.GruJudicialTrabalhistaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.financeiro.custas.domain.GruResult;

class WorkflowTrabalhistaServiceTest {

    @Test
    void deveGerarGruRecursalNoFluxoTrabalhista() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        GruJudicialTrabalhistaRepository gruRepository = mock(GruJudicialTrabalhistaRepository.class);
        DepositoRecursalRepository depositoRepository = mock(DepositoRecursalRepository.class);
        when(processoRepository.findById(15L)).thenReturn(Optional.of(Processo.builder().id(15L).uf("CE").tribunal("TRT7").ramoDireito(RamoDireito.TRABALHISTA).build()));
        when(gruRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        WorkflowTrabalhistaService service = new WorkflowTrabalhistaService(
                processoRepository,
                gruRepository,
                depositoRepository,
                (tipo, valor, uf) -> new GruResult("TRAB", "linha", "barra", "nosso"),
                new TrabalhistaWorkflowProperties(true, "IPCA-E", 8, new BigDecimal("15000.00")),
                mock(AuditLedgerService.class),
                new ReadAfterWriteConsistencyPolicy(Clock.fixed(Instant.now(), ZoneOffset.UTC), Duration.ofSeconds(5))
        );
        var result = service.gerarGruRecursal(15L, "PREPARO_RECURSAL", new BigDecimal("1000.00"));
        assertThat(result.linhaDigitavel()).isEqualTo("linha");
    }

    @Test
    void deveRegistrarDepositoELevarParaExecucao() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        GruJudicialTrabalhistaRepository gruRepository = mock(GruJudicialTrabalhistaRepository.class);
        DepositoRecursalRepository depositoRepository = mock(DepositoRecursalRepository.class);
        when(processoRepository.findById(16L)).thenReturn(Optional.of(Processo.builder().id(16L).uf("CE").tribunal("TRT7").ramoDireito(RamoDireito.TRABALHISTA).build()));
        when(depositoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(processoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        WorkflowTrabalhistaService service = new WorkflowTrabalhistaService(
                processoRepository,
                gruRepository,
                depositoRepository,
                (tipo, valor, uf) -> new GruResult("TRAB", "linha", "barra", "nosso"),
                new TrabalhistaWorkflowProperties(true, "IPCA-E", 8, new BigDecimal("15000.00")),
                mock(AuditLedgerService.class),
                new ReadAfterWriteConsistencyPolicy(Clock.fixed(Instant.now(), ZoneOffset.UTC), Duration.ofSeconds(5))
        );
        var result = service.registrarDepositoRecursal(16L, "TRT", new BigDecimal("15000.00"), "HASH");
        assertThat(result.status()).isEqualTo("CONFIRMADO");
    }
}
