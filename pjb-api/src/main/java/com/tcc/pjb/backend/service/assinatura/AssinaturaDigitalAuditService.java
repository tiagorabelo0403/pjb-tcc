package com.tcc.pjb.backend.service.assinatura;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AssinaturaDigitalAuditService {

    public record AssinaturaAuditEntry(
            UUID auditId,
            UUID documentoId,
            String cpfAssinante,
            String acao,
            LocalDateTime dataHora,
            boolean valida,
            String nivel,
            String ip,
            String hashDocumento
    ) {}

    public record AuditRelatorio(
            UUID documentoId,
            List<AssinaturaAuditEntry> historico,
            int totalAssinaturas,
            int assinaturasValidas,
            boolean integridadeConfirmada,
            LocalDateTime dataRelatorio
    ) {}

    public AuditRelatorio gerarRelatorio(UUID documentoId, List<AssinaturaAuditEntry> entradas) {
        List<AssinaturaAuditEntry> doDocumento = entradas.stream()
                .filter(e -> e.documentoId().equals(documentoId))
                .toList();

        long validas = doDocumento.stream().filter(AssinaturaAuditEntry::valida).count();
        boolean integridadeOk = !doDocumento.isEmpty()
                && doDocumento.stream().allMatch(e ->
                        doDocumento.getFirst().hashDocumento().equals(e.hashDocumento()));

        return new AuditRelatorio(
                documentoId,
                doDocumento,
                doDocumento.size(),
                (int) validas,
                integridadeOk,
                LocalDateTime.now());
    }

    public AssinaturaAuditEntry registrar(UUID documentoId, String cpfAssinante,
            String acao, boolean valida, String nivel, String ip, String hashDocumento) {
        return new AssinaturaAuditEntry(
                UUID.randomUUID(),
                documentoId,
                cpfAssinante,
                acao,
                LocalDateTime.now(),
                valida,
                nivel,
                ip,
                hashDocumento);
    }
}
