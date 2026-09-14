package com.tcc.pjb.backend.service.advogado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerEntry;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class AdvogadoAuditoriaLedgerServiceTest {

    private final AuditLedgerRepository auditLedgerRepository = mock(AuditLedgerRepository.class);

    private final AdvogadoAuditoriaLedgerService service =
            new AdvogadoAuditoriaLedgerService(auditLedgerRepository);

    private static final Pageable PAGINA = PageRequest.of(0, 20);

    private AuditLedgerEntry entrada() {
        AuditLedgerEntry entrada = mock(AuditLedgerEntry.class);
        when(entrada.getId()).thenReturn(11L);
        when(entrada.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 9, 13, 10, 0));
        when(entrada.getAction()).thenReturn("ADV_PETICAO_ENVIADA");
        when(entrada.getResourceType()).thenReturn("PROCESSO");
        when(entrada.getResourceId()).thenReturn("7");
        when(entrada.getRequestId()).thenReturn("req-1");
        when(entrada.getPayloadHash()).thenReturn("hash-payload");
        when(entrada.getEntryHash()).thenReturn("hash-entrada");
        return entrada;
    }

    @Test
    void filtroEmBrancoViraFiltroAusente() {
        when(auditLedgerRepository.search(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.doAdvogado(42L, "   ", "", "  ", PAGINA);

        verify(auditLedgerRepository)
                .search(eq(42L), isNull(), isNull(), isNull(), eq(PAGINA));
    }

    @Test
    void filtroInformadoChegaAparadoAoRepositorio() {
        when(auditLedgerRepository.search(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.doAdvogado(42L, "  ADV_PET  ", " PROCESSO ", " 7 ", PAGINA);

        verify(auditLedgerRepository).search(eq(42L), eq("ADV_PET"), eq("PROCESSO"), eq("7"), eq(PAGINA));
    }

    @Test
    void eventoEMapeadoParaOContratoDeResposta() {
        // A entrada e montada ANTES do when() externo: stub dentro de thenReturn deixa o Mockito com um
        // when() em aberto e estoura UnfinishedStubbing.
        var entrada = entrada();
        when(auditLedgerRepository.search(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entrada)));

        var pagina = service.doAdvogado(42L, null, null, null, PAGINA);

        assertThat(pagina.getContent()).singleElement().satisfies(evento -> {
            assertThat(evento.id()).isEqualTo(11L);
            assertThat(evento.action()).isEqualTo("ADV_PETICAO_ENVIADA");
            assertThat(evento.resourceType()).isEqualTo("PROCESSO");
            assertThat(evento.entryHash()).isEqualTo("hash-entrada");
        });
    }

    @Test
    void semAdvogadoIdentificadoNaoConsultaOLedger() {
        assertThatThrownBy(() -> service.doAdvogado(null, null, null, null, PAGINA))
                .as("a trilha e sempre escopada a quem pediu; sem ator identificado a consulta nao pode "
                        + "cair em busca sem filtro de dono")
                .isInstanceOf(NullPointerException.class);

        verify(auditLedgerRepository, never()).search(any(), any(), any(), any(), any(Pageable.class));
    }
}
