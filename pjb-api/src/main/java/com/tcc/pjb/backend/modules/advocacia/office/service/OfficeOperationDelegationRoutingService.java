package com.tcc.pjb.backend.modules.advocacia.office.service;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeProcessOperation;
import com.tcc.pjb.backend.modules.advocacia.office.entity.OfficeSignatureQueueItem;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeDelegationMode;
import com.tcc.pjb.backend.modules.advocacia.office.repository.OfficeSignatureQueueRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Decide o modo de delegação da operação (auto-assinatura vs. fila) e resolve o signatário e a
 * fila vinculada. Extraído de {@link OfficeGovernedProcessOperationService} porque
 * {@code officeDelegationService}, {@code usuarioRepository} e
 * {@code officeSignatureQueueRepository} são usados exclusivamente por esse passo de
 * {@code submitGovernedOperation}.
 */
@Service
public class OfficeOperationDelegationRoutingService {

    private final OfficeDelegationService officeDelegationService;
    private final UsuarioRepository usuarioRepository;
    private final OfficeSignatureQueueRepository officeSignatureQueueRepository;

    public OfficeOperationDelegationRoutingService(OfficeDelegationService officeDelegationService,
                                                     UsuarioRepository usuarioRepository,
                                                     OfficeSignatureQueueRepository officeSignatureQueueRepository) {
        this.officeDelegationService = Objects.requireNonNull(officeDelegationService);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.officeSignatureQueueRepository = Objects.requireNonNull(officeSignatureQueueRepository);
    }

    public DelegationRouting resolve(AdvOfficeProcessOperation operation,
                                     Usuario actor,
                                     OfficeActionType actionType,
                                     String payloadHash,
                                     String summary,
                                     Processo processo,
                                     boolean queueRequired) {
        Long equipeId = operation.getEquipe() == null ? null : operation.getEquipe().getId();
        OfficeDelegationService.Decision decision = equipeId == null
                ? new OfficeDelegationService.Decision(OfficeDelegationMode.SELF, null, actor.getId(), actor.getId(), 0, null)
                : officeDelegationService.decideAndRecord(
                        equipeId,
                        actor.getId(),
                        actionType,
                        OfficeGovernedProcessOperationService.RESOURCE_TYPE,
                        String.valueOf(operation.getId()),
                        payloadHash,
                        summary,
                        processo.getRamoDireito() == null ? null : processo.getRamoDireito().name(),
                        queueRequired);

        Usuario signer = decision.signerUserId() == null ? actor : usuarioRepository.findById(decision.signerUserId()).orElse(actor);
        OfficeSignatureQueueItem queueItem = decision.queueItemId() == null
                ? null
                : officeSignatureQueueRepository.findById(decision.queueItemId())
                        .orElseThrow(() -> new EntityNotFoundException("Fila processual nao encontrada."));
        return new DelegationRouting(decision, signer, queueItem);
    }

    public record DelegationRouting(OfficeDelegationService.Decision decision, Usuario signer, OfficeSignatureQueueItem queueItem) {
    }
}
