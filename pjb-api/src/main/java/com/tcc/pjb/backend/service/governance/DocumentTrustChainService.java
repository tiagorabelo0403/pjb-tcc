package com.tcc.pjb.backend.service.governance;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.governance.DocumentTrustChainReportResponse;
import com.tcc.pjb.backend.model.dto.governance.DocumentTrustChainSealResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.pericia.CadeiaCustodiaDigitalLedgerEntry;
import com.tcc.pjb.backend.model.repository.CadeiaCustodiaDigitalLedgerEntryRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class DocumentTrustChainService {

    private final ProcessoRepository processoRepository;
    private final DocumentoProcessualRepository documentoRepository;
    private final CadeiaCustodiaDigitalLedgerEntryRepository cadeiaCustodiaRepository;
    private final CurrentUserService currentUserService;
    private final AuditLedgerService auditLedgerService;

    public DocumentTrustChainService(ProcessoRepository processoRepository,
                                     DocumentoProcessualRepository documentoRepository,
                                     CadeiaCustodiaDigitalLedgerEntryRepository cadeiaCustodiaRepository,
                                     CurrentUserService currentUserService,
                                     AuditLedgerService auditLedgerService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.cadeiaCustodiaRepository = Objects.requireNonNull(cadeiaCustodiaRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional
    public DocumentTrustChainSealResponse selar(Long processoId,
                                                Long documentoId,
                                                String loteReferencia,
                                                String motivo,
                                                boolean contrassinado,
                                                boolean preservaSigilo,
                                                String nivelAssinatura) {
        UUID coerced = documentoId == null ? null : new UUID(0L, documentoId);
        return selar(processoId, coerced, loteReferencia, motivo, contrassinado, preservaSigilo, nivelAssinatura);
    }

    @Transactional
    public DocumentTrustChainSealResponse selar(Long processoId,
                                                UUID documentoId,
                                                String loteReferencia,
                                                String motivo,
                                                boolean contrassinado,
                                                boolean preservaSigilo,
                                                String nivelAssinatura) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        DocumentoProcessual documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("DocumentoProcessual", documentoId));
        Usuario usuario = currentUserService.getRequired();
        String chaveCustodia = "proc:" + processoId;
        String digestDocumento = resolveDigestDocumento(documento);
        String digestColecao = Hashes.sha256Hex(String.join("|",
                chaveCustodia,
                documento.getId().toString(),
                digestDocumento,
                normalize(documento.getTitulo()),
                normalize(nivelAssinatura),
                normalize(motivo),
                Boolean.toString(contrassinado),
                Boolean.toString(preservaSigilo)));
        Optional<CadeiaCustodiaDigitalLedgerEntry> existente = cadeiaCustodiaRepository
                .findTop200ByChaveCustodiaOrderBySealedAtDesc(chaveCustodia)
                .stream()
                .filter(entry -> documento.getId().toString().equals(entry.getEvidenceId()))
                .filter(entry -> digestDocumento.equals(entry.getDigestSha256()))
                .findFirst();
        if (existente.isPresent()) {
            CadeiaCustodiaDigitalLedgerEntry entry = existente.get();
            return new DocumentTrustChainSealResponse(
                    processoId,
                    processo.getNumeroProcesso(),
                    documento.getId().toString(),
                    resolveTituloDocumento(documento),
                    digestDocumento,
                    entry.getDigestColecaoSha256(),
                    entry.getChaveCustodia(),
                    entry.getEntryHash(),
                    false,
                    true,
                    entry.getSealedAt(),
                    entry.getSealedByPerfil(),
                    entry.getLoteReferencia()
            );
        }
        String prevHash = cadeiaCustodiaRepository.findTopByOrderByIdDesc().map(CadeiaCustodiaDigitalLedgerEntry::getEntryHash).orElse(null);
        String metadataCanonica = metadataCanonica(documento, processo, motivo, contrassinado, preservaSigilo, nivelAssinatura);
        String entryHash = Hashes.sha256Hex(String.join("|",
                normalize(prevHash),
                digestColecao,
                chaveCustodia,
                documento.getId().toString(),
                digestDocumento,
                Hashes.sha256Hex(metadataCanonica),
                normalize(loteReferencia),
                String.valueOf(usuario.getId()),
                normalize(resolvePerfil(usuario))));
        CadeiaCustodiaDigitalLedgerEntry entry = CadeiaCustodiaDigitalLedgerEntry.builder()
                .digestColecaoSha256(digestColecao)
                .chaveCustodia(chaveCustodia)
                .loteReferencia(limit(loteReferencia, 160))
                .ordemLote(resolveOrdemLote(chaveCustodia))
                .evidenceId(documento.getId().toString())
                .evidenceNome(limit(resolveTituloDocumento(documento), 255))
                .digestSha256(digestDocumento)
                .evidenceChaveCustodia(Hashes.sha256Hex(chaveCustodia + "|" + documento.getId()))
                .metadataDigestSha256(Hashes.sha256Hex(metadataCanonica))
                .metadataCanonica(metadataCanonica)
                .sealedByUserId(usuario.getId())
                .sealedByPerfil(limit(resolvePerfil(usuario), 80))
                .sealedAt(Instant.now())
                .requestId(RequestContext.getRequestId().orElse(null))
                .prevHash(prevHash)
                .entryHash(entryHash)
                .build();
        cadeiaCustodiaRepository.save(entry);
        auditLedgerService.appendSafely(
                "DOCUMENT_TRUST_SEAL",
                "DOCUMENTO_PROCESSUAL",
                documento.getId().toString(),
                entryHash
        );
        return new DocumentTrustChainSealResponse(
                processoId,
                processo.getNumeroProcesso(),
                documento.getId().toString(),
                resolveTituloDocumento(documento),
                digestDocumento,
                digestColecao,
                chaveCustodia,
                entryHash,
                true,
                false,
                entry.getSealedAt(),
                entry.getSealedByPerfil(),
                entry.getLoteReferencia()
        );
    }

    public DocumentTrustChainReportResponse analisarProcesso(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        List<DocumentoProcessual> documentos = documentoRepository.findByProcessoId(processoId);
        List<CadeiaCustodiaDigitalLedgerEntry> entradas = cadeiaCustodiaRepository.findTop200ByChaveCustodiaOrderBySealedAtDesc("proc:" + processoId);
        List<DocumentTrustChainReportResponse.DocumentTrustChainDocumentView> views = documentos.stream()
                .sorted(Comparator.comparing(DocumentoProcessual::getCriadoEm, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(DocumentoProcessual::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(documento -> {
                    CadeiaCustodiaDigitalLedgerEntry entry = entradas.stream()
                            .filter(it -> documento.getId().toString().equals(it.getEvidenceId()))
                            .findFirst()
                            .orElse(null);
                    return new DocumentTrustChainReportResponse.DocumentTrustChainDocumentView(
                            documento.getId().toString(),
                            resolveTituloDocumento(documento),
                            resolveDigestDocumento(documento),
                            entry != null,
                            entry == null ? null : entry.getChaveCustodia(),
                            entry == null ? null : entry.getSealedAt(),
                            entry == null ? null : entry.getEntryHash(),
                            entry == null ? null : entry.getLoteReferencia(),
                            entry == null ? null : entry.getSealedByPerfil()
                    );
                })
                .collect(Collectors.toList());
        return new DocumentTrustChainReportResponse(
                processoId,
                processo.getNumeroProcesso(),
                documentos.size(),
                entradas.size(),
                views
        );
    }

    private int resolveOrdemLote(String chaveCustodia) {
        return cadeiaCustodiaRepository.findTop200ByChaveCustodiaOrderBySealedAtDesc(chaveCustodia).size() + 1;
    }

    private String resolveTituloDocumento(DocumentoProcessual documento) {
        if (documento.getTitulo() != null && !documento.getTitulo().isBlank()) {
            return documento.getTitulo().trim();
        }
        if (documento.getNomeOriginal() != null && !documento.getNomeOriginal().isBlank()) {
            return documento.getNomeOriginal().trim();
        }
        return documento.getId().toString();
    }

    private String resolveDigestDocumento(DocumentoProcessual documento) {
        if (documento.getSha256() != null && !documento.getSha256().isBlank()) {
            return documento.getSha256().toLowerCase(Locale.ROOT);
        }
        if (documento.getPdf() != null && documento.getPdf().length > 0) {
            return Hashes.sha256HexBytes(documento.getPdf());
        }
        return Hashes.sha256Hex(resolveTituloDocumento(documento));
    }

    private String resolvePerfil(Usuario usuario) {
        return usuario == null || usuario.getTipoUsuario() == null ? "NAO_IDENTIFICADO" : usuario.getTipoUsuario().name();
    }

    private String metadataCanonica(DocumentoProcessual documento,
                                    Processo processo,
                                    String motivo,
                                    boolean contrassinado,
                                    boolean preservaSigilo,
                                    String nivelAssinatura) {
        List<String> linhas = new ArrayList<>();
        linhas.add("processo_id=" + processo.getId());
        linhas.add("processo_numero=" + normalize(processo.getNumeroProcesso()));
        linhas.add("documento_id=" + documento.getId());
        linhas.add("documento_titulo=" + normalize(resolveTituloDocumento(documento)));
        linhas.add("documento_sha256=" + resolveDigestDocumento(documento));
        linhas.add("motivo=" + normalize(motivo));
        linhas.add("contrassinado=" + contrassinado);
        linhas.add("preserva_sigilo=" + preservaSigilo);
        linhas.add("nivel_assinatura=" + normalize(nivelAssinatura));
        linhas.add("content_type=" + normalize(documento.getContentType()));
        linhas.add("storage_backend=" + normalize(documento.getStorageBackend()));
        return String.join("\n", linhas);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String limit(String value, int max) {
        String normalized = normalize(value);
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }
}
