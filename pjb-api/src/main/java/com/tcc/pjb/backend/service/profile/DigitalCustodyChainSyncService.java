package com.tcc.pjb.backend.service.profile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.Mac;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.crypto.KeyMaterialService;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodyLedgerResponse;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncBundleResponse;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncEventResponse;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncExportRequest;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncReplayRequest;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncReplayResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.CadeiaCustodiaSyncDirection;
import com.tcc.pjb.backend.model.entity.enums.CadeiaCustodiaSyncOperation;
import com.tcc.pjb.backend.model.entity.pericia.CadeiaCustodiaDigitalSyncEvent;
import com.tcc.pjb.backend.model.repository.CadeiaCustodiaDigitalSyncEventRepository;

@Service
public class DigitalCustodyChainSyncService {

    private static final HexFormat HEX = HexFormat.of();

    private final CurrentUserService currentUserService;
    private final KeyMaterialService keyMaterialService;
    private final DigitalCustodyChainLedgerService ledgerService;
    private final CadeiaCustodiaDigitalSyncEventRepository repository;
    private final ObjectProvider<HttpServletRequest> requestProvider;

    public DigitalCustodyChainSyncService(CurrentUserService currentUserService,
                                          KeyMaterialService keyMaterialService,
                                          DigitalCustodyChainLedgerService ledgerService,
                                          CadeiaCustodiaDigitalSyncEventRepository repository,
                                          ObjectProvider<HttpServletRequest> requestProvider) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.keyMaterialService = Objects.requireNonNull(keyMaterialService);
        this.ledgerService = Objects.requireNonNull(ledgerService);
        this.repository = Objects.requireNonNull(repository);
        this.requestProvider = Objects.requireNonNull(requestProvider);
    }

    @Transactional
    public ChainOfCustodySyncBundleResponse exportBundle(String chaveCustodia,
                                                         ChainOfCustodySyncExportRequest request) {
        ChainOfCustodyLedgerResponse ledger = ledgerService.findLedger(chaveCustodia)
                .orElseGet(() -> ledgerService.ledger(chaveCustodia));
        if (ledger == null) {
            throw new IllegalArgumentException("ledger_nao_encontrado");
        }
        Usuario actor = currentUserService.getRequired();
        String parceiro = normalizePartner(request == null ? null : request.parceiroInstitucional());
        String noOrigem = normalizeNode(request == null ? null : request.noOrigem(), actor);
        String nonce = normalizeNonce(request == null ? null : request.nonce());
        String justificativa = normalizeJustification(request == null ? null : request.justificativa());
        List<ChainOfCustodySyncBundleResponse.SyncLedgerEntry> entries = ledger.entradas().stream()
                .map(this::toBundleEntry)
                .toList();
        String payloadDigest = sha256(canonicalPayload(parceiro, noOrigem, ledger.chaveCustodia(), ledger.digestColecaoSha256(), entries));
        String signature = sign(payloadDigest, parceiro, noOrigem, nonce);
        Instant now = Instant.now();
        persistEvent(actor, ledger.chaveCustodia(), ledger.digestColecaoSha256(), parceiro, noOrigem, nonce, payloadDigest, signature, true, true, true, entries.size(), CadeiaCustodiaSyncDirection.OUTBOUND, CadeiaCustodiaSyncOperation.EXPORT, now);
        return new ChainOfCustodySyncBundleResponse(
                ledger.chaveCustodia(),
                ledger.digestColecaoSha256(),
                parceiro,
                noOrigem,
                nonce,
                justificativa,
                payloadDigest,
                signature,
                entries.size(),
                now,
                entries
        );
    }

    @Transactional
    public ChainOfCustodySyncReplayResponse replayVerify(ChainOfCustodySyncReplayRequest request) {
        if (request == null || request.entradas() == null || request.entradas().isEmpty()) {
            throw new IllegalArgumentException("replay_bundle_obrigatorio");
        }
        Usuario actor = currentUserService.getRequired();
        String parceiro = normalizePartner(request.parceiroInstitucional());
        String noOrigem = normalizeNode(request.noOrigem(), actor);
        String nonce = normalizeNonce(request.nonce());
        List<ChainOfCustodySyncBundleResponse.SyncLedgerEntry> entries = request.entradas().stream()
                .map(this::toBundleEntry)
                .toList();
        String computedDigest = sha256(canonicalPayload(parceiro, noOrigem, request.chaveCustodia(), request.digestColecaoSha256(), entries));
        boolean digestOk = Objects.equals(normalizeHex(request.payloadDigestSha256(), 64), computedDigest);
        boolean signatureOk = Objects.equals(normalizeHex(request.assinaturaHmacSha256(), 64), sign(computedDigest, parceiro, noOrigem, nonce));
        boolean structuralOk = structuralIntegrity(entries);
        boolean localMatch = ledgerService.findLedger(request.chaveCustodia())
                .map(local -> local.digestColecaoSha256().equalsIgnoreCase(request.digestColecaoSha256())
                        && local.totalEntradas() == entries.size()
                        && local.entradas().stream().map(this::toBundleEntry).toList().equals(entries))
                .orElse(false);
        Instant now = Instant.now();
        persistEvent(actor, request.chaveCustodia(), request.digestColecaoSha256(), parceiro, noOrigem, nonce, computedDigest, safeSignatureHex(request.assinaturaHmacSha256()), digestOk && structuralOk, signatureOk, localMatch, entries.size(), CadeiaCustodiaSyncDirection.INBOUND, CadeiaCustodiaSyncOperation.REPLAY_VERIFY, now);
        return new ChainOfCustodySyncReplayResponse(
                request.chaveCustodia(),
                signatureOk,
                digestOk,
                structuralOk,
                localMatch,
                entries.size(),
                computedDigest,
                now
        );
    }

    @Transactional(readOnly = true)
    public List<ChainOfCustodySyncEventResponse> history(String chaveCustodia) {
        if (chaveCustodia == null || chaveCustodia.isBlank()) {
            throw new IllegalArgumentException("chave_custodia_obrigatoria");
        }
        return repository.findTop100ByChaveCustodiaOrderByOccurredAtDesc(chaveCustodia.trim()).stream()
                .map(event -> new ChainOfCustodySyncEventResponse(
                        event.getChaveCustodia(),
                        event.getDigestColecaoSha256(),
                        event.getDirecao().name(),
                        event.getOperacao().name(),
                        event.getParceiroInstitucional(),
                        event.getNoOrigem(),
                        event.getRequestNonce(),
                        event.isIntegridadeOk(),
                        event.isAssinaturaOk(),
                        event.isCorrespondenciaLocalOk(),
                        event.getTotalEntradas(),
                        event.getOccurredAt(),
                        event.getPayloadDigestSha256()
                ))
                .toList();
    }

    private void persistEvent(Usuario actor,
                              String chaveCustodia,
                              String digestColecao,
                              String parceiro,
                              String noOrigem,
                              String nonce,
                              String payloadDigest,
                              String signature,
                              boolean integridadeOk,
                              boolean assinaturaOk,
                              boolean correspondenciaLocalOk,
                              int totalEntradas,
                              CadeiaCustodiaSyncDirection direction,
                              CadeiaCustodiaSyncOperation operation,
                              Instant occurredAt) {
        repository.save(CadeiaCustodiaDigitalSyncEvent.builder()
                .chaveCustodia(truncate(chaveCustodia, 32))
                .digestColecaoSha256(truncate(digestColecao, 64))
                .direcao(direction)
                .operacao(operation)
                .parceiroInstitucional(parceiro)
                .noOrigem(noOrigem)
                .requestNonce(nonce)
                .payloadDigestSha256(payloadDigest)
                .assinaturaHmacSha256(signature)
                .integridadeOk(integridadeOk)
                .assinaturaOk(assinaturaOk)
                .correspondenciaLocalOk(correspondenciaLocalOk)
                .totalEntradas(totalEntradas)
                .actorUserId(actor.getId())
                .actorPerfil(actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : actor.getPerfil())
                .occurredAt(occurredAt)
                .requestId(RequestContext.getRequestId().orElse(null))
                .ip(resolveIp())
                .build());
    }

    private String canonicalPayload(String parceiro,
                                    String noOrigem,
                                    String chaveCustodia,
                                    String digestColecao,
                                    List<ChainOfCustodySyncBundleResponse.SyncLedgerEntry> entries) {
        StringBuilder builder = new StringBuilder();
        builder.append(parceiro).append('|')
                .append(noOrigem).append('|')
                .append(chaveCustodia).append('|')
                .append(digestColecao).append('|')
                .append(entries.size());
        for (ChainOfCustodySyncBundleResponse.SyncLedgerEntry entry : entries) {
            builder.append('|').append(entry.ordemLote())
                    .append('|').append(entry.evidenceId())
                    .append('|').append(entry.evidenceNome())
                    .append('|').append(entry.digestSha256())
                    .append('|').append(entry.evidenceChaveCustodia())
                    .append('|').append(entry.metadataDigestSha256())
                    .append('|').append(valueOrDash(entry.prevHash()))
                    .append('|').append(entry.entryHash())
                    .append('|').append(String.valueOf(entry.sealedAt()))
                    .append('|').append(valueOrDash(entry.requestId()));
            for (String metadata : entry.metadadosCanonicos()) {
                builder.append('|').append(metadata == null ? "-" : metadata.trim());
            }
        }
        return builder.toString();
    }

    private boolean structuralIntegrity(List<ChainOfCustodySyncBundleResponse.SyncLedgerEntry> entries) {
        if (entries.isEmpty()) {
            return false;
        }
        String prev = null;
        int expected = 1;
        for (ChainOfCustodySyncBundleResponse.SyncLedgerEntry entry : entries.stream().sorted(java.util.Comparator.comparingInt(ChainOfCustodySyncBundleResponse.SyncLedgerEntry::ordemLote)).toList()) {
            if (entry.ordemLote() != expected++) {
                return false;
            }
            if (!Objects.equals(prev, entry.prevHash())) {
                return false;
            }
            if (normalizeHex(entry.entryHash(), 64) == null || normalizeHex(entry.digestSha256(), 64) == null || normalizeHex(entry.metadataDigestSha256(), 64) == null) {
                return false;
            }
            prev = entry.entryHash();
        }
        return true;
    }

    private ChainOfCustodySyncBundleResponse.SyncLedgerEntry toBundleEntry(ChainOfCustodyLedgerResponse.LedgerEntry entry) {
        return new ChainOfCustodySyncBundleResponse.SyncLedgerEntry(
                entry.ordemLote(),
                entry.evidenceId(),
                entry.evidenceNome(),
                entry.digestSha256(),
                entry.evidenceChaveCustodia(),
                entry.metadataDigestSha256(),
                entry.metadadosCanonicos(),
                entry.prevHash(),
                entry.entryHash(),
                entry.sealedAt(),
                entry.requestId()
        );
    }

    private ChainOfCustodySyncBundleResponse.SyncLedgerEntry toBundleEntry(ChainOfCustodySyncReplayRequest.SyncLedgerEntry entry) {
        return new ChainOfCustodySyncBundleResponse.SyncLedgerEntry(
                entry.ordemLote(),
                entry.evidenceId(),
                entry.evidenceNome(),
                normalizeHex(entry.digestSha256(), 64),
                truncate(entry.evidenceChaveCustodia(), 64),
                normalizeHex(entry.metadataDigestSha256(), 64),
                entry.metadadosCanonicos() == null ? List.of() : entry.metadadosCanonicos().stream().filter(java.util.Objects::nonNull).map(String::trim).filter(v -> !v.isBlank()).toList(),
                truncate(entry.prevHash(), 64),
                normalizeHex(entry.entryHash(), 64),
                entry.sealedAt(),
                truncate(entry.requestId(), 80)
        );
    }

    private String sign(String payloadDigest, String parceiro, String noOrigem, String nonce) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keyMaterialService.getCustodyMeshSigningKey());
            byte[] data = String.join("|", payloadDigest, parceiro, noOrigem, nonce).getBytes(StandardCharsets.UTF_8);
            return HEX.formatHex(mac.doFinal(data));
        } catch (Exception ex) {
            throw new IllegalStateException("custody_mesh_signature_unavailable", ex);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HEX.formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("sha256_unavailable", ex);
        }
    }

    private String resolveIp() {
        HttpServletRequest request = requestProvider.getIfAvailable();
        if (request == null) {
            return null;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return truncate(xff.split(",")[0].trim(), 80);
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return truncate(realIp.trim(), 80);
        }
        return truncate(request.getRemoteAddr(), 80);
    }

    private String normalizePartner(String value) {
        String normalized = value == null || value.isBlank() ? "MALHA_INTERINSTITUCIONAL_PJB" : value.trim().toUpperCase();
        return truncate(normalized, 120);
    }

    private String normalizeNode(String value, Usuario actor) {
        if (value != null && !value.isBlank()) {
            return truncate(value.trim().toUpperCase(), 120);
        }
        String perfil = actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : actor.getPerfil();
        return truncate("PJB-" + perfil, 120);
    }

    private String normalizeNonce(String value) {
        String normalized = value == null || value.isBlank() ? UUID.randomUUID().toString() : value.trim();
        return truncate(normalized, 80);
    }

    private String normalizeJustification(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return truncate(value.trim(), 255);
    }

    private String normalizeHex(String value, int len) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase().replaceAll("[^0-9a-f]", "");
        return normalized.length() == len ? normalized : null;
    }

    private String safeSignatureHex(String value) {
        String normalized = normalizeHex(value, 64);
        return normalized != null ? normalized : "0".repeat(64);
    }

    private String truncate(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }
}
