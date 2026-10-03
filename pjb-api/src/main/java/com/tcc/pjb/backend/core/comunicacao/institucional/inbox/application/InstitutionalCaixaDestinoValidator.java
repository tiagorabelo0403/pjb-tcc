package com.tcc.pjb.backend.core.comunicacao.institucional.inbox.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.CatalogoInstitucionalUnificadoService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.EstruturaCaixaInstitucionalService;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalCaixaDestinoValidator {

    private final CatalogoInstitucionalUnificadoService catalogoInstitucionalUnificadoService;
    private final EstruturaCaixaInstitucionalService estruturaCaixaInstitucionalService;

    public InstitutionalCaixaDestinoValidator(CatalogoInstitucionalUnificadoService catalogoInstitucionalUnificadoService,
                                              EstruturaCaixaInstitucionalService estruturaCaixaInstitucionalService) {
        this.catalogoInstitucionalUnificadoService = Objects.requireNonNull(catalogoInstitucionalUnificadoService);
        this.estruturaCaixaInstitucionalService = Objects.requireNonNull(estruturaCaixaInstitucionalService);
    }

    public void validar(String unidadeCodigo, String caixaDestinoCodigo) {
        var unidade = catalogoInstitucionalUnificadoService.listarPorTipo(null).stream()
                .filter(candidate -> candidate.codigo().equalsIgnoreCase(unidadeCodigo))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("UnidadeInstitucional", unidadeCodigo));
        boolean exists = estruturaCaixaInstitucionalService.expandir(unidade).stream()
                .anyMatch(caixa -> caixa.codigo().equalsIgnoreCase(caixaDestinoCodigo));
        if (!exists) {
            throw new RecursoNaoEncontradoException("CaixaInstitucional", caixaDestinoCodigo);
        }
    }
}
