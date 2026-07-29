package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialEconomicReferenceResponse;
import com.tcc.pjb.backend.model.dto.shared.calculo.CalculoJudicialInssReferenceDto;
import com.tcc.pjb.backend.model.dto.shared.calculo.CalculoJudicialSalarioMinimoDto;
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
    private static final String FONTE_INSS_2026 = "https://www.gov.br/inss/pt-br/assuntos/com-reajuste-de-3-9-teto-do-inss-chega-a-r-8-475-55-em-2026";
    private static final String FONTE_SALARIO_2026 = "https://www.planalto.gov.br/ccivil_03/_ato2023-2026/2025/decreto/d12797.htm";

    private final SalarioMinimoNacionalService salarioMinimoNacionalService;

    public CalculoJudicialEconomicReferenceService(SalarioMinimoNacionalService salarioMinimoNacionalService) {
        this.salarioMinimoNacionalService = Objects.requireNonNull(salarioMinimoNacionalService);
    }

    public CalculoJudicialEconomicReferenceResponse current() {
        LocalDate hoje = LocalDate.now();

        CalculoJudicialSalarioMinimoDto salario = new CalculoJudicialSalarioMinimoDto(
                salarioMinimoNacionalService.valorVigente(),
                hoje.withDayOfYear(1).toString(),
                salarioMinimoNacionalService.valorPorAno(hoje.getYear() - 1),
                salarioMinimoNacionalService.valorPorAno(hoje.getYear()),
                "Decreto nº 12.797/2025",
                FONTE_SALARIO_2026
        );

        CalculoJudicialInssReferenceDto inss = new CalculoJudicialInssReferenceDto(
                TETO_INSS_2026,
                "2026-01-01",
                FONTE_INSS_2026,
                "referencia_previdenciaria_e_classificacao_rpv_precatorio"
        );

        Map<String, String> fontes = Map.of(
                "salarioMinimoPlanalto2026", FONTE_SALARIO_2026,
                "salarioMinimoPlanalto2025", "https://www.planalto.gov.br/ccivil_03/_ato2023-2026/2024/decreto/d12342.htm",
                "inss2026", FONTE_INSS_2026,
                "pjeCalcOficial", "https://www.csjt.jus.br/web/csjt/pje-calc",
                "manualCjf", "https://sicom.cjf.jus.br/arquivos/pdf/manual_de_calculos_2025_vf.pdf"
        );

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("refreshMode", "official_seed_plus_internal_service");
        metadata.put("salaryService", "SalarioMinimoNacionalService");
        metadata.put("panelReady", Boolean.TRUE);
        metadata.put("asOfYear", hoje.getYear());

        return new CalculoJudicialEconomicReferenceResponse(
                hoje.toString(),
                salario,
                inss,
                fontes,
                safeMetadata(metadata),
                Instant.now()
        );
    }

    public Map<String, Object> panelSnapshot() {
        CalculoJudicialEconomicReferenceResponse response = current();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("salarioMinimoVigente", response.salarioMinimoNacional().vigente());
        snapshot.put("salarioMinimoNorma", response.salarioMinimoNacional().normaReferencia());
        snapshot.put("tetoInss2026", response.inss().tetoBeneficio2026());
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
