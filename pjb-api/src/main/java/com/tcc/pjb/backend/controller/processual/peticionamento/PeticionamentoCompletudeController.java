package com.tcc.pjb.backend.controller.processual.peticionamento;

import com.tcc.pjb.backend.core.protocolo.completude.ContextoValidacaoCompletude;
import com.tcc.pjb.backend.core.protocolo.completude.ProtocoloCompletudeOverrideService;
import com.tcc.pjb.backend.core.protocolo.completude.ProtocoloCompletudeValidator;
import com.tcc.pjb.backend.core.protocolo.completude.domain.ResultadoValidacao;
import com.tcc.pjb.backend.core.protocolo.completude.domain.ViolacaoCompletude;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.completude.OverrideCompletudeRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.completude.PreValidacaoCompletudeRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.completude.PreValidacaoCompletudeResponse;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.ProtocoloCompletudeStatus;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoRepresentanteProcessual;
import com.tcc.pjb.backend.model.entity.protocolo.ProtocoloPendencia;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/peticionamento/completude")
@PreAuthorize("isAuthenticated()")
public class PeticionamentoCompletudeController {

    private final ProtocoloCompletudeValidator validator;
    private final ProtocoloCompletudeOverrideService overrideService;
    private final CurrentUserService currentUserService;

    public PeticionamentoCompletudeController(ProtocoloCompletudeValidator validator,
                                               ProtocoloCompletudeOverrideService overrideService,
                                               CurrentUserService currentUserService) {
        this.validator = Objects.requireNonNull(validator);
        this.overrideService = Objects.requireNonNull(overrideService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
    }

    @PostMapping("/pre-validar")
    public ResponseEntity<PreValidacaoCompletudeResponse> preValidar(
            Authentication authentication,
            @Valid @RequestBody PreValidacaoCompletudeRequest request) {

        ContextoValidacaoCompletude contexto = new ContextoValidacaoCompletude(
                request.rito(),
                resolveRepresentante(authentication),
                request.tipoPartePrincipal(),
                request.condicoesAplicaveis() != null ? request.condicoesAplicaveis() : java.util.Set.of(),
                request.documentosAnexados() != null ? request.documentosAnexados() : java.util.List.of(),
                LocalDate.now());

        ResultadoValidacao resultado = validator.validar(contexto);

        PreValidacaoCompletudeResponse response = new PreValidacaoCompletudeResponse(
                resultado.statusResultante(),
                !resultado.temBloqueante(),
                resultado.versaoRegraAplicada(),
                resultado.violacoes().stream().map(this::toDto).toList());

        return ResponseEntity.ok(response);
    }

    private PreValidacaoCompletudeResponse.ViolacaoCompletudeDto toDto(ViolacaoCompletude v) {
        boolean sensivel = v.nivelSensibilidade() != null && v.nivelSensibilidade().exigeCredencial();
        PreValidacaoCompletudeResponse.FundamentoDto fund = v.fundamento() == null ? null
                : new PreValidacaoCompletudeResponse.FundamentoDto(
                        v.fundamento().tipo() != null ? v.fundamento().tipo().name() : null,
                        v.fundamento().identificador(),
                        v.fundamento().resumo(),
                        v.fundamento().grau() != null ? v.fundamento().grau().name() : null);

        return new PreValidacaoCompletudeResponse.ViolacaoCompletudeDto(
                v.codigo(),
                v.severidade().name(),
                sensivel ? "DADO_SENSIVEL" : v.campo(),
                sensivel ? "Contacte a secretaria para orientações sobre este documento." : v.acaoCorretiva(),
                fund);
    }

    private TipoRepresentanteProcessual resolveRepresentante(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) return TipoRepresentanteProcessual.PARTE_SEM_ADVOGADO;
        for (var authority : auth.getAuthorities()) {
            if (authority == null) continue;
            String a = authority.getAuthority();
            if (a == null) continue;
            String upper = a.toUpperCase();
            if (upper.contains("DEFENSOR")) return TipoRepresentanteProcessual.DEFENSOR_PUBLICO;
            if (upper.contains("PROMOTOR") || upper.contains("PROCURADOR") || upper.contains("MINISTERIO_PUBLICO"))
                return TipoRepresentanteProcessual.MINISTERIO_PUBLICO;
            if (upper.contains("ADVOGADO") || upper.contains("OAB"))
                return TipoRepresentanteProcessual.ADVOGADO_PRIVADO;
        }
        return TipoRepresentanteProcessual.PARTE_SEM_ADVOGADO;
    }

    @PostMapping("/override/{protocoloId}")
    @PreAuthorize("hasAnyRole('SERVIDOR_FORUM','ADMIN','MAGISTRADO','JUIZ')")
    public ResponseEntity<Map<String, Object>> dispensar(
            @PathVariable Long protocoloId,
            @Valid @RequestBody OverrideCompletudeRequest request) {

        Long servidorId = currentUserService.getRequired().getId();
        ProtocoloPendencia dispensada = overrideService.dispensar(protocoloId, request.justificativa(), servidorId);

        return ResponseEntity.ok(Map.of(
                "protocoloId", protocoloId,
                "status", dispensada.getStatus().name(),
                "dispensadoPor", servidorId,
                "mensagem", "Pendência dispensada. O protocolo pode avançar para distribuição."));
    }
}
