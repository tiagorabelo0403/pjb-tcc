package com.tcc.pjb.backend.core.security.geofence;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.modules.suporte.entity.SupportTicketCategoria;
import com.tcc.pjb.backend.modules.suporte.event.SupportTicketResolvedEvent;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@ActiveProfiles("test")
@Import(SupportTicketTravelExceptionListener.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SupportTicketTravelExceptionListenerPersistenciaTest {

    private static final long TICKET_ID = 987_654L;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JudgeTravelExceptionRepository repository;

    @AfterEach
    void limpar() {
        repository.findAll().stream()
                .filter(excecao -> Long.valueOf(TICKET_ID).equals(excecao.getTicketOrigemId()))
                .forEach(repository::delete);
    }

    @Test
    void excecaoDeViagemAprovadaFicaGravadaDepoisDoCommitDoChamado() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> publisher.publishEvent(
                new SupportTicketResolvedEvent(TICKET_ID, SupportTicketCategoria.EXCECAO_VIAGEM_CARREIRA_JURIDICA,
                        true, 5L, "DF", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10))));

        assertThat(repository.findAll())
                .filteredOn(excecao -> Long.valueOf(TICKET_ID).equals(excecao.getTicketOrigemId()))
                .singleElement()
                .satisfies(excecao -> {
                    assertThat(excecao.getUsuarioId()).isEqualTo(5L);
                    assertThat(excecao.getUfOuPaisDestino()).isEqualTo("DF");
                });
    }
}
