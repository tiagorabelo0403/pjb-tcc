package com.tcc.pjb.backend.controller.consulta;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.service.identity.surface.ProntuarioNacionalSurfaceFacadeService;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/v1/prontuarios")
@PreAuthorize("isAuthenticated()")
public class ProntuarioNacionalController {

    private final ProntuarioNacionalSurfaceFacadeService facadeService;
    private final CurrentUserService currentUserService;
    private final DocumentoNacionalValidator documentoValidator;

    public ProntuarioNacionalController(ProntuarioNacionalSurfaceFacadeService facadeService,
                                        CurrentUserService currentUserService,
                                        DocumentoNacionalValidator documentoValidator) {
        this.facadeService = Objects.requireNonNull(facadeService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.documentoValidator = Objects.requireNonNull(documentoValidator);
    }

    @GetMapping("/meu")
    public ResponseEntity<SurfaceSnapshotResponse> meuProntuario() {
        Usuario usuario = currentUserService.getRequired();
        if (usuario.getCpf() == null || usuario.getCpf().isBlank()) {
            throw new ResponseStatusException(FORBIDDEN, "Usuario sem documento apto para consulta do prontuario nacional");
        }
        return ResponseEntity.ok(facadeService.consultarPorDocumento(usuario.getCpf()));
    }

    @GetMapping("/documento/{documento}")
    public ResponseEntity<SurfaceSnapshotResponse> consultarPorDocumento(@PathVariable String documento) {
        Usuario usuario = currentUserService.getRequired();
        validarAcessoDocumento(usuario, documento);
        return ResponseEntity.ok(facadeService.consultarPorDocumento(documento));
    }

    @GetMapping("/conflitos")
    public ResponseEntity<SurfaceSnapshotResponse> detectarConflitos(@RequestParam("autor") String documentoAutor,
                                                                     @RequestParam("reu") String documentoReu,
                                                                     @RequestParam("ramo") RamoDireito ramoDireito) {
        Usuario usuario = currentUserService.getRequired();
        if (!podeConsultarTerceiros(usuario)) {
            throw new ResponseStatusException(FORBIDDEN, "Perfil sem permissao para analise nacional de conflitos processuais");
        }
        return ResponseEntity.ok(facadeService.detectarConflitos(documentoAutor, documentoReu, ramoDireito));
    }

    private void validarAcessoDocumento(Usuario usuario, String documento) {
        String solicitado = documentoValidator.normalizarDocumento(documento);
        String proprio = documentoValidator.normalizarDocumento(usuario.getCpf());
        if (solicitado.equals(proprio)) {
            return;
        }
        if (!podeConsultarTerceiros(usuario)) {
            throw new ResponseStatusException(FORBIDDEN, "Perfil sem permissao para consultar prontuario de terceiros");
        }
    }

    private static boolean podeConsultarTerceiros(Usuario usuario) {
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return false;
        }
        return usuario.isAdvogado()
                || usuario.isMagistrado()
                || usuario.isMinisterioPublico()
                || usuario.isDefensoriaPublica()
                || usuario.isAdminForum()
                || usuario.getTipoUsuario().isAdministradorSistema()
                || usuario.getTipoUsuario().isProcuradoria();
    }
}
