package com.tcc.pjb.backend.service.recursal;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.configs.EquipeContexto;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshDashboardRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshPlanRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshSearchRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshTransitionRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade.RecursalAdmissibilityRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.ia.RecursalIaConferenciaRequest;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.platform.security.rbac.RbacGrantedRoleResolver;

@Service
public class RecursalContextualAccessService {

    private static final Set<String> GLOBAL_RECURSAL_READ_AUTHORITIES = Set.of(
            "ROLE_MEMBRO_MINISTERIO_PUBLICO",
            "ROLE_PROMOTOR_ELEITORAL",
            "ROLE_PROMOTOR_TRABALHISTA",
            "ROLE_PROCURADOR_GERAL_REPUBLICA",
            "ROLE_DEFENSOR_PUBLICO",
            "ROLE_DEFENSOR_PUBLICO_FEDERAL",
            "ROLE_PROCURADOR",
            "ROLE_PROCURADORIA",
            "ROLE_PROCURADORIA_MUNICIPAL",
            "ROLE_PROCURADORIA_ESTADUAL",
            "ROLE_PROCURADORIA_FEDERAL",
            "ROLE_JUIZ",
            "ROLE_JUIZ_ESTADUAL",
            "ROLE_JUIZ_FEDERAL",
            "ROLE_JUIZ_ESPECIAL",
            "ROLE_JUIZ_ELEITORAL",
            "ROLE_JUIZ_TRABALHISTA",
            "ROLE_JUIZ_MILITAR",
            "ROLE_MAGISTRADO",
            "ROLE_DESEMBARGADOR",
            "ROLE_DESEMBARGADOR_FEDERAL",
            "ROLE_MINISTRO",
            "ROLE_SERVIDOR",
            "ROLE_SERVIDOR_FORUM",
            "ROLE_ADMINISTRADOR",
            "ROLE_ADMIN"
    );

    private static final int CONTEXTUAL_SCOPE_LIMIT = 500;

    private final CurrentUserService currentUserService;
    private final ProcessoRepository processoRepository;
    private final MembroEquipeRepository membroEquipeRepository;
    private final PjbAuthorizationService authorizationService;
    private final RecursalEffectiveSecrecyService secrecyService;
    private final RbacGrantedRoleResolver rbacGrantedRoleResolver;

