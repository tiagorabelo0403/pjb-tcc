package com.tcc.pjb.backend.core.icp;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilTimelineQuery;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilTrustAnchorQuery;
import com.tcc.pjb.backend.core.icp.domain.IcpCertificateHealthQuery;
import com.tcc.pjb.backend.core.icp.domain.IcpTrustAnchorHealthQuery;
import com.tcc.pjb.backend.core.icp.domain.IcpValidationHealthQuery;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IcpBrasilApplicationService {

    private final IcpBrasilChainValidator chainValidator;
    private final AuditLedgerService auditLedgerService;

    public IcpBrasilApplicationService(IcpBrasilChainValidator chainValidator,
                                       AuditLedgerService auditLedgerService) {
        this.chainValidator = Objects.requireNonNull(chainValidator);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.icp.domain.IcpBrasilSignaturePolicySnapshot policy() {
        return chainValidator.policySnapshot();
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.icp.domain.IcpValidationHealthResult validationHealth() {
        return chainValidator.validationHealth(new IcpValidationHealthQuery(null));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.icp.domain.IcpTrustAnchorHealthResult trustAnchorHealth(String alias, String acSigla) {
        return chainValidator.trustAnchorHealth(new IcpTrustAnchorHealthQuery(alias, normalize(acSigla)));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.icp.domain.IcpBrasilTrustAnchorResult trustAnchor(String acSigla) {
        String normalized = normalize(acSigla);
        var result = chainValidator.trustAnchorResult(new IcpBrasilTrustAnchorQuery(normalized));
        auditLedgerService.appendSafely("ICP_TRUST_ANCHOR_QUERY", "ICP", normalized, null, String.valueOf(result.anchor().active()));
        return result;
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.icp.domain.IcpCertificateHealthResult certificateHealth(String issuerDn, String serialHex) {
        return chainValidator.certificateHealthResult(new IcpCertificateHealthQuery(issuerDn, serialHex));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspHealthSnapshot ocspHealth() {
        return chainValidator.ocspHealthSnapshot();
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.icp.domain.IcpBrasilSignerSelectionSnapshot signerSelection(String source,
                                                                                                  String keyAlias,
                                                                                                  boolean available,
                                                                                                  boolean pkcs11) {
        return chainValidator.signerSelectionSnapshot(source, keyAlias, available, pkcs11);
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.icp.domain.IcpBrasilTimelineResult timeline(String docHash) {
        String normalized = docHash == null ? null : docHash.trim();
        var timeline = chainValidator.timeline(new IcpBrasilTimelineQuery(normalized));
        auditLedgerService.appendSafely("ICP_TIMELINE_QUERY", "ICP", normalized, null, "entries=" + timeline.entries().size());
        return timeline;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("acSigla obrigatoria");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
