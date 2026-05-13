package com.tcc.pjb.backend.model.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.ChatMensagem;

@Repository
public interface ChatMensagemRepository extends JpaRepository<ChatMensagem, Long> {
    List<ChatMensagem> findByProcesso_IdOrderByDataEnvioAsc(Long processoId);
    List<ChatMensagem> findTop80ByProcesso_IdOrderByDataEnvioDesc(Long processoId);
}
