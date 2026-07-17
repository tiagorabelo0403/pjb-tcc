package com.tcc.pjb.backend.service.territorial;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.processual.AncoraTerritorial;
import com.tcc.pjb.backend.model.dto.processual.EnderecosProcessuaisRequest;
import com.tcc.pjb.backend.model.entity.competencia.ModoCompetencia;
import com.tcc.pjb.backend.model.entity.enums.processual.CriterioTerritorial;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.JurisdicaoTerritorialRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompetenciaTerritorialResolver {

    private final JurisdicaoTerritorialRepository repository;

    public CompetenciaTerritorialResolver(JurisdicaoTerritorialRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ResolucaoTerritorial resolver(RitoProcessual rito, TipoJustica tipoJustica,
            EnderecosProcessuaisRequest enderecos, LocalDate referencia) {
        Optional<CriterioTerritorial> criterioOpt = rito.criterioTerritorial();
        if (criterioOpt.isEmpty()) {
            return new ResolucaoTerritorial.CriterioNaoMapeado(rito);
        }
        CriterioTerritorial criterio = criterioOpt.get();
        AncoraTerritorial ancora = enderecos.ancoraPara(criterio);
        if (ancora == null || !ancora.resolvivel()) {
            return new ResolucaoTerritorial.AncoraAusente(criterio, criterio.fundamentoLegal());
        }
        return repository.findVigente(ancora.municipioIbge(), tipoJustica.name(), referencia)
                .<ResolucaoTerritorial>map(j -> new ResolucaoTerritorial.Resolvida(
                        j.getMunicipioIbge(),
                        criterio,
                        ModoCompetencia.valueOf(j.getModoCompetencia()),
                        List.copyOf(j.getUnidadesElegiveis()),
                        j.getTribunalCodigo(),
                        criterio.fundamentoLegal(),
                        j.getFonteNormativa(),
                        j.getVigenciaInicio()))
                .orElseGet(() -> new ResolucaoTerritorial.MunicipioForaDoCatalogo(
                        ancora.municipioIbge(), tipoJustica));
    }
}
