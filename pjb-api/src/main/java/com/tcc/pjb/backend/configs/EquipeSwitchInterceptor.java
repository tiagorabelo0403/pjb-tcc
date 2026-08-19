package com.tcc.pjb.backend.configs;

import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficePersonalScopeService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeProcessWorkspaceScopeService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeProcessWorkspaceScopeService.WorkspaceProcessFilterProfile;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeWorkspaceModeService;
import com.tcc.pjb.backend.service.exception.EquipeNaoSelecionadaException;
import lombok.RequiredArgsConstructor;

/**
 * Resolve o contexto de equipe/usuario da requisicao (quem, qual equipe, qual perfil de
 * workspace). A ativacao efetiva dos Hibernate {@code @Filter} NAO acontece mais aqui: com
 * {@code spring.jpa.open-in-view=false}, o preHandle roda antes de qualquer transacao de
 * negocio abrir, entao um {@code entityManager.unwrap(Session.class)} chamado neste ponto nunca
 * afeta a Session que a transacao real vai usar depois. O contexto resolvido aqui e apenas
 * guardado em {@link EquipeFiltroContexto} (ThreadLocal) e consumido por
 * {@link EquipeFiltroRepositoryAspect}, que ativa os filtros no momento em que
 * ProcessoRepository/ClienteRepository sao de fato chamados — ponto em que a Session real
 * ja esta garantidamente vinculada a transacao de negocio.
 *
 * <p>Sem {@code @ConditionalOnBean}: MembroEquipeRepository e EntityManager sao registrados
 * via auto-configuracao do Spring Boot (fase adiada), depois da fase de component-scan em que
 * este bean (um {@code @Component} comum, nao uma classe de auto-configuracao) tem sua propria
 * condicao avaliada — checar esses tipos aqui sempre resolvia falso e o bean nunca era criado,
 * deixando o mecanismo inteiro de isolamento por equipe morto silenciosamente (WebConfig usa
 * ObjectProvider e simplesmente pulava o registro do interceptor, sem erro nenhum).
 */
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@RequiredArgsConstructor
public class EquipeSwitchInterceptor implements HandlerInterceptor {

    public static final String HEADER_EQUIPE_ID = "X-Equipe-ID";
    public static final String HIBERNATE_FILTER_EQUIPE = "filtroEquipe";
    public static final String FILTER_PARAM_EQUIPE_ID = "equipeIdParam";
    public static final String FILTER_PARAM_USUARIO_ID = "usuarioIdParam";
    public static final String HIBERNATE_FILTER_PROCESSO = "filtroEquipeProcesso";

    private final MembroEquipeRepository membroRepository;
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
        EquipeFiltroContexto.clear();
        if (usuario == null || usuario.getId() == null || !usuario.isAdvogado()) {
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
        WorkspaceProcessFilterProfile profile = officeProcessWorkspaceScopeService.currentFilterProfile(request);
        EquipeFiltroContexto.set(new EquipeFiltroContexto.Estado(usuarioId, equipeIdParam, profile));
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        EquipeContexto.clear();
        EquipeFiltroContexto.clear();
    }

    private Long extractLong(String value) {
        try {
            return value == null ? null : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
