package com.tcc.pjb.backend.service.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.tcc.pjb.backend.model.entity.ChatMensagem;
import com.tcc.pjb.backend.model.entity.Usuario;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgreementChatLedgerServiceTest {

    private final AgreementChatLedgerService service = new AgreementChatLedgerService();

    @Test
    void shouldParseNegotiationRoundsAndAttachmentsFromChatLedger() {
        Usuario usuario = new Usuario();
        usuario.setNome("Conciliador");
        ChatMensagem round = ChatMensagem.builder()
                .conteudo("[SISTEMA] Rodada negocial 2 registrada. Versão R2.V3 da CONTRAPROPOSTA consolidada.")
                .usuario(usuario)
                .dataEnvio(LocalDateTime.now())
                .build();
        ChatMensagem attachment = ChatMensagem.builder()
                .conteudo("Anexo negocial registrado: MEMORIA_CALCULO | rótulo=Planilha final | url=https://pjb.local/files/1 | mime=application/pdf | hash=abc123 | bytes=2048")
                .usuario(usuario)
                .dataEnvio(LocalDateTime.now())
                .build();

        var rounds = service.buildRoundTimeline(List.of(round, attachment));
        var attachments = service.buildStructuredAttachments(List.of(round, attachment));

        assertEquals(1, rounds.size());
        assertEquals(2, rounds.get(0).round());
        assertEquals("R2.V3", rounds.get(0).version());
        assertEquals("CONTRAPROPOSTA", rounds.get(0).eventType());
        assertEquals(1, attachments.size());
        assertEquals("MEMORIA_CALCULO", attachments.get(0).kind());
        assertEquals("Planilha final", attachments.get(0).label());
    }

    @Test
    void shouldGenerateNextRoundSnapshotFromMajorNegotiationStep() {
        ChatMensagem base = ChatMensagem.builder()
                .conteudo("[SISTEMA] Rodada negocial 1 registrada. Versão R1.V3 da PROPOSTA consolidada.")
                .dataEnvio(LocalDateTime.now())
                .build();

        var snapshot = service.nextRoundSnapshot(List.of(base), "Nova proposta com cláusula penal e parcelamento");

        assertFalse(snapshot.version().isBlank());
        assertEquals(2, snapshot.round());
    }
}
