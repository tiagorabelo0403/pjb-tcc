package com.tcc.pjb.backend.controller.processual.completude;

import com.tcc.pjb.backend.model.dto.processual.completude.apisurface.ProcessoApiSurfaceSanityResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.certificacao.ProcessoCertificacaoOperacionalResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.codebase.ProcessoCodebaseLearningResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.codebase.ProcessoCodebaseSanityResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.ProcessoCompletudeModuloResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.ProcessoFechamentoTotalResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.infraestrutura.ProcessoInfraestruturaSoberanaResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.substituicao.ProcessoSubstituicaoLegadosResponse;
import com.tcc.pjb.backend.service.processual.completude.ProcessoCompletudeArquiteturalFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processual/unificado")
public class ProcessoCompletudeArquiteturalController {

    private final ProcessoCompletudeArquiteturalFacadeService facadeService;

    public ProcessoCompletudeArquiteturalController(ProcessoCompletudeArquiteturalFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/{processoId}/anti-orfao")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoCompletudeModuloResponse> antiOrfao(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.antiOrfao(processoId));
    }

    @GetMapping("/{processoId}/sinalizacao-regra")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoCompletudeModuloResponse> sinalizacao(@PathVariable Long processoId,
                                                                        @RequestParam(name = "profileCode", required = false) String profileCode) {
        return ResponseEntity.ok(facadeService.sinalizacao(processoId, profileCode));
    }

    @GetMapping("/{processoId}/plantao-substituicao")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoCompletudeModuloResponse> plantaoSubstituicao(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.plantaoSubstituicao(processoId));
    }

    @GetMapping("/{processoId}/analytics-nacional")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoCompletudeModuloResponse> analyticsNacional(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.analyticsNacional(processoId));
    }

    @GetMapping("/{processoId}/operacao-transversal")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoCompletudeModuloResponse> operacaoTransversal(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.operacaoTransversal(processoId));
    }

    @GetMapping("/{processoId}/infraestrutura-soberana")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoInfraestruturaSoberanaResponse> infraestruturaSoberana(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.infraestruturaSoberana(processoId));
    }

    @GetMapping("/{processoId}/certificacao-operacional")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoCertificacaoOperacionalResponse> certificacaoOperacional(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.certificacaoOperacional(processoId));
    }

    @GetMapping("/{processoId}/substituicao-legados")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSubstituicaoLegadosResponse> substituicaoLegados(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.substituicaoLegados(processoId));
    }

    @GetMapping("/sanidade-codigo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoCodebaseSanityResponse> sanidadeCodigo(@RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {
        return ResponseEntity.ok(facadeService.sanidadeCodigo(refresh));
    }

    @GetMapping("/sanidade-api")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoApiSurfaceSanityResponse> sanidadeApi() {
        return ResponseEntity.ok(facadeService.sanidadeApi());
    }

    @GetMapping("/sanidade-aprendizado")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoCodebaseLearningResponse> sanidadeAprendizado(@RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {
        return ResponseEntity.ok(facadeService.sanidadeAprendizado(refresh));
    }

    @GetMapping("/{processoId}/fechamento-total")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoFechamentoTotalResponse> fechamentoTotal(@PathVariable Long processoId,
                                                                           @RequestParam(name = "profileCode", required = false) String profileCode) {
        return ResponseEntity.ok(facadeService.fechamentoTotal(processoId, profileCode));
    }
}
