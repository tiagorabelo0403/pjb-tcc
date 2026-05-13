package com.tcc.pjb.backend.service.workspace;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.workspace.fila.*;
import com.tcc.pjb.backend.model.dto.workspace.localizador.WorkspaceLocalizadorCriteria;
import com.tcc.pjb.backend.model.dto.workspace.localizador.WorkspaceProcessoResumoResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workspace.*;
import com.tcc.pjb.backend.model.repository.workspace.WorkspaceFilaRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class WorkspaceFilaService {

    private final ObjectMapper objectMapper;

    private final WorkspaceFilaRepository filaRepository;
    private final CurrentUserService currentUserService;
    private final WorkspaceLocalizadorQueryService localizadorQueryService;
    private final WorkspaceFilaWorkItemQueryService workItemQueryService;
    private final WorkspaceFilaProfileResolver profileResolver;
    private final Cache<UUID, Long> filaCountCache;

    public WorkspaceFilaService(WorkspaceFilaRepository filaRepository,
                               CurrentUserService currentUserService,
                               WorkspaceLocalizadorQueryService localizadorQueryService,
                               WorkspaceFilaWorkItemQueryService workItemQueryService,
                               WorkspaceFilaProfileResolver profileResolver,
                               ObjectMapper objectMapper) {
        this.filaRepository = filaRepository;
        this.currentUserService = currentUserService;
        this.localizadorQueryService = localizadorQueryService;
        this.workItemQueryService = workItemQueryService;
        this.profileResolver = profileResolver;
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.filaCountCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(3))
                .maximumSize(4096)
                .build();
    }

    public List<WorkspaceFilaResponse> listar() {
        Usuario u = currentUserService.getRequired();

        List<WorkspaceFila> system = filaRepository.findAllBySistemaTrueOrderByOrderIndexAscNomeAsc();
        List<WorkspaceFila> mine = filaRepository.findAllByOwnerUserIdOrderByOrderIndexAscNomeAsc(u.getId());

        List<WorkspaceFila> merged = new ArrayList<>();
        for (WorkspaceFila f : system) {
            if (f.getAudience() != null && f.getAudience().applies(u.getTipoUsuario())) {
                merged.add(f);
            }
        }
        merged.addAll(mine);

        merged.sort(Comparator
                .comparing(WorkspaceFila::getOrderIndex, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(WorkspaceFila::getNome, Comparator.nullsLast(String::compareToIgnoreCase)));

        List<WorkspaceFilaResponse> out = new ArrayList<>();
        for (WorkspaceFila f : merged) {
            out.add(toResponse(f, countFor(f)));
        }
        return out;
    }

    public WorkspaceFilaResponse obter(UUID id) {
        WorkspaceFila f = getAccessibleFila(id);
        return toResponse(f, countFor(f));
    }

    @Transactional
    public WorkspaceFilaResponse criar(WorkspaceFilaCreateRequest req) {
        Usuario u = currentUserService.getRequired();

        if (filaRepository.existsByOwnerUserIdAndNomeIgnoreCase(u.getId(), req.getNome())) {
            throw new IllegalArgumentException("Já existe uma fila com esse nome.");
        }

        WorkspaceFila fila = WorkspaceFila.builder()
                .ownerUserId(u.getId())
                .sistema(false)
                .audience(WorkspaceFilaAudience.ALL)
                .nome(req.getNome())
                .descricao(req.getDescricao())
                .kind(req.getKind())
                .orderIndex(req.getOrderIndex() != null ? req.getOrderIndex() : 100)
                .compartilhado(false)
                .criterioJson(toCriteriaJson(req.getKind(), req.getProcessoCriteria(), req.getWorkItemCriteria()))
                .build();

        WorkspaceFila saved = filaRepository.save(fila);
        invalidateCount(saved.getId());
        return toResponse(saved, countFor(saved));
    }

    @Transactional
    public WorkspaceFilaResponse atualizar(UUID id, WorkspaceFilaUpdateRequest req) {
        Usuario u = currentUserService.getRequired();

        WorkspaceFila fila = filaRepository.findByOwnerUserIdAndId(u.getId(), id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fila não encontrada"));

        fila.setNome(req.getNome());
        fila.setDescricao(req.getDescricao());
        if (req.getOrderIndex() != null) fila.setOrderIndex(req.getOrderIndex());

        
        if (fila.getKind() == WorkspaceFilaKind.PROCESSO && req.getProcessoCriteria() != null) {
            fila.setCriterioJson(writeJson(req.getProcessoCriteria()));
        }
        if (fila.getKind() == WorkspaceFilaKind.WORKITEM && req.getWorkItemCriteria() != null) {
            fila.setCriterioJson(writeJson(req.getWorkItemCriteria()));
        }

        WorkspaceFila saved = filaRepository.save(fila);
        invalidateCount(saved.getId());
        return toResponse(saved, countFor(saved));
    }

    @Transactional
    public void deletar(UUID id) {
        Usuario u = currentUserService.getRequired();
        WorkspaceFila fila = filaRepository.findByOwnerUserIdAndId(u.getId(), id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fila não encontrada"));

        if (fila.isSistema()) {
            throw new IllegalStateException("Não é permitido deletar filas do sistema.");
        }

        filaRepository.delete(fila);
        invalidateCount(id);
    }

    public Page<WorkspaceProcessoResumoResponse> listarProcessos(UUID filaId,
                                                                int page,
                                                                int size,
                                                                String sortBy,
                                                                String dir) {
        WorkspaceFila fila = getAccessibleFila(filaId);
        if (fila.getKind() != WorkspaceFilaKind.PROCESSO) {
            throw new IllegalArgumentException("Fila não é do tipo PROCESSO.");
        }
        WorkspaceLocalizadorCriteria criteria = parseLenient(fila.getCriterioJson(), WorkspaceLocalizadorCriteria.class, new WorkspaceLocalizadorCriteria());
        return localizadorQueryService.preview(criteria, page, size, sortBy, dir);
    }

    public Page<WorkspaceFilaWorkItemResumoResponse> listarWorkItems(UUID filaId, int page, int size) {
        WorkspaceFila fila = getAccessibleFila(filaId);
        if (fila.getKind() != WorkspaceFilaKind.WORKITEM) {
            throw new IllegalArgumentException("Fila não é do tipo WORKITEM.");
        }
        WorkspaceFilaWorkItemCriteria criteria = parseLenient(fila.getCriterioJson(), WorkspaceFilaWorkItemCriteria.class,
                WorkspaceFilaWorkItemCriteria.builder().mode(WorkspaceFilaWorkItemMode.AUTO_INBOX).build());
        return workItemQueryService.listar(criteria, page, size);
    }

    private WorkspaceFila getAccessibleFila(UUID id) {
        Usuario u = currentUserService.getRequired();

        Optional<WorkspaceFila> mine = filaRepository.findByOwnerUserIdAndId(u.getId(), id);
        if (mine.isPresent()) return mine.get();

        WorkspaceFila system = filaRepository.findById(id)
                .filter(WorkspaceFila::isSistema)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fila não encontrada"));

        if (system.getAudience() != null && system.getAudience().applies(u.getTipoUsuario())) {
            return system;
        }

        throw new RecursoNaoEncontradoException("Fila não encontrada");
    }

    private long countFor(WorkspaceFila fila) {
        if (fila == null) return 0;
        UUID filaId = fila.getId();
        if (filaId == null) {
            return countForFresh(fila);
        }
        return filaCountCache.get(filaId, ignored -> countForFresh(fila));
    }

    private long countForFresh(WorkspaceFila fila) {
        if (fila.getKind() == WorkspaceFilaKind.WORKITEM) {
            WorkspaceFilaWorkItemCriteria criteria = parseLenient(fila.getCriterioJson(), WorkspaceFilaWorkItemCriteria.class,
                    WorkspaceFilaWorkItemCriteria.builder().mode(WorkspaceFilaWorkItemMode.AUTO_INBOX).build());
            return workItemQueryService.count(criteria);
        }
        if (fila.getKind() == WorkspaceFilaKind.PROCESSO) {
            WorkspaceLocalizadorCriteria criteria = parseLenient(fila.getCriterioJson(), WorkspaceLocalizadorCriteria.class, new WorkspaceLocalizadorCriteria());
            return localizadorQueryService.countFast(criteria);
        }
        return 0;
    }

    private void invalidateCount(UUID filaId) {
        if (filaId != null) {
            filaCountCache.invalidate(filaId);
        }
    }

    private WorkspaceFilaResponse toResponse(WorkspaceFila f, long count) {
        WorkspaceFilaProfile profile = profileResolver.resolve(f, count);
        return WorkspaceFilaResponse.builder()
                .id(f.getId())
                .nome(f.getNome())
                .descricao(f.getDescricao())
                .kind(f.getKind())
                .sistema(f.isSistema())
                .audience(f.getAudience())
                .orderIndex(f.getOrderIndex())
                .count(count)
                .descriptor(profile.descriptor())
                .operationalMode(profile.operationalMode())
                .scope(profile.scope())
                .autoRefreshSeconds(profile.autoRefreshSeconds())
                .sortHint(profile.sortHint())
                .workloadBand(profile.workloadBand())
                .assistantDesk(profile.assistantDesk())
                .escalationDesk(profile.escalationDesk())
                .coordinationChannel(profile.coordinationChannel())
                .redistributionEligible(profile.redistributionEligible())
                .audienceSensitive(profile.audienceSensitive())
                .labels(profile.labels())
                .metadata(profile.toMap())
                .build();
    }

    private String toCriteriaJson(WorkspaceFilaKind kind,
                                 WorkspaceLocalizadorCriteria processoCriteria,
                                 WorkspaceFilaWorkItemCriteria workItemCriteria) {
        if (kind == WorkspaceFilaKind.PROCESSO) {
            WorkspaceLocalizadorCriteria c = processoCriteria != null ? processoCriteria : new WorkspaceLocalizadorCriteria();
            return writeJson(c);
        }
        if (kind == WorkspaceFilaKind.WORKITEM) {
            WorkspaceFilaWorkItemCriteria c = workItemCriteria != null ? workItemCriteria : WorkspaceFilaWorkItemCriteria.builder().build();
            if (c.getMode() == null) c.setMode(WorkspaceFilaWorkItemMode.AUTO_INBOX);
            return writeJson(c);
        }
        throw new IllegalArgumentException("Kind inválido");
    }

    private String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalArgumentException("Não foi possível serializar critério JSON.", e);
        }
    }

    private <T> T parseLenient(String json, Class<T> type, T fallback) {
        if (json == null || json.isBlank()) return fallback;
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
