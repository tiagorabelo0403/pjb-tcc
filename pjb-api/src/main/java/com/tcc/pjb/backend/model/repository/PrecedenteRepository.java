package com.tcc.pjb.backend.model.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoPrecedente;
import com.tcc.pjb.backend.model.entity.enums.TribunalFonte;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;

public interface PrecedenteRepository extends JpaRepository<Precedente, Long> {

    
    @Query("""
            select p from Precedente p
            where p.fonte = :fonte
              and p.tipo = :tipo
              and p.identificador = :identificador
            order by case when p.dataPublicacao is null then 1 else 0 end, p.dataPublicacao desc, p.id desc
            """)
    List<Precedente> findByFonteAndTipoAndIdentificadorOrderByDataPublicacaoDescIdDesc(@Param("fonte") TribunalFonte fonte,
                                                                                        @Param("tipo") TipoPrecedente tipo,
                                                                                        @Param("identificador") String identificador,
                                                                                        Pageable pageable);

    default Precedente findFirstByFonteAndTipoAndIdentificador(TribunalFonte fonte, TipoPrecedente tipo, String identificador) {
        return findByFonteAndTipoAndIdentificadorOrderByDataPublicacaoDescIdDesc(fonte, tipo, identificador, PageRequest.of(0, 1)).stream().findFirst().orElse(null);
    }

    @Query("""
            select p from Precedente p
            where (:fonte is null or p.fonte = :fonte)
              and (:tipo is null or p.tipo = :tipo)
              and (:ramo is null or p.ramoSugerido = :ramo)
              and (:rito is null or p.ritoSugerido = :rito)
              and (:q is null or lower(p.titulo) like lower(concat('%',:q,'%')) or lower(p.tese) like lower(concat('%',:q,'%')))
            order by case when p.dataPublicacao is null then 1 else 0 end, p.dataPublicacao desc, p.id desc
            """)
    Page<Precedente> search(@Param("fonte") TribunalFonte fonte,
                           @Param("tipo") TipoPrecedente tipo,
                           @Param("ramo") RamoDireito ramo,
                           @Param("rito") RitoProcessual rito,
                           @Param("q") String q,
                           Pageable pageable);
}
