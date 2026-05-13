package com.tcc.pjb.backend.service.upload;

import java.io.InputStream;
import java.io.PushbackInputStream;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.storage.ObjectStoragePort;
import com.tcc.pjb.backend.core.storage.ObjectWriteResult;
import com.tcc.pjb.backend.model.entity.upload.UploadItem;
import com.tcc.pjb.backend.model.entity.upload.UploadItemStatus;
import com.tcc.pjb.backend.repository.upload.UploadItemRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class BulkUploadIngressService {

    private final UploadItemRepository itemRepository;
    private final UploadTokenService tokenService;
    private final ObjectStoragePort objectStorage;
    private final UploadStreamLimiter limiter;
    private final UploadContentPolicyService uploadContentPolicyService;

    public BulkUploadIngressService(UploadItemRepository itemRepository,
                                   UploadTokenService tokenService,
                                   ObjectStoragePort objectStorage,
                                   UploadStreamLimiter limiter,
                                   UploadContentPolicyService uploadContentPolicyService) {
        this.itemRepository = itemRepository;
        this.tokenService = tokenService;
        this.objectStorage = objectStorage;
        this.limiter = limiter;
        this.uploadContentPolicyService = uploadContentPolicyService;
    }

    @Transactional
    public UploadIngressResult ingest(UUID batchId, UUID itemId, String token, long contentLength, InputStream body) {
        Objects.requireNonNull(batchId, "batchId");
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(body, "body");

        var verified = tokenService.verify(token);
        if (!verified.batchId().equals(batchId) || !verified.itemId().equals(itemId)) {
            throw new SecurityException("token divergente");
        }

        UploadItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("UploadItem", itemId));

        if (!item.getBatch().getId().equals(batchId)) {
            throw new SecurityException("batch divergente");
        }

        if (!item.getStorageKey().equals(verified.storageKey())) {
            throw new SecurityException("key divergente");
        }

        if (item.getStatus() == UploadItemStatus.UPLOADED || item.getStatus() == UploadItemStatus.LINKED_TO_PROCESS) {
            return new UploadIngressResult(item.getStatus().name(), item.getHashSha256(), item.getHashSha384(), item.getStorageUri());
        }

        if (item.getStatus() != UploadItemStatus.RESERVED) {
            throw new IllegalStateException("item não pode receber upload");
        }

        if (item.getTamanhoBytes() != null && contentLength >= 0 && !item.getTamanhoBytes().equals(contentLength)) {
            throw new IllegalArgumentException("tamanho divergente do reservado");
        }

        try (var permit = limiter.acquire(Duration.ofMillis(250))) {
            PushbackInputStream in = new PushbackInputStream(body, 64);
            uploadContentPolicyService.assertMagicBytes(item.getContentType(), in);

            ObjectWriteResult wr = objectStorage.put(
                    item.getStorageKey(),
                    in,
                    contentLength,
                    item.getContentType() != null ? item.getContentType() : "application/pdf",
                    Map.of("batchId", batchId.toString(), "itemId", itemId.toString())
            );

            if (item.getHashSha384() != null && !item.getHashSha384().equalsIgnoreCase(wr.sha384())) {
                item.setStatus(UploadItemStatus.FAILED);
                itemRepository.save(item);
                throw new SecurityException("hash divergente (sha384)");
            }

            item.setHashSha256(wr.sha256());
            item.setHashSha384(wr.sha384());
            item.setUploadedAt(OffsetDateTime.now(ZoneOffset.UTC));
            item.setStatus(UploadItemStatus.UPLOADED);
            itemRepository.save(item);

            return new UploadIngressResult(item.getStatus().name(), wr.sha256(), wr.sha384(), item.getStorageUri());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            item.setStatus(UploadItemStatus.FAILED);
            itemRepository.save(item);
            throw new IllegalStateException("falha no upload", e);
        }
    }


    public record UploadIngressResult(String status, String sha256, String sha384, String storageUri) {
    }
}
