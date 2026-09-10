package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.core.security.crypto.UsuarioBlindIndexService;
import com.tcc.pjb.backend.model.entity.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * {@link UsuarioBlindIndexService} entra por {@link ObjectProvider} e é resolvido só na chamada:
 * {@code UsuarioRepository} é injetado em fatias {@code @DataJpaTest} que não carregam beans
 * {@code @Service} gerais, e uma dependência de construtor direta quebraria a criação do bean
 * mesmo em teste que nunca chame {@code findByCpf}/{@code findByEmail}.
 *
 * <p>O provider preserva esse adiamento e, diferente de um holder estático de contexto, resolve
 * sempre no contexto dono do repositório. Sob Failsafe várias ITs compartilham a mesma JVM com
 * contextos e chaves mestras distintas; resolver por global estático faz o índice cego ser
 * calculado com a chave de outro contexto. Ver {@code D-springcontext-estatico-no-conversor-de-pii}.
 */
@Component
public class UsuarioRepositoryImpl implements UsuarioRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectProvider<UsuarioBlindIndexService> blindIndexProvider;

    public UsuarioRepositoryImpl(ObjectProvider<UsuarioBlindIndexService> blindIndexProvider) {
        this.blindIndexProvider = blindIndexProvider;
    }

    @Override
    public Optional<Usuario> findByCpf(String cpf) {
        String hash = blindIndexProvider.getObject().hashCpf(cpf);
        if (hash == null) {
            return Optional.empty();
        }
        try {
            Usuario u = entityManager.createQuery("SELECT u FROM Usuario u WHERE u.cpfHash = :hash", Usuario.class)
                    .setParameter("hash", hash)
                    .getSingleResult();
            return Optional.of(u);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Usuario> findByEmail(String email) {
        String hash = blindIndexProvider.getObject().hashEmail(email);
        if (hash == null) {
            return Optional.empty();
        }
        try {
            Usuario u = entityManager.createQuery("SELECT u FROM Usuario u WHERE u.emailHash = :hash", Usuario.class)
                    .setParameter("hash", hash)
                    .getSingleResult();
            return Optional.of(u);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}
