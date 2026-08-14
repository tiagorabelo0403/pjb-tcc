package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComarcaRepository extends JpaRepository<Comarca, Long> {

    @Query(value = "SELECT * FROM tb_comarca WHERE uf = :uf AND upper(unaccent(nome)) = upper(unaccent(:nome))",
            nativeQuery = true)
    Optional<Comarca> findByNomeAccentInsensitiveAndUf(@Param("nome") String nome, @Param("uf") String uf);

    @Query(value = "SELECT * FROM tb_comarca WHERE upper(unaccent(nome)) = upper(unaccent(:nome))",
            nativeQuery = true)
    List<Comarca> findAllByNomeAccentInsensitive(@Param("nome") String nome);
}
