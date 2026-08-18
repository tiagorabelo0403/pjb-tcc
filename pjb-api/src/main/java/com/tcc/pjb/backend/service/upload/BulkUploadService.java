package com.tcc.pjb.backend.service.upload;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.kernel.process.ProcessEventStore;
import com.tcc.pjb.backend.core.kernel.process.ProcessEventType;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.edge.EdgeAttestationService;
import com.tcc.pjb.backend.core.storage.ObjectStorageProperties;
import com.tcc.pjb.backend.model.dto.upload.UploadBatchFinalizeResponse;
import com.tcc.pjb.backend.model.dto.upload.UploadItemReserveRequest;
import com.tcc.pjb.backend.model.dto.upload.UploadItemReserveResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.upload.UploadBatch;
import com.tcc.pjb.backend.model.entity.upload.UploadBatchStatus;
import com.tcc.pjb.backend.model.entity.upload.UploadItem;
import com.tcc.pjb.backend.model.entity.upload.UploadItemStatus;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.platform.unified.eventsourcing.JudiciarioEventSourcingEngine;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.repository.upload.UploadBatchRepository;
import com.tcc.pjb.backend.repository.upload.UploadItemRepository;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;

@Service
public class BulkUploadService {

    private final UploadBatchRepository batchRepository;
    private final UploadItemRepository itemRepository;
    private final DocumentoProcessualRepository documentoRepository;
    private final ProcessoRepository processoRepository;
    private final UploadTokenService tokenService;
    private final CurrentUserService currentUserService;
    private final EdgeAttestationService edgeAttestationService;
    private final ObjectStorageProperties storageProperties;
    private final UploadContentPolicyService uploadContentPolicyService;
    private final UploadCapacityGovernanceService uploadCapacityGovernanceService;
    private final ProcessEventStore processEventStore;
    private final EntityManager entityManager;

    public BulkUploadService(UploadBatchRepository batchRepository,
                             UploadItemRepository itemRepository,
                             DocumentoProcessualRepository documentoRepository,
                             ProcessoRepository processoRepository,
                             UploadTokenService tokenService,
                             CurrentUserService currentUserService,
                             EdgeAttestationService edgeAttestationService,
                             ObjectStorageProperties storageProperties,
                             UploadContentPolicyService uploadContentPolicyService,
                             UploadCapacityGovernanceService uploadCapacityGovernanceService,
                             ProcessEventStore processEventStore,
                             EntityManager entityManager) {
        this.batchRepository = batchRepository;
        this.itemRepository = itemRepository;
        this.documentoRepository = documentoRepository;
        this.processoRepository = processoRepository;
        this.tokenService = tokenService;
        this.currentUserService = currentUserService;
        this.edgeAttestationService = edgeAttestationService;
        this.storageProperties = storageProperties;
        this.uploadContentPolicyService = uploadContentPolicyService;
        this.uploadCapacityGovernanceService = uploadCapacityGovernanceService;
        this.processEventStore = processEventStore;
        this.entityManager = entityManager;
    }

