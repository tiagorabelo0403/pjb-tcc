package com.tcc.pjb.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.AuditoriaEvento;
import com.tcc.pjb.backend.model.repository.AuditoriaEventoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuditoriaServiceTest {

    private final AuditoriaEventoRepository repository = mock(AuditoriaEventoRepository.class);
    private final AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);

    private AuditoriaService service;

    @BeforeEach
    void setUp() {
        service = new AuditoriaService(repository, auditLedgerService);
    }

    @Test
    void deveRegistrarEventoEDelegarAoLedger() {
        AuditoriaEvento saved = AuditoriaEvento.builder()
                .id(1L).usuario("juiz").acao("DESPACHAR").alvo("processo:42").build();
        when(repository.save(any())).thenReturn(saved);

        service.registrar("juiz", "DESPACHAR", "processo:42");

        ArgumentCaptor<AuditoriaEvento> captor = ArgumentCaptor.forClass(AuditoriaEvento.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUsuario()).isEqualTo("juiz");
        assertThat(captor.getValue().getAcao()).isEqualTo("DESPACHAR");
        assertThat(captor.getValue().getAlvo()).isEqualTo("processo:42");
        verify(auditLedgerService).append(eq("DESPACHAR"), eq("AuditoriaOperacional"), eq("1"), isNull(), any());
    }

    @Test
    void deveSubstituirAcaoNulaPorPlaceholderPadrao() {
        AuditoriaEvento saved = AuditoriaEvento.builder()
                .id(2L).usuario("user").acao("(sem_acao)").alvo("x").build();
        when(repository.save(any())).thenReturn(saved);

        service.registrar("user", null, "x");

        ArgumentCaptor<AuditoriaEvento> captor = ArgumentCaptor.forClass(AuditoriaEvento.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAcao()).isEqualTo("(sem_acao)");
    }

    @Test
    void deveEngolirExcecaoDoRepositorioSemPropagarParaChamador() {
        when(repository.save(any())).thenThrow(new RuntimeException("DB down"));

        assertThatNoException().isThrownBy(() -> service.registrar("user", "ACESSAR", "alvo"));
    }
}
