package com.tcc.pjb.backend.core.icp;

import com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspResult;
import java.security.KeyStore;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

class IcpBrasilPkixOcspVerifier implements IcpBrasilOcspVerifier {

    private final IcpBrasilTrustAnchorLoader trustAnchorLoader;

    IcpBrasilPkixOcspVerifier(IcpBrasilTrustAnchorLoader trustAnchorLoader) {
        this.trustAnchorLoader = Objects.requireNonNull(trustAnchorLoader);
    }

    @Override
    public IcpBrasilOcspResult check(X509Certificate certificate) {
        if (certificate == null) {
            return IcpBrasilOcspResult.good();
        }
        try {
            Set<TrustAnchor> anchors = trustAnchorLoader.loadAnchors();
            if (anchors.isEmpty()) {
                anchors = jvmDefaultAnchors();
            }
            if (anchors.isEmpty()) {
                return IcpBrasilOcspResult.good();
            }
            CertPathValidator validator = CertPathValidator.getInstance("PKIX");
            PKIXRevocationChecker checker = (PKIXRevocationChecker) validator.getRevocationChecker();
            checker.setOptions(EnumSet.of(
                    PKIXRevocationChecker.Option.PREFER_CRLS,
                    PKIXRevocationChecker.Option.SOFT_FAIL
            ));
            CertPath path = CertificateFactory.getInstance("X.509")
                    .generateCertPath(List.of(certificate));
            PKIXParameters params = new PKIXParameters(anchors);
            params.addCertPathChecker(checker);
            params.setRevocationEnabled(true);
            params.setDate(Date.from(Instant.now()));
            try {
                validator.validate(path, params);
                return IcpBrasilOcspResult.good();
            } catch (CertPathValidatorException e) {
                if (e.getReason() == CertPathValidatorException.BasicReason.REVOKED) {
                    return new IcpBrasilOcspResult(true, null);
                }
                return IcpBrasilOcspResult.good();
            }
        } catch (Exception e) {
            return IcpBrasilOcspResult.good();
        }
    }

    private static Set<TrustAnchor> jvmDefaultAnchors() {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);
            Set<TrustAnchor> result = new LinkedHashSet<>();
            for (javax.net.ssl.TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager x509tm) {
                    for (X509Certificate cert : x509tm.getAcceptedIssuers()) {
                        result.add(new TrustAnchor(cert, null));
                    }
                }
            }
            return result;
        } catch (Exception ignored) {
            return Set.of();
        }
    }
}
