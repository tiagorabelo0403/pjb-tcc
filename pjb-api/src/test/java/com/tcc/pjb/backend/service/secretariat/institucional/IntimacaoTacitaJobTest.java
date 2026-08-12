package com.tcc.pjb.backend.service.secretariat.institucional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.SecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.repository.SecretariaInstitucionalItemRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class IntimacaoTacitaJobTest {

    private final SecretariaInstitucionalItemRepository repository = mock(SecretariaInstitucionalItemRepository.class);
    private final AuditLedgerService auditService = mock(AuditLedgerService.class);
    private final Clock relogioFixo = Clock.fixed(Instant.parse("2026-08-20T00:00:00Z"), ZoneOffset.UTC);
    private final IntimacaoTacitaJob job = new IntimacaoTacitaJob(repository, auditService, relogioFixo);

    @Test
    void itemVencidoRecebeIntimacaoTacitaEMudaStatus() {
        SecretariaInstitucionalItem item = new SecretariaInstitucionalItem();
        item.setStatus(StatusSecretariaInstitucionalItem.PENDENTE);
        when(repository.buscarPendentesSemCienciaAntesDe(any())).thenReturn(List.of(item));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        job.processarIntimacoesTacitas();

        assertThat(item.getIntimacaoTacitaEm()).isEqualTo(Instant.parse("2026-08-20T00:00:00Z"));
        assertThat(item.getStatus()).isEqualTo(StatusSecretariaInstitucionalItem.EM_ANALISE);
        verify(auditService).appendSafely(org.mockito.ArgumentMatchers.eq("SECRETARIA_INSTITUCIONAL_INTIMACAO_TACITA"), any());
    }

    @Test
    void semItemVencidoNaoFazNada() {
        when(repository.buscarPendentesSemCienciaAntesDe(any())).thenReturn(List.of());

        job.processarIntimacoesTacitas();

        verify(repository, org.mockito.Mockito.never()).save(any());
    }
}
