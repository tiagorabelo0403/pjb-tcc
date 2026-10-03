package com.tcc.pjb.backend.modules.atendimento.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoAttachment;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageAttachment;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageAttachmentId;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoAttachmentRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoMessageAttachmentRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class AtendimentoAttachmentQueryServiceTest {

    private final AtendimentoMessageAttachmentRepository msgAttRepo = mock(AtendimentoMessageAttachmentRepository.class);
    private final AtendimentoAttachmentRepository attachmentRepo = mock(AtendimentoAttachmentRepository.class);
    private final AtendimentoAttachmentQueryService service = new AtendimentoAttachmentQueryService(msgAttRepo, attachmentRepo);

    @Test
    void attachmentsForMessageVazioQuandoNaoHaVinculos() {
        when(msgAttRepo.findByMessageIds(any())).thenReturn(List.of());
        assertThat(service.attachmentsForMessage(42L)).isEmpty();
    }

    @Test
    void attachmentsForMessageResolveAnexosPelosVinculos() {
        AtendimentoMessageAttachmentId id = mock(AtendimentoMessageAttachmentId.class);
        when(id.getAttachmentId()).thenReturn(5L);
        AtendimentoMessageAttachment link = mock(AtendimentoMessageAttachment.class);
        when(link.getId()).thenReturn(id);
        when(msgAttRepo.findByMessageIds(any())).thenReturn(List.of(link));
        AtendimentoAttachment att = mock(AtendimentoAttachment.class);
        when(attachmentRepo.findAllById(List.of(5L))).thenReturn(List.of(att));

        assertThat(service.attachmentsForMessage(42L)).containsExactly(att);
    }

    @Test
    void batchAttachmentsVazioQuandoNaoHaVinculos() {
        when(msgAttRepo.findByMessageIds(any())).thenReturn(List.of());

        AtendimentoAttachmentQueryService.AttachmentBatch batch = service.batchAttachments(List.of(1L, 2L));

        assertThat(batch.attIdsByMsg()).isEmpty();
        assertThat(batch.attMap()).isEmpty();
    }

    @Test
    void batchAttachmentsIndexaPorMensagemEPorAnexo() {
        AtendimentoMessageAttachmentId id = mock(AtendimentoMessageAttachmentId.class);
        when(id.getMessageId()).thenReturn(42L);
        when(id.getAttachmentId()).thenReturn(5L);
        AtendimentoMessageAttachment link = mock(AtendimentoMessageAttachment.class);
        when(link.getId()).thenReturn(id);
        when(msgAttRepo.findByMessageIds(any())).thenReturn(List.of(link));
        AtendimentoAttachment att = mock(AtendimentoAttachment.class);
        when(att.getId()).thenReturn(5L);
        when(attachmentRepo.findAllById(any())).thenReturn(List.of(att));

        AtendimentoAttachmentQueryService.AttachmentBatch batch = service.batchAttachments(List.of(42L));

        assertThat(batch.attIdsByMsg()).containsEntry(42L, List.of(5L));
        assertThat(batch.attMap()).containsEntry(5L, att);
    }
}
