package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.judicial.TemaRepercussaoGeral;

public interface TemaRepercussaoGeralRepository extends JpaRepository<TemaRepercussaoGeral, Long> {

    Optional<TemaRepercussaoGeral> findByCodigoIgnoreCase(String codigo);

    List<TemaRepercussaoGeral> findTop100ByOrderByCreatedAtDesc();
}
