package com.tcc.pjb.backend.modules.advocacia.office.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.configs.EquipeContexto;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessAccessView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.upload.UploadBatchFinalizeResponse;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeProcessOperation;
import com.tcc.pjb.backend.modules.advocacia.office.entity.OfficeSignatureQueueItem;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeDelegationMode;
import com.tcc.pjb.backend.modules.advocacia.office.repository.AdvOfficeProcessOperationRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.OfficeSignatureQueueRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.upload.BulkUploadService;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope;
import com.tcc.pjb.backend.service.processual.recursal.RecursalPeticionamentoFacadeService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficeGovernedProcessOperationService {

    public static final String RESOURCE_TYPE = "ADV_PROCESS_OPERATION";
    private static final String STATUS_CREATED = "CREATED";
    private static final String STATUS_PENDING_SIGNER = "PENDING_SIGNER";
    private static final String STATUS_EXECUTED = "EXECUTED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final CurrentUserService currentUserService;
    private final PerfilDashboardContextFactory contextFactory;
    private final PainelServiceCommons commons;
    private final ProcessoRepository processoRepository;
    private final UsuarioRepository usuarioRepository;
    private final WorkItemRepository workItemRepository;
    private final PjbAuthorizationService authorizationService;
    private final RecursalPeticionamentoFacadeService recursalPeticionamentoFacadeService;
    private final InstitutionalActorRoutingService institutionalActorRoutingService;
    private final OfficeDelegationService officeDelegationService;
    private final OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService;
    private final AdvOfficeProcessOperationRepository processOperationRepository;
    private final OfficeSignatureQueueRepository officeSignatureQueueRepository;
    private final DocumentoProcessualRepository documentoProcessualRepository;
    private final BulkUploadService bulkUploadService;
    private final OfficeDocumentBatchGovernanceService officeDocumentBatchGovernanceService;
    private final ObjectProvider<HttpServletRequest> requestProvider;
    private final ObjectMapper objectMapper;
    private final QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService;
    private final AuditLedgerService auditLedgerService;

    public OfficeGovernedProcessOperationService(CurrentUserService currentUserService,
                                                 PerfilDashboardContextFactory contextFactory,
                                                 PainelServiceCommons commons,
                                                 ProcessoRepository processoRepository,
                                                 UsuarioRepository usuarioRepository,
                                                 WorkItemRepository workItemRepository,
                                                 PjbAuthorizationService authorizationService,
                                                 RecursalPeticionamentoFacadeService recursalPeticionamentoFacadeService,
                                                 InstitutionalActorRoutingService institutionalActorRoutingService,
                                                 OfficeDelegationService officeDelegationService,
                                                 OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService,
                                                 AdvOfficeProcessOperationRepository processOperationRepository,
                                                 OfficeSignatureQueueRepository officeSignatureQueueRepository,
                                                 DocumentoProcessualRepository documentoProcessualRepository,
                                                 BulkUploadService bulkUploadService,
                                                 OfficeDocumentBatchGovernanceService officeDocumentBatchGovernanceService,
                                                 ObjectProvider<HttpServletRequest> requestProvider,
                                                 ObjectMapper objectMapper,
                                                 QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService,
                                                 AuditLedgerService auditLedgerService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.contextFactory = Objects.requireNonNull(contextFactory);
        this.commons = Objects.requireNonNull(commons);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.recursalPeticionamentoFacadeService = Objects.requireNonNull(recursalPeticionamentoFacadeService);
        this.institutionalActorRoutingService = Objects.requireNonNull(institutionalActorRoutingService);
        this.officeDelegationService = Objects.requireNonNull(officeDelegationService);
        this.officeProcessWorkspaceScopeService = Objects.requireNonNull(officeProcessWorkspaceScopeService);
        this.processOperationRepository = Objects.requireNonNull(processOperationRepository);
        this.officeSignatureQueueRepository = Objects.requireNonNull(officeSignatureQueueRepository);
        this.documentoProcessualRepository = Objects.requireNonNull(documentoProcessualRepository);
        this.bulkUploadService = Objects.requireNonNull(bulkUploadService);
        this.officeDocumentBatchGovernanceService = Objects.requireNonNull(officeDocumentBatchGovernanceService);
        this.requestProvider = Objects.requireNonNull(requestProvider);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.qualifiedDocumentSignatureEnvelopeService = Objects.requireNonNull(qualifiedDocumentSignatureEnvelopeService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional
    public Map<String, Object> protocolizarPeticao(Long processoId,
                                                   String tipoPeticao,
                                                   String conteudo,
                                                   String fundamentacao) {
        return submitGovernedOperation(
                processoId,
                OfficeActionType.PETICIONAR,
                new PetitionPayload(tipoPeticao, conteudo, fundamentacao),
                summarize("PETICAO", tipoPeticao, conteudo),
                (operation, signer) -> executePeticao(processoId, tipoPeticao, conteudo, fundamentacao, signer, operation)
        );
    }

    @Transactional
    public Map<String, Object> interporRecurso(Long processoId,
                                               String tipoRecurso,
                                               String razoes,
                                               String fundamentacao,
                                               boolean pedidoEfeitoSuspensivo,
                                               boolean preparoDispensado,
                                               String observacoes) {
        return submitGovernedOperation(
                processoId,
                OfficeActionType.RECORRER,
                new AppealPayload(tipoRecurso, razoes, fundamentacao, pedidoEfeitoSuspensivo, preparoDispensado, observacoes),
                summarize("RECURSO", tipoRecurso, razoes),
                (operation, signer) -> recursalPeticionamentoFacadeService.interporRecurso(processoId, tipoRecurso, razoes, fundamentacao, pedidoEfeitoSuspensivo, preparoDispensado, observacoes)
        );
    }


    @Transactional
    public Map<String, Object> juntarDocumentosPorBatch(Long processoId,
                                                        UUID batchId,
                                                        String titulo,
                                                        DocumentoCategoria categoria,
                                                        NivelSigilo nivelSigilo,
                                                        String origemSistema,
                                                        String expectedBatchFingerprint,
                                                        Integer expectedUploadedCount) {
        return submitGovernedOperation(
                processoId,
                OfficeActionType.JUNTAR_DOCUMENTO,
                new DocumentBatchPayload(batchId.toString(), titulo, categoria == null ? null : categoria.name(), nivelSigilo == null ? null : nivelSigilo.name(), origemSistema, expectedBatchFingerprint, expectedUploadedCount),
                summarize("JUNTADA_DOCUMENTAL", titulo == null ? batchId.toString() : titulo, batchId.toString()),
                (operation, signer) -> executeDocumentBatchLink(processoId, batchId, titulo, categoria, nivelSigilo, origemSistema, expectedBatchFingerprint, expectedUploadedCount)
        );
    }

    @Transactional
    public void approveQueuedOperation(Long operationId, Long queueItemId, Long decidedByUserId, String reason) {
        AdvOfficeProcessOperation operation = processOperationRepository.findWithGraphById(operationId)
                .orElseThrow(() -> new EntityNotFoundException("Operacao processual nao encontrada."));
        if (!STATUS_PENDING_SIGNER.equalsIgnoreCase(operation.getStatus())) {
            return;
        }
        if (queueItemId != null && operation.getQueueItem() != null && !Objects.equals(operation.getQueueItem().getId(), queueItemId)) {
            throw new IllegalStateException("Fila vinculada divergente para a operacao processual.");
        }
        Map<String, Object> result = executeOperation(operation);
        operation.setStatus(STATUS_EXECUTED);
        operation.setExecutedAt(LocalDateTime.now());
        operation.setRejectionReason(null);
        operation.setRejectedAt(null);
        operation.setResultPayloadJson(writeJson(result));
        processOperationRepository.save(operation);
        auditLedgerService.appendSafely("ADV_OFFICE_PROCESS_OPERATION_EXECUTED", RESOURCE_TYPE, String.valueOf(operation.getId()), operation.getPayloadHash(), safeReason(reason));
    }

    @Transactional
    public void rejectQueuedOperation(Long operationId, Long queueItemId, Long decidedByUserId, String reason) {
        AdvOfficeProcessOperation operation = processOperationRepository.findWithGraphById(operationId)
                .orElseThrow(() -> new EntityNotFoundException("Operacao processual nao encontrada."));
        if (queueItemId != null && operation.getQueueItem() != null && !Objects.equals(operation.getQueueItem().getId(), queueItemId)) {
            throw new IllegalStateException("Fila vinculada divergente para a operacao processual.");
        }
        operation.setStatus(STATUS_REJECTED);
        operation.setRejectedAt(LocalDateTime.now());
        operation.setRejectionReason(safeReason(reason));
        processOperationRepository.save(operation);
        auditLedgerService.appendSafely("ADV_OFFICE_PROCESS_OPERATION_REJECTED", RESOURCE_TYPE, String.valueOf(operation.getId()), operation.getPayloadHash(), safeReason(reason));
    }

    private Map<String, Object> submitGovernedOperation(Long processoId,
                                                        OfficeActionType actionType,
                                                        Object payload,
                                                        String summary,
                                                        OperationExecutor executor) {
        Usuario actor = currentUserService.getRequired();
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        PjbFrontendOfficeProcessAccessView access = requireScopedAccess(processoId, actionType);
        String payloadJson = writeJson(payload);
        String payloadHash = Hashes.sha256Hex(actionType.name() + '|' + processoId + '|' + payloadJson);
        AdvOfficeProcessOperation operation = new AdvOfficeProcessOperation();
        operation.setProcesso(processo);
        operation.setExecutor(actor);
        operation.setActionType(actionType);
        operation.setStatus(STATUS_CREATED);
        operation.setPayloadHash(payloadHash);
        operation.setPayloadJson(payloadJson);
        operation.setEquipe(resolveEquipeAtual());
        operation = processOperationRepository.save(operation);

        Long equipeId = operation.getEquipe() == null ? null : operation.getEquipe().getId();
        OfficeDelegationService.Decision decision = equipeId == null
                ? new OfficeDelegationService.Decision(OfficeDelegationMode.SELF, null, actor.getId(), actor.getId(), 0, null)
                : officeDelegationService.decideAndRecord(
                        equipeId,
                        actor.getId(),
                        actionType,
                        RESOURCE_TYPE,
                        String.valueOf(operation.getId()),
                        payloadHash,
                        summary,
                        processo.getRamoDireito() == null ? null : processo.getRamoDireito().name(),
                        access.queueRequired());

        Usuario signer = decision.signerUserId() == null ? actor : usuarioRepository.findById(decision.signerUserId()).orElse(actor);
        operation.setSigner(signer);
        operation.setSignerNameSnapshot(resolveSignerName(signer));
        operation.setSignerRegistrationSnapshot(resolveSignerRegistration(signer));
        if (decision.queueItemId() != null) {
            OfficeSignatureQueueItem queueItem = officeSignatureQueueRepository.findById(decision.queueItemId())
                    .orElseThrow(() -> new EntityNotFoundException("Fila processual nao encontrada."));
            operation.setQueueItem(queueItem);
        }

        if (decision.mode() == OfficeDelegationMode.QUEUE) {
            operation.setStatus(STATUS_PENDING_SIGNER);
            processOperationRepository.save(operation);
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("status", "PENDING_SIGNER");
            out.put("processoId", processoId);
            out.put("operationId", operation.getId());
            out.put("queueItemId", decision.queueItemId());
            out.put("signerUserId", decision.signerUserId());
            out.put("signerNome", resolveSignerName(signer));
            out.put("signerRegistration", resolveSignerRegistration(signer));
            out.put("trustScore", decision.trustScore());
            out.put("actionType", actionType.name());
            out.put("signatureMode", resolveSignatureMode(actor, signer));
            out.put("signatureEnvelopeReady", Boolean.FALSE);
            out.put("warnings", access.warnings());
            auditLedgerService.appendSafely("ADV_OFFICE_PROCESS_OPERATION_PENDING_SIGNER", RESOURCE_TYPE, String.valueOf(operation.getId()), payloadHash, actionType.name());
            return out;
        }

        Map<String, Object> result = executor.execute(operation, signer);
        operation.setStatus(STATUS_EXECUTED);
        operation.setExecutedAt(LocalDateTime.now());
        operation.setResultPayloadJson(writeJson(result));
        processOperationRepository.save(operation);

        LinkedHashMap<String, Object> enriched = new LinkedHashMap<>(result);
        enriched.put("operationId", operation.getId());
        enriched.put("delegationMode", decision.mode().name());
        enriched.put("signerUserId", decision.signerUserId());
        enriched.putIfAbsent("signerNome", resolveSignerName(signer));
        enriched.putIfAbsent("signerRegistration", resolveSignerRegistration(signer));
        enriched.putIfAbsent("signatureMode", resolveSignatureMode(actor, signer));
        return enriched;
    }

    private Map<String, Object> executeOperation(AdvOfficeProcessOperation operation) {
        if (operation.getActionType() == OfficeActionType.PETICIONAR) {
            PetitionPayload payload = readJson(operation.getPayloadJson(), PetitionPayload.class);
            return executePeticao(operation.getProcesso().getId(), payload.tipoPeticao(), payload.conteudo(), payload.fundamentacao(), operation.getSigner(), operation);
        }
        if (operation.getActionType() == OfficeActionType.RECORRER) {
            AppealPayload payload = readJson(operation.getPayloadJson(), AppealPayload.class);
            return recursalPeticionamentoFacadeService.interporRecurso(
                    operation.getProcesso().getId(),
                    payload.tipoRecurso(),
                    payload.razoes(),
                    payload.fundamentacao(),
                    payload.pedidoEfeitoSuspensivo(),
                    payload.preparoDispensado(),
                    payload.observacoes());
        }
        if (operation.getActionType() == OfficeActionType.JUNTAR_DOCUMENTO) {
            DocumentBatchPayload payload = readJson(operation.getPayloadJson(), DocumentBatchPayload.class);
            return executeDocumentBatchLink(
                    operation.getProcesso().getId(),
                    UUID.fromString(payload.batchId()),
                    payload.titulo(),
                    DocumentoCategoria.fromString(payload.categoria()),
                    NivelSigilo.fromString(payload.nivelSigilo()),
                    payload.origemSistema(),
                    payload.expectedBatchFingerprint(),
                    payload.expectedUploadedCount());
        }
        throw new IllegalStateException("Acao processual nao suportada para fila governada: " + operation.getActionType());
    }

    private Map<String, Object> executePeticao(Long processoId,
                                               String tipoPeticao,
                                               String conteudo,
                                               String fundamentacao,
                                               Usuario signer,
                                               AdvOfficeProcessOperation operation) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        Usuario resolvedSigner = signer == null ? usuario : signer;
        authorizationService.requireRole(usuario, "ROLE_ADVOGADO", "ROLE_OAB_PRESIDENTE_SECCIONAL");
        authorizationService.requireReadProcesso(processo);
        SignedDocumentEnvelope signedContent = qualifiedDocumentSignatureEnvelopeService.signGovernedContent(
                processo,
                resolvedSigner,
                petitionTitle(tipoPeticao),
                conteudo,
                resolvePetitionSignerRole(usuario, resolvedSigner),
                resolvePetitionPolicy(usuario, resolvedSigner),
                processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO,
                petitionGovernanceTags(operation, usuario, resolvedSigner, tipoPeticao)
        );
        String dedupKey = UUID.nameUUIDFromBytes(("PETICAO:" + processoId + ':' + tipoPeticao + ':' + stableActorKey(resolvedSigner)).getBytes(StandardCharsets.UTF_8)).toString();
        InstitutionalActorRoutingService.InstitutionalRoute route = institutionalActorRoutingService.secretaryReceipt(processoId, "PETICAO_" + normalizeToken(tipoPeticao));
        WorkItem peticao = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode(dedupKey)
                .type(WorkItemType.PETICAO)
                .titulo(tipoPeticao + " — " + processo.getNumeroProcesso())
                .descricao(signedContent.renderedContent())
                .queueCode(route.queueCode())
                .inboxKey(route.inboxKey())
                .assignedRole(route.assignedRole())
                .status(WorkItemStatus.PENDENTE)
                .prioridade(2)
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .baseLegal(fundamentacao)
                .dueAt(Instant.now().plus(4, ChronoUnit.HOURS))
                .build();
        peticao = workItemRepository.save(peticao);
        if (operation != null) {
            LinkedHashMap<String, Object> signatureSnapshot = new LinkedHashMap<>();
            signatureSnapshot.put("renderedContent", signedContent.renderedContent());
            signatureSnapshot.put("assinaturaQualificada", signedContent.assinaturaQualificada());
            signatureSnapshot.put("validacaoSoberana", signedContent.validacaoSoberana());
            operation.setSignaturePayloadJson(writeJson(signatureSnapshot));
            operation.setSignatureHash(signedContent.contentHash());
            operation.setSignerNameSnapshot(resolveSignerName(resolvedSigner));
            operation.setSignerRegistrationSnapshot(resolveSignerRegistration(resolvedSigner));
        }
        commons.publishUserHistory(usuario, "ADVOGADO", "PETICAO_PROTOCOLIZADA", tipoPeticao + " protocolizada.", processo, processoId);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "PETIÇÃO_PROTOCOLIZADA");
        out.put("tipo", tipoPeticao);
        out.put("processoId", processoId);
        out.put("workItemId", peticao.getId());
        out.put("dedupKey", dedupKey);
        out.put("signerUserId", resolvedSigner.getId());
        out.put("signerNome", resolveSignerName(resolvedSigner));
        out.put("signerRegistration", resolveSignerRegistration(resolvedSigner));
        out.put("signatureMode", resolveSignatureMode(usuario, resolvedSigner));
        out.put("signatureEnvelopeReady", Boolean.TRUE);
        out.put("signedContentHash", signedContent.contentHash());
        out.put("signedContent", signedContent.renderedContent());
        out.put("signatureEnvelope", signedContent.assinaturaQualificada());
        return out;
    }


    private Map<String, Object> executeDocumentBatchLink(Long processoId,
                                                         UUID batchId,
                                                         String titulo,
                                                         DocumentoCategoria categoria,
                                                         NivelSigilo nivelSigilo,
                                                         String origemSistema,
                                                         String expectedBatchFingerprint,
                                                         Integer expectedUploadedCount) {
        OfficeDocumentBatchGovernanceService.DocumentBatchSnapshot snapshot = officeDocumentBatchGovernanceService.snapshot(batchId);
        if (!Objects.equals(snapshot.processoId(), processoId)) {
            throw new IllegalStateException("Lote de upload vinculado a outro processo.");
        }
        if (!"INITIATED".equals(snapshot.status())) {
            throw new IllegalStateException("Lote de upload nao esta apto para juntada governada.");
        }
        if (snapshot.uploadedCount() <= 0) {
            throw new IllegalStateException("Lote de upload sem documentos enviados.");
        }
        if (expectedUploadedCount != null && snapshot.uploadedCount() != expectedUploadedCount.longValue()) {
            throw new IllegalStateException("Lote de upload alterado desde o preview do frontend.");
        }
        if (expectedBatchFingerprint != null && !expectedBatchFingerprint.isBlank() && !Objects.equals(snapshot.fingerprint(), expectedBatchFingerprint)) {
            throw new IllegalStateException("Lote de upload alterado desde o preview do frontend.");
        }
        UploadBatchFinalizeResponse response = bulkUploadService.finalizeBatch(batchId);
        java.util.List<DocumentoProcessual> docs = documentoProcessualRepository.findAllById(response.documentoIds());
        DocumentoCategoria resolvedCategoria = categoria == null ? DocumentoCategoria.PUBLICO : categoria;
        NivelSigilo resolvedNivelSigilo = nivelSigilo == null ? NivelSigilo.PUBLICO : nivelSigilo;
        String resolvedOrigem = origemSistema == null || origemSistema.isBlank() ? "FRONTEND_OFFICE_WORKSPACE" : origemSistema.trim();
        for (DocumentoProcessual doc : docs) {
            if (titulo != null && !titulo.isBlank()) {
                String normalizedTitle = titulo.trim();
                doc.setTitulo(docs.size() == 1 ? normalizedTitle : normalizedTitle + " — " + (doc.getNomeOriginal() == null ? doc.getId() : doc.getNomeOriginal()));
            }
            doc.setCategoria(resolvedCategoria);
            doc.setNivelSigilo(resolvedNivelSigilo);
            doc.setOrigemSistema(resolvedOrigem);
        }
        documentoProcessualRepository.saveAll(docs);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "DOCUMENTOS_JUNTADOS");
        out.put("processoId", processoId);
        out.put("batchId", batchId.toString());
        out.put("linkedCount", response.documentosCriados());
        out.put("linkedDocumentIds", response.documentoIds());
        out.put("batchFingerprint", snapshot.fingerprint());
        return out;
    }

    private PjbFrontendOfficeProcessAccessView requireScopedAccess(Long processoId, OfficeActionType actionType) {
        HttpServletRequest request = requestProvider.getIfAvailable();
        PjbFrontendOfficeProcessAccessView access = officeProcessWorkspaceScopeService.access(processoId, actionType, request);
        if (!access.allowed()) {
            throw new IllegalStateException("Processo fora do escopo operacional do workspace: " + String.join(", ", access.blockers()));
        }
        return access;
    }

    private com.tcc.pjb.backend.model.entity.Equipe resolveEquipeAtual() {
        MembroEquipe membro = EquipeContexto.getMembroDaEquipeAtiva();
        return membro == null ? null : membro.getEquipe();
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar payload da operacao processual.", ex);
        }
    }

    private <T> T readJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao desserializar payload da operacao processual.", ex);
        }
    }

    private String summarize(String prefixo, String subtipo, String conteudo) {
        String base = prefixo + ':' + normalizeToken(subtipo) + ':' + normalizeToken(conteudo);
        return base.length() > 220 ? base.substring(0, 220) : base;
    }

    private String stableActorKey(Usuario usuario) {
        if (usuario == null) {
            return "anon";
        }
        if (usuario.getId() != null) {
            return "id:" + usuario.getId();
        }
        if (usuario.getCpf() != null && !usuario.getCpf().isBlank()) {
            return "cpf:" + usuario.getCpf().replaceAll("[^0-9]", "");
        }
        return "email:" + (usuario.getEmail() == null ? "sem_email" : usuario.getEmail().trim().toLowerCase());
    }

    private String normalizeToken(String value) {
        if (value == null || value.isBlank()) {
            return "NA";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
    }

    private String safeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        String normalized = reason.trim();
        return normalized.length() > 240 ? normalized.substring(0, 240) : normalized;
    }

    private record PetitionPayload(String tipoPeticao, String conteudo, String fundamentacao) {
    }

    private record AppealPayload(String tipoRecurso,
                                 String razoes,
                                 String fundamentacao,
                                 boolean pedidoEfeitoSuspensivo,
                                 boolean preparoDispensado,
                                 String observacoes) {
    }

    private java.util.List<String> petitionGovernanceTags(AdvOfficeProcessOperation operation,
                                                         Usuario executor,
                                                         Usuario signer,
                                                         String tipoPeticao) {
        java.util.ArrayList<String> tags = new java.util.ArrayList<>();
        tags.add("advocacia");
        tags.add("peticionamento");
        tags.add("workspace_escritorio");
        if (tipoPeticao != null && !tipoPeticao.isBlank()) {
            tags.add("peticao_" + normalizeToken(tipoPeticao).replace(' ', '_').toLowerCase(java.util.Locale.ROOT));
        }
        if (operation != null && operation.getEquipe() != null && operation.getEquipe().getId() != null) {
            tags.add("equipe_" + operation.getEquipe().getId());
        }
        if (signer != null && executor != null && !Objects.equals(signer.getId(), executor.getId())) {
            tags.add("assinatura_patrono");
        }
        return java.util.List.copyOf(tags);
    }

    private String petitionTitle(String tipoPeticao) {
        return (tipoPeticao == null || tipoPeticao.isBlank() ? "Petição" : tipoPeticao.trim()) + " — assinatura qualificada";
    }

    private String resolvePetitionSignerRole(Usuario executor, Usuario signer) {
        if (signer != null && executor != null && !Objects.equals(signer.getId(), executor.getId())) {
            return "ADVOGADO_PATRONO";
        }
        return "ADVOGADO";
    }

    private String resolvePetitionPolicy(Usuario executor, Usuario signer) {
        if (signer != null && executor != null && !Objects.equals(signer.getId(), executor.getId())) {
            return "ADVOCACIA_ESCRITORIO_CERTIFICADO_PATRONO";
        }
        return "ADVOCACIA_PETICAO_QUALIFICADA";
    }

    private String resolveSignatureMode(Usuario executor, Usuario signer) {
        if (signer != null && executor != null && !Objects.equals(signer.getId(), executor.getId())) {
            return "PATRONO_CERTIFICATE";
        }
        return "SELF_CERTIFICATE";
    }

    private String resolveSignerName(Usuario signer) {
        if (signer == null || signer.getNome() == null || signer.getNome().isBlank()) {
            return null;
        }
        return signer.getNome().trim();
    }

    private String resolveSignerRegistration(Usuario signer) {
        if (signer == null) {
            return null;
        }
        if (signer.getOab() != null && !signer.getOab().isBlank()) {
            return signer.getOab().trim();
        }
        if (signer.getRegistroProfissional() != null && !signer.getRegistroProfissional().isBlank()) {
            return signer.getRegistroProfissional().trim();
        }
        if (signer.getCpf() != null && !signer.getCpf().isBlank()) {
            return signer.getCpf().replaceAll("[^0-9]", "");
        }
        return null;
    }

    private record DocumentBatchPayload(String batchId,
                                        String titulo,
                                        String categoria,
                                        String nivelSigilo,
                                        String origemSistema,
                                        String expectedBatchFingerprint,
                                        Integer expectedUploadedCount) {
    }

    @FunctionalInterface
    private interface OperationExecutor {
        Map<String, Object> execute(AdvOfficeProcessOperation operation, Usuario signer);
    }
}
