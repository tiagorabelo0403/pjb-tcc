package com.tcc.pjb.backend.configs;

import java.util.Optional;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficePersonalScopeService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeProcessWorkspaceScopeService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeWorkspaceModeService;
import com.tcc.pjb.backend.service.exception.EquipeNaoSelecionadaException;
import lombok.RequiredArgsConstructor;

@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBean({MembroEquipeRepository.class, EntityManager.class, CurrentUserService.class, OfficePersonalScopeService.class})
@RequiredArgsConstructor
public class EquipeSwitchInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(EquipeSwitchInterceptor.class);

    public static final String HEADER_EQUIPE_ID = "X-Equipe-ID";
    public static final String HIBERNATE_FILTER_EQUIPE = "filtroEquipe";
    public static final String FILTER_PARAM_EQUIPE_ID = "equipeIdParam";
    public static final String FILTER_PARAM_USUARIO_ID = "usuarioIdParam";
    public static final String HIBERNATE_FILTER_PROCESSO = "filtroEquipeProcesso";

    private final MembroEquipeRepository membroRepository;
    private final EntityManager entityManager;
    private final CurrentUserService currentUserService;
    private final OfficePersonalScopeService officePersonalScopeService;
    private final OfficeWorkspaceModeService officeWorkspaceModeService;
    private final OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {

        Long equipeIdSolicitada = extractLong(request.getHeader(HEADER_EQUIPE_ID));
        final Usuario usuario = currentUserService.getOrNull();
        EquipeContexto.clear();
        if (usuario == null || usuario.getId() == null || !usuario.isAdvogado()) {
            safeDisableHibernateFilter();
            return true;
        }

        final long usuarioId = usuario.getId();

        if (equipeIdSolicitada == null) {
            equipeIdSolicitada = officeWorkspaceModeService.resolvePreferredEquipeId(usuarioId, request);
        }

        if (equipeIdSolicitada == null) {
            OfficePersonalScopeService.ScopeDecision scope = officePersonalScopeService.decide(usuarioId);
            if (scope.personalBlocked()) {
                if (scope.requireEquipeHeader()) {
                    throw new EquipeNaoSelecionadaException("Equipe não selecionada. Informe o header X-Equipe-ID.", HEADER_EQUIPE_ID, scope.candidateEquipeIds());
                }
                if (scope.resolvedEquipeId() != null) {
                    equipeIdSolicitada = scope.resolvedEquipeId();
                }
            }
        }

        if (equipeIdSolicitada != null) {
            Optional<MembroEquipe> membro = membroRepository.findByUsuario_IdAndEquipe_Id(usuarioId, equipeIdSolicitada);
            if (membro.isEmpty() || !membro.get().isAtivo()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
            EquipeContexto.setMembroAtivo(membro.get());
        }
        long equipeIdParam = equipeIdSolicitada == null ? -1L : equipeIdSolicitada;
        safeEnableHibernateFilter(usuarioId, equipeIdParam);
        safeEnableProcessoFilter(request);
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        EquipeContexto.clear();
    }

    private Long extractLong(String value) {
        try {
            return value == null ? null : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }


    private void safeDisableHibernateFilter() {
        try {
            Session session = entityManager.unwrap(Session.class);
            session.disableFilter(HIBERNATE_FILTER_EQUIPE);
            session.disableFilter(HIBERNATE_FILTER_PROCESSO);
        } catch (Exception e) {
            log.debug("falha ao desabilitar filtro de equipe", e);
        }
    }

    private void safeEnableHibernateFilter(long usuarioId, long equipeIdParam) {
        try {
            Session session = entityManager.unwrap(Session.class);
            session.disableFilter(HIBERNATE_FILTER_EQUIPE);
            session.disableFilter(HIBERNATE_FILTER_PROCESSO);
            session.enableFilter(HIBERNATE_FILTER_EQUIPE)
                    .setParameter(FILTER_PARAM_USUARIO_ID, usuarioId)
                    .setParameter(FILTER_PARAM_EQUIPE_ID, equipeIdParam);
        } catch (Exception e) {
            log.debug("falha ao habilitar filtro de equipe", e);
        }
    }


    private void safeEnableProcessoFilter(HttpServletRequest request) {
        try {
            Session session = entityManager.unwrap(Session.class);
            session.disableFilter(HIBERNATE_FILTER_PROCESSO);
            OfficeProcessWorkspaceScopeService.WorkspaceProcessFilterProfile profile = officeProcessWorkspaceScopeService.currentFilterProfile(request);
            session.enableFilter(HIBERNATE_FILTER_PROCESSO)
                    .setParameter(FILTER_PARAM_USUARIO_ID, profile.usuarioId())
                    .setParameter(FILTER_PARAM_EQUIPE_ID, profile.activeEquipeId() == null ? -1L : profile.activeEquipeId())
                    .setParameter("includePersonalParam", profile.includePersonalOwnCases())
                    .setParameter("allRamosParam", profile.canViewAllRamos())
                    .setParameterList("allowedRamosParam", profile.allowedRamos())
                    .setParameter("allowSensitiveParam", profile.allowSensitive())
                    .setParameter("blockPersonalCasesParam", profile.blockPersonalCases())
                    .setParameter("userCpfParam", profile.userCpf() == null ? "" : profile.userCpf());
        } catch (Exception e) {
            log.debug("falha ao habilitar filtro de processo", e);
        }
    }
}
