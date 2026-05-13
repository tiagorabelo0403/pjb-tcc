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
import java.util.Optional;
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
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodyLedgerResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceProcessFormalizationRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceProcessFormalizationResponse;
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
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCertidaoDocumentoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCertidaoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorEncerramentoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorFormalizacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;

@Service
public class DiligenceProcessFormalizationService {

    private static final HexFormat HEX = HexFormat.of();

    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;
    private final KeyMaterialService keyMaterialService;
    private final DiligenciaOperadorEncerramentoRepository encerramentoRepository;
    private final DiligenciaOperadorCertidaoRepository certidaoRepository;
    private final DiligenciaOperadorCertidaoDocumentoRepository certidaoDocumentoRepository;
    private final DiligenciaOperadorFormalizacaoProcessualRepository formalizacaoRepository;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final MovimentacaoProcessualRepository movimentacaoRepository;
    private final DocumentoProcessualRepository documentoRepository;
    private final ProcessEventStore processEventStore;
    private final DigitalCustodyChainLedgerService custodyLedgerService;
    private final DiligenceOperationalMinutePdfService minutePdfService;
    private final QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService;

    public DiligenceProcessFormalizationService(CurrentUserService currentUserService,
                                                PjbAuthorizationService authorizationService,
                                                KeyMaterialService keyMaterialService,
                                                DiligenciaOperadorEncerramentoRepository encerramentoRepository,
                                                DiligenciaOperadorCertidaoRepository certidaoRepository,
                                                DiligenciaOperadorCertidaoDocumentoRepository certidaoDocumentoRepository,
                                                DiligenciaOperadorFormalizacaoProcessualRepository formalizacaoRepository,
                                                ProcessoRepository processoRepository,
                                                WorkItemRepository workItemRepository,
                                                MovimentacaoProcessualRepository movimentacaoRepository,
                                                DocumentoProcessualRepository documentoRepository,
                                                ProcessEventStore processEventStore,
                                                DigitalCustodyChainLedgerService custodyLedgerService,
                                                DiligenceOperationalMinutePdfService minutePdfService,
                                                QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.keyMaterialService = Objects.requireNonNull(keyMaterialService);
        this.encerramentoRepository = Objects.requireNonNull(encerramentoRepository);
        this.certidaoRepository = Objects.requireNonNull(certidaoRepository);
        this.certidaoDocumentoRepository = Objects.requireNonNull(certidaoDocumentoRepository);
        this.formalizacaoRepository = Objects.requireNonNull(formalizacaoRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.movimentacaoRepository = Objects.requireNonNull(movimentacaoRepository);
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.processEventStore = Objects.requireNonNull(processEventStore);
        this.custodyLedgerService = Objects.requireNonNull(custodyLedgerService);
        this.minutePdfService = Objects.requireNonNull(minutePdfService);
        this.qualifiedDocumentSignatureEnvelopeService = Objects.requireNonNull(qualifiedDocumentSignatureEnvelopeService);
    }

    @Transactional
    public DiligenceProcessFormalizationResponse formalize(TelemetriaOperacionalCanal canal,
                                                           String diligenceReference,
                                                           DiligenceProcessFormalizationRequest request) {
        if (canal == null) {
            throw new IllegalArgumentException("canal_obrigatorio");
        }
        if (diligenceReference == null || diligenceReference.isBlank()) {
            throw new IllegalArgumentException("diligencia_referencia_obrigatoria");
        }
        boolean registrarMovimentacao = request == null || request.registrarMovimentacao() == null || request.registrarMovimentacao();
        boolean gerarMinuta = request == null || request.gerarMinuta() == null || request.gerarMinuta();
        if (!registrarMovimentacao && !gerarMinuta) {
            throw new IllegalArgumentException("formalizacao_sem_operacao");
        }
        Usuario actor = currentUserService.getRequired();
        String normalizedReference = diligenceReference.trim();
        DiligenciaOperadorEncerramento encerramento = resolveEncerramento(actor, canal, normalizedReference, request);
        DiligenciaOperadorCertidao certidao = resolveCertidao(actor, canal, normalizedReference, request, encerramento);
        validateCrossReference(canal, normalizedReference, encerramento, certidao);
        Processo processo = processoRepository.findById(certidao.getProcessoId())
                .orElseThrow(() -> new IllegalArgumentException("processo_da_certidao_nao_encontrado"));
        authorizationService.requireReadProcesso(processo);
        WorkItem workItem = certidao.getWorkItemId() != null ? workItemRepository.findById(certidao.getWorkItemId()).orElse(null) : null;
        String idempotencyKey = resolveIdempotencyKey(actor, canal, normalizedReference, encerramento, certidao, request, registrarMovimentacao, gerarMinuta);
        DiligenciaOperadorFormalizacaoProcessual replay = formalizacaoRepository
                .findFirstByOperatorUserIdAndCanalAndDiligenceReferenceAndIdempotencyKey(actor.getId(), canal, normalizedReference, idempotencyKey)
                .orElse(null);
        if (replay != null) {
            return toResponse(actor, replay);
        }
        List<DiligenciaOperadorCertidaoDocumento> documentosReferenciados = certidaoDocumentoRepository.findByCertidaoIdOrderByCreatedAtDesc(certidao.getId());
        String evidenceChaveCustodia = firstNonBlank(request != null ? request.evidenceChaveCustodia() : null, certidao.getEvidenceChaveCustodia());
        Boolean evidenceIntegrityOk = resolveEvidenceIntegrity(evidenceChaveCustodia);
        DocumentoProcessual minuta = gerarMinuta
                ? resolveOrCreateMinute(actor, canal, normalizedReference, processo, certidao, encerramento, documentosReferenciados, request, evidenceChaveCustodia, evidenceIntegrityOk)
                : null;
        MovimentacaoProcessual movimentacao = registrarMovimentacao
                ? Objects.requireNonNull(createMovement(actor, canal, normalizedReference, processo, certidao, encerramento, minuta, documentosReferenciados, evidenceChaveCustodia, evidenceIntegrityOk), "movimentacao")
                : null;
        var movementEvent = registrarMovimentacao
                ? processEventStore.append(processo.getId(), ProcessEventType.MOVEMENT_RECORDED, new OperationalMovementPayload(
                        UUID.randomUUID(),
                        processo.getId(),
                        processo.getNumeroProcesso(),
                        canal.name(),
                        normalizedReference,
                        encerramento.getId(),
                        certidao.getId(),
                        certidao.getCheckpointEventId(),
                        certidao.getWorkItemId(),
                        movimentacao.getId(),
                        minuta != null ? minuta.getId() : null,
                        minuta != null ? minuta.getSha256() : null,
                        certidao.getCertificateDigestSha256(),
                        evidenceChaveCustodia,
                        evidenceIntegrityOk,
                        documentosReferenciados.stream().map(link -> link.getDocumentoId() != null ? link.getDocumentoId().toString() : null).filter(Objects::nonNull).toList(),
                        actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : actor.getPerfil(),
                        Instant.now()
                ))
                : null;
        var documentEvent = minuta != null
                ? processEventStore.append(processo.getId(), ProcessEventType.DOCUMENT_ADDED, new GeneratedMinuteDocumentPayload(
                        minuta.getId(),
                        "db://tb_documento_processual/" + minuta.getId(),
                        minuta.getSha384(),
                        minuta.getTamanhoBytes() != null ? minuta.getTamanhoBytes() : 0L,
                        encerramento.getId(),
                        certidao.getId(),
                        certidao.getWorkItemId(),
                        normalizedReference,
                        canal.name(),
                        Instant.now()
                ))
                : null;
        String formalizationDigest = formalizationDigest(actor, canal, normalizedReference, encerramento, certidao, movimentacao, movementEvent != null ? movementEvent.getSeq() : null, minuta, documentEvent != null ? documentEvent.getSeq() : null, evidenceChaveCustodia, evidenceIntegrityOk, documentosReferenciados.size(), idempotencyKey);
        DiligenciaOperadorFormalizacaoProcessual entity = DiligenciaOperadorFormalizacaoProcessual.builder()
                .operatorUserId(actor.getId())
                .operatorTipoUsuario(actor.getTipoUsuario())
                .canal(canal)
                .diligenceReference(normalizedReference)
                .workItemId(certidao.getWorkItemId())
                .processoId(processo.getId())
                .processoNumero(processo.getNumeroProcesso())
                .encerramentoId(encerramento.getId())
                .certidaoId(certidao.getId())
                .checkpointEventId(certidao.getCheckpointEventId())
                .movimentacaoId(movimentacao != null ? movimentacao.getId() : null)
                .movimentacaoEventSeq(movementEvent != null ? movementEvent.getSeq() : null)
                .minutaDocumentoId(minuta != null ? minuta.getId() : null)
                .minutaEventSeq(documentEvent != null ? documentEvent.getSeq() : null)
                .minutaTitulo(minuta != null ? minuta.getTitulo() : null)
                .minutaSha256(minuta != null ? minuta.getSha256() : null)
                .minutaSha384(minuta != null ? minuta.getSha384() : null)
                .certidaoDigestSha256(certidao.getCertificateDigestSha256())
                .evidenceChaveCustodia(evidenceChaveCustodia)
                .evidenceIntegrityOk(evidenceIntegrityOk)
                .documentosReferenciados(documentosReferenciados.size())
                .idempotencyKey(idempotencyKey)
                .formalizationDigestSha256(formalizationDigest)
                .requestId(RequestContext.getRequestId().orElse(null))
                .build();
        return toResponse(actor, formalizacaoRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<DiligenceProcessFormalizationResponse> history(TelemetriaOperacionalCanal canal,
                                                               String diligenceReference,
                                                               int limit) {
        if (canal == null) {
            throw new IllegalArgumentException("canal_obrigatorio");
        }
        if (diligenceReference == null || diligenceReference.isBlank()) {
            throw new IllegalArgumentException("diligencia_referencia_obrigatoria");
        }
        Usuario actor = currentUserService.getRequired();
        return formalizacaoRepository.findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(actor.getId(), canal, diligenceReference.trim()).stream()
                .limit(Math.max(1, Math.min(limit, 20)))
                .map(item -> toResponse(actor, item))
                .toList();
    }

    private DiligenciaOperadorEncerramento resolveEncerramento(Usuario actor,
                                                               TelemetriaOperacionalCanal canal,
                                                               String diligenceReference,
                                                               DiligenceProcessFormalizationRequest request) {
        if (request != null && request.encerramentoId() != null) {
            return encerramentoRepository.findById(request.encerramentoId())
                    .orElseThrow(() -> new IllegalArgumentException("encerramento_operacional_nao_encontrado"));
        }
        return encerramentoRepository.findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(actor.getId(), canal, diligenceReference)
                .orElseThrow(() -> new IllegalArgumentException("encerramento_operacional_obrigatorio"));
    }

    private DiligenciaOperadorCertidao resolveCertidao(Usuario actor,
                                                       TelemetriaOperacionalCanal canal,
                                                       String diligenceReference,
                                                       DiligenceProcessFormalizationRequest request,
                                                       DiligenciaOperadorEncerramento encerramento) {
        Long certidaoId = request != null && request.certidaoId() != null ? request.certidaoId() : encerramento.getCertidaoId();
        if (certidaoId != null) {
            return certidaoRepository.findById(certidaoId)
                    .orElseThrow(() -> new IllegalArgumentException("certidao_nao_encontrada"));
        }
        return certidaoRepository.findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(actor.getId(), canal, diligenceReference)
                .orElseThrow(() -> new IllegalArgumentException("certidao_operacional_obrigatoria"));
    }

    private void validateCrossReference(TelemetriaOperacionalCanal canal,
                                        String diligenceReference,
                                        DiligenciaOperadorEncerramento encerramento,
                                        DiligenciaOperadorCertidao certidao) {
        if (encerramento.getCanal() != canal || certidao.getCanal() != canal) {
            throw new IllegalArgumentException("canal_incompativel_com_formalizacao");
        }
        if (!Objects.equals(encerramento.getDiligenceReference(), diligenceReference) || !Objects.equals(certidao.getDiligenceReference(), diligenceReference)) {
            throw new IllegalArgumentException("diligencia_incompativel_com_formalizacao");
        }
        if (!Objects.equals(encerramento.getCertidaoId(), certidao.getId())) {
            throw new IllegalArgumentException("encerramento_e_certidao_incompativeis");
        }
        if (!Objects.equals(encerramento.getProcessoId(), certidao.getProcessoId())) {
            throw new IllegalArgumentException("certidao_e_encerramento_sem_mesmo_processo");
        }
    }

    private Boolean resolveEvidenceIntegrity(String evidenceChaveCustodia) {
        if (evidenceChaveCustodia == null || evidenceChaveCustodia.isBlank()) {
            return null;
        }
        Optional<ChainOfCustodyLedgerResponse> ledger = custodyLedgerService.findLedger(evidenceChaveCustodia.trim());
        return ledger.map(ChainOfCustodyLedgerResponse::integrityOk).orElse(Boolean.FALSE);
    }

    private DocumentoProcessual resolveOrCreateMinute(Usuario actor,
                                                      TelemetriaOperacionalCanal canal,
                                                      String diligenceReference,
                                                      Processo processo,
                                                      DiligenciaOperadorCertidao certidao,
                                                      DiligenciaOperadorEncerramento encerramento,
                                                      List<DiligenciaOperadorCertidaoDocumento> documentosReferenciados,
                                                      DiligenceProcessFormalizationRequest request,
                                                      String evidenceChaveCustodia,
                                                      Boolean evidenceIntegrityOk) {
        var rendered = minutePdfService.render(actor, canal, diligenceReference, processo, certidao, encerramento, documentosReferenciados, request != null ? request.minutaTitulo() : null, request != null ? request.complementoNarrativo() : null, evidenceChaveCustodia, evidenceIntegrityOk);
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
                .storageUri("db://inline/operational-minute")
                .externalizedAt(null)
                .pdf(rendered.pdf())
                .nivelSigilo(processo.getNivelSigilo() != null ? processo.getNivelSigilo() : NivelSigilo.PUBLICO)
                .categoria(resolveCategoria(processo))
                .origemSistema("PJB_FORMALIZACAO_OPERACIONAL")
                .criadoPor(actor.getId())
                .criadoEm(LocalDateTime.ofInstant(now, ZoneOffset.UTC))
                .build();
        return documentoRepository.save(entity);
    }

    private MovimentacaoProcessual createMovement(Usuario actor,
                                                  TelemetriaOperacionalCanal canal,
                                                  String diligenceReference,
                                                  Processo processo,
                                                  DiligenciaOperadorCertidao certidao,
                                                  DiligenciaOperadorEncerramento encerramento,
                                                  DocumentoProcessual minuta,
                                                  List<DiligenciaOperadorCertidaoDocumento> documentosReferenciados,
                                                  String evidenceChaveCustodia,
                                                  Boolean evidenceIntegrityOk) {
        String descricao = movementDescription(canal, diligenceReference, certidao, encerramento, minuta, documentosReferenciados, evidenceChaveCustodia, evidenceIntegrityOk);
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

    private String movementDescription(TelemetriaOperacionalCanal canal,
                                       String diligenceReference,
                                       DiligenciaOperadorCertidao certidao,
                                       DiligenciaOperadorEncerramento encerramento,
                                       DocumentoProcessual minuta,
                                       List<DiligenciaOperadorCertidaoDocumento> documentosReferenciados,
                                       String evidenceChaveCustodia,
                                       Boolean evidenceIntegrityOk) {
        String headline = canal == TelemetriaOperacionalCanal.OFICIAL_JUSTICA
                ? "Formalização processual de cumprimento operacional georreferenciado"
                : "Formalização processual de diligência investigativa auditável";
        return String.join("\n",
                headline,
                "diligencia_referencia=" + diligenceReference,
                "encerramento_id=" + encerramento.getId(),
                "encerramento_resultado=" + nv(encerramento.getOutcome()),
                "certidao_id=" + certidao.getId(),
                "certidao_digest_sha256=" + nv(certidao.getCertificateDigestSha256()),
                "checkpoint_event_id=" + nv(certidao.getCheckpointEventId()),
                "work_item_id=" + nv(certidao.getWorkItemId()),
                "minuta_documento_id=" + nv(minuta != null ? minuta.getId() : null),
                "documentos_referenciados=" + documentosReferenciados.size(),
                "evidence_chave_custodia=" + nv(evidenceChaveCustodia),
                "evidence_integrity_ok=" + nv(evidenceIntegrityOk));
    }

    private String resolveIdempotencyKey(Usuario actor,
                                         TelemetriaOperacionalCanal canal,
                                         String diligenceReference,
                                         DiligenciaOperadorEncerramento encerramento,
                                         DiligenciaOperadorCertidao certidao,
                                         DiligenceProcessFormalizationRequest request,
                                         boolean registrarMovimentacao,
                                         boolean gerarMinuta) {
        if (request != null && request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            return request.idempotencyKey().trim();
        }
        return sha256(String.join("|",
                nv(actor.getId()),
                canal.name(),
                diligenceReference,
                nv(encerramento.getId()),
                nv(certidao.getId()),
                Boolean.toString(registrarMovimentacao),
                Boolean.toString(gerarMinuta),
                nv(request != null ? request.minutaTitulo() : null),
                nv(request != null ? request.complementoNarrativo() : null),
                nv(request != null ? request.evidenceChaveCustodia() : null)
        ));
    }

    private String formalizationDigest(Usuario actor,
                                       TelemetriaOperacionalCanal canal,
                                       String diligenceReference,
                                       DiligenciaOperadorEncerramento encerramento,
                                       DiligenciaOperadorCertidao certidao,
                                       MovimentacaoProcessual movimentacao,
                                       Long movementSeq,
                                       DocumentoProcessual minuta,
                                       Long minuteSeq,
                                       String evidenceChaveCustodia,
                                       Boolean evidenceIntegrityOk,
                                       int documentosReferenciados,
                                       String idempotencyKey) {
        String payload = String.join("|",
                nv(actor.getId()),
                canal.name(),
                diligenceReference,
                nv(encerramento.getId()),
                nv(certidao.getId()),
                nv(certidao.getCertificateDigestSha256()),
                nv(movimentacao != null ? movimentacao.getId() : null),
                nv(movementSeq),
                nv(minuta != null ? minuta.getId() : null),
                nv(minuteSeq),
                nv(minuta != null ? minuta.getSha256() : null),
                nv(evidenceChaveCustodia),
                nv(evidenceIntegrityOk),
                Integer.toString(documentosReferenciados),
                idempotencyKey);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keyMaterialService.getOperationalCertificateSigningKey());
            return HEX.formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("diligence_process_formalization_signature_unavailable", ex);
        }
    }

    private DocumentoCategoria resolveCategoria(Processo processo) {
        NivelSigilo sigilo = processo.getNivelSigilo() != null ? processo.getNivelSigilo() : NivelSigilo.PUBLICO;
        return sigilo.getNivel() >= NivelSigilo.SIGILO_N2.getNivel() ? DocumentoCategoria.PESSOAL : DocumentoCategoria.PUBLICO;
    }

    private String safeFileName(String value) {
        String normalized = value == null || value.isBlank() ? "formalizacao_operacional" : value.trim();
        normalized = normalized.replaceAll("[^\\p{L}\\p{N}._ -]", " ").replaceAll("\\s+", " ").trim();
        normalized = normalized.isBlank() ? "formalizacao_operacional" : normalized;
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

    private String firstNonBlank(String first,
                                 String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }

    private String nv(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private DiligenceProcessFormalizationResponse toResponse(Usuario actor,
                                                             DiligenciaOperadorFormalizacaoProcessual entity) {
        QualifiedDocumentSignatureEnvelopeService.SignedContent signedContent = qualifiedDocumentSignatureEnvelopeService.signFreeContent(
                null,
                actor,
                entity.getMinutaTitulo() != null ? entity.getMinutaTitulo() : "FORMALIZACAO_PROCESSUAL",
                formalizationCanonicalText(entity),
                resolveSigningRole(entity.getCanal(), actor),
                resolveSigningPolicy(entity.getCanal(), "FORMALIZACAO_PROCESSUAL"),
                true,
                List.of(
                        "formalizacao_processual_assinada",
                        "assinatura_transversal_completa",
                        entity.getCanal().name().toLowerCase(Locale.ROOT),
                        "documento_cumprimento"
                )
        );
        return new DiligenceProcessFormalizationResponse(
                entity.getId(),
                actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : actor.getPerfil(),
                entity.getCanal().name(),
                entity.getDiligenceReference(),
                entity.getWorkItemId(),
                entity.getProcessoId(),
                entity.getProcessoNumero(),
                entity.getEncerramentoId(),
                entity.getCertidaoId(),
                entity.getCheckpointEventId(),
                entity.getMovimentacaoId(),
                entity.getMovimentacaoEventSeq(),
                entity.getMinutaDocumentoId(),
                entity.getMinutaEventSeq(),
                entity.getMinutaTitulo(),
                entity.getMinutaSha256(),
                entity.getCertidaoDigestSha256(),
                entity.getEvidenceChaveCustodia(),
                entity.getEvidenceIntegrityOk(),
                entity.getDocumentosReferenciados(),
                entity.getIdempotencyKey(),
                entity.getFormalizationDigestSha256(),
                entity.getCreatedAt(),
                signedContent.assinaturaQualificada(),
                signedContent.validacaoSoberana()
        );
    }

    private String formalizationCanonicalText(DiligenciaOperadorFormalizacaoProcessual entity) {
        return String.join("\n",
                "formalizacao_id=" + nv(entity.getId()),
                "canal=" + nv(entity.getCanal()),
                "diligencia_referencia=" + nv(entity.getDiligenceReference()),
                "processo_id=" + nv(entity.getProcessoId()),
                "processo_numero=" + nv(entity.getProcessoNumero()),
                "encerramento_id=" + nv(entity.getEncerramentoId()),
                "certidao_id=" + nv(entity.getCertidaoId()),
                "checkpoint_event_id=" + nv(entity.getCheckpointEventId()),
                "movimentacao_id=" + nv(entity.getMovimentacaoId()),
                "movimentacao_event_seq=" + nv(entity.getMovimentacaoEventSeq()),
                "minuta_documento_id=" + nv(entity.getMinutaDocumentoId()),
                "minuta_event_seq=" + nv(entity.getMinutaEventSeq()),
                "minuta_titulo=" + nv(entity.getMinutaTitulo()),
                "minuta_sha256=" + nv(entity.getMinutaSha256()),
                "certidao_digest_sha256=" + nv(entity.getCertidaoDigestSha256()),
                "evidence_chave_custodia=" + nv(entity.getEvidenceChaveCustodia()),
                "evidence_integrity_ok=" + nv(entity.getEvidenceIntegrityOk()),
                "documentos_referenciados=" + nv(entity.getDocumentosReferenciados()),
                "idempotency_key=" + nv(entity.getIdempotencyKey()),
                "formalization_digest_sha256=" + nv(entity.getFormalizationDigestSha256()),
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

    private record OperationalMovementPayload(
            UUID eventoId,
            Long processoId,
            String processoNumero,
            String canal,
            String diligenceReference,
            Long encerramentoId,
            Long certidaoId,
            Long checkpointEventId,
            Long workItemId,
            Long movimentacaoId,
            UUID minutaDocumentoId,
            String minutaSha256,
            String certidaoDigestSha256,
            String evidenceChaveCustodia,
            Boolean evidenceIntegrityOk,
            List<String> documentosReferenciados,
            String actor,
            Instant timestamp
    ) {
    }

    private record GeneratedMinuteDocumentPayload(
            UUID docId,
            String storageUri,
            String hashSha384,
            long tamanhoBytes,
            Long encerramentoId,
            Long certidaoId,
            Long workItemId,
            String diligenceReference,
            String canal,
            Instant timestamp
    ) {
    }
}
