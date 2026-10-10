package com.tcc.pjb.backend.core.security.geofence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;

import com.tcc.pjb.backend.configs.datasource.ExecucaoEmContextoDeSistema;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.modules.suporte.entity.SupportTicketCategoria;
import com.tcc.pjb.backend.modules.suporte.event.SupportTicketResolvedEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@ActiveProfiles("test")
@Import({SupportTicketTravelExceptionListener.class, ExecucaoEmContextoDeSistema.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SupportTicketTravelExceptionListenerPersistenciaTest {

    private static final long TICKET_ID = 987_654L;

    private final Authentication tecnicoDeSuporte = new UsernamePasswordAuthenticationToken(
            "tecnico", "n/a", AuthorityUtils.createAuthorityList("ROLE_SUPORTE_TECNICO"));

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoSpyBean
    private JudgeTravelExceptionRepository repository;

    @MockitoBean
    private AuditLedgerService auditService;

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
        gravadasDoChamado().forEach(repository::delete);
    }

    @Test
    void excecaoDeViagemAprovadaPeloSuporteFicaGravadaEmContextoDeSistemaDepoisDoCommit() {
        List<Optional<Authentication>> atorNoSave = new ArrayList<>();
        doAnswer(chamada -> {
            atorNoSave.add(Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication()));
            return mockingDetails(repository).getMockCreationSettings().getDefaultAnswer().answer(chamada);
        }).when(repository).save(any(JudgeTravelException.class));
        SecurityContextHolder.getContext().setAuthentication(tecnicoDeSuporte);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> publisher.publishEvent(
                new SupportTicketResolvedEvent(TICKET_ID, SupportTicketCategoria.EXCECAO_VIAGEM_CARREIRA_JURIDICA,
                        true, 5L, "DF", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10))));

        List<JudgeTravelException> gravadas = gravadasDoChamado();
        assertThat(gravadas).singleElement().satisfies(excecao -> {
            assertThat(excecao.getUsuarioId()).isEqualTo(5L);
            assertThat(excecao.getUfOuPaisDestino()).isEqualTo("DF");
        });
        assertThat(atorNoSave)
                .as("a gravacao roda sem ator para a RLS de judge_travel_exception aplicar o contexto de sistema")
                .containsExactly(Optional.empty());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(tecnicoDeSuporte);
        verify(auditService).appendSafely("GEOFENCE_EXCECAO_VIAGEM_CONCEDIDA", "JUDGE_TRAVEL_EXCEPTION",
                String.valueOf(gravadas.getFirst().getId()));
    }

    private List<JudgeTravelException> gravadasDoChamado() {
        return repository.findAll().stream()
                .filter(excecao -> Long.valueOf(TICKET_ID).equals(excecao.getTicketOrigemId()))
                .toList();
    }
}
