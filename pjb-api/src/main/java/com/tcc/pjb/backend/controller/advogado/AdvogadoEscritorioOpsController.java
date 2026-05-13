package com.tcc.pjb.backend.controller.advogado;

import java.time.LocalDateTime;
import java.util.List;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.configs.EquipeContexto;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.modules.advocacia.office.dto.OfficeDelegatedActionOpsDto;
import com.tcc.pjb.backend.modules.advocacia.office.dto.OfficeSignerDashboardRowDto;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeDelegationMode;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeQueueStatus;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeOpsService;
import com.tcc.pjb.backend.service.exception.EquipeNaoSelecionadaException;

@RestController
@RequestMapping("/api/v1/advogado/escritorio/ops")
@Validated
@PreAuthorize("hasAuthority('ROLE_ADVOGADO')")
public class AdvogadoEscritorioOpsController {

    private final CurrentUserService currentUserService;
    private final OfficeOpsService opsService;

    public AdvogadoEscritorioOpsController(CurrentUserService currentUserService, OfficeOpsService opsService) {
        this.currentUserService = currentUserService;
        this.opsService = opsService;
    }

    @GetMapping("/dashboard")
    public List<OfficeSignerDashboardRowDto> dashboard(@RequestParam(value = "equipeId", required = false) Long equipeId,
                                                      @RequestParam(value = "from", required = false) LocalDateTime from,
                                                      @RequestParam(value = "to", required = false) LocalDateTime to) {
        long signerUserId = currentUserService.currentUserIdOrZero();
        return opsService.dashboard(resolveEquipeId(equipeId), signerUserId, from, to);
    }

    @GetMapping("/delegated-actions")
    public Page<OfficeDelegatedActionOpsDto> delegatedActions(@RequestParam(value = "equipeId", required = false) Long equipeId,
                                                             @RequestParam(value = "executorUserId", required = false) @Positive Long executorUserId,
                                                             @RequestParam(value = "signerUserId", required = false) @Positive Long signerUserId,
                                                             @RequestParam(value = "actionType", required = false) OfficeActionType actionType,
                                                             @RequestParam(value = "mode", required = false) OfficeDelegationMode mode,
                                                             @RequestParam(value = "queueStatus", required = false) OfficeQueueStatus queueStatus,
                                                             @RequestParam(value = "resourceType", required = false) String resourceType,
                                                             @RequestParam(value = "resourceId", required = false) String resourceId,
                                                             @RequestParam(value = "from", required = false) LocalDateTime from,
                                                             @RequestParam(value = "to", required = false) LocalDateTime to,
                                                             Pageable pageable) {
        return opsService.delegatedActions(resolveEquipeId(equipeId), executorUserId, signerUserId, actionType, mode, queueStatus, resourceType, resourceId, from, to, pageable);
    }

    private Long resolveEquipeId(Long equipeId) {
        if (equipeId != null) return equipeId;
        MembroEquipe m = EquipeContexto.getMembroDaEquipeAtiva();
        if (m == null || m.getEquipe() == null || m.getEquipe().getId() == null) {
            throw new EquipeNaoSelecionadaException("Equipe não selecionada. Informe o header X-Equipe-ID.", "X-Equipe-ID", List.of());
        }
        return m.getEquipe().getId();
    }
}
