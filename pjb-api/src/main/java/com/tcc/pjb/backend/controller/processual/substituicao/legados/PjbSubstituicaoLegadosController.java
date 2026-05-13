package com.tcc.pjb.backend.controller.processual.substituicao.legados;

import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.controller.processual.substituicao.routes.PjbSubstituicaoNacionalRoutes;
import com.tcc.pjb.backend.model.dto.processual.substituicao.legados.PjbSubstituicaoLegadosResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import com.tcc.pjb.backend.service.processual.substituicao.legados.PjbSubstituicaoLegadosFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(PjbSubstituicaoNacionalRoutes.CANONICAL_BASE)
public class PjbSubstituicaoLegadosController {

    private final PjbSubstituicaoLegadosFacadeService pjbSubstituicaoLegadosFacadeService;
    private final ApiResponseFactory apiResponseFactory;

    public PjbSubstituicaoLegadosController(PjbSubstituicaoLegadosFacadeService pjbSubstituicaoLegadosFacadeService,
                                            ApiResponseFactory apiResponseFactory) {
        this.pjbSubstituicaoLegadosFacadeService = pjbSubstituicaoLegadosFacadeService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @GetMapping(PjbSubstituicaoNacionalRoutes.PATH_LEGADOS_PROCESSO)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiQueryResponse<PjbSubstituicaoLegadosResponse>> avaliar(@PathVariable Long processoId) {
        PjbSubstituicaoLegadosResponse response = pjbSubstituicaoLegadosFacadeService.avaliar(processoId);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.fundamentos()));
    }
}
