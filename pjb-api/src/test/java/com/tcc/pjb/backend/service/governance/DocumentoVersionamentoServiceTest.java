package com.tcc.pjb.backend.service.governance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.pericia.CadeiaCustodiaDigitalLedgerEntry;
import com.tcc.pjb.backend.model.repository.CadeiaCustodiaDigitalLedgerEntryRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DocumentoVersionamentoServiceTest {

    @Test
    void shouldCalculateNextVersionForSameBaseTitle() {
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        DocumentoProcessualRepository documentoRepository = Mockito.mock(DocumentoProcessualRepository.class);
        CadeiaCustodiaDigitalLedgerEntryRepository cadeiaRepository = Mockito.mock(CadeiaCustodiaDigitalLedgerEntryRepository.class);
        Processo processo = new Processo();
        processo.setId(3L);
        processo.setNumeroProcesso("0003");
        DocumentoProcessual v1 = DocumentoProcessual.builder().id(UUID.randomUUID()).titulo("Minuta de sentença v1").criadoEm(LocalDateTime.of(2026, 3, 1, 10, 0)).build();
        DocumentoProcessual v2 = DocumentoProcessual.builder().id(UUID.randomUUID()).titulo("Minuta de sentença v2").sha256("hash2").criadoEm(LocalDateTime.of(2026, 3, 2, 10, 0)).build();
        when(processoRepository.findById(3L)).thenReturn(Optional.of(processo));
        when(documentoRepository.findByProcessoId(3L)).thenReturn(List.of(v1, v2));
        when(cadeiaRepository.findTop200ByChaveCustodiaOrderBySealedAtDesc("proc:3")).thenReturn(List.of(new CadeiaCustodiaDigitalLedgerEntry()));
        DocumentoVersionamentoService service = new DocumentoVersionamentoService(processoRepository, documentoRepository, cadeiaRepository);
        var response = service.historico(3L, "Minuta de sentença", false, false);
        assertEquals(3, response.proximaVersao());
        assertTrue(response.proximoTitulo().contains("v3"));
    }
}
