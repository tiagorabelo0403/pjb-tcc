package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.core.infra.spring.SpringContext;
import com.tcc.pjb.backend.core.security.crypto.UsuarioBlindIndexService;
import com.tcc.pjb.backend.model.entity.Processo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Resolve {@link UsuarioBlindIndexService} sob demanda (via {@link SpringContext}), pelo mesmo
 * motivo de {@link UsuarioRepositoryImpl}: não acoplar a simples criação do bean {@code Processo
 * Repository} a beans de criptografia ausentes em fatias de teste estreitas.
 */
@Component
public class ProcessoRepositoryImpl implements ProcessoRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Processo> findAllByPartesCpf(String cpf) {
        String cpfHash = SpringContext.getBean(UsuarioBlindIndexService.class).hashCpf(cpf);
        return entityManager.createQuery("""
                        SELECT DISTINCT p FROM Processo p
                        LEFT JOIN FETCH p.usuario u
                        LEFT JOIN FETCH p.jurisdicao
                        WHERE p.parteAutoraCpf = :cpf OR p.parteReuCpf = :cpf
                           OR (:cpfHash IS NOT NULL AND u.cpfHash = :cpfHash)
                        """, Processo.class)
                .setParameter("cpf", cpf)
                .setParameter("cpfHash", cpfHash)
                .getResultList();
    }
}
