package com.tcc.pjb.backend.controller.intelligence;

import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import jakarta.validation.Valid;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.triagem.TriagemNacionalRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.service.triagem.TriagemNacionalIAEngine;

@RestController
@RequestMapping("/api/v1/intelligence/triagem")
@PreAuthorize("isAuthenticated()")
public class TriagemNacionalController {

    private final TriagemNacionalIAEngine triagemNacionalIAEngine;
    private final CurrentUserService currentUserService;

    public TriagemNacionalController(TriagemNacionalIAEngine triagemNacionalIAEngine,
                                     CurrentUserService currentUserService) {
        this.triagemNacionalIAEngine = Objects.requireNonNull(triagemNacionalIAEngine);
        this.currentUserService = Objects.requireNonNull(currentUserService);
    }

    @PostMapping("/analisar")
    public ResponseEntity<TriagemNacionalIAEngine.ResultadoTriagem> analisar(@Valid @RequestBody TriagemNacionalRequest request) {
        validarAcesso();
        TriagemNacionalIAEngine.PedidoTriagem pedido = new TriagemNacionalIAEngine.PedidoTriagem(
                request.nupnProvisorio(),
                request.classeTpuSugerida(),
                request.assuntoTpuSugerido(),
                request.ramoDireito(),
                request.valorCausa(),
                request.textoFatosResumido(),
                request.cpfCnpjAutor(),
                request.cpfCnpjReu(),
                request.oabAdvogado(),
                request.ufAdvogado(),
                request.documentosAnexados() == null ? List.of() : request.documentosAnexados(),
                request.dataFatoGerador(),
                Boolean.TRUE.equals(request.requerLiminar()),
                Boolean.TRUE.equals(request.atoJurisdicionalAnterior()),
                null
        );
        return ResponseEntity.ok(triagemNacionalIAEngine.triarERegistrar(pedido));
    }

    @GetMapping("/analises/{nupnProvisorio}")
    public ResponseEntity<TriagemNacionalIAEngine.AnaliseRegistradaView> buscarUltima(@PathVariable String nupnProvisorio) {
        validarAcesso();
        return triagemNacionalIAEngine.buscarUltimaPorNupn(nupnProvisorio)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/analises")
    public ResponseEntity<TriagemNacionalIAEngine.AnaliseRegistradaView> buscarPorProcesso(@RequestParam("processoId") Long processoId) {
        validarAcesso();
        return triagemNacionalIAEngine.buscarUltimaPorProcessoId(processoId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private void validarAcesso() {
        Usuario usuario = currentUserService.getRequired();
        if (usuario.isAdvogado()
                || usuario.isMagistrado()
                || usuario.isMinisterioPublico()
                || usuario.isDefensoriaPublica()
                || usuario.isServidorJudiciario()
                || usuario.isAdminForum()) {
            return;
        }
        if (usuario.getTipoUsuario() != null && (usuario.getTipoUsuario().isAdministradorSistema() || usuario.getTipoUsuario().isProcuradoria())) {
            return;
        }
        throw new ResponseStatusException(FORBIDDEN, "Perfil sem permissao para triagem nacional assistida");
    }
}
