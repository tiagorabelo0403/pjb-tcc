package com.tcc.pjb.backend.service.profile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.Mac;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.kernel.process.ProcessEventStore;
import com.tcc.pjb.backend.core.kernel.process.ProcessEventType;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.security.crypto.KeyMaterialService;
import com.tcc.pjb.backend.model.dto.profile.DiligenceAutomaticFilingRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceAutomaticFilingResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCertidao;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCertidaoDocumento;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorEncerramento;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorFormalizacaoProcessual;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorJuntadaProcessual;
import com.tcc.pjb.backend.model.entity.kernel.ProcessEventEnvelope;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCertidaoDocumentoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCertidaoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorEncerramentoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorFormalizacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorJuntadaProcessualRepository;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;

@Service
public class DiligenceAutomaticFilingService {

    private static final HexFormat HEX = HexFormat.of();

    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;
    private final KeyMaterialService keyMaterialService;
    private final DiligenciaOperadorFormalizacaoProcessualRepository formalizacaoRepository;
    private final DiligenciaOperadorJuntadaProcessualRepository juntadaRepository;
    private final DiligenciaOperadorCertidaoRepository certidaoRepository;
    private final DiligenciaOperadorEncerramentoRepository encerramentoRepository;
    private final DiligenciaOperadorCertidaoDocumentoRepository certidaoDocumentoRepository;
    private final ProcessoRepository processoRepository;
    private final DocumentoProcessualRepository documentoRepository;
    private final MovimentacaoProcessualRepository movimentacaoRepository;
    private final ProcessEventStore processEventStore;
    private final DiligenceAutomaticFilingPdfService filingPdfService;
    private final QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService;

