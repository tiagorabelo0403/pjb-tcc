package com.tcc.pjb.backend.service.governance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.pericia.CadeiaCustodiaDigitalLedgerEntry;
import com.tcc.pjb.backend.model.repository.CadeiaCustodiaDigitalLedgerEntryRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DocumentTrustChainServiceTest {

    @Test
    void shouldSealDocumentAndReturnCustodyKey() {
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        DocumentoProcessualRepository documentoRepository = Mockito.mock(DocumentoProcessualRepository.class);
        CadeiaCustodiaDigitalLedgerEntryRepository cadeiaRepository = Mockito.mock(CadeiaCustodiaDigitalLedgerEntryRepository.class);
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        AuditLedgerService auditLedgerService = Mockito.mock(AuditLedgerService.class);
        Processo processo = new Processo();
        processo.setId(2L);
        processo.setNumeroProcesso("0002");
        UUID documentoId = UUID.randomUUID();
        DocumentoProcessual documento = DocumentoProcessual.builder().id(documentoId).titulo("Sentença").sha256("abc123").build();
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setTipoUsuario(TipoUsuario.JUIZ);
        when(processoRepository.findById(2L)).thenReturn(Optional.of(processo));
        when(documentoRepository.findById(documentoId)).thenReturn(Optional.of(documento));
        when(cadeiaRepository.findTop200ByChaveCustodiaOrderBySealedAtDesc("proc:2")).thenReturn(List.of());
        when(cadeiaRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(cadeiaRepository.save(any(CadeiaCustodiaDigitalLedgerEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        when(currentUserService.getRequired()).thenReturn(usuario);
        DocumentTrustChainService service = new DocumentTrustChainService(
                processoRepository,
                documentoRepository,
                cadeiaRepository,
                currentUserService,
                auditLedgerService
        );
        var response = service.selar(2L, documentoId, "L1", "assinatura principal", false, true, "QUALIFICADA");
        assertTrue(response.persistido());
        assertEquals("proc:2", response.chaveCustodia());
    }
}
