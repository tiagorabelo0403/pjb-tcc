package com.tcc.pjb.backend.service.ajuizamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.PjbFlowItBase;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.outbox.OutboxEvent;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.repository.outbox.OutboxEventRepository;
import com.tcc.pjb.backend.service.AjuizamentoService;
import com.tcc.pjb.backend.service.exception.ErroDeTetoException;
import com.tcc.pjb.backend.service.triagem.TriagemNacionalIAEngine;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
        "spring.cache.type=none",
        "pjb.workflow.enabled=false",
        "pjb.outbox.ingress.enabled=false"
})
class AjuizamentoServiceFlowIT extends PjbFlowItBase {

    @Autowired
    private AjuizamentoService ajuizamentoService;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private WorkItemRepository workItemRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private TriagemNacionalIAEngine triagemNacionalIAEngine;

    @AfterAll
    void truncateAfterAll() {
        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT t.tablename
                FROM pg_tables t
                WHERE t.schemaname = 'public'
                  AND t.tablename NOT IN ('flyway_schema_history')
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
    }

    @Test
    void deveAjuizarEmitirOutboxECriarFilaDeRevisaoDaSecretariaQuandoHouverPendenciaInicial() {
        Processo processo = novoProcessoBase("AJUI-IT-2026-0001", "0008101-11.2026.8.06.0001");
        processo.setParteReuNome(null);
        processo.setParteReuCpf(null);

        Processo salvo = ajuizamentoService.ajuizar(processo);

        Processo persistido = awaitAtMost(
                "processo ajuizado com snapshot inicial consolidado",
                () -> processoRepository.findById(salvo.getId()).orElse(null),
                value -> value != null
                        && notBlank(value.getTribunalCodigoRoteado())
                        && notBlank(value.getUnidadeJudiciariaCodigo())
                        && notBlank(value.getPreProtocoloStatus())
                        && value.getStatusProcesso() != null
        );

        assertThat(persistido.getTipoJustica()).isEqualTo(TipoJustica.ESTADUAL);
        assertThat(persistido.getStatusProcesso()).isIn(
                StatusProcesso.DISTRIBUIDO,
                StatusProcesso.EM_ANDAMENTO,
                StatusProcesso.CITACAO_REALIZADA
        );

        List<OutboxEvent> outboxEvents = awaitAtMost(
                "evento de outbox do ajuizamento",
                () -> outboxEventRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc("Processo", String.valueOf(salvo.getId())),
                value -> value != null && !value.isEmpty()
        );

        assertThat(outboxEvents).anySatisfy(event -> {
            assertThat(event.getEventType()).isEqualTo(AjuizamentoService.EVT_PROCESSO_CRIADO);
            assertThat(event.getRoutingKey()).isEqualTo("processo:" + salvo.getId());
            assertThat(event.getAggregateType()).isEqualTo("Processo");
            assertThat(event.getAggregateId()).isEqualTo(String.valueOf(salvo.getId()));
            assertThat(event.getPayloadJson()).contains("\"processoId\":" + salvo.getId());
        });

        var workItems = awaitAtMost(
                "work item de revisão inicial da secretaria",
                () -> workItemRepository.findAllByProcesso(salvo.getId()),
                value -> value != null && !value.isEmpty()
        );

        assertThat(workItems).anySatisfy(item -> {
            assertThat(item.getTemplateCode()).isEqualTo("AJUIZAMENTO:REVISAO_SECRETARIA:" + salvo.getId());
            assertThat(item.getType()).isEqualTo(WorkItemType.AJUIZAMENTO);
            assertThat(item.getQueueCode()).isEqualTo("SECRETARIA_AJUIZAMENTO_INICIAL");
            assertThat(item.getAssignedRole()).isEqualTo(TipoUsuario.SERVIDOR_FORUM);
            assertThat(item.getStatus()).isEqualTo(WorkItemStatus.PENDENTE);
            assertThat(item.isBlocking()).isFalse();
            assertThat(item.getDescricao()).contains("Réu sem qualificação mínima");
        });
    }

