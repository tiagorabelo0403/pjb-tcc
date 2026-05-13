package com.tcc.pjb.backend.core.dje;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.dje.domain.DjePartesNotificacaoResult;
import com.tcc.pjb.backend.model.entity.judicial.DjePublicacao;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DjePartesNotificacaoService {

    private final DjePartesNotificacaoPort notificacaoPort;
    private final AuditLedgerService auditLedger;

    public DjePartesNotificacaoService(DjePartesNotificacaoPort notificacaoPort,
                                       AuditLedgerService auditLedger) {
        this.notificacaoPort = Objects.requireNonNull(notificacaoPort);
        this.auditLedger = Objects.requireNonNull(auditLedger);
    }

    public DjePartesNotificacaoResult notificarPartes(DjePublicacao publicacao) {
        Objects.requireNonNull(publicacao);
        DjePartesNotificacaoResult result = notificacaoPort.notificar(publicacao);
        auditLedger.appendSafely(
                "DJE_NOTIFICACAO_PARTES",
                "PROCESSO",
                String.valueOf(publicacao.getProcessoId()),
                "djeId=" + publicacao.getId() + " tipoAto=" + publicacao.getTipoAto() + " success=" + result.success());
        return result;
    }
}
