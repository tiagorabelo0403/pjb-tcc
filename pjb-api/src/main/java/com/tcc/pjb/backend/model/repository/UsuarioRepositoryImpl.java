package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.core.infra.spring.SpringContext;
import com.tcc.pjb.backend.core.security.crypto.UsuarioBlindIndexService;
import com.tcc.pjb.backend.model.entity.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Resolve {@link UsuarioBlindIndexService} sob demanda (via {@link SpringContext}, não injeção no
 * construtor) de propósito: {@code UsuarioRepository} é injetado em fatias de teste estreitas
 * ({@code @DataJpaTest}) que não carregam beans {@code @Service} gerais. Se a dependência fosse de
 * construtor, a simples criação do bean {@code UsuarioRepository} quebraria qualquer teste desses,
 * mesmo um que nunca chame {@code findByCpf}/{@code findByEmail}. Mesma técnica já usada pelo
 * {@code @PrePersist} de {@link Usuario} e por {@code SensitiveDataConverter}.
 */
@Component
public class UsuarioRepositoryImpl implements UsuarioRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Usuario> findByCpf(String cpf) {
        String hash = SpringContext.getBean(UsuarioBlindIndexService.class).hashCpf(cpf);
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
        String hash = SpringContext.getBean(UsuarioBlindIndexService.class).hashEmail(email);
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
