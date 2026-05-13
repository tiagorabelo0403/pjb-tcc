package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.icp.IcpBrasilTrustAnchor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IcpBrasilTrustAnchorRepository extends JpaRepository<IcpBrasilTrustAnchor, Long> {

    List<IcpBrasilTrustAnchor> findByAtivoTrueOrderByAcSiglaAsc();
}
