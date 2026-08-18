package com.tcc.pjb.backend.core.audit.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("integration")
class AuditLedgerServicePayloadHashNuloIT extends PjbIntegrationTestBase {

    @Autowired
    private AuditLedgerService auditLedgerService;

    @Autowired
    private AuditLedgerRepository auditLedgerRepository;

    @Test
    void appendSafelyComPayloadHashNuloPersisteDeVerdadeContraPostgresReal() {
        assertThatCode(() -> auditLedgerService.appendSafely(
                "SALARIO_MINIMO_ATUALIZADO", "SALARIO_MINIMO_NACIONAL", "2027", null, "valorMensal=1700.00"
        )).doesNotThrowAnyException();

        AuditLedgerEntry persistido = auditLedgerRepository.findTopByOrderByIdDesc().orElseThrow();

        assertThat(persistido.getAction()).isEqualTo("SALARIO_MINIMO_ATUALIZADO");
        assertThat(persistido.getResourceId()).isEqualTo("2027");
        assertThat(persistido.getPayloadHash()).isNotNull().matches("^[0-9a-f]{64}$");
        assertThat(persistido.getEntryHash()).isNotNull();
    }
}
