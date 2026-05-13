package com.tcc.pjb.backend.model.repository.security;

import com.tcc.pjb.backend.model.entity.security.OperationalFunctionCredential;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OperationalFunctionCredentialRepository extends JpaRepository<OperationalFunctionCredential, Long> {

    Optional<OperationalFunctionCredential> findByUsuarioIdAndFunctionCode(Long usuarioId, String functionCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from OperationalFunctionCredential c join fetch c.usuario where c.usuario.id = :usuarioId and c.functionCode = :functionCode")
    Optional<OperationalFunctionCredential> findLockedByUsuarioIdAndFunctionCode(@Param("usuarioId") Long usuarioId,
                                                                                 @Param("functionCode") String functionCode);

    @Query("select c from OperationalFunctionCredential c join fetch c.usuario where c.usuario.id = :usuarioId order by c.functionCode asc")
    List<OperationalFunctionCredential> findAllByUsuarioIdOrderByFunctionCode(@Param("usuarioId") Long usuarioId);

    @Query("select c from OperationalFunctionCredential c join fetch c.usuario where c.usuario.id = :usuarioId and c.functionCode in :functionCodes order by c.functionCode asc")
    List<OperationalFunctionCredential> findAllByUsuarioIdAndFunctionCodeIn(@Param("usuarioId") Long usuarioId,
                                                                            @Param("functionCodes") List<String> functionCodes);
}
