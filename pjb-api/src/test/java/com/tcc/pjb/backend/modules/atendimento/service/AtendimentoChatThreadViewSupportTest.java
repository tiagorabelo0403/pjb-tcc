package com.tcc.pjb.backend.modules.atendimento.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.advocacia.repository.ClienteRepository;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoAdvogadoDto;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoThreadDto;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoReadState;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThread;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThreadMemberSettings;
import com.tcc.pjb.backend.modules.atendimento.model.AtendimentoThreadStatus;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoChecklistItemRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoReadStateRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoThreadMemberSettingsRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoThreadRepository;
import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AtendimentoChatThreadViewSupportTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-04-17T15:00:00Z"), ZoneOffset.UTC);
    private final AtendimentoThreadRepository threadRepository = mock(AtendimentoThreadRepository.class);
    private final AtendimentoReadStateRepository readStateRepository = mock(AtendimentoReadStateRepository.class);
    private final AtendimentoChecklistItemRepository checklistItemRepository = mock(AtendimentoChecklistItemRepository.class);
    private final AtendimentoThreadMemberSettingsRepository settingsRepository = mock(AtendimentoThreadMemberSettingsRepository.class);
    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final LaianeProcuracaoRepository procuracaoRepository = mock(LaianeProcuracaoRepository.class);
    private final ClienteRepository clienteRepository = mock(ClienteRepository.class);

    private final AtendimentoChatThreadViewSupport support = new AtendimentoChatThreadViewSupport(
            clock,
            threadRepository,
            readStateRepository,
            checklistItemRepository,
            settingsRepository,
            processoRepository,
            usuarioRepository,
            procuracaoRepository,
            clienteRepository
    );

    @Test
    void deveMontarThreadDtoComMuteChecklistEUnread() {
        Usuario actor = Usuario.builder().id(10L).tipoUsuario(TipoUsuario.CIDADAO).cpf("11111111111").build();
        Usuario advogado = Usuario.builder().id(22L).tipoUsuario(TipoUsuario.ADVOGADO).nome("Dra. Alice").oab("CE1234").build();
        AtendimentoThread thread = AtendimentoThread.builder()
                .id(77L)
                .processoId(90L)
                .advogadoId(22L)
                .cidadaoUsuarioId(10L)
                .lastMessageId(15L)
                .updatedAt(Instant.parse("2026-04-17T14:59:00Z"))
                .status(AtendimentoThreadStatus.ATIVO)
                .build();
        Processo processo = Processo.builder()
                .id(90L)
                .numeroUnificado("0001234-56.2026.8.06.0001")
                .classeProcessual("Procedimento Comum")
                .assunto("Indenização por dano moral")
                .build();
        AtendimentoReadState readState = new AtendimentoReadState();
        readState.setThreadId(77L);
        readState.setUsuarioId(10L);
        readState.setLastReadMessageId(9L);
        AtendimentoThreadMemberSettings settings = AtendimentoThreadMemberSettings.builder()
                .threadId(77L)
                .usuarioId(10L)
                .mutedUntil(Instant.parse("2026-04-17T15:30:00Z"))
                .build();

        when(usuarioRepository.findAllById(any())).thenReturn(List.of(advogado));
        when(readStateRepository.findByThreadIdAndUsuarioId(77L, 10L)).thenReturn(Optional.of(readState));
        when(settingsRepository.findByThreadIdAndUsuarioId(77L, 10L)).thenReturn(Optional.of(settings));
        when(checklistItemRepository.aggregateByThreadIds(eq(List.of(77L)), eq(com.tcc.pjb.backend.modules.atendimento.model.AtendimentoChecklistItemStatus.OPEN), any()))
                .thenReturn(List.of(new AtendimentoChecklistItemRepository.ThreadChecklistAgg() {
                    @Override
                    public Long getThreadId() {
                        return 77L;
                    }

                    @Override
                    public long getOpenCnt() {
                        return 3;
                    }

                    @Override
                    public long getOverdueCnt() {
                        return 1;
                    }

                    @Override
                    public Instant getNextDueAt() {
                        return Instant.parse("2026-04-17T16:00:00Z");
                    }

                    @Override
                    public Instant getOldestOverdueAt() {
                        return Instant.parse("2026-04-17T13:00:00Z");
                    }
                }));

        AtendimentoThreadDto dto = support.toThreadDto(actor, thread, processo);

        assertThat(dto.threadId()).isEqualTo(77L);
        assertThat(dto.otherParty()).isEqualTo("Dra. Alice");
        assertThat(dto.hasUnread()).isTrue();
        assertThat(dto.mutedNow()).isTrue();
        assertThat(dto.openChecklistCount()).isEqualTo(3);
        assertThat(dto.overdueChecklistCount()).isEqualTo(1);
        assertThat(dto.titulo()).contains("Procedimento Comum");
    }

    @Test
    void deveFiltrarAdvogadosSemRelacaoDeCliente() {
        Usuario actor = Usuario.builder().id(11L).tipoUsuario(TipoUsuario.CIDADAO).cpf("111.222.333-44").build();
        Usuario advogadoValido = Usuario.builder().id(41L).tipoUsuario(TipoUsuario.ADVOGADO).nome("Dr. Válido").oab("CE9999").build();
        Usuario advogadoInvalido = Usuario.builder().id(42L).tipoUsuario(TipoUsuario.ADVOGADO).nome("Dr. Fora").oab("CE8888").build();

        when(procuracaoRepository.findDistinctAdvogadosByProcessoIdAndStatus(5L, LaianeProcuracaoStatus.ATIVA))
                .thenReturn(List.of(advogadoValido, advogadoInvalido));
        when(clienteRepository.existsByCpfHashAndAdvogado_Id(AtendimentoChatSupportUtils.cpfHash(actor.getCpf()), 41L)).thenReturn(true);
        when(clienteRepository.existsByCpfHashAndAdvogado_Id(AtendimentoChatSupportUtils.cpfHash(actor.getCpf()), 42L)).thenReturn(false);

        List<AtendimentoAdvogadoDto> advogados = support.listAdvogadosForProcesso(actor, 5L);

        assertThat(advogados).extracting(AtendimentoAdvogadoDto::usuarioId).containsExactly(41L);
        assertThat(advogados.get(0).nome()).isEqualTo("Dr. Válido");
    }
}
