package com.tcc.pjb.backend.service.territorial;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.competencia.ModoCompetencia;
import com.tcc.pjb.backend.model.entity.enums.processual.CriterioTerritorial;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.LocalDate;
import java.util.List;

public sealed interface ResolucaoTerritorial {

    record Resolvida(
            String municipioIbge,
            CriterioTerritorial criterio,
            ModoCompetencia modo,
            List<String> unidadesElegiveis,
            String tribunalCodigo,
            String fundamentoLegal,
            String fonteNormativa,
            LocalDate catalogoVigenciaInicio
    ) implements ResolucaoTerritorial {}

    record CriterioNaoMapeado(
            RitoProcessual rito
    ) implements ResolucaoTerritorial {}

    record AncoraAusente(
            CriterioTerritorial criterioExigido,
            String fundamentoLegal
    ) implements ResolucaoTerritorial {}

    record MunicipioForaDoCatalogo(
            String municipioIbge,
            TipoJustica tipoJustica
    ) implements ResolucaoTerritorial {}
}
