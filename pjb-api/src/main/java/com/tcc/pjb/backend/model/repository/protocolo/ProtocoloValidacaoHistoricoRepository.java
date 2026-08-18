package com.tcc.pjb.backend.model.repository.protocolo;

import com.tcc.pjb.backend.model.entity.protocolo.ProtocoloValidacaoHistorico;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProtocoloValidacaoHistoricoRepository extends JpaRepository<ProtocoloValidacaoHistorico, Long> {

    List<ProtocoloValidacaoHistorico> findByProtocoloIdOrderByExecutadoEmDesc(Long protocoloId);
}
