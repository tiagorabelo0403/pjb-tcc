package com.tcc.pjb.backend.model.repository.security;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.security.SecurityAlert;

public interface SecurityAlertRepository extends JpaRepository<SecurityAlert, Long> {

    @Query("select a from SecurityAlert a where a.usuario.id = :userId order by a.criadoEm desc")
    List<SecurityAlert> findByUserOrderByCriadoEmDesc(@Param("userId") Long userId, Pageable pageable);

    default List<SecurityAlert> findTop50ByUser(Long userId) {
        return findByUserOrderByCriadoEmDesc(userId, PageRequest.of(0, 50));
    }
}
