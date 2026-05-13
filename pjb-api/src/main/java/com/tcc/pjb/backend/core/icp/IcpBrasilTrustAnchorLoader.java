package com.tcc.pjb.backend.core.icp;

import com.tcc.pjb.backend.model.entity.icp.IcpBrasilTrustAnchor;
import com.tcc.pjb.backend.model.repository.IcpBrasilTrustAnchorRepository;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class IcpBrasilTrustAnchorLoader {

    private final IcpBrasilTrustAnchorRepository repository;

    public IcpBrasilTrustAnchorLoader(IcpBrasilTrustAnchorRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public Set<TrustAnchor> loadAnchors() {
        List<IcpBrasilTrustAnchor> anchors = repository.findByAtivoTrueOrderByAcSiglaAsc();
        Set<TrustAnchor> resolved = new LinkedHashSet<>();
        for (IcpBrasilTrustAnchor anchor : anchors) {
            X509Certificate certificate = toCertificate(anchor.getCertificadoDer());
            if (certificate != null) {
                resolved.add(new TrustAnchor(certificate, null));
            }
        }
        return resolved;
    }

    public int countActiveAnchors() {
        return repository.findByAtivoTrueOrderByAcSiglaAsc().size();
    }

    public boolean exists(String acSigla) {
        if (acSigla == null || acSigla.isBlank()) {
            return false;
        }
        return repository.findByAtivoTrueOrderByAcSiglaAsc().stream()
                .anyMatch(anchor -> acSigla.equalsIgnoreCase(anchor.getAcSigla()));
    }

    private X509Certificate toCertificate(byte[] rawDer) {
        if (rawDer == null || rawDer.length == 0) {
            return null;
        }
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(rawDer));
        } catch (Exception ignored) {
            return null;
        }
    }
}
