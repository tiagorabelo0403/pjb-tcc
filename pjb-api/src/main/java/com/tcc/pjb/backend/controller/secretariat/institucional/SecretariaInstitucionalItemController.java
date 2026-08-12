package com.tcc.pjb.backend.controller.secretariat.institucional;

import com.tcc.pjb.backend.service.secretariat.institucional.TomarCienciaService;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecretariaInstitucionalItemController {

    private final TomarCienciaService tomarCienciaService;

    public SecretariaInstitucionalItemController(TomarCienciaService tomarCienciaService) {
        this.tomarCienciaService = Objects.requireNonNull(tomarCienciaService);
    }

    @PostMapping("/api/v1/secretaria-institucional/itens/{itemId}/tomar-ciencia")
    @PreAuthorize("hasAnyRole('MEMBRO_MINISTERIO_PUBLICO','PROMOTOR_ELEITORAL','PROMOTOR_TRABALHISTA','PROCURADOR_GERAL_REPUBLICA',"
            + "'DEFENSOR_PUBLICO','DEFENSOR_PUBLICO_FEDERAL',"
            + "'PROCURADOR','PROCURADORIA_MUNICIPAL','PROCURADORIA_ESTADUAL','PROCURADORIA_FEDERAL',"
            + "'SERVIDOR_FORUM','ADMINISTRADOR')")
    public ResponseEntity<Void> tomarCiencia(@PathVariable Long itemId) {
        tomarCienciaService.tomarCiencia(itemId);
        return ResponseEntity.ok().build();
    }
}
