package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.Usuario;
import java.util.Optional;

/**
 * {@code cpf}/{@code email} de {@link Usuario} são criptografados (IV aleatório, não comparáveis em
 * {@code WHERE}); a implementação real busca pelo índice cego. Mantido como interface separada (não
 * declarado diretamente em {@link UsuarioRepository}) só para não colidir com a tentativa do Spring
 * Data de gerar uma query derivada contra a coluna {@code cpf}/{@code email} em texto puro.
 */
public interface UsuarioRepositoryCustom {

    Optional<Usuario> findByCpf(String cpf);

    Optional<Usuario> findByEmail(String email);
}
