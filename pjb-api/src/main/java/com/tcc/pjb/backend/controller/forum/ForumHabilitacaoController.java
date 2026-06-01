package com.tcc.pjb.backend.controller.forum;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import com.tcc.pjb.backend.model.dto.forum.ForumHabilitacaoDecisaoRequest;
import com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer.LaianeLawyerProcuracaoResponse;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeProcuracao;
import com.tcc.pjb.backend.service.forum.ForumHabilitacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyJsonExtractor;

@RestController
@RequestMapping(OperationalApiRoutes.FORUM_HABILITACOES_BASE)
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_SERVIDOR_JUDICIARIO','ROLE_SERVIDOR_FORUM','ROLE_ADMIN','ROLE_ADMINISTRADOR')")
public class ForumHabilitacaoController {

    private final ForumHabilitacaoService habilitacaoService;
    private final ObjectMapper objectMapper;

    @GetMapping(OperationalApiRoutes.PATH_FORUM_HABILITACOES_PENDENTES)
    public Page<LaianeLawyerProcuracaoResponse> listarPendentes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Page<LaianeProcuracao> p = habilitacaoService.listarPendentes(page, Math.min(Math.max(size, 1), 200));
        return p.map(this::toResponse);
    }

    @PostMapping(OperationalApiRoutes.PATH_FORUM_HABILITACOES_DEFERIR)
    public LaianeLawyerProcuracaoResponse deferir(
            @PathVariable Long id,
            @Valid @RequestBody ForumHabilitacaoDecisaoRequest req
    ) {
        LaianeProcuracao p = habilitacaoService.deferir(id, req.getMotivo());
        return toResponse(p);
    }

    @PostMapping(OperationalApiRoutes.PATH_FORUM_HABILITACOES_INDEFERIR)
    public LaianeLawyerProcuracaoResponse indeferir(
            @PathVariable Long id,
            @Valid @RequestBody ForumHabilitacaoDecisaoRequest req
    ) {
        LaianeProcuracao p = habilitacaoService.indeferir(id, req.getMotivo());
        return toResponse(p);
    }

    private LaianeLawyerProcuracaoResponse toResponse(LaianeProcuracao proc) {
        return LaianeLawyerProcuracaoResponse.builder()
                .id(proc.getId())
                .clienteId(proc.getClienteId())
                .status(proc.getStatus())
                .processoId(proc.getProcessoId())
                .inicioVigencia(proc.getInicioVigencia())
                .fimVigencia(proc.getFimVigencia())
                .poderes(proc.getPoderes())
                .anexosJson(proc.getAnexosJson())
                .representacaoPolicy(extractRepresentacaoPolicy(proc.getPoderes()))
                .build();
    }

    private java.util.Map<String, Object> extractRepresentacaoPolicy(String poderes) {
        return RepresentacaoProcessualPolicyJsonExtractor.extract(objectMapper, poderes);
    }
}