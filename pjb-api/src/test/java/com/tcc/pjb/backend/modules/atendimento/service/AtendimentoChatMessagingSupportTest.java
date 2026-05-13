package com.tcc.pjb.backend.modules.atendimento.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoMessageDto;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoAttachment;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoAttachmentStatus;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessage;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageAttachment;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageAttachmentId;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageReceipt;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageStatus;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoAttachmentRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoMessageAttachmentRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoMessageReceiptRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoMessageRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoThreadMemberSettingsRepository;
import com.tcc.pjb.backend.service.ui.UiHistoryService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class AtendimentoChatMessagingSupportTest {

    private final AtendimentoInboxLiveHub liveHub = mock(AtendimentoInboxLiveHub.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final UiHistoryService uiHistoryService = mock(UiHistoryService.class);
    private final AtendimentoThreadMemberSettingsRepository settingsRepository = mock(AtendimentoThreadMemberSettingsRepository.class);
    private final AtendimentoAttachmentRepository attachmentRepository = mock(AtendimentoAttachmentRepository.class);
    private final AtendimentoMessageAttachmentRepository messageAttachmentRepository = mock(AtendimentoMessageAttachmentRepository.class);
    private final AtendimentoMessageReceiptRepository receiptRepository = mock(AtendimentoMessageReceiptRepository.class);
    private final AtendimentoMessageRepository messageRepository = mock(AtendimentoMessageRepository.class);

    private final AtendimentoChatMessagingSupport support = new AtendimentoChatMessagingSupport(
            Clock.fixed(Instant.parse("2026-04-17T15:00:00Z"), ZoneOffset.UTC),
            new ObjectMapper(),
            liveHub,
            usuarioRepository,
            processoRepository,
            uiHistoryService,
            settingsRepository,
            attachmentRepository,
            messageAttachmentRepository,
            receiptRepository,
            messageRepository
    );

    @Test
    void deveBloquearLimiteDeAnexosQuandoExcedido() {
        assertThatThrownBy(() -> support.validateAttachments(8L, List.of(1L, 2L, 3L), true, 2, 1024L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("attachments_limit");
    }

    @Test
    void deveBloquearAnexosQuandoDesabilitados() {
        assertThatThrownBy(() -> support.validateAttachments(8L, List.of(1L), false, 3, 1024L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("attachments_disabled");
    }

    @Test
    void deveMontarDtoComPreviewDeRespostaEAnexo() {
        AtendimentoMessage message = AtendimentoMessage.builder()
                .id(11L)
                .threadId(7L)
                .senderUsuarioId(100L)
                .senderTipo(TipoUsuario.ADVOGADO.name())
                .body("Resposta principal")
                .replyToMessageId(5L)
                .status(AtendimentoMessageStatus.DELIVERED)
                .createdAt(Instant.parse("2026-04-17T15:00:00Z"))
                .build();
        AtendimentoMessage replyTarget = AtendimentoMessage.builder()
                .id(5L)
                .threadId(7L)
                .senderUsuarioId(200L)
                .senderTipo(TipoUsuario.CIDADAO.name())
                .body("Mensagem anterior longa que deve ser resumida para o preview")
                .status(AtendimentoMessageStatus.DELIVERED)
                .createdAt(Instant.parse("2026-04-17T14:50:00Z"))
                .build();
        Usuario sender = Usuario.builder().id(100L).tipoUsuario(TipoUsuario.ADVOGADO).nome("Dra. Alice").oab("CE1234").build();
        Usuario replySender = Usuario.builder().id(200L).tipoUsuario(TipoUsuario.CIDADAO).nome("Cliente Bruno").build();
        AtendimentoAttachment attachment = new AtendimentoAttachment();
        attachment.setId(901L);
        attachment.setFileName("documento.pdf");
        attachment.setContentType("application/pdf");
        attachment.setSizeBytes(2048L);
        attachment.setStatus(AtendimentoAttachmentStatus.READY);
        AtendimentoMessageAttachment link = new AtendimentoMessageAttachment();
        link.setId(new AtendimentoMessageAttachmentId(11L, 901L));
        link.setThreadId(7L);
        AtendimentoMessageReceipt receipt = AtendimentoMessageReceipt.builder()
                .messageId(11L)
                .threadId(7L)
                .usuarioId(200L)
                .deliveredAt(Instant.parse("2026-04-17T15:01:00Z"))
                .readAt(Instant.parse("2026-04-17T15:02:00Z"))
                .createdAt(Instant.parse("2026-04-17T15:00:00Z"))
                .updatedAt(Instant.parse("2026-04-17T15:02:00Z"))
                .build();

        when(usuarioRepository.findAllById(any())).thenReturn(List.of(sender, replySender));
        when(messageRepository.findAllById(eq(List.of(5L)))).thenReturn(List.of(replyTarget));
        when(messageAttachmentRepository.findByMessageIds(List.of(11L))).thenReturn(List.of(link));
        when(attachmentRepository.findAllById(List.of(901L))).thenReturn(List.of(attachment));
        when(receiptRepository.findByThreadIdAndUsuarioIdAndMessageIdIn(7L, 200L, List.of(11L))).thenReturn(List.of(receipt));

        List<AtendimentoMessageDto> dtos = support.toDtosWithAttachmentsAndReply(List.of(message), 100L, 200L, 11L, 11L);

        assertThat(dtos).hasSize(1);
        AtendimentoMessageDto dto = dtos.get(0);
        assertThat(dto.replyTo()).isNotNull();
        assertThat(dto.replyTo().messageId()).isEqualTo(5L);
        assertThat(dto.attachments()).hasSize(1);
        assertThat(dto.attachments().get(0).fileName()).isEqualTo("documento.pdf");
        assertThat(dto.deliveredToOther()).isTrue();
        assertThat(dto.readByOther()).isTrue();
        assertThat(dto.senderDisplayName()).isEqualTo("Dra. Alice");
    }
}
