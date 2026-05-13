package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalBulkActionSummary;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstitutionalInboxBatchApplicationService {

    private final com.tcc.pjb.backend.core.comunicacao.institucional.inbox.application.InstitutionalInboxApplicationService inboxApplicationService;

    public InstitutionalInboxBatchApplicationService(com.tcc.pjb.backend.core.comunicacao.institucional.inbox.application.InstitutionalInboxApplicationService inboxApplicationService) {
        this.inboxApplicationService = Objects.requireNonNull(inboxApplicationService);
    }

    @Transactional
    public InstitutionalBulkActionSummary receberLote(List<String> expedicoes, String detalhe) {
        return execute("RECEBER_LOTE", expedicoes, id -> inboxApplicationService.receber(id, detalhe == null ? "recebimento_em_lote" : detalhe));
    }

    @Transactional
    public InstitutionalBulkActionSummary certificarCienciaLote(List<String> expedicoes, String detalhe) {
        return execute("CERTIFICAR_CIENCIA_LOTE", expedicoes, id -> inboxApplicationService.certificarCiencia(id, detalhe == null ? "ciencia_em_lote" : detalhe));
    }

    private InstitutionalBulkActionSummary execute(String operation,
                                                   List<String> expedicoes,
                                                   java.util.function.Function<String, Object> action) {
        List<String> success = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        List<String> ids = expedicoes == null ? List.of() : expedicoes.stream().filter(Objects::nonNull).map(String::trim).filter(v -> !v.isBlank()).distinct().toList();
        for (String id : ids) {
            try {
                action.apply(id);
                success.add(id);
            } catch (RuntimeException ex) {
                failures.add(id + ": " + ex.getMessage());
            }
        }
        return new InstitutionalBulkActionSummary(operation, ids.size(), success.size(), failures.size(), success, failures, Instant.now());
    }
}
