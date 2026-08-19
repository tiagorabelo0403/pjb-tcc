package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.enums.FuncaoServidorJudiciario;
import com.tcc.pjb.backend.model.entity.servidor.FuncaoServidorJudiciarioEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FuncaoServidorJudiciarioRepository
        extends JpaRepository<FuncaoServidorJudiciarioEntity, Long> {

    List<FuncaoServidorJudiciarioEntity> findByUnidadeIdAndAtivo(Long unidadeId, boolean ativo);

    Optional<FuncaoServidorJudiciarioEntity> findByUsuarioIdAndUnidadeIdAndFuncaoAndAtivo(
            Long usuarioId, Long unidadeId, FuncaoServidorJudiciario funcao, boolean ativo);

    List<FuncaoServidorJudiciarioEntity> findByUsuarioIdAndUnidadeIdAndAtivo(
            Long usuarioId, Long unidadeId, boolean ativo);
}
