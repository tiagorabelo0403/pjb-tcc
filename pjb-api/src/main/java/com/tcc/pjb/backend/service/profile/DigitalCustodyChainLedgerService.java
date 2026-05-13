package com.tcc.pjb.backend.service.profile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodyLedgerResponse;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySealResponse;
import com.tcc.pjb.backend.model.entity.pericia.CadeiaCustodiaDigitalLedgerEntry;
import com.tcc.pjb.backend.model.repository.CadeiaCustodiaDigitalLedgerEntryRepository;

@Service
public class DigitalCustodyChainLedgerService {

    private static final HexFormat HEX = HexFormat.of();

    private final CadeiaCustodiaDigitalLedgerEntryRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectProvider<HttpServletRequest> requestProvider;
    private final AuditLedgerService auditLedgerService;

    public DigitalCustodyChainLedgerService(CadeiaCustodiaDigitalLedgerEntryRepository repository,
                                            JdbcTemplate jdbcTemplate,
                                            ObjectProvider<HttpServletRequest> requestProvider,
                                            AuditLedgerService auditLedgerService) {
        this.repository = Objects.requireNonNull(repository);
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
        this.requestProvider = Objects.requireNonNull(requestProvider);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional
    public ChainOfCustodySealResponse persist(String loteReferencia,
                                              String digestColecao,
                                              String chaveCustodia,
                                              String sealedBy,
                                              Long sealedByUserId,
                                              Instant sealedAt,
                                              List<ChainOfCustodySealResponse.SealedEvidence> evidencias) {
        validate(digestColecao, chaveCustodia, sealedBy, sealedAt, evidencias);
        List<CadeiaCustodiaDigitalLedgerEntry> existing = repository.findAllByDigestColecaoSha256OrderByOrdemLoteAsc(digestColecao);
        if (!existing.isEmpty()) {
            return toSealResponse(existing);
        }
        tryAdvisoryLock();
        existing = repository.findAllByDigestColecaoSha256OrderByOrdemLoteAsc(digestColecao);
        if (!existing.isEmpty()) {
            return toSealResponse(existing);
        }
        String requestId = RequestContext.getRequestId().orElse(null);
        String ip = resolveIp();
        String userAgent = resolveUserAgent();
        AtomicReference<String> previousHash = new AtomicReference<>(repository.findTopByOrderByIdDesc().map(CadeiaCustodiaDigitalLedgerEntry::getEntryHash).orElse(null));
        List<CadeiaCustodiaDigitalLedgerEntry> batch = buildBatch(loteReferencia, digestColecao, chaveCustodia, sealedBy, sealedByUserId, sealedAt, evidencias, requestId, ip, userAgent, previousHash);
        try {
            List<CadeiaCustodiaDigitalLedgerEntry> saved = repository.saveAll(batch);
            auditLedgerService.appendSafely("CUSTODIA_DIGITAL_SELADA", "CADEIA_CUSTODIA", chaveCustodia, digestColecao, sealedBy);
            return toSealResponse(saved.stream().sorted(java.util.Comparator.comparingInt(CadeiaCustodiaDigitalLedgerEntry::getOrdemLote)).toList());
        } catch (DataIntegrityViolationException ex) {
            List<CadeiaCustodiaDigitalLedgerEntry> replay = repository.findAllByDigestColecaoSha256OrderByOrdemLoteAsc(digestColecao);
            if (!replay.isEmpty()) {
                return toSealResponse(replay);
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Optional<ChainOfCustodyLedgerResponse> findLedger(String chaveCustodia) {
        if (chaveCustodia == null || chaveCustodia.isBlank()) {
            return Optional.empty();
        }
        List<CadeiaCustodiaDigitalLedgerEntry> entries = repository.findTop200ByChaveCustodiaOrderBySealedAtDesc(chaveCustodia.trim()).stream()
                .sorted(java.util.Comparator.comparingInt(CadeiaCustodiaDigitalLedgerEntry::getOrdemLote))
                .toList();
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toLedgerResponse(entries));
    }

    @Transactional(readOnly = true)
    public ChainOfCustodyLedgerResponse ledger(String chaveCustodia) {
        if (chaveCustodia == null || chaveCustodia.isBlank()) {
            throw new IllegalArgumentException("chave_custodia_obrigatoria");
        }
        List<CadeiaCustodiaDigitalLedgerEntry> entries = repository.findTop200ByChaveCustodiaOrderBySealedAtDesc(chaveCustodia.trim()).stream()
                .sorted(java.util.Comparator.comparingInt(CadeiaCustodiaDigitalLedgerEntry::getOrdemLote))
                .toList();
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("cadeia_custodia_nao_encontrada");
        }
        return toLedgerResponse(entries);
    }

    private void validate(String digestColecao,
                          String chaveCustodia,
                          String sealedBy,
                          Instant sealedAt,
                          List<ChainOfCustodySealResponse.SealedEvidence> evidencias) {
        if (digestColecao == null || digestColecao.isBlank()) {
            throw new IllegalArgumentException("digest_colecao_obrigatorio");
        }
        if (chaveCustodia == null || chaveCustodia.isBlank()) {
            throw new IllegalArgumentException("chave_custodia_obrigatoria");
        }
        if (sealedBy == null || sealedBy.isBlank()) {
            throw new IllegalArgumentException("sealed_by_obrigatorio");
        }
        if (sealedAt == null) {
            throw new IllegalArgumentException("sealed_at_obrigatorio");
        }
        if (evidencias == null || evidencias.isEmpty()) {
            throw new IllegalArgumentException("evidencias_obrigatorias");
        }
    }

    private List<CadeiaCustodiaDigitalLedgerEntry> buildBatch(String loteReferencia,
                                                              String digestColecao,
                                                              String chaveCustodia,
                                                              String sealedBy,
                                                              Long sealedByUserId,
                                                              Instant sealedAt,
                                                              List<ChainOfCustodySealResponse.SealedEvidence> evidencias,
                                                              String requestId,
                                                              String ip,
                                                              String userAgent,
                                                              AtomicReference<String> previousHash) {
        return java.util.stream.IntStream.range(0, evidencias.size())
                .mapToObj(index -> toEntry(index + 1, loteReferencia, digestColecao, chaveCustodia, sealedBy, sealedByUserId, sealedAt, evidencias.get(index), requestId, ip, userAgent, previousHash))
                .toList();
    }

    private CadeiaCustodiaDigitalLedgerEntry toEntry(int order,
                                                     String loteReferencia,
                                                     String digestColecao,
                                                     String chaveCustodia,
                                                     String sealedBy,
                                                     Long sealedByUserId,
                                                     Instant sealedAt,
                                                     ChainOfCustodySealResponse.SealedEvidence evidence,
                                                     String requestId,
                                                     String ip,
                                                     String userAgent,
                                                     AtomicReference<String> previousHash) {
        String metadataCanonical = joinMetadata(evidence.metadadosCanonicos());
        String metadataDigest = sha256(metadataCanonical);
        String prevHash = previousHash.get();
        String entryHash = sha256(String.join("|",
                valueOrGenesis(prevHash),
                digestColecao,
                chaveCustodia,
                String.valueOf(order),
                safe(evidence.id()),
                safe(evidence.nome()),
                safe(evidence.digestSha256()),
                safe(evidence.chaveCustodia()),
                metadataDigest,
                String.valueOf(sealedAt),
                safe(sealedBy),
                safe(requestId),
                safe(ip),
                safe(userAgent)
        ));
        previousHash.set(entryHash);
        return CadeiaCustodiaDigitalLedgerEntry.builder()
                .digestColecaoSha256(digestColecao)
                .chaveCustodia(chaveCustodia)
                .loteReferencia(blankToNull(loteReferencia))
                .ordemLote(order)
                .evidenceId(evidence.id())
                .evidenceNome(evidence.nome())
                .digestSha256(evidence.digestSha256())
                .evidenceChaveCustodia(evidence.chaveCustodia())
                .metadataDigestSha256(metadataDigest)
                .metadataCanonica(blankToNull(metadataCanonical))
                .sealedByUserId(sealedByUserId)
                .sealedByPerfil(sealedBy)
                .sealedAt(sealedAt)
                .requestId(blankToNull(requestId))
                .ip(blankToNull(ip))
                .userAgent(truncate(userAgent, 255))
                .prevHash(prevHash)
                .entryHash(entryHash)
                .build();
    }

    private ChainOfCustodySealResponse toSealResponse(List<CadeiaCustodiaDigitalLedgerEntry> entries) {
        CadeiaCustodiaDigitalLedgerEntry first = entries.getFirst();
        List<ChainOfCustodySealResponse.SealedEvidence> evidencias = entries.stream()
                .sorted(java.util.Comparator.comparingInt(CadeiaCustodiaDigitalLedgerEntry::getOrdemLote))
                .map(entry -> new ChainOfCustodySealResponse.SealedEvidence(
                        entry.getEvidenceId(),
                        entry.getEvidenceNome(),
                        entry.getDigestSha256(),
                        entry.getEvidenceChaveCustodia(),
                        entry.getSealedAt(),
                        parseMetadata(entry.getMetadataCanonica())
                ))
                .toList();
        return new ChainOfCustodySealResponse(
                first.getLoteReferencia(),
                first.getDigestColecaoSha256(),
                first.getChaveCustodia(),
                first.getSealedByPerfil(),
                first.getSealedAt(),
                evidencias
        );
    }

    private ChainOfCustodyLedgerResponse toLedgerResponse(List<CadeiaCustodiaDigitalLedgerEntry> entries) {
        CadeiaCustodiaDigitalLedgerEntry first = entries.getFirst();
        boolean integrity = integrity(entries);
        return new ChainOfCustodyLedgerResponse(
                first.getLoteReferencia(),
                first.getDigestColecaoSha256(),
                first.getChaveCustodia(),
                first.getSealedByPerfil(),
                first.getSealedAt(),
                entries.size(),
                integrity,
                entries.stream().map(entry -> new ChainOfCustodyLedgerResponse.LedgerEntry(
                        entry.getOrdemLote(),
                        entry.getEvidenceId(),
                        entry.getEvidenceNome(),
                        entry.getDigestSha256(),
                        entry.getEvidenceChaveCustodia(),
                        entry.getMetadataDigestSha256(),
                        parseMetadata(entry.getMetadataCanonica()),
                        entry.getPrevHash(),
                        entry.getEntryHash(),
                        entry.getSealedAt(),
                        entry.getRequestId()
                )).toList()
        );
    }

    private boolean integrity(List<CadeiaCustodiaDigitalLedgerEntry> entries) {
        String prev = null;
        for (CadeiaCustodiaDigitalLedgerEntry entry : entries.stream().sorted(java.util.Comparator.comparingInt(CadeiaCustodiaDigitalLedgerEntry::getOrdemLote)).toList()) {
            String metadataDigest = sha256(joinMetadata(parseMetadata(entry.getMetadataCanonica())));
            String rebuilt = sha256(String.join("|",
                    valueOrGenesis(prev),
                    entry.getDigestColecaoSha256(),
                    entry.getChaveCustodia(),
                    String.valueOf(entry.getOrdemLote()),
                    safe(entry.getEvidenceId()),
                    safe(entry.getEvidenceNome()),
                    safe(entry.getDigestSha256()),
                    safe(entry.getEvidenceChaveCustodia()),
                    metadataDigest,
                    String.valueOf(entry.getSealedAt()),
                    safe(entry.getSealedByPerfil()),
                    safe(entry.getRequestId()),
                    safe(entry.getIp()),
                    safe(entry.getUserAgent())
            ));
            if (!Objects.equals(prev, entry.getPrevHash()) || !Objects.equals(rebuilt, entry.getEntryHash())) {
                return false;
            }
            prev = entry.getEntryHash();
        }
        return true;
    }

    private String joinMetadata(List<String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "";
        }
        return metadata.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .collect(Collectors.joining("\n"));
    }

    private List<String> parseMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return List.of();
        }
        return metadata.lines().map(String::trim).filter(v -> !v.isBlank()).toList();
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

    private String resolveUserAgent() {
        HttpServletRequest request = requestProvider.getIfAvailable();
        return request == null ? null : truncate(request.getHeader("User-Agent"), 255);
    }

    private void tryAdvisoryLock() {
        try {
            jdbcTemplate.execute("select pg_advisory_xact_lock(hashtext('PJB_CUSTODIA_DIGITAL_LEDGER'))");
        } catch (Exception ignored) {
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

    private String truncate(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String valueOrGenesis(String value) {
        return value == null || value.isBlank() ? "GENESIS" : value;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