    public RecursalContextualAccessService(CurrentUserService currentUserService,
                                           ProcessoRepository processoRepository,
                                           MembroEquipeRepository membroEquipeRepository,
                                           PjbAuthorizationService authorizationService,
                                           RecursalEffectiveSecrecyService secrecyService,
                                           RbacGrantedRoleResolver rbacGrantedRoleResolver) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.membroEquipeRepository = Objects.requireNonNull(membroEquipeRepository);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.secrecyService = Objects.requireNonNull(secrecyService);
        this.rbacGrantedRoleResolver = Objects.requireNonNull(rbacGrantedRoleResolver);
    }

    public void requireReadProcesso(Long processoId) {
        Processo processo = resolveProcesso(processoId);
        authorizationService.requireReadProcesso(processo);
        authorizationService.requireReadProcessoAtSecrecy(processo, secrecyService.effectiveSecrecyForProcesso(processo.getId()));
    }

    public void requireWriteProcesso(Long processoId) {
        authorizationService.requireWriteProcesso(resolveProcesso(processoId));
    }

    public void requirePlanningAccess(RecursalMeshPlanRequest request) {
        Long processoId = request == null || request.context() == null ? null : request.context().processoId();
        if (processoId != null) {
            requireReadProcesso(processoId);
            return;
        }
        requireInstitutionalGlobalReadOrContextualScope();
    }

    public void requireTransitionAccess(RecursalMeshTransitionRequest request) {
        Long processoId = request == null || request.context() == null ? null : request.context().processoId();
        if (processoId != null) {
            requireWriteProcesso(processoId);
            return;
        }
        throw new AccessDeniedException("Transição recursal exige contexto processual vinculado.");
    }

    public void requireAdmissibilityAccess(RecursalAdmissibilityRequest request) {
        Long processoId = request == null || request.context() == null ? null : request.context().processoId();
        if (processoId != null) {
            requireWriteProcesso(processoId);
            return;
        }
        requireInstitutionalGlobalReadOrContextualScope();
    }

    public void requireIaConferenciaAccess(RecursalIaConferenciaRequest request) {
        Long processoId = request == null ? null : request.processoId();
        if (processoId == null && request != null && request.admissibilidade() != null && request.admissibilidade().context() != null) {
            processoId = request.admissibilidade().context().processoId();
        }
        if (processoId != null) {
            requireReadProcesso(processoId);
            return;
        }
        requireInstitutionalGlobalReadOrContextualScope();
    }

    public RecursalMeshSearchRequest scopeSearchRequest(RecursalMeshSearchRequest request) {
        RecursalMeshSearchRequest normalized = request == null
                ? new RecursalMeshSearchRequest(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 50)
                : request;
        if (normalized.processoId() != null) {
            requireReadProcesso(normalized.processoId());
            return normalized;
        }
        if (hasInstitutionalGlobalReadAuthority()) {
            return normalized;
        }
        return normalized.withProcessoIds(resolveContextualProcessScopeIds());
    }

    public RecursalMeshDashboardRequest scopeDashboardRequest(RecursalMeshDashboardRequest request) {
        RecursalMeshDashboardRequest normalized = request == null
                ? new RecursalMeshDashboardRequest(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 500, 10)
                : request;
        if (normalized.processoId() != null) {
            requireReadProcesso(normalized.processoId());
            return normalized;
        }
        if (hasInstitutionalGlobalReadAuthority()) {
            return normalized;
        }
        return normalized.withProcessoIds(resolveContextualProcessScopeIds());
    }

    public void requireInstitutionalGlobalReadOrContextualScope() {
        if (hasInstitutionalGlobalReadAuthority()) {
            return;
        }
        if (resolveContextualProcessScopeIds().isEmpty()) {
            throw new AccessDeniedException("Pesquisa recursal global exige autoridade institucional ou vínculo processual contextual.");
        }
    }

    private Processo resolveProcesso(Long processoId) {
        if (processoId == null || processoId <= 0) {
            throw new AccessDeniedException("Contexto processual recursal inválido.");
        }
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new AccessDeniedException("Processo não encontrado para o contexto recursal."));
    }

    private boolean hasInstitutionalGlobalReadAuthority() {
        Usuario usuario = currentUserService.getOrNull();
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return false;
        }
        return rbacGrantedRoleResolver.resolveGrantedRoles(usuario.getTipoUsuario()).stream()
                .anyMatch(GLOBAL_RECURSAL_READ_AUTHORITIES::contains);
    }

    private List<Long> resolveContextualProcessScopeIds() {
        Usuario usuario = currentUserService.getOrNull();
        if (usuario == null || usuario.getId() == null) {
            throw new AccessDeniedException("Usuário autenticado não encontrado para o escopo recursal.");
        }
        LinkedHashSet<Long> scoped = new LinkedHashSet<>();
        String cpf = trimToNull(usuario.getCpf());
        TipoUsuario tipoUsuario = usuario.getTipoUsuario();
        if (tipoUsuario == TipoUsuario.CIDADAO && cpf != null) {
            scoped.addAll(processoRepository.findIdsByCidadaoCpf(cpf, PageRequest.of(0, CONTEXTUAL_SCOPE_LIMIT)));
        }
        if ((tipoUsuario != null && tipoUsuario.isAdvocacia()) || usuario.isAdvogado()) {
            if (cpf != null) {
                scoped.addAll(processoRepository.findIdsByAdvogadoCpf(cpf, PageRequest.of(0, CONTEXTUAL_SCOPE_LIMIT)));
            }
            List<Long> equipeIds = resolveScopedEquipeIds(usuario.getId());
            if (!equipeIds.isEmpty()) {
                scoped.addAll(processoRepository.findIdsByEquipeIds(equipeIds, PageRequest.of(0, CONTEXTUAL_SCOPE_LIMIT)));
            }
        }
        List<Long> result = scoped.stream().limit(CONTEXTUAL_SCOPE_LIMIT).toList();
        if (result.isEmpty()) {
            throw new AccessDeniedException("Nenhum processo acessível foi encontrado no escopo recursal atual.");
        }
        return result;
    }

    private List<Long> resolveScopedEquipeIds(Long usuarioId) {
        MembroEquipe membroAtivo = EquipeContexto.getMembroDaEquipeAtiva();
        if (membroAtivo != null && membroAtivo.isAtivo() && membroAtivo.getEquipe() != null && membroAtivo.getEquipe().getId() != null) {
            return List.of(membroAtivo.getEquipe().getId());
        }
        return membroEquipeRepository.findByUsuario_Id(usuarioId).stream()
                .filter(MembroEquipe::isAtivo)
                .map(MembroEquipe::getEquipe)
                .filter(Objects::nonNull)
                .map(equipe -> equipe.getId())
                .filter(Objects::nonNull)
                .distinct()
                .limit(50)
                .toList();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
