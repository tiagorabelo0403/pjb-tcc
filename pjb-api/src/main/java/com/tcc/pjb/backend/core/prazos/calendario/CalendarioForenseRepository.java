package com.tcc.pjb.backend.core.prazos.calendario;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CalendarioForenseRepository extends JpaRepository<CalendarioForenseEntry, Long> {

    @Query("select e from CalendarioForenseEntry e where (e.uf is null or e.uf = :uf) and (e.comarca is null or e.comarca = :comarca) and e.dia between :ini and :fim")
    List<CalendarioForenseEntry> findApplicableBetween(String uf, String comarca, LocalDate ini, LocalDate fim);

    boolean existsByUfAndComarcaAndDiaAndTipo(String uf, String comarca, LocalDate dia, String tipo);
}
