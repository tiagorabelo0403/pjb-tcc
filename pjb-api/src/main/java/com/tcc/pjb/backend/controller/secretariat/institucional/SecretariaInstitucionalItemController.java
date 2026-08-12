package com.tcc.pjb.backend.controller.secretariat.institucional;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.secretariat.SecretariaInstitucionalFilaResponse;
import com.tcc.pjb.backend.model.entity.SecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.repository.SecretariaInstitucionalItemRepository;
import com.tcc.pjb.backend.service.secretariat.institucional.SecretariaInstitucionalFilaService;
import com.tcc.pjb.backend.service.secretariat.institucional.TomarCienciaService;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecretariaInstitucionalItemController {

    private final TomarCienciaService tomarCienciaService;
    private final SecretariaInstitucionalFilaService filaService;
    private final CurrentUserService currentUserService;
    private final SecretariaInstitucionalItemRepository itemRepository;

    public SecretariaInstitucionalItemController(TomarCienciaService tomarCienciaService,
                                                  SecretariaInstitucionalFilaService filaService,
                                                  CurrentUserService currentUserService,
                                                  SecretariaInstitucionalItemRepository itemRepository) {
        this.tomarCienciaService = Objects.requireNonNull(tomarCienciaService);
        this.filaService = Objects.requireNonNull(filaService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.itemRepository = Objects.requireNonNull(itemRepository);
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

    @GetMapping("/api/v1/secretaria-institucional/{unidadeId}/fila")
    public ResponseEntity<SecretariaInstitucionalFilaResponse> consultarFila(@PathVariable Long unidadeId) {
        Usuario usuario = currentUserService.getRequired();
        return ResponseEntity.ok(filaService.consultarFila(usuario, unidadeId));
    }

    @GetMapping("/api/v1/secretaria-institucional/sem-unidade-resolvida")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<SecretariaInstitucionalItem>> itensSemUnidadeResolvida() {
        return ResponseEntity.ok(itemRepository.findByStatus(StatusSecretariaInstitucionalItem.SEM_UNIDADE_RESOLVIDA));
    }
}
