package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionDescriptor;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(PjbFlowItBaseEsperaGravacaoAssincronaIT.ConferenciaDepoisDaLimpeza.class)
class PjbFlowItBaseEsperaGravacaoAssincronaIT extends PjbFlowItBase {

    private static final String ACAO = "GRAVACAO_ASSINCRONA_TARDIA";

    @Autowired
    private PjbExecutionOrchestrator orchestrator;

    @Autowired
    private AuditLedgerService auditLedgerService;

    @Test
    void gravacaoAssincronaQueTerminaDepoisDoTesteNaoSobreviveALimpeza() {
        orchestrator.run(PjbExecutionDescriptor.io("teste.gravacao-assincrona-tardia", Duration.ofSeconds(10)), () -> {
            try {
                Thread.sleep(700L);
            } catch (InterruptedException interrompido) {
                Thread.currentThread().interrupt();
                return;
            }
            auditLedgerService.appendSafely(ACAO, "TESTE", "1");
        });
    }

    static class ConferenciaDepoisDaLimpeza implements AfterEachCallback {

        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            Thread.sleep(1500L);
            JdbcTemplate jdbcTemplate = SpringExtension.getApplicationContext(context).getBean(JdbcTemplate.class);
            Integer restantes = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM pjb_audit_ledger WHERE action = ?", Integer.class, ACAO);
            assertThat(restantes)
                    .as("a gravacao assincrona terminou depois do @AfterEach e sobreviveu ao TRUNCATE")
                    .isZero();
        }
    }
}
