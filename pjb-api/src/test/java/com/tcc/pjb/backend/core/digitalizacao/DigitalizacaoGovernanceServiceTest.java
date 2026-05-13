package com.tcc.pjb.backend.core.digitalizacao;

import com.tcc.pjb.backend.core.digitalizacao.domain.RegistrarRevisaoPaginaCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.digitalizacao.DigitalizacaoJob;
import com.tcc.pjb.backend.model.entity.digitalizacao.DigitalizacaoPagina;
import com.tcc.pjb.backend.model.repository.DigitalizacaoJobRepository;
import com.tcc.pjb.backend.model.repository.DigitalizacaoPaginaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class DigitalizacaoGovernanceServiceTest {

    @Test
    void deveConcluirJobQuandoUltimaPaginaForRevisada() {
        DigitalizacaoJobRepository jobRepository = mock(DigitalizacaoJobRepository.class);
        DigitalizacaoPaginaRepository paginaRepository = mock(DigitalizacaoPaginaRepository.class);
        DigitalizacaoJob job = DigitalizacaoJob.builder().id(1L).status("REVISAO_HUMANA").paginasProcessadas(2).createdAt(Instant.now()).build();
        DigitalizacaoPagina pagina = DigitalizacaoPagina.builder().id(100L).jobId(1L).numeroPagina(2).confianca(BigDecimal.valueOf(90)).revisado(false).build();
        when(paginaRepository.findById(100L)).thenReturn(Optional.of(pagina));
        when(paginaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paginaRepository.countByJobIdAndRevisadoFalse(1L)).thenReturn(0L);
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paginaRepository.findByJobIdOrderByNumeroPaginaAsc(1L)).thenReturn(List.of(
                DigitalizacaoPagina.builder().id(99L).jobId(1L).numeroPagina(1).confianca(BigDecimal.valueOf(80)).revisado(true).build(),
                DigitalizacaoPagina.builder().id(100L).jobId(1L).numeroPagina(2).confianca(BigDecimal.valueOf(90)).revisado(true).build()));
        DigitalizacaoGovernanceService service = new DigitalizacaoGovernanceService(jobRepository, paginaRepository, new DigitalizacaoProperties(true, 70.0, "por", 20, 60, 300000), mock(AuditLedgerService.class), mock(ReadAfterWriteConsistencyPolicy.class));
        service.registrarRevisaoPagina(new RegistrarRevisaoPaginaCommand(100L, "texto revisado", "SENTENCA"));
        assertThat(job.getStatus()).isEqualTo("CONCLUIDO");
        assertThat(job.isRevisaoRequerida()).isFalse();
    }

    @Test
    void deveRetornarFilaDeRevisao() {
        DigitalizacaoJobRepository jobRepository = mock(DigitalizacaoJobRepository.class);
        DigitalizacaoPaginaRepository paginaRepository = mock(DigitalizacaoPaginaRepository.class);
        when(jobRepository.findReviewQueue(any(Pageable.class))).thenReturn(List.of(
                DigitalizacaoJob.builder().id(1L).status("REVISAO_HUMANA").createdAt(Instant.now()).build(),
                DigitalizacaoJob.builder().id(2L).status("REVISAO_HUMANA").createdAt(Instant.now()).build()));
        DigitalizacaoGovernanceService service = new DigitalizacaoGovernanceService(jobRepository, paginaRepository, new DigitalizacaoProperties(true, 70.0, "por", 20, 60, 300000), mock(AuditLedgerService.class), mock(ReadAfterWriteConsistencyPolicy.class));
        var snapshot = service.reviewQueue();
        assertThat(snapshot.total()).isEqualTo(2);
        assertThat(snapshot.jobIds()).containsExactly(1L, 2L);
    }
}
