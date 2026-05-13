package com.tcc.pjb.backend.service.offline;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.offline.PwaOfflineBundle;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.service.offline.domain.ConflictResolution;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class OfflineConflictResolver {

    private final MovimentacaoProcessualRepository movimentacaoRepository;
    private final AuditLedgerService auditLedger;

    public OfflineConflictResolver(MovimentacaoProcessualRepository movimentacaoRepository,
                                   AuditLedgerService auditLedger) {
        this.movimentacaoRepository = Objects.requireNonNull(movimentacaoRepository);
        this.auditLedger = Objects.requireNonNull(auditLedger);
    }

    public ConflictResolution resolve(PwaOfflineBundle bundle, List<Map<String, Object>> acoesOffline) {
        if (acoesOffline == null || acoesOffline.isEmpty()) {
            return ConflictResolution.noConflict();
        }
        Instant bundleCreatedAt = bundle == null ? null : bundle.getCreatedAt();
        if (bundle == null || bundle.getProcesso() == null || bundle.getProcesso().getId() == null || bundleCreatedAt == null) {
            return ConflictResolution.replayWithNote(acoesOffline.size(), "bundle sem baseline temporal completo");
        }
        List<MovimentacaoProcessual> onlineMovs = movimentacaoRepository
                .findByProcesso_IdAndDataMovimentacaoAfterOrderByDataMovimentacaoAsc(bundle.getProcesso().getId(), bundleCreatedAt);
        if (onlineMovs.isEmpty()) {
            return ConflictResolution.replaySafe(acoesOffline.size());
        }
        long decisoesOffline = acoesOffline.stream().filter(this::isDecisaoOuAssinatura).count();
        if (decisoesOffline > 0) {
            auditLedger.appendSafely(
                    "OFFLINE_CONFLICT_DETECTED",
                    "BUNDLE",
                    bundle.getBundleToken(),
                    "acoesOffline=" + acoesOffline.size() + " movsOnline=" + onlineMovs.size() + " decisoesConflitantes=" + decisoesOffline
            );
            return ConflictResolution.requiresReview(
                    acoesOffline.size(),
                    onlineMovs.size(),
                    "Decisões offline colidem com " + onlineMovs.size() + " movimentação(ões) online"
            );
        }
        return ConflictResolution.replayWithNote(
                acoesOffline.size(),
                "Movimentações online não-decisórias detectadas — replay com aviso"
        );
    }

    private boolean isDecisaoOuAssinatura(Map<String, Object> acao) {
        String tipo = String.valueOf(acao == null ? "" : acao.getOrDefault("tipo", ""));
        return tipo.startsWith("DECISAO")
                || tipo.startsWith("SENTENCA")
                || tipo.startsWith("ASSINATURA")
                || tipo.startsWith("DESPACHO");
    }

}
