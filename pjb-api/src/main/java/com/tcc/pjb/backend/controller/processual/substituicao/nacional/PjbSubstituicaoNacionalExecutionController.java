package com.tcc.pjb.backend.controller.processual.substituicao.nacional;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoAcao;
import com.tcc.pjb.backend.controller.processual.substituicao.routes.PjbSubstituicaoNacionalRoutes;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoSituacao;
import com.tcc.pjb.backend.model.dto.api.ApiCommandResponse;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.cockpit.PjbSubstituicaoNacionalCockpitResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.execucao.PjbSubstituicaoNacionalExecucaoCommandRequest;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.execucao.PjbSubstituicaoNacionalExecucaoCommandResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.execucao.PjbSubstituicaoNacionalExecucaoControleRequest;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.execucao.PjbSubstituicaoNacionalExecucaoOperacionalResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.execucao.PjbSubstituicaoNacionalExecucaoResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.tribunal.PjbSubstituicaoTribunalEvidenciaExportavelResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.tribunal.PjbSubstituicaoTribunalReconciliacaoResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import com.tcc.pjb.backend.service.processual.substituicao.nacional.execucao.PjbSubstituicaoNacionalExecutionFacadeService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(prefix = "pjb.runtime.barrier.features", name = "substituicao-nacional", havingValue = "true", matchIfMissing = true)
@RequestMapping(PjbSubstituicaoNacionalRoutes.CANONICAL_BASE)
public class PjbSubstituicaoNacionalExecutionController {

    private final PjbSubstituicaoNacionalExecutionFacadeService facadeService;
    private final ApiResponseFactory apiResponseFactory;

    public PjbSubstituicaoNacionalExecutionController(PjbSubstituicaoNacionalExecutionFacadeService facadeService,
                                                      ApiResponseFactory apiResponseFactory) {
        this.facadeService = Objects.requireNonNull(facadeService);
        this.apiResponseFactory = Objects.requireNonNull(apiResponseFactory);
    }

    @PostMapping(PjbSubstituicaoNacionalRoutes.PATH_EXECUCOES)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiCommandResponse<PjbSubstituicaoNacionalExecucaoCommandResponse>> submeter(@Valid @RequestBody PjbSubstituicaoNacionalExecucaoCommandRequest request,
                                                                                                        @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
                                                                                                        Authentication authentication) {
        String requestedBy = authentication == null ? "sistema" : Objects.toString(authentication.getName(), "sistema");
        PjbSubstituicaoNacionalExecucaoCommandResponse response = facadeService.submeter(request, requestedBy, idempotencyKey);
        return ResponseEntity.accepted().body(apiResponseFactory.commandAccepted("Execução nacional enfileirada para orquestração governada.", response, List.of()));
    }

    @GetMapping(PjbSubstituicaoNacionalRoutes.PATH_EXECUCOES)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiQueryResponse<List<PjbSubstituicaoNacionalExecucaoResponse>>> listar(@RequestParam(required = false) String tribunalCodigo,
                                                                                                   @RequestParam(required = false) PjbSubstituicaoExecucaoAcao acao,
                                                                                                   @RequestParam(required = false) PjbSubstituicaoExecucaoSituacao situacao) {
        List<PjbSubstituicaoNacionalExecucaoResponse> response = facadeService.listar(tribunalCodigo, acao, situacao);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, List.of()));
    }

    @GetMapping(PjbSubstituicaoNacionalRoutes.PATH_EXECUCAO_ID)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiQueryResponse<PjbSubstituicaoNacionalExecucaoResponse>> detalhar(@PathVariable Long execucaoId) {
        PjbSubstituicaoNacionalExecucaoResponse response = facadeService.detalhar(execucaoId);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, List.of()));
    }

    @GetMapping(PjbSubstituicaoNacionalRoutes.PATH_EXECUCAO_OPERACIONAL)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiQueryResponse<PjbSubstituicaoNacionalExecucaoOperacionalResponse>> detalharOperacional(@PathVariable Long execucaoId) {
        PjbSubstituicaoNacionalExecucaoOperacionalResponse response = facadeService.detalharOperacional(execucaoId);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, List.of()));
    }

    @GetMapping(PjbSubstituicaoNacionalRoutes.PATH_COCKPIT)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiQueryResponse<PjbSubstituicaoNacionalCockpitResponse>> cockpit(@RequestParam(required = false) String tribunalCodigo) {
        PjbSubstituicaoNacionalCockpitResponse response = facadeService.cockpit(tribunalCodigo);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, List.of()));
    }

    @GetMapping(PjbSubstituicaoNacionalRoutes.PATH_RECONCILIACAO_TRIBUNAL)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiQueryResponse<PjbSubstituicaoTribunalReconciliacaoResponse>> reconciliarTribunal(@PathVariable String tribunalCodigo) {
        PjbSubstituicaoTribunalReconciliacaoResponse response = facadeService.reconciliarTribunal(tribunalCodigo);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, List.of()));
    }

    @GetMapping(PjbSubstituicaoNacionalRoutes.PATH_EVIDENCIA_EXPORTAVEL_TRIBUNAL)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiQueryResponse<PjbSubstituicaoTribunalEvidenciaExportavelResponse>> evidenciasExportaveisTribunal(@PathVariable String tribunalCodigo) {
        PjbSubstituicaoTribunalEvidenciaExportavelResponse response = facadeService.evidenciasExportaveisTribunal(tribunalCodigo);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, List.of()));
    }

    @PutMapping(PjbSubstituicaoNacionalRoutes.PATH_EXECUCAO_CONTROLE)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
    public ResponseEntity<ApiCommandResponse<PjbSubstituicaoNacionalExecucaoResponse>> controlar(@PathVariable Long execucaoId,
                                                                                                  @Valid @RequestBody PjbSubstituicaoNacionalExecucaoControleRequest request) {
        PjbSubstituicaoNacionalExecucaoResponse response = facadeService.controlar(execucaoId, request);
        return ResponseEntity.ok(apiResponseFactory.commandOk("Controle operacional aplicado à execução nacional.", response, List.of()));
    }
}
