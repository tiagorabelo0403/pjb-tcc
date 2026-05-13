package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.icp.IcpSignatureEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IcpSignatureEventRepository extends JpaRepository<IcpSignatureEvent, Long> {
    List<IcpSignatureEvent> findTop20ByDocHashOrderBySignedAtAsc(String docHash);
}
