package com.tcc.pjb.backend.service.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.offline.PwaOfflineBundle;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OfflineConflictResolverTest {

    @Mock MovimentacaoProcessualRepository movimentacaoRepository;
    @Mock AuditLedgerService auditLedgerService;

    @Test
    void shouldRequireReviewWhenDecisionConflictsWithOnlineMovement() {
        OfflineConflictResolver resolver = new OfflineConflictResolver(movimentacaoRepository, auditLedgerService);
        Processo processo = Processo.builder().id(77L).build();
        PwaOfflineBundle bundle = new PwaOfflineBundle();
        bundle.setProcesso(processo);
        bundle.setBundleToken("B-1");
        bundle.setCreatedAt(Instant.now().minusSeconds(3600));
        when(movimentacaoRepository.findByProcesso_IdAndDataMovimentacaoAfterOrderByDataMovimentacaoAsc(org.mockito.ArgumentMatchers.eq(77L), org.mockito.ArgumentMatchers.any(Instant.class)))
                .thenReturn(List.of(new MovimentacaoProcessual()));

        com.tcc.pjb.backend.service.offline.domain.ConflictResolution resolution = resolver.resolve(bundle, List.of(Map.of("tipo", "DECISAO_INTERLOCUTORIA")));

        assertThat(resolution.requiresReview()).isTrue();
        assertThat(resolution.safe()).isFalse();
        verify(auditLedgerService).appendSafely(org.mockito.ArgumentMatchers.eq("OFFLINE_CONFLICT_DETECTED"), org.mockito.ArgumentMatchers.eq("BUNDLE"), org.mockito.ArgumentMatchers.eq("B-1"), org.mockito.ArgumentMatchers.contains("decisoesConflitantes=1"));
    }

    @Test
    void shouldAllowReplayWhenNoOnlineMovementExists() {
        OfflineConflictResolver resolver = new OfflineConflictResolver(movimentacaoRepository, auditLedgerService);
        Processo processo = Processo.builder().id(78L).build();
        PwaOfflineBundle bundle = new PwaOfflineBundle();
        bundle.setProcesso(processo);
        bundle.setBundleToken("B-2");
        bundle.setCreatedAt(Instant.now().minusSeconds(3600));
        when(movimentacaoRepository.findByProcesso_IdAndDataMovimentacaoAfterOrderByDataMovimentacaoAsc(org.mockito.ArgumentMatchers.eq(78L), org.mockito.ArgumentMatchers.any(Instant.class)))
                .thenReturn(List.of());

        com.tcc.pjb.backend.service.offline.domain.ConflictResolution resolution = resolver.resolve(bundle, List.of(Map.of("tipo", "ANOTACAO")));

        assertThat(resolution.safe()).isTrue();
        assertThat(resolution.requiresReview()).isFalse();
        verify(auditLedgerService, never()).appendSafely(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }
}
