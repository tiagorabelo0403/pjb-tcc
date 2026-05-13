package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    
    Optional<Usuario> findByEmail(String email);

    
    Optional<Usuario> findByCpf(String cpf);

    
    Optional<Usuario> findByOabNormalizada(String oabNormalizada);

    
    boolean existsByOabNormalizada(String oabNormalizada);

    
    List<Usuario> findByPerfil(String perfil);

    
    List<Usuario> findByTipoUsuario(TipoUsuario tipoUsuario);

    
    List<Usuario> findByComarcaAndAtivoTrue(String comarca);

    Page<Usuario> findByAtivoTrue(Pageable pageable);

}
