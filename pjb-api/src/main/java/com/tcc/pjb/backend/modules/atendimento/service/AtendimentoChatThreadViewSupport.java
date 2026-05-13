package com.tcc.pjb.backend.modules.atendimento.service;

import com.tcc.pjb.backend.model.dto.ui.UiToken;
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
import com.tcc.pjb.backend.modules.atendimento.model.AtendimentoChecklistItemStatus;
import com.tcc.pjb.backend.modules.atendimento.model.AtendimentoThreadStatus;
import com.tcc.pjb.backend.modules.atendimento.model.ChecklistThreadAgg;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoChecklistItemRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoReadStateRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoThreadMemberSettingsRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoThreadRepository;
import com.tcc.pjb.backend.modules.atendimento.util.AtendimentoParticipantLabelUtils;
import com.tcc.pjb.backend.modules.atendimento.util.ChecklistBadgeUtils;
import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
class AtendimentoChatThreadViewSupport {

    private final Clock clock;
    private final AtendimentoThreadRepository threadRepository;
    private final AtendimentoReadStateRepository readStateRepository;
    private final AtendimentoChecklistItemRepository checklistItemRepository;
    private final AtendimentoThreadMemberSettingsRepository settingsRepository;
    private final ProcessoRepository processoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LaianeProcuracaoRepository procuracaoRepository;
    private final ClienteRepository clienteRepository;

