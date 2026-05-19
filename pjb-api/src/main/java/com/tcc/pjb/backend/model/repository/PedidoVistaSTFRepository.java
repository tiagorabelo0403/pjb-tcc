package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.processo.PedidoVistaSTF;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PedidoVistaSTFRepository extends JpaRepository<PedidoVistaSTF, Long> {
}
