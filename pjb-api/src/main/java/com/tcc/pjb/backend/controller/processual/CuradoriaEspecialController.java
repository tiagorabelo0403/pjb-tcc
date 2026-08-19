package com.tcc.pjb.backend.controller.processual;

import com.tcc.pjb.backend.core.comunicacao.judicial.CuradorEspecialAutomaticoService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.curadoria.NomearCuradorRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import jakarta.validation.Valid;
import java.util.Objects;
import java.util.Optional;
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
@RequestMapping("/api/v1/processo/curadoria-especial")
public class CuradoriaEspecialController {

    private static final String VISIBILIDADE_ROLES =
            "hasAnyRole('DEFENSOR_PUBLICO','DEFENSOR_PUBLICO_FEDERAL','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR')";
    private static final String JUDGE_ROLES = "hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR')";

    private final CuradorEspecialAutomaticoService curadorService;
    private final CurrentUserService currentUserService;

    public CuradoriaEspecialController(CuradorEspecialAutomaticoService curadorService,
                                       CurrentUserService currentUserService) {
        this.curadorService = Objects.requireNonNull(curadorService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
    }

    @GetMapping("/processos/{processoId}/necessidade")
    @PreAuthorize(VISIBILIDADE_ROLES)
    public ResponseEntity<Optional<CuradorEspecialAutomaticoService.NecessidadeCurador>> necessidade(@PathVariable Long processoId) {
        return ResponseEntity.ok(curadorService.consultarNecessidade(processoId));
    }

    @GetMapping("/processos/{processoId}/nomeacao")
    @PreAuthorize(VISIBILIDADE_ROLES)
    public ResponseEntity<Optional<CuradorEspecialAutomaticoService.NomeacaoCurador>> nomeacao(@PathVariable Long processoId) {
        return ResponseEntity.ok(curadorService.consultarNomeacao(processoId));
    }

    @PostMapping("/processos/{processoId}/nomear")
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<CuradorEspecialAutomaticoService.NomeacaoCurador> nomear(@PathVariable Long processoId,
                                                                                    @Valid @RequestBody NomearCuradorRequest request) {
        Usuario juiz = currentUserService.getRequired();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(curadorService.nomear(processoId, juiz.getId(), request.nomeCurador(), request.oabOuFuncional()));
    }
}