    @Test
    void deveCriarConclusaoInicialAoGabineteQuandoAjuizamentoJaEstiverSaneado() {
        Processo processo = novoProcessoBase("AJUI-IT-2026-0002", "0008102-22.2026.8.06.0001");
        processo.setParteReuNome("Banco do Sertão S.A.");
        processo.setParteReuCpf("00000000000");
        processo.setTribunalCodigoRoteado("TJCE");
        processo.setUnidadeJudiciariaCodigo("VARA-CIV-001");
        processo.setPreProtocoloStatus("APTA");
        processo.setCompetenciaTerritorialModo("DOMICILIO_REU");
        processo.setPreventionMode("SEM_PREVENCAO");
        processo.setLinkageMode("SEM_CONEXAO");
        processo.setConnectorSubmissionStatus("SINCRONIZADO");

        Processo salvo = ajuizamentoService.ajuizar(processo);

        Processo persistido = awaitAtMost(
                "processo distribuído para conclusão inicial",
                () -> processoRepository.findById(salvo.getId()).orElse(null),
                value -> value != null && value.getStatusProcesso() != null
        );

        assertThat(persistido.getStatusProcesso()).isIn(
                StatusProcesso.DISTRIBUIDO,
                StatusProcesso.EM_ANDAMENTO,
                StatusProcesso.CITACAO_REALIZADA
        );

        var workItems = awaitAtMost(
                "work item de conclusão inicial ao gabinete",
                () -> workItemRepository.findAllByProcesso(salvo.getId()),
                value -> value != null && !value.isEmpty()
        );

        assertThat(workItems).anySatisfy(item -> {
            assertThat(item.getTemplateCode()).isEqualTo("AJUIZAMENTO:CONCLUSAO_INICIAL:" + salvo.getId());
            assertThat(item.getType()).isEqualTo(WorkItemType.DESPACHO);
            assertThat(item.getQueueCode()).isEqualTo("GABINETE_AJUIZAMENTO_INICIAL");
            assertThat(item.getAssignedRole()).isEqualTo(TipoUsuario.JUIZ);
            assertThat(item.getStatus()).isEqualTo(WorkItemStatus.PENDENTE);
            assertThat(item.isBlocking()).isFalse();
            assertThat(item.getDescricao()).contains("despacho inaugural");
        });
    }

    @Test
    void deveBloquearAjuizamentoDeJuizadoComValorAcimaDaAlcadaSemPersistirProcessoNemOutbox() {
        long outboxAntes = outboxEventRepository.count();
        long processosAntes = processoRepository.count();

        Processo processo = novoProcessoBase("AJUI-IT-2026-0003", "0008103-33.2026.8.06.0001");
        processo.setClasseProcessual("Juizado Especial Cível");
        processo.setRito(RitoProcessual.JUIZADO_ESPECIAL_CIVEL);
        processo.setValorCausa(new BigDecimal("1000000.00"));

        assertThatThrownBy(() -> ajuizamentoService.ajuizar(processo))
                .isInstanceOf(ErroDeTetoException.class)
                .hasMessageContaining("alçada");

        assertThat(processoRepository.findByNumeroProcesso("AJUI-IT-2026-0003")).isEmpty();
        assertThat(processoRepository.count()).isEqualTo(processosAntes);
        assertThat(outboxEventRepository.count()).isEqualTo(outboxAntes);
    }

    private Processo novoProcessoBase(String numeroProcesso, String numeroUnificado) {
        return Processo.builder()
                .numeroProcesso(numeroProcesso)
                .numeroUnificado(numeroUnificado)
                .tipoJustica(TipoJustica.ESTADUAL)
                .ramoDireito(RamoDireito.CIVIL)
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .classeProcessual("Ação de cobrança")
                .assunto("Cobrança contratual")
                .pedidoPrincipal("Condenação ao pagamento da dívida contratual")
                .parteAutoraNome("Maria da Silva")
                .parteAutoraCpf("12345678909")
                .tribunal("TJCE")
                .comarca("Fortaleza")
                .vara("2ª Vara Cível")
                .uf("CE")
                .valorCausa(new BigDecimal("12000.00"))
                .nivelSigilo(NivelSigilo.PUBLICO)
                .build();
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static <T> T awaitAtMost(String description, Supplier<T> supplier, Predicate<T> predicate) {
        long timeoutNanos = Duration.ofSeconds(5).toNanos();
        long deadline = System.nanoTime() + timeoutNanos;
        T current = supplier.get();
        while (!predicate.test(current)) {
            if (System.nanoTime() >= deadline) {
                throw new AssertionError("Timeout aguardando: " + description + " | valor atual=" + current);
            }
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrompido aguardando: " + description, ex);
            }
            current = supplier.get();
        }
        return current;
    }
}
