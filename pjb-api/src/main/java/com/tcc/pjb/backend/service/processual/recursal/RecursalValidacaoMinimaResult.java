package com.tcc.pjb.backend.service.processual.recursal;

import com.tcc.pjb.backend.service.recurso.RecursoAdmissibilidadeService;
import com.tcc.pjb.backend.service.recurso.RecursoProcessualTipo;
import com.tcc.pjb.backend.service.recurso.RecursoTempestividadeGuardService;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public record RecursalValidacaoMinimaResult(
        RecursoProcessualTipo tipoProcessual,
        LocalDate dataReferencia,
        RecursoAdmissibilidadeService.AdmissibilidadeResult admissibilidade,
        RecursoTempestividadeGuardService.TempestividadeResult tempestividade) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("tipoProcessual", tipoProcessual == null ? null : tipoProcessual.name());
        out.put("dataReferencia", dataReferencia);
        if (admissibilidade != null) {
            out.put("admissivel", admissibilidade.admissivel());
            out.put("obstaculos", admissibilidade.obstaculos());
            out.put("fundamentacaoAdmissibilidade", admissibilidade.fundamentacao());
        }
        if (tempestividade != null) {
            out.put("tempestivo", tempestividade.tempestivo());
            out.put("prazoFinal", tempestividade.prazoFinal());
            out.put("diasUteisPrazo", tempestividade.diasUteisPrazo());
            out.put("diasUteisRestantes", tempestividade.diasUteisRestantes());
            out.put("fundamentacaoTempestividade", tempestividade.fundamentacao());
        }
        return Map.copyOf(out);
    }
}