    public DiligenceAutomaticFilingService(CurrentUserService currentUserService,
                                           PjbAuthorizationService authorizationService,
                                           KeyMaterialService keyMaterialService,
                                           DiligenciaOperadorFormalizacaoProcessualRepository formalizacaoRepository,
                                           DiligenciaOperadorJuntadaProcessualRepository juntadaRepository,
                                           DiligenciaOperadorCertidaoRepository certidaoRepository,
                                           DiligenciaOperadorEncerramentoRepository encerramentoRepository,
                                           DiligenciaOperadorCertidaoDocumentoRepository certidaoDocumentoRepository,
                                           ProcessoRepository processoRepository,
                                           DocumentoProcessualRepository documentoRepository,
                                           MovimentacaoProcessualRepository movimentacaoRepository,
                                           ProcessEventStore processEventStore,
                                           DiligenceAutomaticFilingPdfService filingPdfService,
                                           QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.keyMaterialService = Objects.requireNonNull(keyMaterialService);
        this.formalizacaoRepository = Objects.requireNonNull(formalizacaoRepository);
        this.juntadaRepository = Objects.requireNonNull(juntadaRepository);
        this.certidaoRepository = Objects.requireNonNull(certidaoRepository);
        this.encerramentoRepository = Objects.requireNonNull(encerramentoRepository);
        this.certidaoDocumentoRepository = Objects.requireNonNull(certidaoDocumentoRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.movimentacaoRepository = Objects.requireNonNull(movimentacaoRepository);
        this.processEventStore = Objects.requireNonNull(processEventStore);
        this.filingPdfService = Objects.requireNonNull(filingPdfService);
        this.qualifiedDocumentSignatureEnvelopeService = Objects.requireNonNull(qualifiedDocumentSignatureEnvelopeService);
    }

    @Transactional
    public DiligenceAutomaticFilingResponse file(TelemetriaOperacionalCanal canal,
                                                 String diligenceReference,
                                                 DiligenceAutomaticFilingRequest request) {
        if (canal == null) {
            throw new IllegalArgumentException("canal_obrigatorio");
        }
        if (diligenceReference == null || diligenceReference.isBlank()) {
            throw new IllegalArgumentException("diligencia_referencia_obrigatoria");
        }
        boolean gerarPacotePdf = request == null || request.gerarPacotePdf() == null || request.gerarPacotePdf();
        boolean registrarMovimentacao = request == null || request.registrarMovimentacao() == null || request.registrarMovimentacao();
        if (!gerarPacotePdf && !registrarMovimentacao) {
            throw new IllegalArgumentException("juntada_sem_operacao");
        }
        Usuario actor = currentUserService.getRequired();
        String normalizedReference = diligenceReference.trim();
        DiligenciaOperadorFormalizacaoProcessual formalizacao = resolveFormalizacao(actor, canal, normalizedReference, request);
        Processo processo = processoRepository.findById(formalizacao.getProcessoId())
                .orElseThrow(() -> new IllegalArgumentException("processo_da_formalizacao_nao_encontrado"));
        authorizationService.requireWriteProcesso(processo);
        DiligenciaOperadorCertidao certidao = certidaoRepository.findById(formalizacao.getCertidaoId())
                .orElseThrow(() -> new IllegalArgumentException("certidao_da_formalizacao_nao_encontrada"));
        DiligenciaOperadorEncerramento encerramento = formalizacao.getEncerramentoId() != null
                ? encerramentoRepository.findById(formalizacao.getEncerramentoId()).orElse(null)
                : null;
        validateCrossReference(canal, normalizedReference, formalizacao, certidao, encerramento);
        List<DiligenciaOperadorCertidaoDocumento> documentosReferenciados = certidaoDocumentoRepository.findByCertidaoIdOrderByCreatedAtDesc(certidao.getId());
        DocumentoProcessual minuta = formalizacao.getMinutaDocumentoId() != null
                ? documentoRepository.findById(formalizacao.getMinutaDocumentoId()).orElse(null)
                : null;
        String idempotencyKey = resolveIdempotencyKey(actor, canal, normalizedReference, formalizacao, request, registrarMovimentacao, gerarPacotePdf);
        DiligenciaOperadorJuntadaProcessual replay = juntadaRepository
                .findFirstByOperatorUserIdAndCanalAndDiligenceReferenceAndIdempotencyKey(actor.getId(), canal, normalizedReference, idempotencyKey)
                .orElse(null);
        if (replay != null) {
            return toResponse(actor, replay);
        }
        boolean exportarMalha = request != null && Boolean.TRUE.equals(request.exportarMalhaExterna());
        String externalSystemCode = normalize(request != null ? request.externalSystemCode() : null, 40);
        String bundleReference = resolveBundleReference(canal, processo, formalizacao, externalSystemCode, exportarMalha);
        DocumentoProcessual pacote = gerarPacotePdf
                ? resolveOrCreatePacket(actor, canal, normalizedReference, processo, formalizacao, certidao, encerramento, documentosReferenciados, minuta, request, externalSystemCode, bundleReference)
                : null;
        MovimentacaoProcessual movimentacao = registrarMovimentacao
                ? Objects.requireNonNull(createMovement(actor, canal, normalizedReference, processo, formalizacao, certidao, documentosReferenciados, pacote, externalSystemCode, bundleReference), "movimentacao")
                : null;
        ProcessEventEnvelope movementEvent = registrarMovimentacao
                ? processEventStore.append(processo.getId(), ProcessEventType.MOVEMENT_RECORDED, new AutomaticFilingMovementPayload(
                        UUID.randomUUID(),
                        processo.getId(),
                        processo.getNumeroProcesso(),
                        canal.name(),
                        normalizedReference,
                        formalizacao.getId(),
                        formalizacao.getEncerramentoId(),
                        formalizacao.getCertidaoId(),
                        formalizacao.getWorkItemId(),
                        movimentacao.getId(),
                        pacote != null ? pacote.getId() : null,
                        pacote != null ? pacote.getSha256() : null,
                        formalizacao.getEvidenceChaveCustodia(),
                        formalizacao.getEvidenceIntegrityOk(),
                        documentosReferenciados.stream().map(link -> link.getDocumentoId() != null ? link.getDocumentoId().toString() : null).filter(Objects::nonNull).toList(),
                        externalSystemCode,
                        bundleReference,
                        actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : actor.getPerfil(),
                        Instant.now()
                ))
                : null;
        ProcessEventEnvelope packetEvent = pacote != null
                ? processEventStore.append(processo.getId(), ProcessEventType.DOCUMENTS_BULK_ADDED, new AutomaticFilingPacketPayload(
                        UUID.randomUUID(),
                        processo.getId(),
                        formalizacao.getId(),
                        pacote.getId(),
                        pacote.getSha256(),
                        pacote.getTamanhoBytes() != null ? pacote.getTamanhoBytes() : 0L,
                        formalizacao.getMinutaDocumentoId(),
                        documentosReferenciados.stream().map(link -> new PacketDocumentRef(link.getDocumentoId() != null ? link.getDocumentoId().toString() : null, link.getDocumentoTitulo(), link.getDocumentoSha256())).toList(),
                        formalizacao.getEvidenceChaveCustodia(),
                        externalSystemCode,
                        bundleReference,
                        Instant.now()
                ))
                : null;
        String bundleDigest = bundleDigest(actor, canal, normalizedReference, formalizacao, documentosReferenciados, pacote, externalSystemCode, bundleReference, idempotencyKey);
        String bundleSignature = bundleSignature(bundleDigest, bundleReference);
        DiligenciaOperadorJuntadaProcessual entity = DiligenciaOperadorJuntadaProcessual.builder()
                .operatorUserId(actor.getId())
                .operatorTipoUsuario(actor.getTipoUsuario())
                .canal(canal)
                .diligenceReference(normalizedReference)
                .workItemId(formalizacao.getWorkItemId())
                .processoId(processo.getId())
                .processoNumero(processo.getNumeroProcesso())
                .formalizacaoId(formalizacao.getId())
                .encerramentoId(formalizacao.getEncerramentoId())
                .certidaoId(formalizacao.getCertidaoId())
                .movimentacaoId(movimentacao != null ? movimentacao.getId() : null)
                .movimentacaoEventSeq(movementEvent != null ? movementEvent.getSeq() : null)
                .pacoteDocumentoId(pacote != null ? pacote.getId() : null)
                .pacoteEventSeq(packetEvent != null ? packetEvent.getSeq() : null)
                .minutaDocumentoId(formalizacao.getMinutaDocumentoId())
                .pacoteTitulo(pacote != null ? pacote.getTitulo() : null)
                .pacoteSha256(pacote != null ? pacote.getSha256() : null)
                .certidaoDigestSha256(formalizacao.getCertidaoDigestSha256())
                .formalizationDigestSha256(formalizacao.getFormalizationDigestSha256())
                .evidenceChaveCustodia(formalizacao.getEvidenceChaveCustodia())
                .evidenceIntegrityOk(formalizacao.getEvidenceIntegrityOk())
                .documentosReferenciados(documentosReferenciados.size())
                .exportarMalhaExterna(exportarMalha)
                .externalSystemCode(externalSystemCode)
                .bundleReference(bundleReference)
                .bundleDigestSha256(bundleDigest)
                .bundleSignatureHmacSha256(bundleSignature)
                .idempotencyKey(idempotencyKey)
                .requestId(RequestContext.getRequestId().orElse(null))
                .build();
        return toResponse(actor, juntadaRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<DiligenceAutomaticFilingResponse> history(TelemetriaOperacionalCanal canal,
                                                          String diligenceReference,
                                                          int limit) {
        if (canal == null) {
            throw new IllegalArgumentException("canal_obrigatorio");
        }
        if (diligenceReference == null || diligenceReference.isBlank()) {
            throw new IllegalArgumentException("diligencia_referencia_obrigatoria");
        }
        Usuario actor = currentUserService.getRequired();
        return juntadaRepository.findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(actor.getId(), canal, diligenceReference.trim()).stream()
                .limit(Math.max(1, Math.min(limit, 20)))
                .map(item -> toResponse(actor, item))
                .toList();
    }

    private DiligenciaOperadorFormalizacaoProcessual resolveFormalizacao(Usuario actor,
                                                                         TelemetriaOperacionalCanal canal,
                                                                         String diligenceReference,
                                                                         DiligenceAutomaticFilingRequest request) {
        if (request != null && request.formalizacaoId() != null) {
            return formalizacaoRepository.findById(request.formalizacaoId())
                    .orElseThrow(() -> new IllegalArgumentException("formalizacao_processual_nao_encontrada"));
        }
        return formalizacaoRepository.findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(actor.getId(), canal, diligenceReference)
                .orElseThrow(() -> new IllegalArgumentException("formalizacao_processual_obrigatoria"));
    }

    private void validateCrossReference(TelemetriaOperacionalCanal canal,
                                        String diligenceReference,
                                        DiligenciaOperadorFormalizacaoProcessual formalizacao,
                                        DiligenciaOperadorCertidao certidao,
                                        DiligenciaOperadorEncerramento encerramento) {
        if (formalizacao.getCanal() != canal || certidao.getCanal() != canal || (encerramento != null && encerramento.getCanal() != canal)) {
            throw new IllegalArgumentException("canal_incompativel_com_juntada");
        }
        if (!Objects.equals(formalizacao.getDiligenceReference(), diligenceReference) || !Objects.equals(certidao.getDiligenceReference(), diligenceReference)
                || (encerramento != null && !Objects.equals(encerramento.getDiligenceReference(), diligenceReference))) {
            throw new IllegalArgumentException("diligencia_incompativel_com_juntada");
        }
        if (!Objects.equals(formalizacao.getCertidaoId(), certidao.getId())) {
            throw new IllegalArgumentException("formalizacao_e_certidao_incompativeis");
        }
        if (encerramento != null && !Objects.equals(formalizacao.getEncerramentoId(), encerramento.getId())) {
            throw new IllegalArgumentException("formalizacao_e_encerramento_incompativeis");
        }
    }

    private DocumentoProcessual resolveOrCreatePacket(Usuario actor,
                                                      TelemetriaOperacionalCanal canal,
                                                      String diligenceReference,
                                                      Processo processo,
                                                      DiligenciaOperadorFormalizacaoProcessual formalizacao,
                                                      DiligenciaOperadorCertidao certidao,
                                                      DiligenciaOperadorEncerramento encerramento,
                                                      List<DiligenciaOperadorCertidaoDocumento> documentosReferenciados,
                                                      DocumentoProcessual minuta,
                                                      DiligenceAutomaticFilingRequest request,
                                                      String externalSystemCode,
                                                      String bundleReference) {
        var rendered = filingPdfService.render(actor, canal, diligenceReference, processo, formalizacao, certidao, encerramento, documentosReferenciados, minuta,
                request != null ? request.pacoteTitulo() : null,
                request != null ? request.complementoNarrativo() : null,
                externalSystemCode,
                bundleReference);
        String sha256 = sha256(rendered.pdf());
        DocumentoProcessual existing = documentoRepository.findFirstByProcesso_IdAndSha256(processo.getId(), sha256).orElse(null);
        if (existing != null) {
            return existing;
        }
        Instant now = Instant.now();
        DocumentoProcessual entity = DocumentoProcessual.builder()
                .processo(processo)
                .nomeOriginal(safeFileName(rendered.titulo()) + ".pdf")
                .titulo(rendered.titulo())
                .contentType("application/pdf")
                .tamanhoBytes((long) rendered.pdf().length)
                .sha256(sha256)
                .sha384(sha384(rendered.pdf()))
                .storageBackend("INLINE_DB")
                .storageUri("db://inline/automatic-filing-packet")
                .externalizedAt(null)
                .pdf(rendered.pdf())
                .nivelSigilo(processo.getNivelSigilo() != null ? processo.getNivelSigilo() : NivelSigilo.PUBLICO)
                .categoria(resolveCategoria(processo))
                .origemSistema("PJB_JUNTADA_AUTOMATICA")
                .criadoPor(actor.getId())
                .criadoEm(LocalDateTime.ofInstant(now, ZoneOffset.UTC))
                .build();
        return documentoRepository.save(entity);
    }

    private MovimentacaoProcessual createMovement(Usuario actor,
                                                  TelemetriaOperacionalCanal canal,
                                                  String diligenceReference,
                                                  Processo processo,
                                                  DiligenciaOperadorFormalizacaoProcessual formalizacao,
                                                  DiligenciaOperadorCertidao certidao,
                                                  List<DiligenciaOperadorCertidaoDocumento> documentosReferenciados,
                                                  DocumentoProcessual pacote,
                                                  String externalSystemCode,
                                                  String bundleReference) {
        String headline = canal == TelemetriaOperacionalCanal.OFICIAL_JUSTICA
                ? "Juntada automática de certidão operacional judicial"
                : "Juntada automática de certidão operacional investigativa";
        String descricao = String.join("\n",
                headline,
                "diligencia_referencia=" + diligenceReference,
                "formalizacao_id=" + nv(formalizacao.getId()),
                "certidao_id=" + nv(certidao.getId()),
                "work_item_id=" + nv(formalizacao.getWorkItemId()),
                "pacote_documento_id=" + nv(pacote != null ? pacote.getId() : null),
                "documentos_referenciados=" + documentosReferenciados.size(),
                "evidence_chave_custodia=" + nv(formalizacao.getEvidenceChaveCustodia()),
                "evidence_integrity_ok=" + nv(formalizacao.getEvidenceIntegrityOk()),
                "external_system_code=" + nv(externalSystemCode),
                "bundle_reference=" + nv(bundleReference));
        MovimentacaoProcessual entity = MovimentacaoProcessual.builder()
                .processo(processo)
                .faseDe(processo.getFaseAtual())
                .fasePara(processo.getFaseAtual())
                .descricao(descricao)
                .ator(actor)
                .build();
        MovimentacaoProcessual saved = movimentacaoRepository.save(entity);
        processo.setDataUltimaMovimentacao(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
        processoRepository.save(processo);
        return saved;
    }

    private String resolveIdempotencyKey(Usuario actor,
                                         TelemetriaOperacionalCanal canal,
                                         String diligenceReference,
                                         DiligenciaOperadorFormalizacaoProcessual formalizacao,
                                         DiligenceAutomaticFilingRequest request,
                                         boolean registrarMovimentacao,
                                         boolean gerarPacotePdf) {
        if (request != null && request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            return request.idempotencyKey().trim();
        }
        return sha256(String.join("|",
                nv(actor.getId()),
                canal.name(),
                diligenceReference,
                nv(formalizacao.getId()),
                Boolean.toString(registrarMovimentacao),
                Boolean.toString(gerarPacotePdf),
                nv(request != null ? request.exportarMalhaExterna() : null),
                nv(request != null ? request.externalSystemCode() : null),
                nv(request != null ? request.pacoteTitulo() : null),
                nv(request != null ? request.complementoNarrativo() : null)
        ));
    }

    private String resolveBundleReference(TelemetriaOperacionalCanal canal,
                                          Processo processo,
                                          DiligenciaOperadorFormalizacaoProcessual formalizacao,
                                          String externalSystemCode,
                                          boolean exportarMalha) {
        String system = externalSystemCode != null ? externalSystemCode : (exportarMalha ? "MALHA_EXTERNA" : "PJB_LOCAL");
        return String.join(":",
                canal.name(),
                system,
                String.valueOf(processo.getId()),
                String.valueOf(formalizacao.getId()),
                String.valueOf(formalizacao.getCertidaoId()));
    }

    private String bundleDigest(Usuario actor,
                                TelemetriaOperacionalCanal canal,
                                String diligenceReference,
                                DiligenciaOperadorFormalizacaoProcessual formalizacao,
                                List<DiligenciaOperadorCertidaoDocumento> documentosReferenciados,
                                DocumentoProcessual pacote,
                                String externalSystemCode,
                                String bundleReference,
                                String idempotencyKey) {
        String docs = documentosReferenciados.stream()
                .map(link -> nv(link.getDocumentoId()) + ":" + nv(link.getDocumentoSha256()))
                .sorted()
                .reduce((a, b) -> a + "|" + b)
                .orElse("-");
        String payload = String.join("|",
                nv(actor.getId()),
                canal.name(),
                diligenceReference,
                nv(formalizacao.getId()),
                nv(formalizacao.getFormalizationDigestSha256()),
                nv(formalizacao.getCertidaoDigestSha256()),
                nv(pacote != null ? pacote.getId() : null),
                nv(pacote != null ? pacote.getSha256() : null),
                Integer.toString(documentosReferenciados.size()),
                docs,
                nv(formalizacao.getEvidenceChaveCustodia()),
                nv(formalizacao.getEvidenceIntegrityOk()),
                nv(externalSystemCode),
                nv(bundleReference),
                idempotencyKey);
        return sha256(payload);
    }

    private String bundleSignature(String bundleDigest,
                                   String bundleReference) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keyMaterialService.getCustodyMeshSigningKey());
            return HEX.formatHex(mac.doFinal((bundleDigest + "|" + nv(bundleReference)).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("diligence_automatic_filing_signature_unavailable", ex);
        }
    }

    private DocumentoCategoria resolveCategoria(Processo processo) {
        NivelSigilo sigilo = processo.getNivelSigilo() != null ? processo.getNivelSigilo() : NivelSigilo.PUBLICO;
        return sigilo.getNivel() >= NivelSigilo.SIGILO_N2.getNivel() ? DocumentoCategoria.PESSOAL : DocumentoCategoria.PUBLICO;
    }

    private String safeFileName(String value) {
        String normalized = value == null || value.isBlank() ? "juntada_automatica" : value.trim();
        normalized = normalized.replaceAll("[^\\p{L}\\p{N}._ -]", " ").replaceAll("\\s+", " ").trim();
        normalized = normalized.isBlank() ? "juntada_automatica" : normalized;
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120);
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HEX.formatHex(digest.digest(bytes));
        } catch (Exception ex) {
            throw new IllegalStateException("sha256_unavailable", ex);
        }
    }

    private String sha256(String value) {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sha384(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-384");
            return HEX.formatHex(digest.digest(bytes));
        } catch (Exception ex) {
            throw new IllegalStateException("sha384_unavailable", ex);
        }
    }

    private String normalize(String value,
                             int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }

    private String nv(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private DiligenceAutomaticFilingResponse toResponse(Usuario actor,
                                                        DiligenciaOperadorJuntadaProcessual entity) {
        QualifiedDocumentSignatureEnvelopeService.SignedContent signedContent = qualifiedDocumentSignatureEnvelopeService.signFreeContent(
                null,
                actor,
                entity.getBundleReference() != null ? entity.getBundleReference() : "JUNTADA_AUTOMATICA",
                filingCanonicalText(entity),
                resolveSigningRole(entity.getCanal(), actor),
                resolveSigningPolicy(entity.getCanal(), "JUNTADA_PROCESSUAL"),
                true,
                List.of(
                        "juntada_automatica_assinada",
                        "assinatura_transversal_completa",
                        entity.getCanal().name().toLowerCase(Locale.ROOT),
                        "bundle_processual"
                )
        );
        return new DiligenceAutomaticFilingResponse(
                entity.getId(),
                actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : actor.getPerfil(),
                entity.getCanal().name(),
                entity.getDiligenceReference(),
                entity.getWorkItemId(),
                entity.getProcessoId(),
                entity.getProcessoNumero(),
                entity.getFormalizacaoId(),
                entity.getEncerramentoId(),
                entity.getCertidaoId(),
                entity.getMinutaDocumentoId(),
                entity.getPacoteDocumentoId(),
                entity.getMovimentacaoId(),
                entity.getMovimentacaoEventSeq(),
                entity.getPacoteEventSeq(),
                entity.getEvidenceChaveCustodia(),
                entity.getEvidenceIntegrityOk(),
                entity.getDocumentosReferenciados(),
                entity.getExternalSystemCode(),
                entity.getBundleReference(),
                entity.getBundleDigestSha256(),
                entity.getBundleSignatureHmacSha256(),
                entity.getIdempotencyKey(),
                entity.getCreatedAt(),
                signedContent.assinaturaQualificada(),
                signedContent.validacaoSoberana()
        );
    }

    private String filingCanonicalText(DiligenciaOperadorJuntadaProcessual entity) {
        return String.join("\n",
                "juntada_id=" + nv(entity.getId()),
                "canal=" + nv(entity.getCanal()),
                "diligencia_referencia=" + nv(entity.getDiligenceReference()),
                "processo_id=" + nv(entity.getProcessoId()),
                "processo_numero=" + nv(entity.getProcessoNumero()),
                "formalizacao_id=" + nv(entity.getFormalizacaoId()),
                "encerramento_id=" + nv(entity.getEncerramentoId()),
                "certidao_id=" + nv(entity.getCertidaoId()),
                "minuta_documento_id=" + nv(entity.getMinutaDocumentoId()),
                "pacote_documento_id=" + nv(entity.getPacoteDocumentoId()),
                "movimentacao_id=" + nv(entity.getMovimentacaoId()),
                "movimentacao_event_seq=" + nv(entity.getMovimentacaoEventSeq()),
                "pacote_event_seq=" + nv(entity.getPacoteEventSeq()),
                "documentos_referenciados=" + nv(entity.getDocumentosReferenciados()),
                "external_system_code=" + nv(entity.getExternalSystemCode()),
                "bundle_reference=" + nv(entity.getBundleReference()),
                "bundle_digest_sha256=" + nv(entity.getBundleDigestSha256()),
                "bundle_signature_hmac_sha256=" + nv(entity.getBundleSignatureHmacSha256()),
                "idempotency_key=" + nv(entity.getIdempotencyKey()),
                "created_at=" + nv(entity.getCreatedAt())
        );
    }

    private String resolveSigningRole(TelemetriaOperacionalCanal canal,
                                      Usuario actor) {
        if (canal == TelemetriaOperacionalCanal.OFICIAL_JUSTICA) {
            return "OFICIAL_JUSTICA";
        }
        return actor != null && actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : "PERFIL_NAO_IDENTIFICADO";
    }

    private String resolveSigningPolicy(TelemetriaOperacionalCanal canal,
                                        String axis) {
        if (canal == TelemetriaOperacionalCanal.OFICIAL_JUSTICA) {
            return "OFICIAL_JUSTICA_" + axis + "_QUALIFICADA_SOBERANA";
        }
        return canal.name() + '_' + axis + "_QUALIFICADA_SOBERANA";
    }

    private record AutomaticFilingMovementPayload(
            UUID eventoId,
            Long processoId,
            String processoNumero,
            String canal,
            String diligenceReference,
            Long formalizacaoId,
            Long encerramentoId,
            Long certidaoId,
            Long workItemId,
            Long movimentacaoId,
            UUID pacoteDocumentoId,
            String pacoteSha256,
            String evidenceChaveCustodia,
            Boolean evidenceIntegrityOk,
            List<String> documentosReferenciados,
            String externalSystemCode,
            String bundleReference,
            String actor,
            Instant timestamp
    ) {
    }

    private record AutomaticFilingPacketPayload(
            UUID eventoId,
            Long processoId,
            Long formalizacaoId,
            UUID pacoteDocumentoId,
            String pacoteSha256,
            long pacoteBytes,
            UUID minutaDocumentoId,
            List<PacketDocumentRef> documentosReferenciados,
            String evidenceChaveCustodia,
            String externalSystemCode,
            String bundleReference,
            Instant timestamp
    ) {
    }

    private record PacketDocumentRef(
            String documentoId,
            String titulo,
            String sha256
    ) {
    }
}
