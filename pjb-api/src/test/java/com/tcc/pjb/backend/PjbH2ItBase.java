package com.tcc.pjb.backend;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerRepository;
import com.tcc.pjb.backend.core.backfill.persistence.BackfillRunRepository;
import com.tcc.pjb.backend.core.jobs.persistence.repo.JobRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.advocacia.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * PjbH2ItBase — isolamento mínimo de contexto H2 para o módulo advocacia enquanto as
 * classes filhas rodam em H2 sem Flyway (spring.profiles.active=test, ddl-auto=create-drop).
 * PONTE até a D23 (migração H2 → Testcontainers Postgres + PjbFlowItBase). REMOVER quando a
 * D23 migrar essas classes. NÃO estender para novos ITs — novos ITs usam PjbFlowItBase (Postgres).
 */
public abstract class PjbH2ItBase {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AuditLedgerRepository auditLedgerRepository;

    @Autowired
    private BackfillRunRepository backfillRunRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void cleanH2Context() {
        clienteRepository.deleteAll();
        auditLedgerRepository.deleteAll();
        backfillRunRepository.deleteAll();
        jobRepository.deleteAll();
        usuarioRepository.deleteAll();
    }
}
