package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.judicial.MniRecepcao;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MniRecepcaoRepository extends JpaRepository<MniRecepcao, Long> {

    Optional<MniRecepcao> findByMniPayloadHash(String mniPayloadHash);
}
