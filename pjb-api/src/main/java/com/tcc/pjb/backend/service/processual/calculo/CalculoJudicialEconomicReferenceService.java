package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialEconomicReferenceResponse;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CalculoJudicialEconomicReferenceService {

    private static final BigDecimal TETO_INSS_2026 = new BigDecimal("8475.55");

    private final SalarioMinimoNacionalService salarioMinimoNacionalService;

    public CalculoJudicialEconomicReferenceService(SalarioMinimoNacionalService salarioMinimoNacionalService) {
        this.salarioMinimoNacionalService = Objects.requireNonNull(salarioMinimoNacionalService);
    }

    public CalculoJudicialEconomicReferenceResponse current() {
        LocalDate hoje = LocalDate.now();
        Map<String, Object> salario = new LinkedHashMap<>();
        salario.put("vigente", salarioMinimoNacionalService.valorVigente());
        salario.put("vigenteEm", hoje.withDayOfYear(1).toString());
        salario.put("referencia2025", salarioMinimoNacionalService.valorPorAno(2025));
        salario.put("referencia2026", salarioMinimoNacionalService.valorPorAno(2026));
        salario.put("normaReferencia", "Decreto nº 12.797/2025");
        salario.put("fonteOficial", "https://www.planalto.gov.br/ccivil_03/_ato2023-2026/2025/decreto/d12797.htm");

        Map<String, Object> inss = new LinkedHashMap<>();
        inss.put("tetoBeneficio2026", TETO_INSS_2026);
        inss.put("vigenteDesde", "2026-01-01");
        inss.put("fonteOficial", "https://www.gov.br/inss/pt-br/assuntos/com-reajuste-de-3-9-teto-do-inss-chega-a-r-8-475-55-em-2026");
        inss.put("regraUso", "referencia_previdenciaria_e_classificacao_rpv_precatorio");

        Map<String, Object> fontes = new LinkedHashMap<>();
        fontes.put("salarioMinimoPlanalto2026", salario.get("fonteOficial"));
        fontes.put("salarioMinimoPlanalto2025", "https://www.planalto.gov.br/ccivil_03/_ato2023-2026/2024/decreto/d12342.htm");
        fontes.put("inss2026", inss.get("fonteOficial"));
        fontes.put("pjeCalcOficial", "https://www.csjt.jus.br/web/csjt/pje-calc");
        fontes.put("manualCjf", "https://sicom.cjf.jus.br/arquivos/pdf/manual_de_calculos_2025_vf.pdf");

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("refreshMode", "official_seed_plus_internal_service");
        metadata.put("salaryService", "SalarioMinimoNacionalService");
        metadata.put("panelReady", Boolean.TRUE);
        metadata.put("asOfYear", hoje.getYear());

        return new CalculoJudicialEconomicReferenceResponse(
                hoje.toString(),
                Map.copyOf(salario),
                Map.copyOf(inss),
                Map.copyOf(fontes),
                safeMetadata(metadata),
                Instant.now()
        );
    }

    public Map<String, Object> panelSnapshot() {
        CalculoJudicialEconomicReferenceResponse response = current();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("salarioMinimoVigente", response.salarioMinimoNacional().get("vigente"));
        snapshot.put("salarioMinimoNorma", response.salarioMinimoNacional().get("normaReferencia"));
        snapshot.put("tetoInss2026", response.inss().get("tetoBeneficio2026"));
        snapshot.put("referenciaTemporal", response.referenciaTemporal());
        snapshot.put("fontesOficiais", response.fontesOficiais());
        snapshot.put("metadata", response.metadata());
        return Map.copyOf(snapshot);
    }

    private Map<String, Object> safeMetadata(Map<String, Object> metadata) {
        Map<String, Object> safe = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
        safe.entrySet().removeIf(entry -> entry.getValue() == null);
        return Map.copyOf(safe);
    }
}
