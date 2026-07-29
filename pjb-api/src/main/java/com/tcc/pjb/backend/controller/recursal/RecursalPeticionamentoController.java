package com.tcc.pjb.backend.controller.recursal;

import com.tcc.pjb.backend.model.dto.profile.operational.InstitutionalRecursoRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.processual.recursal.RecursalPeticionamentoPerfilRouter;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recursal")
@Validated
@Tag(name = "Recursal — Peticionamento",
     description = "Superfície unificada de interposição recursal por perfil profissional. Substitui, em coexistência aditiva, os quatro endpoints legados dos controllers de advocacia, defensoria pública, ministério público e procuradoria. O perfil ativo é resolvido a partir do TipoUsuario autenticado e delega ao intermediate service correspondente, preservando os guards materiais por perfil.")
public class RecursalPeticionamentoController {

    private static final String ROLES_RECURSAL_UNIFICADAS =
            "hasAnyRole('ADVOGADO','OAB_PRESIDENTE_SECCIONAL',"
            + "'DEFENSOR_PUBLICO','DEFENSOR_PUBLICO_FEDERAL',"
            + "'MEMBRO_MINISTERIO_PUBLICO','PROMOTOR_ELEITORAL','PROMOTOR_TRABALHISTA','PROCURADOR_GERAL_REPUBLICA',"
            + "'PROCURADOR','PROCURADORIA_MUNICIPAL','PROCURADORIA_ESTADUAL','PROCURADORIA_FEDERAL')";

    private final RecursalPeticionamentoPerfilRouter router;
    private final SurfaceProjectionSupport projectionSupport;
    private final CapabilityRateLimiter rateLimiter;

    public RecursalPeticionamentoController(RecursalPeticionamentoPerfilRouter router,
                                            SurfaceProjectionSupport projectionSupport,
                                            CapabilityRateLimiter rateLimiter) {
        this.router = router;
        this.projectionSupport = projectionSupport;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/processos/{processoId}/recurso")
    @PreAuthorize(ROLES_RECURSAL_UNIFICADAS)
    @Operation(summary = "Interpor recurso — superfície unificada por perfil",
               description = "Recebe a interposição recursal de qualquer perfil legitimado (advocacia, defensoria pública, ministério público, procuradoria). A resposta é envelopada em SurfaceActionResponse com scope 'recursal.peticionamento.<perfil>' e o corpo do engine em fields[]. Rate limit por capability única 'recursal_peticionamento_recurso' com domínio derivado do perfil (LAWYER × INSTITUCIONAL).")
    public ResponseEntity<SurfaceActionResponse> interporRecurso(@PathVariable Long processoId,
                                                                 @Valid @RequestBody InstitutionalRecursoRequest request,
                                                                 Authentication authentication) {
        RecursalPeticionamentoPerfilRouter.Perfil perfil = router.resolverPerfilAtivo();
        rateLimiter.enforce(perfil.rateLimitDomain(), authentication, "recursal_peticionamento_recurso", ApiVersion.V1);
        Map<String, Object> resultado = router.interporRecurso(
                perfil,
                processoId,
                request.tipoRecurso(),
                request.razoes(),
                request.fundamentacao(),
                request.pedidoEfeitoSuspensivo(),
                request.preparoDispensado(),
                request.observacoes()
        );
        String scope = "recursal.peticionamento." + perfil.scopeToken();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectionSupport.action(scope, "interpor-recurso", processoId, resultado));
    }
}
