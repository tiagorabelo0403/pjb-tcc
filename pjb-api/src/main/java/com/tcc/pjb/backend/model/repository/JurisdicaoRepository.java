package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.dto.projections.JurisdicaoContextProjection;
import com.tcc.pjb.backend.model.dto.projections.JurisdicaoResumoProjection;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;

@Repository
public interface JurisdicaoRepository extends JpaRepository<Jurisdicao, Long>, JpaSpecificationExecutor<Jurisdicao> {

    
    
    

    Optional<Jurisdicao> findBySiglaIgnoreCase(String sigla);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsBySiglaIgnoreCase(String sigla);

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<Jurisdicao> findByCodigo(String codigo);

    Optional<Jurisdicao> findBySigla(String sigla);

    
    
    

    @Query("""
        SELECT j FROM Jurisdicao j
        WHERE j.ativo = true
        AND (
            LOWER(j.nome) LIKE LOWER(CONCAT('%', :termo, '%'))
            OR LOWER(j.sigla) LIKE LOWER(CONCAT('%', :termo, '%'))
            OR j.codigo LIKE CONCAT('%', :termo, '%')
        )
    """)
    Page<Jurisdicao> pesquisar(@Param("termo") String termo, Pageable pageable);

    @Query("SELECT j FROM Jurisdicao j WHERE j.estado = :uf AND j.ativo = true")
    List<Jurisdicao> findByUf(@Param("uf") String uf);

    
    
    

    Page<Jurisdicao> findByMateriaAndAtivoTrue(MateriaJurisdicao materia, Pageable pageable);

    Slice<Jurisdicao> findByEsferaAndAtivoTrue(EsferaJurisdicao esfera, Pageable pageable);

    List<Jurisdicao> findByGrauAndMateriaAndAtivoTrue(GrauJurisdicao grau, MateriaJurisdicao materia);

    
    
    

    @Query("SELECT j FROM Jurisdicao j WHERE j.recursoPara.id = :idPai AND j.ativo = true")
    List<Jurisdicao> findFilhas(@Param("idPai") Long idJurisdicaoSuperior);

    long countByRecursoPara_IdAndAtivoTrue(Long idJurisdicaoSuperior);

    
    
    

    @Query("SELECT j.id as id, j.nome as nome, j.sigla as sigla FROM Jurisdicao j WHERE j.ativo = true")
    List<JurisdicaoResumoProjection> listarResumosAtivos();

    
    
    

    @Query("""
        SELECT j.id as id, j.nome as nome, j.sigla as sigla, j.materia as materia
        FROM Jurisdicao j
        WHERE j.ativo = true
          AND j.esfera = :esfera
          AND j.grau = :grau
          AND UPPER(j.estado) = UPPER(:uf)
          AND LOWER(j.comarca) = LOWER(:comarca)
        ORDER BY j.nome ASC
    """)
    List<JurisdicaoContextProjection> listarContextoPorPrimeiroGrau(
            @Param("esfera") EsferaJurisdicao esfera,
            @Param("grau") GrauJurisdicao grau,
            @Param("uf") String uf,
            @Param("comarca") String comarca
    );

    @Query("""
        SELECT j.id as id, j.nome as nome, j.sigla as sigla, j.materia as materia
        FROM Jurisdicao j
        WHERE j.ativo = true
          AND j.esfera = :esfera
          AND j.grau = :grau
          AND UPPER(j.estado) = UPPER(:uf)
        ORDER BY j.nome ASC
    """)
    List<JurisdicaoContextProjection> listarContextoPorSegundoGrau(
            @Param("esfera") EsferaJurisdicao esfera,
            @Param("grau") GrauJurisdicao grau,
            @Param("uf") String uf
    );

    @Query("""
        SELECT j.id as id, j.nome as nome, j.sigla as sigla, j.materia as materia
        FROM Jurisdicao j
        WHERE j.ativo = true
          AND j.esfera = :esfera
          AND j.grau = :grau
        ORDER BY j.nome ASC
    """)
    List<JurisdicaoContextProjection> listarContextoPorSuperior(
            @Param("esfera") EsferaJurisdicao esfera,
            @Param("grau") GrauJurisdicao grau
    );

