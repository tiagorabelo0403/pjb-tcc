package com.tcc.pjb.backend.controller.advogado;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoOfficeBulkApproveResponse;
import com.tcc.pjb.backend.modules.advocacia.office.dto.BulkApproveRequest;
import com.tcc.pjb.backend.modules.advocacia.office.dto.DecisionRequest;
import com.tcc.pjb.backend.modules.advocacia.office.dto.DelegacaoRegraDto;
import com.tcc.pjb.backend.modules.advocacia.office.dto.OfficePolicyDto;
import com.tcc.pjb.backend.modules.advocacia.office.dto.OfficeQueueItemDto;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeQueueStatus;
import com.tcc.pjb.backend.service.advogado.surface.AdvogadoSurfaceFacadeService;

@RestController
@RequestMapping("/api/v1/advogado/escritorio")
@Validated
@PreAuthorize("hasAuthority('ROLE_ADVOGADO')")
public class AdvogadoEscritorioController {

    private final AdvogadoSurfaceFacadeService facadeService;

    public AdvogadoEscritorioController(AdvogadoSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/policy")
    public OfficePolicyDto getPolicy(@RequestParam(value = "equipeId", required = false) Long equipeId) {
        return facadeService.getPolicy(equipeId);
    }

    @PutMapping("/policy")
    public OfficePolicyDto updatePolicy(@RequestParam(value = "equipeId", required = false) Long equipeId,
                                        @Valid @RequestBody(required = false) OfficePolicyDto req) {
        return facadeService.updatePolicy(equipeId, req);
    }

    @PutMapping("/delegacoes/{usuarioId}")
    public DelegacaoRegraDto upsertRegra(@RequestParam(value = "equipeId", required = false) Long equipeId,
                                         @PathVariable @Positive Long usuarioId,
                                         @Valid @RequestBody(required = false) DelegacaoRegraDto req) {
        return facadeService.upsertRegra(equipeId, usuarioId, req);
    }

    @GetMapping("/delegacoes/{usuarioId}")
    public DelegacaoRegraDto getRegra(@RequestParam(value = "equipeId", required = false) Long equipeId,
                                      @PathVariable @Positive Long usuarioId) {
        return facadeService.getRegra(equipeId, usuarioId);
    }

    @GetMapping("/delegacoes")
    public List<DelegacaoRegraDto> listRegras(@RequestParam(value = "equipeId", required = false) Long equipeId) {
        return facadeService.listRegras(equipeId);
    }

    @GetMapping("/assinaturas/queue")
    public Page<OfficeQueueItemDto> listQueue(@RequestParam(value = "status", required = false) OfficeQueueStatus status,
                                              Pageable pageable) {
        return facadeService.listQueue(status, pageable);
    }

    @PostMapping("/assinaturas/queue/{id}/approve")
    @ResponseStatus(HttpStatus.OK)
    public OfficeQueueItemDto approve(@PathVariable("id") @Positive Long id,
                                      @Valid @RequestBody(required = false) DecisionRequest req) {
        return facadeService.approve(id, req);
    }

    @PostMapping("/assinaturas/queue/{id}/reject")
    @ResponseStatus(HttpStatus.OK)
    public OfficeQueueItemDto reject(@PathVariable("id") @Positive Long id,
                                     @Valid @RequestBody(required = false) DecisionRequest req) {
        return facadeService.reject(id, req);
    }

    @PostMapping("/assinaturas/queue/bulk-approve")
    public AdvogadoOfficeBulkApproveResponse bulkApprove(@Valid @RequestBody BulkApproveRequest req) {
        return facadeService.bulkApprove(req);
    }
}
