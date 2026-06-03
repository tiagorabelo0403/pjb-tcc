package com.tcc.pjb.backend.model.repository.protocolo;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.protocolo.RequisitoDocumental;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequisitoDocumentalRepository extends JpaRepository<RequisitoDocumental, Long> {

    @Query("""
            SELECT r FROM RequisitoDocumental r
            WHERE r.ritoCodigo = :rito
              AND r.obrigatorio = true
              AND r.vigenteAPartirDe <= :dataProtocolo
              AND (r.vigenteAte IS NULL OR r.vigenteAte >= :dataProtocolo)
            ORDER BY r.tipoDocumento
            """)
    List<RequisitoDocumental> findVigentesNaData(
            @Param("rito") RitoProcessual rito,
            @Param("dataProtocolo") LocalDate dataProtocolo);
}
