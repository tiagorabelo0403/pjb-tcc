package com.tcc.pjb.backend.modules.laiane.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeProtocolPackage;

@Repository
public interface LaianeProtocolPackageRepository extends JpaRepository<LaianeProtocolPackage, Long> {
    List<LaianeProtocolPackage> findTop50ByUsuario_IdOrderByCreatedAtDesc(Long usuarioId);
}
