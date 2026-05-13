package com.tcc.pjb.backend.core.icp;

import java.time.Instant;

public record IcpBrasilCertProfile(
        String subjectDn,
        String issuerDn,
        String serialHex,
        String cpfTitular,
        String cnpjTitular,
        String nomeTitular,
        String certType,
        String acSigla,
        Instant validFrom,
        Instant validUntil
) {
}
