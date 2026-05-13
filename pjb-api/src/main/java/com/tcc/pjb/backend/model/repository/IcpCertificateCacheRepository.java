package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.icp.IcpCertificateCache;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IcpCertificateCacheRepository extends JpaRepository<IcpCertificateCache, Long> {
    Optional<IcpCertificateCache> findBySerialHex(String serialHex);
    Optional<IcpCertificateCache> findByIssuerDnAndSerialHex(String issuerDn, String serialHex);
}
