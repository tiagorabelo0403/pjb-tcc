package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.Equipe;

@Repository
public interface EquipeRepository extends
        JpaRepository<Equipe, Long>,
        JpaSpecificationExecutor<Equipe> {

    Optional<Equipe> findByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCase(String nome);

    List<Equipe> findByAtivoTrue();

    List<Equipe> findByAtivoFalse();

    long countByAtivoTrue();

    long countByAtivoFalse();

    @Query("SELECT e FROM Equipe e LEFT JOIN FETCH e.membros WHERE e.id = :id")
    Optional<Equipe> carregarComMembros(Long id);

    @Query("SELECT e FROM Equipe e LEFT JOIN FETCH e.membros WHERE LOWER(e.nome) = LOWER(:nome)")
    Optional<Equipe> carregarComMembrosPorNome(String nome);

    @Query("SELECT DISTINCT e FROM Equipe e WHERE LOWER(e.nome) LIKE LOWER(CONCAT('%', :termo, '%'))")
    List<Equipe> pesquisarPorTermo(String termo);

    @Query("SELECT COUNT(m) FROM Equipe e JOIN e.membros m WHERE e.id = :id")
    long contarMembros(Long id);

    @Query("""
            SELECT e.id, e.nome, COUNT(m)
            FROM Equipe e
            LEFT JOIN e.membros m
            GROUP BY e.id, e.nome
            ORDER BY COUNT(m) DESC
           """)
    List<Object[]> rankingPorQuantidadeDeMembros();

    @Query("SELECT e FROM Equipe e ORDER BY e.dataCriacao DESC")
    List<Equipe> listarMaisRecentes();

    @Query("SELECT e FROM Equipe e ORDER BY e.nome ASC")
    List<Equipe> listarOrdenadoPorNome();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Equipe e SET e.ativo = true WHERE e.id = :id")
    int ativar(Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Equipe e SET e.ativo = false WHERE e.id = :id")
    int desativar(Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Equipe e WHERE e.id = :id")
    int excluirPorId(Long id);
}
