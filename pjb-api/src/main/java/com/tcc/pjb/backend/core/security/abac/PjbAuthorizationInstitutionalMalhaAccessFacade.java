package com.tcc.pjb.backend.core.security.abac;

import com.tcc.pjb.backend.core.security.abac.policy.PolicyRegistry;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.repository.PoloProcessualRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.oficial_justica.OficialJusticaProcessoVinculoService;
import java.util.List;
import org.springframework.data.domain.PageRequest;

final class PjbAuthorizationInstitutionalMalhaAccessFacade {

    private final PolicyRegistry policyRegistry;
    private final PoloProcessualRepository poloProcessualRepository;
    private final WorkItemRepository workItemRepository;
    private final OficialJusticaProcessoVinculoService oficialJusticaProcessoVinculoService;

    PjbAuthorizationInstitutionalMalhaAccessFacade(PolicyRegistry policyRegistry,
                                                   PoloProcessualRepository poloProcessualRepository,
                                                   WorkItemRepository workItemRepository,
                                                   OficialJusticaProcessoVinculoService oficialJusticaProcessoVinculoService) {
        this.policyRegistry = policyRegistry;
        this.poloProcessualRepository = poloProcessualRepository;
        this.workItemRepository = workItemRepository;
        this.oficialJusticaProcessoVinculoService = oficialJusticaProcessoVinculoService;
    }

    AuthzDecision evaluate(Usuario usuario, Long processoId) {
        String policyVersion = policyRegistry.active().policy().version();
        if (usuario == null || usuario.getId() == null || usuario.getTipoUsuario() == null || processoId == null) {
            return AuthzDecision.deny("malha_institucional_contexto_invalido", policyVersion);
        }
        TipoUsuario tipo = usuario.getTipoUsuario();

        if (tipo.isAdmin()) {
            return AuthzDecision.allow("malha_institucional_admin", policyVersion);
        }

        TipoPolo tipoPoloDoSegmento = tipoPoloDoSegmento(tipo);
        if (tipoPoloDoSegmento != null) {
            boolean vinculado = poloProcessualRepository.findByProcessoIdAndTipoPolo(processoId, tipoPoloDoSegmento)
                    .stream().anyMatch(polo -> polo.isAtivo());
            return vinculado
                    ? AuthzDecision.allow("malha_institucional_polo_vinculado", policyVersion)
                    : AuthzDecision.deny("malha_institucional_sem_polo_no_processo", policyVersion);
        }

        if (tipo == TipoUsuario.OFICIAL_JUSTICA || tipo == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR) {
            boolean vinculado = oficialJusticaProcessoVinculoService.possuiVinculoDireto(processoId, usuario.getId(), tipo);
            return vinculado
                    ? AuthzDecision.allow("malha_institucional_mandado_vinculado", policyVersion)
                    : AuthzDecision.deny("malha_institucional_sem_mandado_no_processo", policyVersion);
        }

        if (tipo.isMagistratura() || tipo.isDelegadoOuAgente()) {
            boolean vinculado = !workItemRepository.findByProcessoIdAndAssignedUserIdAndRolesAndStatusNot(
                    processoId, usuario.getId(), List.of(tipo), WorkItemStatus.CANCELADO, PageRequest.of(0, 1)
            ).isEmpty();
            return vinculado
                    ? AuthzDecision.allow("malha_institucional_workitem_atribuido", policyVersion)
                    : AuthzDecision.deny("malha_institucional_sem_workitem_no_processo", policyVersion);
        }

        return AuthzDecision.deny("malha_institucional_papel_nao_reconhecido", policyVersion);
    }

    private static TipoPolo tipoPoloDoSegmento(TipoUsuario tipo) {
        if (tipo.isMinisterioPublico()) {
            return TipoPolo.MINISTERIO_PUBLICO;
        }
        if (tipo.isDefensoriaPublica()) {
            return TipoPolo.DEFENSORIA;
        }
        if (tipo.isProcuradoria()) {
            return TipoPolo.PROCURADORIA;
        }
        return null;
    }
}
