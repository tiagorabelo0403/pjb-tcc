package com.tcc.pjb.backend.controller.secretariat.institucional;

import com.tcc.pjb.backend.model.dto.secretariat.AdicionarAbrangenciaRequest;
import com.tcc.pjb.backend.model.dto.secretariat.CriarInstituicaoRequest;
import com.tcc.pjb.backend.model.dto.secretariat.CriarUnidadeInstituicaoRequest;
import com.tcc.pjb.backend.model.entity.Instituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstitucionalAbrangencia;
import com.tcc.pjb.backend.service.secretariat.institucional.UnidadeInstitucionalAdminService;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UnidadeInstitucionalAdminController {

    private final UnidadeInstitucionalAdminService service;

    public UnidadeInstitucionalAdminController(UnidadeInstitucionalAdminService service) {
        this.service = Objects.requireNonNull(service);
    }

    @PostMapping("/api/v1/secretaria-institucional/instituicoes")
    public ResponseEntity<Instituicao> criarInstituicao(@Valid @RequestBody CriarInstituicaoRequest request) {
        return ResponseEntity.ok(service.criarInstituicao(request.tipo(), request.nome(), request.sigla()));
    }

    @PostMapping("/api/v1/secretaria-institucional/unidades")
    public ResponseEntity<UnidadeInstituicao> criarUnidade(@Valid @RequestBody CriarUnidadeInstituicaoRequest request) {
        UnidadeInstituicao unidade = service.criarUnidade(request.instituicaoId(), request.nome(), request.tipo(),
                request.comarca(), request.uf());
        service.reprocessarBacklogAposCriacaoDeUnidade(unidade);
        return ResponseEntity.ok(unidade);
    }

    @PostMapping("/api/v1/secretaria-institucional/unidades/{unidadeId}/abrangencia")
    public ResponseEntity<UnidadeInstitucionalAbrangencia> adicionarAbrangencia(@PathVariable Long unidadeId,
                                                                                @Valid @RequestBody AdicionarAbrangenciaRequest request) {
        return ResponseEntity.ok(service.adicionarAbrangencia(unidadeId, request.comarcaAtendida()));
    }

    @PostMapping("/api/v1/secretaria-institucional/unidades/{unidadeId}/desativar")
    public ResponseEntity<Void> desativarUnidade(@PathVariable Long unidadeId) {
        service.desativarUnidade(unidadeId);
        return ResponseEntity.ok().build();
    }
}
