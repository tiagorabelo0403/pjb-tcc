package com.tcc.pjb.backend.service.secretariat.institucional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.SecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.repository.SecretariaInstitucionalItemRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TomarCienciaServiceTest {

    private final SecretariaInstitucionalItemRepository repository = mock(SecretariaInstitucionalItemRepository.class);
    private final AuditLedgerService auditService = mock(AuditLedgerService.class);
    private final TomarCienciaService service = new TomarCienciaService(repository, auditService);

    @Test
    void tomarCienciaCarimbaIntimadoEmEMudaStatus() {
        SecretariaInstitucionalItem item = new SecretariaInstitucionalItem();
        ReflectionTestUtils.setField(item, "id", 1L);
        item.setStatus(StatusSecretariaInstitucionalItem.PENDENTE);
        when(repository.findById(1L)).thenReturn(Optional.of(item));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.tomarCiencia(1L);

        assertThat(item.getIntimadoEm()).isNotNull();
        assertThat(item.getStatus()).isEqualTo(StatusSecretariaInstitucionalItem.EM_ANALISE);
        verify(auditService).appendSafely(eq("SECRETARIA_INSTITUCIONAL_CIENCIA"), any());
    }

    @Test
    void chamarTomarCienciaDuasVezesNaoSobrescreveOCarimboOriginal() {
        java.time.Instant primeiraCiencia = java.time.Instant.parse("2026-08-10T10:00:00Z");
        SecretariaInstitucionalItem item = new SecretariaInstitucionalItem();
        ReflectionTestUtils.setField(item, "id", 2L);
        item.setStatus(StatusSecretariaInstitucionalItem.EM_ANALISE);
        item.setIntimadoEm(primeiraCiencia);
        when(repository.findById(2L)).thenReturn(Optional.of(item));

        service.tomarCiencia(2L);

        assertThat(item.getIntimadoEm()).isEqualTo(primeiraCiencia);
        verify(repository, never()).save(any());
    }

    @Test
    void itemInexistenteLancaIllegalArgumentException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.tomarCiencia(99L)).isInstanceOf(IllegalArgumentException.class);
    }
}
