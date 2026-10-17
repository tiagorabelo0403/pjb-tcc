package com.tcc.pjb.backend.core.financeiro.trabalhista;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.magistratura.lotacao.MagistradoLotacaoAccessService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
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
import org.mockito.InOrder;
import com.tcc.pjb.backend.modules.custas.domain.GruResult;

class WorkflowTrabalhistaServiceTest {

    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final GruJudicialTrabalhistaRepository gruRepository = mock(GruJudicialTrabalhistaRepository.class);
    private final DepositoRecursalRepository depositoRepository = mock(DepositoRecursalRepository.class);
    private final MagistradoLotacaoAccessService lotacaoAccess = mock(MagistradoLotacaoAccessService.class);
    private final WorkflowTrabalhistaService service = new WorkflowTrabalhistaService(
            processoRepository,
            gruRepository,
            depositoRepository,
            (tipo, valor, uf) -> new GruResult("TRAB", "linha", "barra", "nosso"),
            new TrabalhistaWorkflowProperties(true, "IPCA-E", 8, new BigDecimal("15000.00")),
            mock(AuditLedgerService.class),
            new ReadAfterWriteConsistencyPolicy(Clock.fixed(Instant.now(), ZoneOffset.UTC), Duration.ofSeconds(5)),
            lotacaoAccess
    );

    @Test
    void deveGerarGruRecursalNoFluxoTrabalhista() {
        when(processoRepository.findById(15L)).thenReturn(Optional.of(trabalhista(15L)));
        when(gruRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var result = service.gerarGruRecursal(15L, "PREPARO_RECURSAL", new BigDecimal("1000.00"));
        assertThat(result.linhaDigitavel()).isEqualTo("linha");
    }

    @Test
    void deveRegistrarDepositoELevarParaExecucao() {
        when(processoRepository.findById(16L)).thenReturn(Optional.of(trabalhista(16L)));
        when(depositoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(processoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var result = service.registrarDepositoRecursal(16L, "TRT", new BigDecimal("15000.00"), "HASH");
        assertThat(result.status()).isEqualTo("CONFIRMADO");
    }

    @Test
    void homologacaoDoAcordoTrabalhistaExigeMagistradoLotadoAntesDeAlterarOProcesso() {
        Processo processo = trabalhista(17L);
        when(processoRepository.findById(17L)).thenReturn(Optional.of(processo));
        when(processoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.homologarAcordo(17L, "acordo de R$ 5.000,00");

        InOrder ordem = inOrder(lotacaoAccess, processoRepository);
        ordem.verify(lotacaoAccess).requireMagistradoLotadoNaUnidadeDoProcesso(processo);
        ordem.verify(processoRepository).save(processo);
        assertThat(result.statusProcesso()).isEqualTo(StatusProcesso.ACORDO_HOMOLOGADO.name());
    }

    @Test
    void semLotacaoNaUnidadeOAcordoTrabalhistaNaoEHomologado() {
        Processo processo = trabalhista(18L);
        when(processoRepository.findById(18L)).thenReturn(Optional.of(processo));
        when(lotacaoAccess.requireMagistradoLotadoNaUnidadeDoProcesso(processo))
                .thenThrow(new AccessDeniedPjbException("Acesso negado: magistrado sem lotação"));

        assertThatThrownBy(() -> service.homologarAcordo(18L, "acordo"))
                .isInstanceOf(AccessDeniedPjbException.class);
        verify(processoRepository, never()).save(any());
        assertThat(processo.getStatusProcesso()).isNotEqualTo(StatusProcesso.ACORDO_HOMOLOGADO);
    }

    private static Processo trabalhista(Long id) {
        return Processo.builder().id(id).uf("CE").tribunal("TRT7").ramoDireito(RamoDireito.TRABALHISTA).build();
    }
}
