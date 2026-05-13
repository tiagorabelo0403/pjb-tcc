package com.tcc.pjb.backend.model.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.CredencialAcesso;
import com.tcc.pjb.backend.model.entity.Processo;

public interface CredencialAcessoRepository extends JpaRepository<CredencialAcesso, UUID> {

    
    Optional<CredencialAcesso> findByProcessoAndAtivaTrue(Processo processo);

    
    Optional<CredencialAcesso> findByLoginAndAtivaTrue(String login);

    
    @Query("""
        SELECT c FROM CredencialAcesso c
        WHERE c.login = :login
          AND c.ativa = true
          AND c.validade > :agora
    """)
    Optional<CredencialAcesso> buscarCredencialValida(
            @Param("login") String login,
            @Param("agora") LocalDateTime agora
    );

    
    @Query("""
        SELECT c FROM CredencialAcesso c
        WHERE c.validade <= :agora
          AND c.ativa = true
    """)
    List<CredencialAcesso> listarCredenciaisExpiradas(
            @Param("agora") LocalDateTime agora
    );

    
    boolean existsByProcessoAndAtivaTrue(Processo processo);
}
