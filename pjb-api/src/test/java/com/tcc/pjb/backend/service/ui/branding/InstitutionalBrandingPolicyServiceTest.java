package com.tcc.pjb.backend.service.ui.branding;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.configs.ui.InstitutionalBrandingProperties;
import org.junit.jupiter.api.Test;

class InstitutionalBrandingPolicyServiceTest {

    private final InstitutionalBrandingPolicyService service = new InstitutionalBrandingPolicyService(new InstitutionalBrandingProperties());

    @Test
    void governanceSummaryShouldForbidDatabaseBlobAndSvg() {
        assertThat(service.governanceSummary())
                .containsEntry("databaseBlobForbidden", true)
                .containsEntry("allowSvg", false)
                .containsEntry("objectStorageOnly", true);
    }

    @Test
    void sanitizeShouldApproveGovernedBannerReference() {
        InstitutionalBrandingPolicyService.SanitizedAssetReference ref = service.sanitize(
                "BANNER",
                "ui/institutional-branding/ministerio-publico/banner.webp",
                "image/webp",
                140000L,
                1920,
                320
        );
        assertThat(ref.approved()).isTrue();
        assertThat(ref.deliveryPath()).contains("assetKey=");
    }

    @Test
    void sanitizeShouldRejectOversizedOrWrongMime() {
        InstitutionalBrandingPolicyService.SanitizedAssetReference ref = service.sanitize(
                "LOGO",
                "ui/institutional-branding/ministerio-publico/logo.svg",
                "image/svg+xml",
                1000L,
                256,
                256
        );
        assertThat(ref.approved()).isFalse();
    }
}
