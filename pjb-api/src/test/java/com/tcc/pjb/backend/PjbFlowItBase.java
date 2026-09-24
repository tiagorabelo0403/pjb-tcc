package com.tcc.pjb.backend;

import com.tcc.pjb.backend.platform.runtime.PjbBoundedExecutorService;
import com.tcc.pjb.backend.service.competencia.UnidadesJudiciariasAlteradasEvent;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Base para flow ITs (RANDOM_PORT / requisição HTTP real).
 * Não usa @Transactional — a requisição HTTP roda em thread/conexão separada,
 * fora de qualquer TX do teste. Rollback transacional não isola o que a app comita.
 *
 * <p>Isolamento por TRUNCATE CASCADE autodescoberto no @BeforeEach e no @AfterEach: cada teste
 * entra com banco limpo, e o que ele gravou de forma síncrona é apagado ao sair. A descoberta usa
 * pg_tables filtrando filhas de partição via pg_inherits — zero lista manual de tabelas, zero
 * divergência com futuras migrations.
 *
 * <p>Exceção deliberada: catálogos semeados pelo Flyway e nunca escritos pelo fluxo em
 * teste ({@code tb_jurisdicao_territorial}, {@code tb_jurisdicao_territorial_unidade},
 * {@code tb_tribunal} e {@code tb_comarca}, os dois últimos semeados por V319 a partir do
 * catálogo territorial) ficam fora do TRUNCATE. Sem forkCount/reuseForks configurado no pom, Failsafe roda todas
 * as ITs do lote na mesma JVM/mesmo banco ({@link PjbIntegrationTestBase}); truncar esses
 * catálogos aqui os apaga para o resto do fork sem repor via nova migration, quebrando ITs
 * que dependem deles (ex.: {@code Trt7CearaJurisdicaoCargaIT}) só quando rodadas em lote
 * amplo — nunca isoladas. As classes desta base que gravam nesses catálogos apagam o que gravaram
 * em @AfterEach; o {@code test_isolation_guard} reprova a que não apaga.
 *
 * <p>Premissa de segurança do TRUNCATE (ACCESS EXCLUSIVE):
 * O perfil integration-test desabilita schedulers ({@code spring.task.scheduling.enabled=false}),
 * pg-listen ({@code pjb.jobs.pg-listen.enabled=false}) e dispatcher
 * ({@code pjb.jobs.dispatcher.enabled=false}). Entre testes, nenhum TX de background
 * fica aberto — o TRUNCATE não encontra bloqueio. Se algum IT habilitar job de background
 * no perfil integration-test, o TRUNCATE pode travar aguardando o lock ACCESS EXCLUSIVE.
 * Manter os jobs desabilitados é pré-requisito para herdar esta base. Gravações assíncronas
 * disparadas pela própria requisição, como a auditoria em {@code runInNewTransaction}, rodam nos
 * executores limitados da plataforma: antes de truncar, a base espera esses executores ficarem sem
 * tarefa ativa.
 */
public abstract class PjbFlowItBase extends PjbIntegrationTestBase {

    private static final Duration ESPERA_DE_TAREFAS_ASSINCRONAS = Duration.ofSeconds(30);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private Map<String, PjbBoundedExecutorService> executoresDaPlataforma;

    @BeforeEach
    void truncateDatabaseBeforeEach() {
        truncateAllTrackedTables();
    }

    @AfterEach
    void truncateDatabaseAfterEach() {
        executoresDaPlataforma.forEach((nome, executor) -> {
            if (!executor.awaitQuiescence(ESPERA_DE_TAREFAS_ASSINCRONAS)) {
                throw new IllegalStateException("Executor " + nome + " ainda tem tarefa ativa depois de "
                        + ESPERA_DE_TAREFAS_ASSINCRONAS + "; truncar agora apagaria o banco sob uma gravacao em curso.");
            }
        });
        truncateAllTrackedTables();
    }

    /**
     * Nunca duplicar esta query: a exclusão dos catálogos Flyway acima é o próprio fix de
     * {@code D-testes-it-contaminacao-em-lote-amplo-service-package}, e uma cópia divergente
     * reintroduz o vazamento.
     */
    private void truncateAllTrackedTables() {
        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT t.tablename
                FROM pg_tables t
                WHERE t.schemaname = 'public'
                  AND t.tablename NOT IN (
                      'flyway_schema_history',
                      'tb_jurisdicao_territorial',
                      'tb_jurisdicao_territorial_unidade',
                      'tb_tribunal',
                      'tb_comarca'
                  )
                  AND t.tablename NOT IN (
                      SELECT c.relname FROM pg_inherits i
                      JOIN pg_class c ON i.inhrelid = c.oid
                      JOIN pg_namespace n ON c.relnamespace = n.oid
                      WHERE n.nspname = 'public'
                  )
                """,
                String.class);
        if (!tables.isEmpty()) {
            String tableList = String.join(", ", tables.stream().map(t -> "\"" + t + "\"").toList());
            jdbcTemplate.execute("TRUNCATE " + tableList + " RESTART IDENTITY CASCADE");
        }
        eventPublisher.publishEvent(new UnidadesJudiciariasAlteradasEvent());
    }
}