    AtendimentoChatThreadViewSupport(Clock clock,
                                     AtendimentoThreadRepository threadRepository,
                                     AtendimentoReadStateRepository readStateRepository,
                                     AtendimentoChecklistItemRepository checklistItemRepository,
                                     AtendimentoThreadMemberSettingsRepository settingsRepository,
                                     ProcessoRepository processoRepository,
                                     UsuarioRepository usuarioRepository,
                                     LaianeProcuracaoRepository procuracaoRepository,
                                     ClienteRepository clienteRepository) {
        this.clock = Objects.requireNonNull(clock);
        this.threadRepository = Objects.requireNonNull(threadRepository);
        this.readStateRepository = Objects.requireNonNull(readStateRepository);
        this.checklistItemRepository = Objects.requireNonNull(checklistItemRepository);
        this.settingsRepository = Objects.requireNonNull(settingsRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.procuracaoRepository = Objects.requireNonNull(procuracaoRepository);
        this.clienteRepository = Objects.requireNonNull(clienteRepository);
    }

    Page<AtendimentoThreadDto> listThreads(Usuario usuario, Pageable pageable) {
        Pageable normalized = AtendimentoChatSupportUtils.normalizePageable(pageable);
        Page<AtendimentoThread> page = loadThreadsPage(usuario, normalized);
        if (page.isEmpty()) {
            return new PageImpl<>(List.of(), normalized, 0);
        }

        Map<Long, Processo> processos = loadProcessos(page.getContent());
        Map<Long, Usuario> users = loadUsers(page.getContent());
        List<Long> threadIds = page.getContent().stream().map(AtendimentoThread::getId).filter(Objects::nonNull).toList();
        Map<Long, AtendimentoThreadMemberSettings> settingsByThread = threadIds.isEmpty()
                ? Map.of()
                : settingsRepository.findByUsuarioIdAndThreadIdIn(usuario.getId(), threadIds).stream()
                .collect(Collectors.toMap(AtendimentoThreadMemberSettings::getThreadId, value -> value, (left, right) -> left));
        Instant now = Instant.now(clock);
        Map<Long, ChecklistThreadAgg> checklistAggByThread = loadChecklistAgg(threadIds, now);

        List<AtendimentoThreadDto> output = new ArrayList<>(page.getNumberOfElements());
        for (AtendimentoThread thread : page.getContent()) {
            Processo processo = processos.get(thread.getProcessoId());
            Usuario other = AtendimentoChatSupportUtils.otherParty(usuario, thread, users);
            boolean hasUnread = hasUnread(usuario.getId(), thread);
            AtendimentoThreadMemberSettings settings = settingsByThread.get(thread.getId());
            boolean mutedNow = AtendimentoChatSupportUtils.isMutedNow(settings, now);
            Instant mutedUntil = settings != null ? settings.getMutedUntil() : null;
            ChecklistThreadAgg agg = checklistAggByThread.getOrDefault(thread.getId(), ChecklistThreadAgg.empty());
            output.add(buildThreadDto(thread, processo, other, hasUnread, now, mutedNow, mutedUntil, agg));
        }
        return new PageImpl<>(output, normalized, page.getTotalElements());
    }

    List<AtendimentoThreadDto> listThreadsForProcesso(Usuario usuario, Processo processo, Long processoId) {
        List<AtendimentoThread> threads;
        if (usuario.getTipoUsuario() == TipoUsuario.ADVOGADO) {
            threads = threadRepository.findByProcessoIdAndAdvogadoIdOrderByUpdatedAtDesc(processoId, usuario.getId());
        } else if (usuario.getTipoUsuario() == TipoUsuario.CIDADAO) {
            threads = threadRepository.findByProcessoIdAndCidadaoUsuarioIdOrderByUpdatedAtDesc(processoId, usuario.getId());
        } else {
            throw new AccessDeniedException("Acesso negado");
        }
        Map<Long, Usuario> users = loadUsers(threads);
        List<Long> threadIds = threads.stream().map(AtendimentoThread::getId).filter(Objects::nonNull).toList();
        Map<Long, AtendimentoThreadMemberSettings> settingsByThread = threadIds.isEmpty()
                ? Map.of()
                : settingsRepository.findByUsuarioIdAndThreadIdIn(usuario.getId(), threadIds).stream()
                .collect(Collectors.toMap(AtendimentoThreadMemberSettings::getThreadId, value -> value, (left, right) -> left));
        Instant now = Instant.now(clock);
        Map<Long, ChecklistThreadAgg> checklistAggByThread = loadChecklistAgg(threadIds, now);

        List<AtendimentoThreadDto> output = new ArrayList<>(threads.size());
        for (AtendimentoThread thread : threads) {
            Usuario other = AtendimentoChatSupportUtils.otherParty(usuario, thread, users);
            boolean hasUnread = hasUnread(usuario.getId(), thread);
            AtendimentoThreadMemberSettings settings = settingsByThread.get(thread.getId());
            boolean mutedNow = AtendimentoChatSupportUtils.isMutedNow(settings, now);
            Instant mutedUntil = settings != null ? settings.getMutedUntil() : null;
            ChecklistThreadAgg agg = checklistAggByThread.getOrDefault(thread.getId(), ChecklistThreadAgg.empty());
            output.add(buildThreadDto(thread, processo, other, hasUnread, now, mutedNow, mutedUntil, agg));
        }
        return output;
    }

    List<AtendimentoAdvogadoDto> listAdvogadosForProcesso(Usuario actor, Long processoId) {
        if (actor.getTipoUsuario() != TipoUsuario.CIDADAO) {
            throw new AccessDeniedException("Acesso negado");
        }
        String cpfHash = AtendimentoChatSupportUtils.cpfHash(actor.getCpf());
        List<Usuario> advogados = procuracaoRepository.findDistinctAdvogadosByProcessoIdAndStatus(processoId, LaianeProcuracaoStatus.ATIVA);
        if (advogados == null || advogados.isEmpty()) {
            return List.of();
        }
        List<AtendimentoAdvogadoDto> output = new ArrayList<>(advogados.size());
        for (Usuario advogado : advogados) {
            if (advogado == null || advogado.getId() == null) {
                continue;
            }
            if (!clienteRepository.existsByCpfHashAndAdvogado_Id(cpfHash, advogado.getId())) {
                continue;
            }
            output.add(new AtendimentoAdvogadoDto(
                    advogado.getId(),
                    advogado.getNome(),
                    AtendimentoParticipantLabelUtils.oabLabel(advogado),
                    AtendimentoParticipantLabelUtils.participantLabel(advogado)
            ));
        }
        return output;
    }

    AtendimentoThreadDto toThreadDto(Usuario actor, AtendimentoThread thread, Processo processo) {
        Map<Long, Usuario> users = loadUsers(List.of(thread));
        Usuario other = AtendimentoChatSupportUtils.otherParty(actor, thread, users);
        boolean hasUnread = hasUnread(actor.getId(), thread);
        Instant now = Instant.now(clock);
        AtendimentoThreadMemberSettings settings = settingsRepository.findByThreadIdAndUsuarioId(thread.getId(), actor.getId()).orElse(null);
        boolean mutedNow = AtendimentoChatSupportUtils.isMutedNow(settings, now);
        Instant mutedUntil = settings != null ? settings.getMutedUntil() : null;
        ChecklistThreadAgg agg = safeChecklistAgg(thread.getId(), now);
        return buildThreadDto(thread, processo, other, hasUnread, now, mutedNow, mutedUntil, agg);
    }

    private AtendimentoThreadDto buildThreadDto(AtendimentoThread thread,
                                                Processo processo,
                                                Usuario other,
                                                boolean hasUnread,
                                                Instant now,
                                                boolean mutedNow,
                                                Instant mutedUntil,
                                                ChecklistThreadAgg agg) {
        return new AtendimentoThreadDto(
                thread.getId(),
                thread.getProcessoId(),
                processo != null ? processo.getNumeroUnificado() : null,
                processo != null ? AtendimentoChatSupportUtils.safeTitle(processo) : null,
                other != null ? other.getNome() : null,
                thread.getUpdatedAt(),
                null,
                hasUnread,
                thread.getStatus() != null ? thread.getStatus().name() : AtendimentoThreadStatus.ATIVO.name(),
                mutedNow,
                mutedUntil,
                agg.openCount(),
                agg.overdueCount(),
                agg.nextDueAt(),
                ChecklistBadgeUtils.computeNextDueInMinutes(now, agg.nextDueAt()),
                ChecklistBadgeUtils.computeOverdueSinceMinutes(now, agg),
                other != null ? other.getId() : null,
                other != null && other.getTipoUsuario() != null ? other.getTipoUsuario().name() : null,
                AtendimentoParticipantLabelUtils.oabLabel(other),
                AtendimentoParticipantLabelUtils.participantLabel(other)
        );
    }

    private Page<AtendimentoThread> loadThreadsPage(Usuario usuario, Pageable pageable) {
        if (usuario.getTipoUsuario() == TipoUsuario.ADVOGADO) {
            return threadRepository.findByAdvogadoIdOrderByUpdatedAtDesc(usuario.getId(), pageable);
        }
        if (usuario.getTipoUsuario() == TipoUsuario.CIDADAO) {
            return threadRepository.findByCidadaoUsuarioIdOrderByUpdatedAtDesc(usuario.getId(), pageable);
        }
        throw new AccessDeniedException("Acesso negado");
    }

    private Map<Long, Processo> loadProcessos(List<AtendimentoThread> threads) {
        Set<Long> ids = threads.stream().map(AtendimentoThread::getProcessoId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return processoRepository.findAllById(ids).stream().collect(Collectors.toMap(Processo::getId, value -> value));
    }

    private Map<Long, Usuario> loadUsers(List<AtendimentoThread> threads) {
        Set<Long> ids = threads.stream()
                .flatMap(thread -> java.util.stream.Stream.of(thread.getAdvogadoId(), thread.getCidadaoUsuarioId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return usuarioRepository.findAllById(ids).stream().collect(Collectors.toMap(Usuario::getId, value -> value));
    }

    private Map<Long, ChecklistThreadAgg> loadChecklistAgg(List<Long> threadIds, Instant now) {
        if (threadIds == null || threadIds.isEmpty()) {
            return Map.of();
        }
        Instant reference = now != null ? now : Instant.now(clock);
        try {
            List<AtendimentoChecklistItemRepository.ThreadChecklistAgg> rows = checklistItemRepository.aggregateByThreadIds(threadIds, AtendimentoChecklistItemStatus.OPEN, reference);
            if (rows == null || rows.isEmpty()) {
                return Map.of();
            }
            Map<Long, ChecklistThreadAgg> output = new HashMap<>(rows.size() * 2);
            for (AtendimentoChecklistItemRepository.ThreadChecklistAgg row : rows) {
                if (row == null || row.getThreadId() == null) {
                    continue;
                }
                output.put(row.getThreadId(), new ChecklistThreadAgg(
                        AtendimentoChatSupportUtils.clampToInt(row.getOpenCnt()),
                        AtendimentoChatSupportUtils.clampToInt(row.getOverdueCnt()),
                        row.getNextDueAt(),
                        row.getOldestOverdueAt()
                ));
            }
            return output;
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private ChecklistThreadAgg safeChecklistAgg(Long threadId, Instant now) {
        if (threadId == null) {
            return ChecklistThreadAgg.empty();
        }
        return loadChecklistAgg(List.of(threadId), now).getOrDefault(threadId, ChecklistThreadAgg.empty());
    }

    private boolean hasUnread(Long usuarioId, AtendimentoThread thread) {
        if (usuarioId == null || thread.getLastMessageId() == null) {
            return false;
        }
        Optional<AtendimentoReadState> readState = readStateRepository.findByThreadIdAndUsuarioId(thread.getId(), usuarioId);
        Long lastRead = readState.map(AtendimentoReadState::getLastReadMessageId).orElse(null);
        return lastRead == null || thread.getLastMessageId() > lastRead;
    }
}
