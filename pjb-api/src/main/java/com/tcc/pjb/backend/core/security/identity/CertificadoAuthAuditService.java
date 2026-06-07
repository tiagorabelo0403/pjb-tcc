package com.tcc.pjb.backend.core.security.identity;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.util.Hashes;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CertificadoAuthAuditService {

    private final AuditLedgerService auditLedger;

    public CertificadoAuthAuditService(AuditLedgerService auditLedger) {
        this.auditLedger = Objects.requireNonNull(auditLedger);
    }

    public void registrar(String etapa, boolean sucesso, String motivo) {
        String etapaNormalizada = normalizar(etapa);
        String resultado = sucesso ? "SUCESSO" : "FALHA";
        String motivoNormalizado = normalizar(motivo);
        String evento = "AUTH_CERTIFICADO_" + etapaNormalizada + "_" + resultado;
        String payloadHash = Hashes.sha256Hex(evento + "#" + motivoNormalizado);
        auditLedger.appendSafely(
                evento,
                "AUTH_CERTIFICADO",
                etapaNormalizada,
                payloadHash,
                "motivo=" + motivoNormalizado);
    }

    private static String normalizar(String valor) {
        String texto = valor == null || valor.isBlank() ? "NAO_INFORMADO" : valor;
        return texto
                .replaceAll("[^A-Za-z0-9_:-]", "_")
                .toUpperCase(Locale.ROOT);
    }
}
