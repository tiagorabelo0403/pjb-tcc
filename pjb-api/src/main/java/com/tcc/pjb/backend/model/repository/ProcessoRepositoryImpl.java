package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.core.security.crypto.UsuarioBlindIndexService;
import com.tcc.pjb.backend.model.entity.Processo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * {@link UsuarioBlindIndexService} entra por {@link ObjectProvider} pelo mesmo motivo de
 * {@link UsuarioRepositoryImpl}: não acoplar a criação do bean a beans de criptografia ausentes
 * em fatias de teste estreitas, e ainda assim resolver no contexto dono em vez de num holder
 * estático compartilhado por toda a JVM.
 */
@Component
public class ProcessoRepositoryImpl implements ProcessoRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectProvider<UsuarioBlindIndexService> blindIndexProvider;

    public ProcessoRepositoryImpl(ObjectProvider<UsuarioBlindIndexService> blindIndexProvider) {
        this.blindIndexProvider = blindIndexProvider;
    }

    @Override
    public List<Processo> findAllByPartesCpf(String cpf) {
        String cpfHash = blindIndexProvider.getObject().hashCpf(cpf);
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
