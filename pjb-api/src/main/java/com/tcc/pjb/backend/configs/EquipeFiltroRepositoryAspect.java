package com.tcc.pjb.backend.configs;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeProcessWorkspaceScopeService.WorkspaceProcessFilterProfile;

/**
 * Ativa os Hibernate {@code @Filter} de isolamento por equipe no ponto em que {@code
 * ProcessoRepository}/{@code ClienteRepository} sao efetivamente chamados, e nao no {@code
 * preHandle} do {@link EquipeSwitchInterceptor}.
 *
 * <p>Com {@code spring.jpa.open-in-view=false}, uma chamada de repositorio Spring Data so
 * acontece depois que alguma transacao {@code @Transactional} (do proprio service de negocio,
 * que e sempre quem chama o repositorio nesta base) ja abriu e ja vinculou a {@link Session}
 * real na thread via {@code TransactionSynchronizationManager}. Isso elimina o problema de
 * timing que quebrou as duas tentativas anteriores (ativar o filtro no {@code preHandle} ou via
 * {@code TransactionExecutionListener}): aqui o {@code entityManager.unwrap(Session.class)}
 * so roda quando a Session de negocio ja esta garantidamente ativa, porque o join point so
 * existe se o metodo do repositorio foi de fato invocado.
 *
 * <p>{@code target(...)} (nao {@code execution(...)}) e usado de proposito: os metodos de CRUD
 * herdados (findById, save, count, ...) sao declarados em {@code JpaRepository}/{@code
 * CrudRepository}, nao nas interfaces {@code ProcessoRepository}/{@code ClienteRepository} —
 * um {@code execution()} apontando so para essas duas interfaces perderia justamente os metodos
 * mais usados. {@code target()} casa pelo tipo real do proxy em runtime, entao cobre qualquer
 * metodo alcancavel atraves dele.
 */
@Aspect
@Component
public class EquipeFiltroRepositoryAspect {

    private static final Logger log = LoggerFactory.getLogger(EquipeFiltroRepositoryAspect.class);

    private final EntityManager entityManager;

    public EquipeFiltroRepositoryAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Before("target(com.tcc.pjb.backend.model.repository.ProcessoRepository) "
            + "|| target(com.tcc.pjb.backend.modules.advocacia.repository.ClienteRepository)")
    public void ativarFiltroEquipeAntesDaConsulta() {
        EquipeFiltroContexto.Estado estado = EquipeFiltroContexto.getOrNull();
        if (estado == null) {
            return;
        }
        try {
            Session session = entityManager.unwrap(Session.class);
            aplicarFiltroEquipe(session, estado);
            aplicarFiltroProcesso(session, estado);
        } catch (Exception e) {
            log.debug("falha ao habilitar filtro de equipe/processo via aspecto de repositorio", e);
        }
    }

    private void aplicarFiltroEquipe(Session session, EquipeFiltroContexto.Estado estado) {
        if (session.getEnabledFilter(EquipeSwitchInterceptor.HIBERNATE_FILTER_EQUIPE) != null) {
            return;
        }
        session.enableFilter(EquipeSwitchInterceptor.HIBERNATE_FILTER_EQUIPE)
                .setParameter(EquipeSwitchInterceptor.FILTER_PARAM_USUARIO_ID, estado.usuarioId())
                .setParameter(EquipeSwitchInterceptor.FILTER_PARAM_EQUIPE_ID, estado.equipeIdParam());
    }

    private void aplicarFiltroProcesso(Session session, EquipeFiltroContexto.Estado estado) {
        if (session.getEnabledFilter(EquipeSwitchInterceptor.HIBERNATE_FILTER_PROCESSO) != null) {
            return;
        }
        WorkspaceProcessFilterProfile profile = estado.processoProfile();
        if (profile == null) {
            return;
        }
        session.enableFilter(EquipeSwitchInterceptor.HIBERNATE_FILTER_PROCESSO)
                .setParameter(EquipeSwitchInterceptor.FILTER_PARAM_USUARIO_ID, profile.usuarioId())
                .setParameter(EquipeSwitchInterceptor.FILTER_PARAM_EQUIPE_ID, profile.activeEquipeId() == null ? -1L : profile.activeEquipeId())
                .setParameter("includePersonalParam", profile.includePersonalOwnCases())
                .setParameter("allRamosParam", profile.canViewAllRamos())
                .setParameterList("allowedRamosParam", profile.allowedRamos())
                .setParameter("allowSensitiveParam", profile.allowSensitive())
                .setParameter("blockPersonalCasesParam", profile.blockPersonalCases())
                .setParameter("userCpfParam", profile.userCpf() == null ? "" : profile.userCpf());
    }
}
