package com.tcc.pjb.backend.modules.advocacia.office.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeProcessOperation;
import com.tcc.pjb.backend.modules.advocacia.office.entity.OfficeSignatureQueueItem;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeDelegationMode;
import com.tcc.pjb.backend.modules.advocacia.office.repository.OfficeSignatureQueueRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OfficeOperationDelegationRoutingServiceTest {

    private final OfficeDelegationService officeDelegationService = mock(OfficeDelegationService.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final OfficeSignatureQueueRepository officeSignatureQueueRepository = mock(OfficeSignatureQueueRepository.class);
    private final OfficeOperationDelegationRoutingService service = new OfficeOperationDelegationRoutingService(
            officeDelegationService, usuarioRepository, officeSignatureQueueRepository);

    private AdvOfficeProcessOperation operationWithEquipe(Long equipeId) {
        AdvOfficeProcessOperation operation = new AdvOfficeProcessOperation();
        operation.setId(1L);
        if (equipeId != null) {
            Equipe equipe = new Equipe();
            equipe.setId(equipeId);
            operation.setEquipe(equipe);
        }
        return operation;
    }

    private Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        return usuario;
    }

    @Test
    void resolve_semEquipe_naoConsultaDelegacaoEUsaAtorComoSigner() {
        AdvOfficeProcessOperation operation = operationWithEquipe(null);
        Usuario actor = usuario(10L);
        Processo processo = new Processo();

        OfficeOperationDelegationRoutingService.DelegationRouting routing = service.resolve(
                operation, actor, OfficeActionType.PETICIONAR, "hash", "resumo", processo, true);

        assertThat(routing.decision().mode()).isEqualTo(OfficeDelegationMode.SELF);
        assertThat(routing.signer().getId()).isEqualTo(10L);
        assertThat(routing.queueItem()).isNull();
        verifyNoInteractions(officeDelegationService);
    }

    @Test
    void resolve_comEquipe_resolveSignerDivergentePeloUsuarioRepository() {
        AdvOfficeProcessOperation operation = operationWithEquipe(44L);
        Usuario actor = usuario(10L);
        Usuario signer = usuario(77L);
        Processo processo = new Processo();

        OfficeDelegationService.Decision decision = new OfficeDelegationService.Decision(
                OfficeDelegationMode.AUTO, 44L, 10L, 77L, 90, null);
        when(officeDelegationService.decideAndRecord(44L, 10L, OfficeActionType.PETICIONAR,
                OfficeGovernedProcessOperationService.RESOURCE_TYPE, "1", "hash", "resumo", null, true))
                .thenReturn(decision);
        when(usuarioRepository.findById(77L)).thenReturn(Optional.of(signer));

        OfficeOperationDelegationRoutingService.DelegationRouting routing = service.resolve(
                operation, actor, OfficeActionType.PETICIONAR, "hash", "resumo", processo, true);

        assertThat(routing.signer().getId()).isEqualTo(77L);
        assertThat(routing.queueItem()).isNull();
    }

    @Test
    void resolve_comQueueItemId_buscaFilaEFalhaSeAusente() {
        AdvOfficeProcessOperation operation = operationWithEquipe(44L);
        Usuario actor = usuario(10L);
        Processo processo = new Processo();

        OfficeDelegationService.Decision decision = new OfficeDelegationService.Decision(
                OfficeDelegationMode.QUEUE, 44L, 10L, 10L, 20, 555L);
        when(officeDelegationService.decideAndRecord(44L, 10L, OfficeActionType.PETICIONAR,
                OfficeGovernedProcessOperationService.RESOURCE_TYPE, "1", "hash", "resumo", null, true))
                .thenReturn(decision);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(actor));
        when(officeSignatureQueueRepository.findById(555L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve(operation, actor, OfficeActionType.PETICIONAR, "hash", "resumo", processo, true))
                .isInstanceOf(EntityNotFoundException.class);

        OfficeSignatureQueueItem queueItem = new OfficeSignatureQueueItem();
        when(officeSignatureQueueRepository.findById(555L)).thenReturn(Optional.of(queueItem));

        OfficeOperationDelegationRoutingService.DelegationRouting routing = service.resolve(
                operation, actor, OfficeActionType.PETICIONAR, "hash", "resumo", processo, true);
        assertThat(routing.queueItem()).isSameAs(queueItem);
    }
}
