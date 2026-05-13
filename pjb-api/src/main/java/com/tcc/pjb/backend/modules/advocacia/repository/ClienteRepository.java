package com.tcc.pjb.backend.modules.advocacia.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.modules.advocacia.entity.Cliente;
import com.tcc.pjb.backend.modules.advocacia.enums.StatusCliente;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

    List<Cliente> findByAdvogado_IdOrderByNomeAsc(Long advogadoId);

    boolean existsByCpfHashAndAdvogado_Id(String cpfHash, Long advogadoId);

    boolean existsByCpfHashAndAdvogado_IdAndIdNot(String cpfHash, Long advogadoId, Long id);

    boolean existsByAdvogado_IdAndCpfHashIn(Long advogadoId, Collection<String> cpfHashes);

    boolean existsByAdvogado_IdAndCpfHashInAndIdNot(Long advogadoId, Collection<String> cpfHashes, Long id);

    long countByAdvogado_IdAndStatus(Long advogadoId, StatusCliente status);

    long count();

    @Query("select c from Cliente c where c.id > :afterId and (:untilId is null or c.id <= :untilId) order by c.id asc")
    List<Cliente> findBatchForCanonicalize(@Param("afterId") long afterId,
                                          @Param("untilId") Long untilId,
                                          Pageable pageable);

    @Query("select count(c) from Cliente c where c.id > :afterId and (:untilId is null or c.id <= :untilId)")
    long countForCanonicalize(@Param("afterId") long afterId,
                              @Param("untilId") Long untilId);

    @Query("select min(c.id) from Cliente c where c.advogado.id = :advogadoId and c.cpfHash = :cpfHash")
    Long minIdByAdvogadoAndCpfHash(@Param("advogadoId") long advogadoId,
                                   @Param("cpfHash") String cpfHash);

    @Query("select c from Cliente c where c.status = :status and c.cpfHash is null and (:advogadoId is null or c.advogado.id = :advogadoId) order by c.id desc")
    Page<Cliente> findDuplicatesForReview(@Param("status") StatusCliente status,
                                         @Param("advogadoId") Long advogadoId,
                                         Pageable pageable);


    long countByCpfHashIsNull();

    long countByStatus(StatusCliente status);

    long countByStatusAndCpfHashIsNull(StatusCliente status);

}
