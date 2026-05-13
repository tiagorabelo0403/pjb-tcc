package com.tcc.pjb.backend.service.workspace;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.workspace.localizador.WorkspaceLocalizadorCreateRequest;
import com.tcc.pjb.backend.model.dto.workspace.localizador.WorkspaceLocalizadorCriteria;
import com.tcc.pjb.backend.model.dto.workspace.localizador.WorkspaceLocalizadorResponse;
import com.tcc.pjb.backend.model.dto.workspace.localizador.WorkspaceLocalizadorUpdateRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workspace.WorkspaceLocalizador;
import com.tcc.pjb.backend.model.repository.workspace.WorkspaceLocalizadorRepository;
import com.tcc.pjb.backend.service.exception.RecursoJaExistenteException;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class WorkspaceLocalizadorService {

    private final WorkspaceLocalizadorRepository localizadorRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    public WorkspaceLocalizadorService(WorkspaceLocalizadorRepository localizadorRepository,
                                      CurrentUserService currentUserService,
                                      ObjectMapper objectMapper) {
        this.localizadorRepository = localizadorRepository;
        this.currentUserService = currentUserService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WorkspaceLocalizadorResponse criar(WorkspaceLocalizadorCreateRequest req) {
        Usuario u = currentUserService.getRequired();
        String nome = safe(req.getNome());
        if (localizadorRepository.findByOwnerUserIdAndNomeIgnoreCase(u.getId(), nome).isPresent()) {
            throw new RecursoJaExistenteException("Localizador já existe: " + nome);
        }

        WorkspaceLocalizadorCriteria criteria = req.getCriteria();
        String json = toJson(criteria);

        WorkspaceLocalizador l = WorkspaceLocalizador.builder()
                .id(UUID.randomUUID())
                .ownerUserId(u.getId())
                .nome(nome)
                .descricao(safe(req.getDescricao()))
                .criterioJson(json)
                .compartilhado(Boolean.TRUE.equals(req.getCompartilhado()))
                .build();

        return toResponse(localizadorRepository.save(l));
    }

    public List<WorkspaceLocalizadorResponse> listar() {
        Usuario u = currentUserService.getRequired();
        return localizadorRepository.listForUser(u.getId()).stream().map(this::toResponse).toList();
    }

    public WorkspaceLocalizadorResponse obter(UUID id) {
        Usuario u = currentUserService.getRequired();
        WorkspaceLocalizador l = localizadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Localizador não encontrado"));

        if (!l.isCompartilhado() && !u.getId().equals(l.getOwnerUserId())) {
            throw new SecurityException("Localizador não pertence ao usuário");
        }
        return toResponse(l);
    }

    @Transactional
    public WorkspaceLocalizadorResponse atualizar(UUID id, WorkspaceLocalizadorUpdateRequest req) {
        Usuario u = currentUserService.getRequired();
        WorkspaceLocalizador l = localizadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Localizador não encontrado"));

        if (!u.getId().equals(l.getOwnerUserId())) {
            throw new SecurityException("Somente o dono pode alterar o localizador");
        }

        String nome = safe(req.getNome());
        localizadorRepository.findByOwnerUserIdAndNomeIgnoreCase(u.getId(), nome)
                .filter(other -> !other.getId().equals(l.getId()))
                .ifPresent(other -> { throw new RecursoJaExistenteException("Localizador já existe: " + nome); });

        l.setNome(nome);
        l.setDescricao(safe(req.getDescricao()));
        l.setCompartilhado(Boolean.TRUE.equals(req.getCompartilhado()));
        l.setCriterioJson(toJson(req.getCriteria()));

        return toResponse(localizadorRepository.save(l));
    }

    @Transactional
    public void deletar(UUID id) {
        Usuario u = currentUserService.getRequired();
        WorkspaceLocalizador l = localizadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Localizador não encontrado"));

        if (!u.getId().equals(l.getOwnerUserId())) {
            throw new SecurityException("Somente o dono pode remover o localizador");
        }
        localizadorRepository.delete(l);
    }

    private WorkspaceLocalizadorResponse toResponse(WorkspaceLocalizador l) {
        return WorkspaceLocalizadorResponse.builder()
                .id(l.getId())
                .nome(l.getNome())
                .descricao(l.getDescricao())
                .criteria(fromJson(l.getCriterioJson()))
                .compartilhado(l.isCompartilhado())
                .ownerUserId(l.getOwnerUserId())
                .criadoEm(l.getCriadoEm())
                .atualizadoEm(l.getAtualizadoEm())
                .build();
    }

    private WorkspaceLocalizadorCriteria fromJson(String json) {
        if (json == null || json.isBlank()) return new WorkspaceLocalizadorCriteria();
        try {
            return objectMapper.readValue(json, WorkspaceLocalizadorCriteria.class);
        } catch (Exception e) {
            
            return new WorkspaceLocalizadorCriteria();
        }
    }

    private String toJson(WorkspaceLocalizadorCriteria criteria) {
        try {
            return objectMapper.writeValueAsString(criteria == null ? new WorkspaceLocalizadorCriteria() : criteria);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("criteria inválido", e);
        }
    }

    private static String safe(String s) {
        if (s == null) return null;
        String v = s.trim();
        if (v.isBlank()) return null;
        return v;
    }
}
