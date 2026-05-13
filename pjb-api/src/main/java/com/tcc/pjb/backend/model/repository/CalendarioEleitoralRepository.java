package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.eleitoral.CalendarioEleitoral;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CalendarioEleitoralRepository extends JpaRepository<CalendarioEleitoral, Long> {

    @Query("""
            select (count(c) > 0)
            from CalendarioEleitoral c
            where (:uf is null or c.uf = :uf or c.uf is null)
              and :data between c.dataInicio and c.dataFim
            """)
    boolean existsByUfAndDataBetween(@Param("uf") String uf, @Param("data") LocalDate data);


    @Query("""
            select c
            from CalendarioEleitoral c
            where (:uf is null or c.uf = :uf or c.uf is null)
              and (:fase is null or upper(c.fase) = upper(:fase))
              and :data between c.dataInicio and c.dataFim
            order by case when c.uf = :uf then 0 else 1 end, c.dataInicio asc
            """)
    java.util.List<CalendarioEleitoral> findApplicableWindows(@Param("uf") String uf,
                                                              @Param("fase") String fase,
                                                              @Param("data") LocalDate data);

}
