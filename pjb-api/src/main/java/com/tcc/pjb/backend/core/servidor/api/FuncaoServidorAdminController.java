package com.tcc.pjb.backend.core.servidor.api;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.servidor.api.dto.DesignarServidorRequest;
import com.tcc.pjb.backend.core.servidor.api.dto.EncerrarDesignacaoRequest;
import com.tcc.pjb.backend.core.servidor.api.dto.FuncaoServidorDesignacaoResponse;
import com.tcc.pjb.backend.core.servidor.api.dto.UnidadeCandidataResponse;
import com.tcc.pjb.backend.core.servidor.application.FuncaoServidorApplicationService;
import com.tcc.pjb.backend.core.servidor.application.FuncaoServidorDesignacaoService;
import com.tcc.pjb.backend.model.repository.UnidadeJudiciariaCompetenciaRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/servidores/designacoes")
@PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR')")
public class FuncaoServidorAdminController {

    private final FuncaoServidorDesignacaoService designacaoService;
    private final FuncaoServidorApplicationService funcaoServidorApplicationService;
    private final UnidadeJudiciariaCompetenciaRepository unidadeJudiciariaCompetenciaRepository;
    private final CurrentUserService currentUserService;

    public FuncaoServidorAdminController(FuncaoServidorDesignacaoService designacaoService,
                                          FuncaoServidorApplicationService funcaoServidorApplicationService,
                                          UnidadeJudiciariaCompetenciaRepository unidadeJudiciariaCompetenciaRepository,
                                          CurrentUserService currentUserService) {
        this.designacaoService = Objects.requireNonNull(designacaoService);
        this.funcaoServidorApplicationService = Objects.requireNonNull(funcaoServidorApplicationService);
        this.unidadeJudiciariaCompetenciaRepository = Objects.requireNonNull(unidadeJudiciariaCompetenciaRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
    }

    @PostMapping
    public FuncaoServidorDesignacaoResponse designar(@Valid @RequestBody DesignarServidorRequest request) {
        Long designadoPorId = currentUserService.getRequired().getId();
        var entidade = designacaoService.designarComLotacao(request.usuarioId(), request.unidadeId(),
                request.funcao(), request.dataInicio(), designadoPorId, request.portaria());
        return FuncaoServidorDesignacaoResponse.from(entidade);
    }

    @PostMapping("/{funcaoId}/encerrar")
    public void encerrar(@PathVariable Long funcaoId, @Valid @RequestBody EncerrarDesignacaoRequest request) {
        Long operadorId = currentUserService.getRequired().getId();
        try {
            funcaoServidorApplicationService.encerrar(funcaoId, request.dataFim(), operadorId);
        } catch (EntityNotFoundException e) {
            throw new RecursoNaoEncontradoException("FuncaoServidorJudiciario", funcaoId);
        }
    }

    @GetMapping("/unidades-candidatas")
    public List<UnidadeCandidataResponse> unidadesCandidatas(@RequestParam String comarcaUf,
                                                               @RequestParam String comarcaNome) {
        return unidadeJudiciariaCompetenciaRepository
                .findAllByUfIgnoreCaseAndComarcaIgnoreCase(comarcaUf, comarcaNome).stream()
                .map(UnidadeCandidataResponse::from)
                .toList();
    }
}
