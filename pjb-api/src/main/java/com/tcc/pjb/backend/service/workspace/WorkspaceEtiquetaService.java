package com.tcc.pjb.backend.service.workspace;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.workspace.etiqueta.WorkspaceEtiquetaCreateRequest;
import com.tcc.pjb.backend.model.dto.workspace.etiqueta.WorkspaceEtiquetaResponse;
import com.tcc.pjb.backend.model.dto.workspace.etiqueta.WorkspaceEtiquetaUpdateRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workspace.WorkspaceEtiqueta;
import com.tcc.pjb.backend.model.entity.workspace.WorkspaceProcessoEtiqueta;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.workspace.WorkspaceEtiquetaRepository;
import com.tcc.pjb.backend.model.repository.workspace.WorkspaceProcessoEtiquetaRepository;
import com.tcc.pjb.backend.service.exception.RecursoJaExistenteException;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class WorkspaceEtiquetaService {

    private final WorkspaceEtiquetaRepository etiquetaRepository;
    private final WorkspaceProcessoEtiquetaRepository processoEtiquetaRepository;
    private final ProcessoRepository processoRepository;
    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;

    public WorkspaceEtiquetaService(WorkspaceEtiquetaRepository etiquetaRepository,
                                   WorkspaceProcessoEtiquetaRepository processoEtiquetaRepository,
                                   ProcessoRepository processoRepository,
                                   CurrentUserService currentUserService,
                                   PjbAuthorizationService authorizationService) {
        this.etiquetaRepository = etiquetaRepository;
        this.processoEtiquetaRepository = processoEtiquetaRepository;
        this.processoRepository = processoRepository;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public WorkspaceEtiquetaResponse criar(WorkspaceEtiquetaCreateRequest req) {
        Usuario u = currentUserService.getRequired();
        String nome = safe(req.getNome());
        if (etiquetaRepository.findByOwnerUserIdAndNomeIgnoreCase(u.getId(), nome).isPresent()) {
            throw new RecursoJaExistenteException("Etiqueta já existe: " + nome);
        }

        WorkspaceEtiqueta e = WorkspaceEtiqueta.builder()
                .id(UUID.randomUUID())
                .ownerUserId(u.getId())
                .nome(nome)
                .corHex(normalizeHex(req.getCorHex()))
                .sistema(false)
                .build();

        return toResponse(etiquetaRepository.save(e));
    }

    public List<WorkspaceEtiquetaResponse> listarMinhas() {
        Usuario u = currentUserService.getRequired();
        return etiquetaRepository.findAllByOwnerUserIdOrderByNomeAsc(u.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public WorkspaceEtiquetaResponse atualizar(UUID etiquetaId, WorkspaceEtiquetaUpdateRequest req) {
        Usuario u = currentUserService.getRequired();
        WorkspaceEtiqueta e = etiquetaRepository.findById(etiquetaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Etiqueta não encontrada"));

        if (!e.isSistema() && !u.getId().equals(e.getOwnerUserId())) {
            throw new SecurityException("Etiqueta não pertence ao usuário");
        }

        String nome = safe(req.getNome());
        etiquetaRepository.findByOwnerUserIdAndNomeIgnoreCase(e.getOwnerUserId(), nome)
                .filter(other -> !other.getId().equals(e.getId()))
                .ifPresent(other -> { throw new RecursoJaExistenteException("Etiqueta já existe: " + nome); });

        e.setNome(nome);
        e.setCorHex(normalizeHex(req.getCorHex()));
        return toResponse(etiquetaRepository.save(e));
    }

    @Transactional
    public void deletar(UUID etiquetaId) {
        Usuario u = currentUserService.getRequired();
        WorkspaceEtiqueta e = etiquetaRepository.findById(etiquetaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Etiqueta não encontrada"));

        if (e.isSistema()) {
            throw new IllegalArgumentException("Etiqueta de sistema não pode ser removida.");
        }
        if (!u.getId().equals(e.getOwnerUserId())) {
            throw new SecurityException("Etiqueta não pertence ao usuário");
        }

        etiquetaRepository.delete(e);
    }

    @Transactional
    public void atribuirAoProcesso(Long processoId, UUID etiquetaId) {
        Usuario u = currentUserService.getRequired();

        Processo p = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo não encontrado"));
        authorizationService.requireReadProcesso(p);

        WorkspaceEtiqueta e = etiquetaRepository.findById(etiquetaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Etiqueta não encontrada"));

        
        if (!e.isSistema() && !u.getId().equals(e.getOwnerUserId())) {
            throw new SecurityException("Etiqueta não pertence ao usuário");
        }

        if (processoEtiquetaRepository.existsByProcesso_IdAndEtiqueta_Id(processoId, etiquetaId)) {
            return; 
        }

        WorkspaceProcessoEtiqueta link = WorkspaceProcessoEtiqueta.builder()
                .id(UUID.randomUUID())
                .processo(p)
                .etiqueta(e)
                .atribuidoPor(u.getId())
                .build();

        processoEtiquetaRepository.save(link);
    }

    @Transactional
    public void removerDoProcesso(Long processoId, UUID etiquetaId) {
        Usuario u = currentUserService.getRequired();
        Processo p = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo não encontrado"));
        authorizationService.requireReadProcesso(p);

        WorkspaceProcessoEtiqueta link = processoEtiquetaRepository.findByProcesso_IdAndEtiqueta_Id(processoId, etiquetaId)
                .orElse(null);
        if (link == null) return;

        WorkspaceEtiqueta e = link.getEtiqueta();
        if (!e.isSistema() && !u.getId().equals(e.getOwnerUserId())) {
            throw new SecurityException("Etiqueta não pertence ao usuário");
        }

        processoEtiquetaRepository.delete(link);
    }

    public List<WorkspaceEtiquetaResponse> listarDoProcesso(Long processoId) {
        Usuario u = currentUserService.getRequired();
        Processo p = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo não encontrado"));
        authorizationService.requireReadProcesso(p);

        
        return processoEtiquetaRepository.findAllByProcessoId(processoId)
                .stream()
                .map(WorkspaceProcessoEtiqueta::getEtiqueta)
                .filter(e -> e.isSistema() || u.getId().equals(e.getOwnerUserId()))
                .map(this::toResponse)
                .toList();
    }

    private WorkspaceEtiquetaResponse toResponse(WorkspaceEtiqueta e) {
        return WorkspaceEtiquetaResponse.builder()
                .id(e.getId())
                .nome(e.getNome())
                .corHex(e.getCorHex())
                .sistema(e.isSistema())
                .criadoEm(e.getCriadoEm())
                .atualizadoEm(e.getAtualizadoEm())
                .build();
    }

    private static String safe(String s) {
        if (s == null) return null;
        String v = s.trim();
        if (v.isBlank()) return null;
        return v;
    }

    private static String normalizeHex(String hex) {
        if (hex == null || hex.isBlank()) return null;
        String v = hex.trim();
        if (!v.startsWith("#")) v = "#" + v;
        return v;
    }
}
