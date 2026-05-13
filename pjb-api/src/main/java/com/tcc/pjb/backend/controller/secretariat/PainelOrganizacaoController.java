package com.tcc.pjb.backend.controller.secretariat;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.model.dto.organizacao.FiltroOrganizacaoRequest;
import com.tcc.pjb.backend.model.dto.organizacao.GrupoProcessualDto;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.secretariat.organizacao.PainelOrganizacaoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping(value = OperationalApiRoutes.PAINEL_ORGANIZACAO_BASE, produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasAnyAuthority('ROLE_SERVIDOR_JUDICIARIO','ROLE_ADMINISTRADOR','ROLE_MAGISTRADO')")
public class PainelOrganizacaoController {

    private final PainelOrganizacaoService service;

    public PainelOrganizacaoController(PainelOrganizacaoService service) {
        this.service = Objects.requireNonNull(service);
    }

    @GetMapping
    public ResponseEntity<List<GrupoProcessualDto>> organizar(
            @RequestParam(required = false) String vara,
            @RequestParam(required = false) String comarca,
            @RequestParam(required = false) String uf,
            @RequestParam(required = false) RitoProcessual rito,
            @RequestParam(required = false) FaseProcessual faseAtual,
            @RequestParam(required = false) StatusProcesso statusProcesso,
            @RequestParam(required = false, defaultValue = "vara") String agruparPor) {
        FiltroOrganizacaoRequest filtro = new FiltroOrganizacaoRequest(
                vara, comarca, uf, rito, faseAtual, statusProcesso, agruparPor);
        return ResponseEntity.ok(service.organizarPorFiltro(filtro));
    }
}
