package com.tcc.pjb.backend.service.upload;

import com.tcc.pjb.backend.core.storage.ObjectStorageProperties;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class UploadCapacityGovernanceService {

    private final ObjectStorageProperties.Upload uploadProperties;
    private final UploadContentPolicyService uploadContentPolicyService;

    public UploadCapacityGovernanceService(ObjectStorageProperties props,
                                           UploadContentPolicyService uploadContentPolicyService) {
        ObjectStorageProperties properties = props == null ? new ObjectStorageProperties() : props;
        this.uploadProperties = properties.getUpload() == null ? new ObjectStorageProperties.Upload() : properties.getUpload();
        this.uploadContentPolicyService = uploadContentPolicyService;
    }

    public void validateBatchCreate(Integer expectedCount) {
        int count = expectedCount == null ? 0 : expectedCount;
        if (count <= 0) {
            throw new IllegalArgumentException("quantidade esperada de uploads deve ser positiva");
        }
        if (count > Math.max(1, uploadProperties.getMaxBatchItems())) {
            throw new IllegalArgumentException("quantidade esperada excedeu o limite prudencial do lote de upload");
        }
    }

    public ReservationBudgetReport validateReservation(long currentReservedBytes,
                                                       long incomingBytes,
                                                       int currentItems,
                                                       UploadContentPolicyService.Policy policy,
                                                       boolean duplicateHash) {
        if (duplicateHash) {
            throw new IllegalArgumentException("arquivo duplicado detectado no lote pelo mesmo hash sha384");
        }
        int nextItems = Math.max(0, currentItems) + 1;
        long nextBytes = saturatingAdd(Math.max(0L, currentReservedBytes), Math.max(0L, incomingBytes));
        int maxBatchItems = Math.max(1, uploadProperties.getMaxBatchItems());
        long maxBatchTotalBytes = positiveOr(uploadProperties.getMaxBatchTotalBytes(), 536_870_912L);
        if (nextItems > maxBatchItems) {
            throw new IllegalArgumentException("o lote excedeu a quantidade máxima prudencial de arquivos");
        }
        if (nextBytes > maxBatchTotalBytes) {
            throw new IllegalArgumentException("o lote excedeu o orçamento prudencial total de bytes");
        }
        UploadContentPolicyService.IngestionPlan ingestionPlan = uploadContentPolicyService.buildIngestionPlan(policy, incomingBytes);
        int warningRatio = maxBatchItems == 0 ? 0 : (nextItems * 100) / maxBatchItems;
        long byteRatio = maxBatchTotalBytes == 0L ? 0L : (nextBytes * 100L) / maxBatchTotalBytes;
        return new ReservationBudgetReport(
                nextItems,
                nextBytes,
                maxBatchItems,
                maxBatchTotalBytes,
                warningRatio >= 80 || byteRatio >= 80L,
                ingestionPlan,
                governanceSummary()
        );
    }

    public Map<String, Object> governanceSummary() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.putAll(uploadContentPolicyService.governanceSummary());
        out.put("maxInlineImageBytes", positiveOr(uploadProperties.getMaxInlineImageBytes(), 10_485_760L));
        out.put("maxInlineAudioBytes", positiveOr(uploadProperties.getMaxInlineAudioBytes(), 25_165_824L));
        out.put("maxInlineVideoBytes", positiveOr(uploadProperties.getMaxInlineVideoBytes(), 100_663_296L));
        out.put("maxInlineTotalBytes", positiveOr(uploadProperties.getMaxInlineTotalBytes(), 167_772_160L));
        out.put("maxInlineBlocks", Math.max(1, uploadProperties.getMaxInlineBlocks()));
        out.put("maxPostPetitionReferences", Math.max(1, uploadProperties.getMaxPostPetitionReferences()));
        out.put("maxAudioDurationMs", positiveOr(uploadProperties.getMaxAudioDurationMs(), 900_000L));
        out.put("maxVideoDurationMs", positiveOr(uploadProperties.getMaxVideoDurationMs(), 900_000L));
        return Collections.unmodifiableMap(out);
    }

    private static long saturatingAdd(long left, long right) {
        long result = left + right;
        return result < 0L ? Long.MAX_VALUE : result;
    }

    private static long positiveOr(long value, long fallback) {
        return value > 0L ? value : fallback;
    }

    public record ReservationBudgetReport(int nextItemCount,
                                          long nextTotalBytes,
                                          int maxBatchItems,
                                          long maxBatchTotalBytes,
                                          boolean nearLimit,
                                          UploadContentPolicyService.IngestionPlan ingestionPlan,
                                          Map<String, Object> governanceSummary) {
    }
}
