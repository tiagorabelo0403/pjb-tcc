package com.tcc.pjb.backend.controller.processual;

import com.tcc.pjb.backend.core.processo.conclusao.application.ConclusaoProcessualApplicationService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.api.ApiCommandResponse;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.model.dto.conclusao.ConclusaoProcessualCriarRequest;
import com.tcc.pjb.backend.model.dto.conclusao.ConclusaoProcessualPainelResponse;
import com.tcc.pjb.backend.model.dto.conclusao.ConclusaoProcessualResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/processo/conclusao")
public class ConclusaoProcessualController {

    private static final String SERVIDOR_ROLES = "hasAnyRole('SERVIDOR','SERVIDOR_FORUM')";
    private static final String JUDGE_ROLES = "hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR')";
    private static final String SERVIDOR_OU_JUIZ_ROLES = "hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR')";

    private final ConclusaoProcessualApplicationService applicationService;
    private final CurrentUserService currentUserService;
    private final ApiResponseFactory apiResponseFactory;

    public ConclusaoProcessualController(ConclusaoProcessualApplicationService applicationService,
                                         CurrentUserService currentUserService,
                                         ApiResponseFactory apiResponseFactory) {
        this.applicationService = Objects.requireNonNull(applicationService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.apiResponseFactory = Objects.requireNonNull(apiResponseFactory);
    }

    @PostMapping
    @PreAuthorize(SERVIDOR_ROLES)
    public ResponseEntity<ApiCommandResponse<?>> concluir(@Valid @RequestBody ConclusaoProcessualCriarRequest request) {
        Usuario servidor = currentUserService.getRequired();
        var conclusao = applicationService.concluir(request.processoId(), request.magistradoId(), servidor.getId(),
                request.tipoConclusao(), request.motivo());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiResponseFactory.commandOk("processo concluso ao magistrado", ConclusaoProcessualResponse.of(conclusao), List.of()));
    }

    @PostMapping("/{conclusaoId}/devolver")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<ApiCommandResponse<?>> devolver(@PathVariable Long conclusaoId) {
        Usuario magistrado = currentUserService.getRequired();
        applicationService.devolver(conclusaoId, magistrado.getId());
        return ResponseEntity.ok(apiResponseFactory.commandOk("conclusão devolvida", Map.of("conclusaoId", conclusaoId), List.of()));
    }

    @GetMapping("/pendentes")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<ApiQueryResponse<?>> pendentes() {
        Usuario magistrado = currentUserService.getRequired();
        List<ConclusaoProcessualResponse> itens = applicationService.pendentesDoMagistrado(magistrado.getId()).stream()
                .map(ConclusaoProcessualResponse::of)
                .toList();
        return ResponseEntity.ok(apiResponseFactory.queryOk(new ConclusaoProcessualPainelResponse(magistrado.getId(), itens.size(), itens), List.of()));
    }

    @PostMapping("/processar-expiradas")
    @PreAuthorize(SERVIDOR_OU_JUIZ_ROLES)
    public ResponseEntity<ApiCommandResponse<?>> processarExpiradas() {
        int total = applicationService.processarExpiradas();
        return ResponseEntity.ok(apiResponseFactory.commandOk("conclusões expiradas processadas", Map.of("total", total), List.of()));
    }
}
