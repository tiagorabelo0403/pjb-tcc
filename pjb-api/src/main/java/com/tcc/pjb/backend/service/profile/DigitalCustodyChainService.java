package com.tcc.pjb.backend.service.profile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySealRequest;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySealResponse;
import com.tcc.pjb.backend.model.entity.Usuario;

@Service
public class DigitalCustodyChainService {

    private static final HexFormat HEX = HexFormat.of();
    private static final Duration IO_TIMEOUT = Duration.ofSeconds(5);

    private final CurrentUserService currentUserService;
    private final DigitalCustodyChainLedgerService ledgerService;
    private final ExecutorService ioExecutor;

    public DigitalCustodyChainService(CurrentUserService currentUserService,
                                      DigitalCustodyChainLedgerService ledgerService,
                                      @Qualifier("pjbIoExecutorService") ExecutorService ioExecutor) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.ledgerService = Objects.requireNonNull(ledgerService);
        this.ioExecutor = Objects.requireNonNull(ioExecutor);
    }

    public ChainOfCustodySealResponse seal(ChainOfCustodySealRequest request) {
        if (request == null || request.evidencias() == null || request.evidencias().isEmpty()) {
            throw new IllegalArgumentException("evidencias_obrigatorias");
        }
        Usuario actor = currentUserService.getRequired();
        Instant now = Instant.now();
        List<ChainOfCustodySealResponse.SealedEvidence> sealed;
        try {
            List<Callable<ChainOfCustodySealResponse.SealedEvidence>> tasks = request.evidencias().stream()
                    .map(item -> (Callable<ChainOfCustodySealResponse.SealedEvidence>) () -> sealItem(item, now))
                    .toList();
            sealed = ioExecutor.invokeAll(tasks, IO_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS).stream().map(future -> {
                if (future.isCancelled()) {
                    throw new IllegalStateException("custodia_timeout_controlado");
                }
                try {
                    return future.get();
                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof RuntimeException runtimeException) {
                        throw runtimeException;
                    }
                    throw new IllegalStateException(cause == null ? ex : cause);
                } catch (Exception ex) {
                    throw new IllegalStateException(ex);
                }
            }).sorted(Comparator.comparing(ChainOfCustodySealResponse.SealedEvidence::id)).toList();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("custodia_interrompida", ex);
        }
        String digestColecao = digestCollection(request.loteReferencia(), sealed);
        String chaveCustodia = "CST-" + digestColecao.substring(0, 24).toUpperCase();
        String perfil = actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : actor.getPerfil();
        return ledgerService.persist(
                request.loteReferencia(),
                digestColecao,
                chaveCustodia,
                perfil,
                actor.getId(),
                now,
                sealed
        );
    }

    private ChainOfCustodySealResponse.SealedEvidence sealItem(ChainOfCustodySealRequest.EvidenceItem item, Instant now) {
        if (item == null) {
            throw new IllegalArgumentException("evidencia_nula");
        }
        String id = hasText(item.id()) ? item.id().trim() : "EVD-" + Math.abs(Objects.hash(item.nome(), item.digestSha256Externo(), item.metadados()));
        String nome = hasText(item.nome()) ? item.nome().trim() : "evidencia";
        String digest = resolveDigest(item);
        List<String> canonicos = canonicalMetadata(item.metadados());
        String custodyKey = "EVD-" + digest.substring(0, 20).toUpperCase();
        return new ChainOfCustodySealResponse.SealedEvidence(id, nome, digest, custodyKey, now, canonicos);
    }

    private String resolveDigest(ChainOfCustodySealRequest.EvidenceItem item) {
        String external = normalizeDigest(item.digestSha256Externo());
        if (hasText(item.conteudoBase64())) {
            byte[] bytes = Base64.getDecoder().decode(item.conteudoBase64());
            String internal = sha256(bytes);
            if (external != null && !external.equals(internal)) {
                throw new IllegalArgumentException("digest_externo_divergente");
            }
            return internal;
        }
        if (external == null) {
            throw new IllegalArgumentException("conteudo_ou_digest_obrigatorio");
        }
        return external;
    }

    private List<String> canonicalMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return List.of();
        }
        TreeMap<String, String> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        sorted.putAll(metadata);
        return sorted.entrySet().stream()
                .map(entry -> entry.getKey().trim() + '=' + String.valueOf(entry.getValue()).trim())
                .toList();
    }

    private String digestCollection(String loteReferencia, List<ChainOfCustodySealResponse.SealedEvidence> sealed) {
        StringBuilder builder = new StringBuilder();
        builder.append(loteReferencia == null ? "" : loteReferencia.trim());
        for (ChainOfCustodySealResponse.SealedEvidence evidence : sealed) {
            builder.append('|').append(evidence.id()).append('|').append(evidence.digestSha256());
            for (String metadata : evidence.metadadosCanonicos()) {
                builder.append('|').append(metadata);
            }
        }
        return sha256(builder.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String normalizeDigest(String value) {
        if (!hasText(value)) {
            return null;
        }
        String normalized = value.trim().toLowerCase().replaceAll("[^0-9a-f]", "");
        return normalized.length() == 64 ? normalized : null;
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HEX.formatHex(digest.digest(bytes));
        } catch (Exception ex) {
            throw new IllegalStateException("sha256_unavailable", ex);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
