package com.tcc.pjb.backend.service.secretariat.institucional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.SecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.SecretariaInstitucionalItemRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class IntimacaoTacitaJobTest {

    private final SecretariaInstitucionalItemRepository repository = mock(SecretariaInstitucionalItemRepository.class);
    private final AuditLedgerService auditService = mock(AuditLedgerService.class);
    private final Clock relogioFixo = Clock.fixed(Instant.parse("2026-08-20T00:00:00Z"), ZoneOffset.UTC);
    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final PrazoFatalCalculator prazoFatalCalculator = mock(PrazoFatalCalculator.class);
    private final IntimacaoTacitaJob job = new IntimacaoTacitaJob(
            repository, auditService, relogioFixo, processoRepository, prazoFatalCalculator);

    private SecretariaInstitucionalItem item(Long id, Long processoId) {
        SecretariaInstitucionalItem item = new SecretariaInstitucionalItem();
        ReflectionTestUtils.setField(item, "id", id);
        item.setProcessoId(processoId);
        item.setStatus(StatusSecretariaInstitucionalItem.PENDENTE);
        item.setPrazoBaseDias(15);
        item.setPrazoEmDobro(true);
        return item;
    }

    @Test
    void itemVencidoRecebeIntimacaoTacitaEMudaStatusECalculaPrazoFatal() {
        SecretariaInstitucionalItem item = item(1L, 50L);
        Processo processo = Processo.builder().id(50L).tribunal("TJCE").uf("CE").comarca("Fortaleza").build();
        Instant prazoFatalEsperado = Instant.parse("2026-09-10T23:59:59Z");
        when(repository.buscarPendentesSemCienciaAntesDe(any())).thenReturn(List.of(item));
        when(processoRepository.findById(50L)).thenReturn(Optional.of(processo));
        when(prazoFatalCalculator.calcular(eq(item), eq(processo), any())).thenReturn(prazoFatalEsperado);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        job.processarIntimacoesTacitas();

        assertThat(item.getIntimacaoTacitaEm()).isEqualTo(Instant.parse("2026-08-20T00:00:00Z"));
        assertThat(item.getStatus()).isEqualTo(StatusSecretariaInstitucionalItem.EM_ANALISE);
        assertThat(item.getPrazoFatal()).isEqualTo(prazoFatalEsperado);
        verify(auditService).appendSafely(eq("SECRETARIA_INSTITUCIONAL_INTIMACAO_TACITA"), any());
    }

    @Test
    void itemComPrazoEmDobroPassaODobroDeDiasParaOCalculadorDePrazoFatal() {
        SecretariaInstitucionalItem item = item(2L, 60L);
        item.setPrazoBaseDias(15);
        item.setPrazoEmDobro(true);
        Processo processo = Processo.builder().id(60L).tribunal("TJCE").uf("CE").comarca("Sobral").build();
        when(repository.buscarPendentesSemCienciaAntesDe(any())).thenReturn(List.of(item));
        when(processoRepository.findById(60L)).thenReturn(Optional.of(processo));
        when(prazoFatalCalculator.calcular(any(), any(), any())).thenReturn(Instant.parse("2026-09-20T23:59:59Z"));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        job.processarIntimacoesTacitas();

        verify(prazoFatalCalculator).calcular(eq(item), eq(processo), eq(Instant.parse("2026-08-20T00:00:00Z")));
        assertThat(item.isPrazoEmDobro()).isTrue();
        assertThat(item.getPrazoBaseDias()).isEqualTo(15);
    }

    @Test
    void semItemVencidoNaoFazNada() {
        when(repository.buscarPendentesSemCienciaAntesDe(any())).thenReturn(List.of());

        job.processarIntimacoesTacitas();

        verify(repository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void processoInexistenteNaoImpedeIntimacaoTacitaSoDeixaPrazoFatalEmBranco() {
        SecretariaInstitucionalItem item = item(3L, 70L);
        when(repository.buscarPendentesSemCienciaAntesDe(any())).thenReturn(List.of(item));
        when(processoRepository.findById(70L)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        job.processarIntimacoesTacitas();

        assertThat(item.getStatus()).isEqualTo(StatusSecretariaInstitucionalItem.EM_ANALISE);
        assertThat(item.getPrazoFatal()).isNull();
        verify(prazoFatalCalculator, org.mockito.Mockito.never()).calcular(any(), any(), any());
    }
}
