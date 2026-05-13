package com.tcc.pjb.backend.service.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.storage.ObjectStorageProperties;
import org.junit.jupiter.api.Test;

class UploadCapacityGovernanceServiceTest {

    private final ObjectStorageProperties properties = new ObjectStorageProperties();
    private final UploadContentPolicyService policyService = new UploadContentPolicyService(properties);
    private final UploadCapacityGovernanceService service = new UploadCapacityGovernanceService(properties, policyService);

    @Test
    void deveBloquearLoteAcimaDoOrcamentoTotal() {
        var policy = policyService.resolveReservation("midia.mp4", "video/mp4", 1024L);
        assertThrows(IllegalArgumentException.class, () -> service.validateReservation(
                properties.getUpload().getMaxBatchTotalBytes(),
                1L,
                1,
                policy,
                false
        ));
    }

    @Test
    void deveProduzirPlanoPesadoParaVideo() {
        var policy = policyService.resolveReservation("midia.mp4", "video/mp4", 8_192L);
        var report = service.validateReservation(0L, 8_192L, 0, policy, false);
        assertEquals("ASYNC_STREAMING_FORENSE", report.ingestionPlan().processingLane());
        assertTrue(report.governanceSummary().containsKey("maxBatchItems"));
    }
}