    @Query("""
        SELECT j.id as id, j.nome as nome, j.sigla as sigla
        FROM Jurisdicao j
        WHERE j.ativo = true
          AND j.esfera = :esfera
          AND j.grau = :grau
          AND UPPER(j.estado) = UPPER(:uf)
          AND LOWER(j.comarca) = LOWER(:comarca)
        ORDER BY j.nome ASC
    """)
    List<JurisdicaoResumoProjection> listarResumosPorContexto(
            @Param("esfera") EsferaJurisdicao esfera,
            @Param("grau") GrauJurisdicao grau,
            @Param("uf") String uf,
            @Param("comarca") String comarca
    );

    @Query("""
        SELECT j.id as id, j.nome as nome, j.sigla as sigla
        FROM Jurisdicao j
        WHERE j.ativo = true
          AND j.esfera = :esfera
          AND j.grau = :grau
          AND UPPER(j.estado) = UPPER(:uf)
        ORDER BY j.nome ASC
    """)
    List<JurisdicaoResumoProjection> listarResumosPorUfEsferaGrau(
            @Param("esfera") EsferaJurisdicao esfera,
            @Param("grau") GrauJurisdicao grau,
            @Param("uf") String uf
    );

    @Query("""
        SELECT j.id as id, j.nome as nome, j.sigla as sigla
        FROM Jurisdicao j
        WHERE j.ativo = true
          AND j.esfera = :esfera
          AND j.grau = :grau
        ORDER BY j.nome ASC
    """)
    List<JurisdicaoResumoProjection> listarResumosPorEsferaGrau(
            @Param("esfera") EsferaJurisdicao esfera,
            @Param("grau") GrauJurisdicao grau
    );

    
    
    

    @Query("""
        SELECT j.id as id, j.nome as nome, j.sigla as sigla, j.materia as materia
        FROM Jurisdicao j
        WHERE j.ativo = true
          AND j.esfera = :esfera
          AND j.grau = :grau
          AND UPPER(j.estado) = UPPER(:uf)
          AND LOWER(j.comarca) = LOWER(:comarca)
        ORDER BY j.nome ASC
    """)
    List<JurisdicaoContextProjection> listarContextoPorEsferaGrauUfComarca(
            @Param("esfera") EsferaJurisdicao esfera,
            @Param("grau") GrauJurisdicao grau,
            @Param("uf") String uf,
            @Param("comarca") String comarca
    );

    @Query("""
        SELECT j.id as id, j.nome as nome, j.sigla as sigla, j.materia as materia
        FROM Jurisdicao j
        WHERE j.ativo = true
          AND j.esfera = :esfera
          AND j.grau = :grau
          AND UPPER(j.estado) = UPPER(:uf)
        ORDER BY j.nome ASC
    """)
    List<JurisdicaoContextProjection> listarContextoPorEsferaGrauUf(
            @Param("esfera") EsferaJurisdicao esfera,
            @Param("grau") GrauJurisdicao grau,
            @Param("uf") String uf
    );

    @Query("""
        SELECT j.id as id, j.nome as nome, j.sigla as sigla, j.materia as materia
        FROM Jurisdicao j
        WHERE j.ativo = true
          AND j.esfera = :esfera
          AND j.grau = :grau
        ORDER BY j.nome ASC
    """)
    List<JurisdicaoContextProjection> listarContextoPorEsferaGrau(
            @Param("esfera") EsferaJurisdicao esfera,
            @Param("grau") GrauJurisdicao grau
    );

    
    
    

    @Query("SELECT j FROM Jurisdicao j WHERE j.materia = :materia")
    List<Jurisdicao> findByMateria(@Param("materia") MateriaJurisdicao materia);

    
    
    

    @Query("SELECT j.id FROM Jurisdicao j WHERE j.ativo = false")
    List<Long> findIdsInativos();

    @Query("SELECT j FROM Jurisdicao j WHERE j.id IN :ids")
    List<Jurisdicao> findAllByIds(@Param("ids") Set<Long> ids);
}