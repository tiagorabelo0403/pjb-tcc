package com.tcc.pjb.backend.core.icp;

import com.tcc.pjb.backend.model.entity.Usuario;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class RecursalIcpBrasilMetadataAssembler {

    public RecursalIcpBrasilMetadata skippedNoCertificate(String profileCandidate) {
        return new RecursalIcpBrasilMetadata(true, false, null, null, profileCandidate, null, null, null, null, null, "SKIPPED_NO_CERTIFICATE");
    }

    public RecursalIcpBrasilMetadata validationFailed(String profileCandidate, String failureReason) {
        return new RecursalIcpBrasilMetadata(true, true, false, failureReason, profileCandidate, null, null, null, null, null, "VALIDATION_FAILED_NON_BLOCKING");
    }

    public RecursalIcpBrasilMetadata validated(String profileCandidate, String profileAchieved, IcpBrasilCertProfile profile, Usuario usuario) {
        Objects.requireNonNull(profile);
        return new RecursalIcpBrasilMetadata(true, true, true, null, profileCandidate, profileAchieved, profile.certType(), profile.acSigla(), profile.serialHex(), usuario == null ? null : usuario.getId(), "RECUSAL_PDF_CHAIN_VALIDATED");
    }
}
