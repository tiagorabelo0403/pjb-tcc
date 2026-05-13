package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.enums.PapelEquipe;

@Repository
public interface MembroEquipeRepository extends
        JpaRepository<MembroEquipe, Long>,
        JpaSpecificationExecutor<MembroEquipe> {

    Optional<MembroEquipe> findByUsuario_IdAndEquipe_Id(Long usuarioId, Long equipeId);

    List<MembroEquipe> findByEquipe_Id(Long equipeId);

    List<MembroEquipe> findByUsuario_Id(Long usuarioId);

    List<MembroEquipe> findByEquipe_IdAndAtivoTrue(Long equipeId);


    boolean existsByUsuario_IdAndEquipe_IdAndAtivoTrue(Long usuarioId, Long equipeId);
    boolean existsByUsuario_IdAndEquipe_Id(Long usuarioId, Long equipeId);

    long countByEquipe_Id(Long equipeId);

    long countByEquipe_IdAndAtivoTrue(Long equipeId);

    List<MembroEquipe> findByPapel(PapelEquipe papel);

    List<MembroEquipe> findByEquipe_IdAndPapel(Long equipeId, PapelEquipe papel);

    @Query("SELECT m FROM MembroEquipe m JOIN FETCH m.usuario WHERE m.equipe.id = :equipeId")
    List<MembroEquipe> carregarComUsuario(Long equipeId);

    @Query("SELECT m FROM MembroEquipe m JOIN FETCH m.equipe WHERE m.usuario.id = :usuarioId")
    List<MembroEquipe> carregarComEquipe(Long usuarioId);

    @Query("SELECT m FROM MembroEquipe m WHERE LOWER(m.usuario.nome) LIKE LOWER(CONCAT('%', :nome, '%'))")
    List<MembroEquipe> pesquisarPorNomeUsuario(String nome);

    @Query("SELECT m FROM MembroEquipe m WHERE LOWER(m.cargo) LIKE LOWER(CONCAT('%', :cargo, '%'))")
    List<MembroEquipe> pesquisarPorCargo(String cargo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE MembroEquipe m SET m.ativo = false WHERE m.id = :id")
    int desativar(Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE MembroEquipe m SET m.ativo = true WHERE m.id = :id")
    int ativar(Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM MembroEquipe m WHERE m.equipe.id = :equipeId AND m.usuario.id = :usuarioId")
    int removerDaEquipe(Long equipeId, Long usuarioId);

    @Query("""
            SELECT m.papel, COUNT(m)
            FROM MembroEquipe m
            GROUP BY m.papel
            ORDER BY COUNT(m) DESC
           """)
    List<Object[]> estatisticasPorPapel();

    @Query("""
            SELECT m.equipe.id, COUNT(m)
            FROM MembroEquipe m
            GROUP BY m.equipe.id
            ORDER BY COUNT(m) DESC
           """)
    List<Object[]> rankingEquipesPorMembros();

    @Query("SELECT m FROM MembroEquipe m ORDER BY m.dataAdmissao DESC")
    List<MembroEquipe> listarMaisRecentes();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM MembroEquipe m WHERE m.id = :id")
    int excluirPorId(Long id);
}
