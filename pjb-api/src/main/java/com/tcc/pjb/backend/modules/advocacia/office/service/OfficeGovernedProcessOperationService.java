package com.tcc.pjb.backend.modules.advocacia.office.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.configs.EquipeContexto;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessAccessView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeProcessOperation;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeDelegationMode;
import com.tcc.pjb.backend.modules.advocacia.office.repository.AdvOfficeProcessOperationRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.processual.recursal.RecursalPeticionamentoFacadeService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
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
    private final ProcessoRepository processoRepository;
    private final RecursalPeticionamentoFacadeService recursalPeticionamentoFacadeService;
    private final OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService;
    private final AdvOfficeProcessOperationRepository processOperationRepository;
    private final ObjectProvider<HttpServletRequest> requestProvider;
    private final ObjectMapper objectMapper;
    private final AuditLedgerService auditLedgerService;
    private final OfficeOperationDelegationRoutingService delegationRoutingService;
    private final OfficeGovernedPetitionExecutionService petitionExecutionService;
    private final OfficeGovernedDocumentBatchLinkService documentBatchLinkService;

    public OfficeGovernedProcessOperationService(CurrentUserService currentUserService,
                                                 ProcessoRepository processoRepository,
                                                 RecursalPeticionamentoFacadeService recursalPeticionamentoFacadeService,
                                                 OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService,
                                                 AdvOfficeProcessOperationRepository processOperationRepository,
                                                 ObjectProvider<HttpServletRequest> requestProvider,
                                                 ObjectMapper objectMapper,
                                                 AuditLedgerService auditLedgerService,
                                                 OfficeOperationDelegationRoutingService delegationRoutingService,
                                                 OfficeGovernedPetitionExecutionService petitionExecutionService,
                                                 OfficeGovernedDocumentBatchLinkService documentBatchLinkService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.recursalPeticionamentoFacadeService = Objects.requireNonNull(recursalPeticionamentoFacadeService);
        this.officeProcessWorkspaceScopeService = Objects.requireNonNull(officeProcessWorkspaceScopeService);
        this.processOperationRepository = Objects.requireNonNull(processOperationRepository);
        this.requestProvider = Objects.requireNonNull(requestProvider);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.delegationRoutingService = Objects.requireNonNull(delegationRoutingService);
        this.petitionExecutionService = Objects.requireNonNull(petitionExecutionService);
        this.documentBatchLinkService = Objects.requireNonNull(documentBatchLinkService);
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
                (operation, signer) -> petitionExecutionService.execute(processoId, tipoPeticao, conteudo, fundamentacao, signer, operation)
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
                (operation, signer) -> documentBatchLinkService.execute(processoId, batchId, titulo, categoria, nivelSigilo, origemSistema, expectedBatchFingerprint, expectedUploadedCount)
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

        OfficeOperationDelegationRoutingService.DelegationRouting routing = delegationRoutingService.resolve(
                operation, actor, actionType, payloadHash, summary, processo, access.queueRequired());
        OfficeDelegationService.Decision decision = routing.decision();
        Usuario signer = routing.signer();
        operation.setSigner(signer);
        operation.setSignerNameSnapshot(resolveSignerName(signer));
        operation.setSignerRegistrationSnapshot(resolveSignerRegistration(signer));
        if (routing.queueItem() != null) {
            operation.setQueueItem(routing.queueItem());
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
            return petitionExecutionService.execute(operation.getProcesso().getId(), payload.tipoPeticao(), payload.conteudo(), payload.fundamentacao(), operation.getSigner(), operation);
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
            return documentBatchLinkService.execute(
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