    @Transactional
    public UploadBatch createBatch(Long processoId, Integer expectedCount) {
        Objects.requireNonNull(processoId, "processoId");
        uploadCapacityGovernanceService.validateBatchCreate(expectedCount);

        Processo p = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));

        Long createdBy = currentUserService.getOptional().map(u -> u.getId()).orElse(null);
        UploadBatch batch = UploadBatch.builder()
                .id(UUID.randomUUID())
                .processoId(p.getId())
                .expectedCount(expectedCount)
                .status(UploadBatchStatus.INITIATED)
                .createdBy(createdBy)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        return batchRepository.save(batch);
    }

    @Transactional
    public UploadItemReserveResponse reserveItem(UUID batchId, UploadItemReserveRequest req) {
        UploadBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("UploadBatch", batchId));

        if (batch.getStatus() != UploadBatchStatus.INITIATED) {
            throw new IllegalStateException("batch não aceita novos itens");
        }

        validateSha384(req.hashSha384(), req.nomeOriginal());
        String safeName = safeFilename(req.nomeOriginal());
        UploadContentPolicyService.Policy policy = uploadContentPolicyService.resolveReservation(safeName, req.contentType(), req.tamanhoBytes());
        int currentItems = Math.toIntExact(itemRepository.countByBatch_Id(batchId));
        long currentBytes = itemRepository.sumTamanhoBytesByBatchId(batchId);
        boolean duplicateHash = itemRepository.existsByBatch_IdAndHashSha384(batchId, req.hashSha384());
        UploadCapacityGovernanceService.ReservationBudgetReport budget = uploadCapacityGovernanceService.validateReservation(
                currentBytes,
                req.tamanhoBytes(),
                currentItems,
                policy,
                duplicateHash
        );
        UUID itemId = UUID.randomUUID();

        String storageBackend = "LOCALFS";
        String key = "batches/" + batchId + "/" + itemId + policy.storageExtension();

        Instant expires = Instant.now().plusSeconds(storageProperties.getUpload().getTokenTtlSeconds());
        String token = tokenService.issue(batchId, itemId, key, expires);

        String uploadUrl = "/api/v1/uploads/direct/" + batchId + "/" + itemId + "?token=" + token;

        long sizeBytes = req.tamanhoBytes();
        EdgeAttestationService.NormalizedAttestation att = edgeAttestationService.normalizeOrThrow(req.edgeAttestationJson(), req.hashSha384(), sizeBytes);
        String attJson = att.canonicalJson();

        UploadItem item = UploadItem.builder()
                .id(itemId)
                .batch(batch)
                .nomeOriginal(safeName)
                .contentType(policy.normalizedContentType())
                .tamanhoBytes(req.tamanhoBytes())
                .hashSha384(req.hashSha384())
                .storageBackend(storageBackend)
                .storageKey(key)
                .storageUri(key)
                .status(UploadItemStatus.RESERVED)
                .edgeAttestationJson(attJson)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        itemRepository.save(item);

        if (budget.nearLimit() && batch.getProcessoId() != null) {
            processEventStore.append(
                    batch.getProcessoId(),
                    ProcessEventType.DOCUMENT_ADDED,
                    Map.of(
                            "uploadBatchId", batchId.toString(),
                            "nextItemCount", budget.nextItemCount(),
                            "nextTotalBytes", budget.nextTotalBytes(),
                            "processingLane", budget.ingestionPlan().processingLane(),
                            "previewProfile", budget.ingestionPlan().previewProfile()
                    )
            );
        }

        return new UploadItemReserveResponse(itemId, uploadUrl, item.getStatus().name());
    }

    @PjbTransactionalBudget(operation = "upload.bulk.finalize-batch", maxMillis = 8000)
    @Transactional
    public UploadBatchFinalizeResponse finalizeBatch(UUID batchId) {
        UploadBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("UploadBatch", batchId));

        if (batch.getStatus() != UploadBatchStatus.INITIATED) {
            throw new IllegalStateException("batch não pode ser finalizado");
        }

        Processo processo = processoRepository.findById(batch.getProcessoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", batch.getProcessoId()));

        int chunkSize = Math.max(50, storageProperties.getUpload().getBulkFinalizeChunkSize());
        Sort sort = Sort.by(Sort.Direction.ASC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id"));

        Page<UploadItem> firstPage = itemRepository.findByBatchIdAndStatus(batchId, UploadItemStatus.UPLOADED, PageRequest.of(0, chunkSize, sort));
        if (firstPage.isEmpty()) {
            throw new ErroDeValidacaoException(TipoErroValidacao.ARQUIVO_CORROMPIDO, "Nenhum item foi enviado")
                    .addMetadado("batchId", batchId.toString());
        }

        List<UUID> docIds = new ArrayList<>(Math.min(1000, chunkSize));
        List<JudiciarioEventSourcingEngine.DocumentoMetadata> docsMeta = new ArrayList<>(chunkSize);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        int pageIdx = 0;
        long totalLinked = 0L;
        boolean more = true;

        while (more) {
            Page<UploadItem> page = pageIdx == 0 ? firstPage : itemRepository.findByBatchIdAndStatus(batchId, UploadItemStatus.UPLOADED, PageRequest.of(pageIdx, chunkSize, sort));
            if (page.isEmpty()) {
                break;
            }

            Processo processoRef = entityManager.getReference(Processo.class, processo.getId());
            List<UploadItem> uploaded = page.getContent();

            List<DocumentoProcessual> docsChunk = new ArrayList<>(uploaded.size());
            List<UUID> chunkDocIds = new ArrayList<>(uploaded.size());
            for (UploadItem item : uploaded) {
                UUID docId = UUID.randomUUID();
                DocumentoProcessual doc = DocumentoProcessual.builder()
                        .id(docId)
                        .processo(processoRef)
                        .nomeOriginal(item.getNomeOriginal())
                        .contentType(item.getContentType())
                        .tamanhoBytes(item.getTamanhoBytes())
                        .sha256(item.getHashSha256())
                        .sha384(item.getHashSha384())
                        .storageBackend(item.getStorageBackend())
                        .storageUri(item.getStorageUri())
                        .externalizedAt(now.toInstant())
                        .pdf(null)
                        .criadoEm(now.toLocalDateTime())
                        .build();
                docsChunk.add(doc);

                item.setStatus(UploadItemStatus.LINKED_TO_PROCESS);
                item.setLinkedDocumentId(docId);
                chunkDocIds.add(docId);

                docsMeta.add(new JudiciarioEventSourcingEngine.DocumentoMetadata(
                        docId,
                        item.getStorageUri(),
                        item.getHashSha384(),
                        item.getTamanhoBytes() != null ? item.getTamanhoBytes() : 0L
                ));
            }

            documentoRepository.saveAll(docsChunk);
            documentoRepository.flush();
            itemRepository.saveAll(uploaded);
            itemRepository.flush();

            totalLinked += uploaded.size();
            docIds.addAll(chunkDocIds);

            entityManager.clear();

            pageIdx++;
            more = page.hasNext();
        }

        batch.setStatus(UploadBatchStatus.FINALIZED);
        batch.setFinalizedAt(OffsetDateTime.now(ZoneOffset.UTC));
        batchRepository.save(batch);

        processEventStore.append(
                processo.getId(),
                ProcessEventType.DOCUMENTS_BULK_ADDED,
                new JudiciarioEventSourcingEngine.DocumentosJuntados(UUID.randomUUID(), processo.getId(), docsMeta, Instant.now())
        );

        return new UploadBatchFinalizeResponse(batchId, (int) totalLinked, docIds);
    }

    public UploadBatchFinalizeResponse finalizeBatchAsJob(UUID batchId, com.tcc.pjb.backend.core.jobs.runtime.JobExecutionContext ctx) {
        UploadBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("UploadBatch", batchId));

        if (batch.getStatus() != UploadBatchStatus.INITIATED) {
            throw new IllegalStateException("batch não pode ser finalizado");
        }

        Processo processo = processoRepository.findById(batch.getProcessoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", batch.getProcessoId()));

        int chunkSize = Math.max(50, storageProperties.getUpload().getBulkFinalizeChunkSize());
        Sort sort = Sort.by(Sort.Direction.ASC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id"));

        Page<UploadItem> firstPage = itemRepository.findByBatchIdAndStatus(batchId, UploadItemStatus.UPLOADED, PageRequest.of(0, chunkSize, sort));
        if (firstPage.isEmpty()) {
            throw new ErroDeValidacaoException(TipoErroValidacao.ARQUIVO_CORROMPIDO, "Nenhum item foi enviado")
                    .addMetadado("batchId", batchId.toString());
        }

        long total = firstPage.getTotalElements();
        ctx.progress(0, total);

        List<UUID> docIds = new ArrayList<>(Math.min(1000, chunkSize));
        List<JudiciarioEventSourcingEngine.DocumentoMetadata> docsMeta = new ArrayList<>(chunkSize);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        int pageIdx = 0;
        long linked = 0L;
        boolean more = true;

        while (more) {
            Page<UploadItem> page = pageIdx == 0 ? firstPage : itemRepository.findByBatchIdAndStatus(batchId, UploadItemStatus.UPLOADED, PageRequest.of(pageIdx, chunkSize, sort));
            if (page.isEmpty()) {
                break;
            }

            Processo processoRef = entityManager.getReference(Processo.class, processo.getId());
            List<UploadItem> uploaded = page.getContent();

            List<DocumentoProcessual> docsChunk = new ArrayList<>(uploaded.size());
            List<UUID> chunkDocIds = new ArrayList<>(uploaded.size());

            for (UploadItem item : uploaded) {
                String itemKey = item.getId() != null ? item.getId().toString() : UUID.randomUUID().toString();
                var jobItem = ctx.findOrCreateItem(itemKey, 3).orElse(null);
                if (jobItem != null) {
                    jobItem.markRunning();
                }

                try {
                    UUID docId = UUID.randomUUID();
                    DocumentoProcessual doc = DocumentoProcessual.builder()
                            .id(docId)
                            .processo(processoRef)
                            .nomeOriginal(item.getNomeOriginal())
                            .contentType(item.getContentType())
                            .tamanhoBytes(item.getTamanhoBytes())
                            .sha256(item.getHashSha256())
                            .sha384(item.getHashSha384())
                            .storageBackend(item.getStorageBackend())
                            .storageUri(item.getStorageUri())
                            .externalizedAt(now.toInstant())
                            .pdf(null)
                            .criadoEm(now.toLocalDateTime())
                            .build();
                    docsChunk.add(doc);

                    item.setStatus(UploadItemStatus.LINKED_TO_PROCESS);
                    item.setLinkedDocumentId(docId);
                    chunkDocIds.add(docId);

                    docsMeta.add(new JudiciarioEventSourcingEngine.DocumentoMetadata(
                            docId,
                            item.getStorageUri(),
                            item.getHashSha384(),
                            item.getTamanhoBytes() != null ? item.getTamanhoBytes() : 0L
                    ));

                    if (jobItem != null) {
                        jobItem.succeed();
                    }
                    linked++;
                    ctx.progress(linked, total);
                } catch (Exception e) {
                    if (jobItem != null) {
                        jobItem.fail(e.getMessage());
                    }
                }
            }

            documentoRepository.saveAll(docsChunk);
            documentoRepository.flush();
            itemRepository.saveAll(uploaded);
            itemRepository.flush();

            docIds.addAll(chunkDocIds);
            entityManager.clear();

            pageIdx++;
            more = page.hasNext();
            ctx.heartbeat();
        }

        batch.setStatus(UploadBatchStatus.FINALIZED);
        batch.setFinalizedAt(OffsetDateTime.now(ZoneOffset.UTC));
        batchRepository.save(batch);

        processEventStore.append(
                processo.getId(),
                ProcessEventType.DOCUMENTS_BULK_ADDED,
                new JudiciarioEventSourcingEngine.DocumentosJuntados(UUID.randomUUID(), processo.getId(), docsMeta, Instant.now())
        );

        return new UploadBatchFinalizeResponse(batchId, (int) linked, docIds);
    }

    private static void validateSha384(String hex, String nome) {
        if (hex == null || hex.isBlank() || hex.length() != 96 || !hex.matches("[0-9a-fA-F]{96}")) {
            throw new ErroDeValidacaoException(TipoErroValidacao.ARQUIVO_CORROMPIDO, nome)
                    .addMetadado("hash_sha384", String.valueOf(hex));
        }
    }

    private static String safeFilename(String nome) {
        if (nome == null || nome.isBlank()) return "documento.pdf";
        String n = nome.replaceAll("[\\r\\n]", " ").trim();
        if (!n.toLowerCase().endsWith(".pdf")) n = n + ".pdf";
        return n;
    }
}
