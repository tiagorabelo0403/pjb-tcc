package com.tcc.pjb.backend.modules.laiane.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeDocumentFingerprint;

@Repository
public interface LaianeDocumentFingerprintRepository extends JpaRepository<LaianeDocumentFingerprint, Long> {
    Optional<LaianeDocumentFingerprint> findByUsuario_IdAndSha256(Long usuarioId, String sha256);
}
