package com.tcc.pjb.backend.core.servidor.api;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.servidor.api.dto.FuncaoServidorSolicitacaoCreateRequest;
import com.tcc.pjb.backend.core.servidor.api.dto.FuncaoServidorSolicitacaoDecisaoRequest;
import com.tcc.pjb.backend.core.servidor.api.dto.FuncaoServidorSolicitacaoResponse;
import com.tcc.pjb.backend.core.servidor.application.FuncaoServidorSolicitacaoService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/servidores/designacoes/solicitacoes")
@PreAuthorize("isAuthenticated()")
public class FuncaoServidorSolicitacaoController {

    private final FuncaoServidorSolicitacaoService solicitacaoService;
    private final CurrentUserService currentUserService;

    public FuncaoServidorSolicitacaoController(FuncaoServidorSolicitacaoService solicitacaoService,
                                                CurrentUserService currentUserService) {
        this.solicitacaoService = Objects.requireNonNull(solicitacaoService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
    }

    @PostMapping
    public FuncaoServidorSolicitacaoResponse solicitar(@Valid @RequestBody FuncaoServidorSolicitacaoCreateRequest request) {
        Long solicitanteId = currentUserService.getRequired().getId();
        var criada = solicitacaoService.solicitar(solicitanteId, request.unidadeId(), request.funcao(), request.motivo());
        return FuncaoServidorSolicitacaoResponse.from(criada);
    }

    @GetMapping("/me")
    public List<FuncaoServidorSolicitacaoResponse> minhasSolicitacoes() {
        Long solicitanteId = currentUserService.getRequired().getId();
        return solicitacaoService.listarPorSolicitante(solicitanteId).stream()
                .map(FuncaoServidorSolicitacaoResponse::from)
                .toList();
    }

    @PostMapping("/{id}/aprovar")
    public FuncaoServidorSolicitacaoResponse aprovar(@PathVariable Long id) {
        Long decisorId = currentUserService.getRequired().getId();
        return FuncaoServidorSolicitacaoResponse.from(solicitacaoService.aprovar(id, decisorId));
    }

    @PostMapping("/{id}/rejeitar")
    public FuncaoServidorSolicitacaoResponse rejeitar(@PathVariable Long id,
                                                        @RequestBody(required = false) FuncaoServidorSolicitacaoDecisaoRequest body) {
        Long decisorId = currentUserService.getRequired().getId();
        String motivo = body != null ? body.motivo() : null;
        return FuncaoServidorSolicitacaoResponse.from(solicitacaoService.rejeitar(id, decisorId, motivo));
    }
}
