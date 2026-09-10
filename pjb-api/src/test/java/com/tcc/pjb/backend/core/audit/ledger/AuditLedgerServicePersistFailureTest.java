package com.tcc.pjb.backend.core.audit.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class AuditLedgerServicePersistFailureTest {

    private AuditLedgerRepository repository;
    private SimpleMeterRegistry registry;
    private AuditLedgerService service;
    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        repository = mock(AuditLedgerRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.getOrNull()).thenReturn(null);
        registry = new SimpleMeterRegistry();
        service = new AuditLedgerService(repository, currentUserService, registry);

        logger = (Logger) LoggerFactory.getLogger(AuditLedgerService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    private double falhasRegistradas() {
        var counter = registry.find("pjb.audit_ledger.persist_failures").counter();
        return counter == null ? 0.0 : counter.count();
    }

    @Test
    void falhaAoPersistirIncrementaOContadorDeFalhas() {
        when(repository.save(any())).thenThrow(new RuntimeException("banco indisponivel"));

        service.appendSafely("PROCESSO_ARQUIVADO", "PROCESSO", "0001234-55.2026.8.06.0001");

        assertThat(falhasRegistradas())
                .as("contador existia mas nunca era incrementado, entao a metrica lia zero mesmo perdendo entrada")
                .isEqualTo(1.0);
    }

    @Test
    void falhaAoPersistirNaoPropagaParaOChamador() {
        when(repository.save(any())).thenThrow(new RuntimeException("banco indisponivel"));

        assertThatCode(() -> service.appendSafely("PROCESSO_ARQUIVADO", "PROCESSO", "0001234-55.2026.8.06.0001"))
                .as("contrato de nunca lancar e o que sustenta os 425 pontos de chamada")
                .doesNotThrowAnyException();
    }

    @Test
    void falhaRegistraIdentidadeSuficienteParaReconstituirOElo() {
        when(repository.save(any())).thenThrow(new RuntimeException("banco indisponivel"));

        service.appendSafely("PROCESSO_ARQUIVADO", "PROCESSO", "0001234-55.2026.8.06.0001", "hash-de-payload");

        assertThat(appender.list)
                .filteredOn(e -> e.getLevel() == Level.ERROR)
                .isNotEmpty();
        String mensagem = appender.list.stream()
                .filter(e -> e.getLevel() == Level.ERROR)
                .findFirst()
                .orElseThrow()
                .getFormattedMessage();
        assertThat(mensagem)
                .contains("AUDIT_LEDGER_PERSIST_FAILURE")
                .contains("PROCESSO_ARQUIVADO")
                .contains("0001234-55.2026.8.06.0001")
                .contains("hash-de-payload");
    }

    @Test
    void falhaEhRegistradaComoErroNaoComoAviso() {
        when(repository.save(any())).thenThrow(new RuntimeException("banco indisponivel"));

        service.appendSafely("PROCESSO_ARQUIVADO", "PROCESSO", "0001234-55.2026.8.06.0001");

        assertThat(appender.list)
                .as("entrada perdida em ledger encadeado por hash quebra o elo; nivel WARN subestima")
                .anyMatch(e -> e.getLevel() == Level.ERROR);
    }

    @Test
    void persistenciaBemSucedidaNaoIncrementaContadorNemRegistraErro() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.appendSafely("PROCESSO_ARQUIVADO", "PROCESSO", "0001234-55.2026.8.06.0001");

        assertThat(falhasRegistradas()).isZero();
        assertThat(appender.list).noneMatch(e -> e.getLevel() == Level.ERROR);
    }
}
