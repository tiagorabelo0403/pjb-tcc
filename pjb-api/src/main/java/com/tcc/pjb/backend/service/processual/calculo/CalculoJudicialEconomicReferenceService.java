package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialEconomicReferenceResponse;
import com.tcc.pjb.backend.model.dto.shared.calculo.CalculoJudicialInssReferenceDto;
import com.tcc.pjb.backend.model.dto.shared.calculo.CalculoJudicialSalarioMinimoDto;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoReferenciaAnual;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CalculoJudicialEconomicReferenceService {

    private static final BigDecimal TETO_INSS_2026 = new BigDecimal("8475.55");
    private static final String FONTE_INSS_2026 = "https://www.gov.br/inss/pt-br/assuntos/com-reajuste-de-3-9-teto-do-inss-chega-a-r-8-475-55-em-2026";

    private final SalarioMinimoNacionalService salarioMinimoNacionalService;
    private final PjbTimeService tempo;

    public CalculoJudicialEconomicReferenceService(SalarioMinimoNacionalService salarioMinimoNacionalService, PjbTimeService tempo) {
        this.salarioMinimoNacionalService = Objects.requireNonNull(salarioMinimoNacionalService);
        this.tempo = Objects.requireNonNull(tempo);
    }

    public CalculoJudicialEconomicReferenceResponse current() {
        Instant agora = tempo.nowUtc();
        LocalDate hoje = LocalDate.ofInstant(agora, tempo.legalZone());
        SalarioMinimoReferenciaAnual vigente = salarioMinimoNacionalService.referenciaEm(hoje);
        Optional<SalarioMinimoReferenciaAnual> anterior = Optional.of(salarioMinimoNacionalService.referenciaAte(vigente.ano() - 1))
                .filter(referencia -> referencia.ano() < vigente.ano());

        CalculoJudicialSalarioMinimoDto salario = new CalculoJudicialSalarioMinimoDto(
                vigente.valor(),
                vigente.vigenteDesde().toString(),
                vigente.ano(),
                anterior.map(SalarioMinimoReferenciaAnual::ano).orElse(null),
                anterior.map(SalarioMinimoReferenciaAnual::valor).orElse(null),
                vigente.normaReferencia(),
                vigente.fonteOficial()
        );

        CalculoJudicialInssReferenceDto inss = new CalculoJudicialInssReferenceDto(
                TETO_INSS_2026,
                "2026-01-01",
                FONTE_INSS_2026,
                "referencia_previdenciaria_e_classificacao_rpv_precatorio"
        );

        Map<String, String> fontes = new LinkedHashMap<>();
        fontes.put("salarioMinimo" + vigente.ano(), vigente.fonteOficial());
        anterior.ifPresent(referencia -> fontes.put("salarioMinimo" + referencia.ano(), referencia.fonteOficial()));
        fontes.put("inss2026", FONTE_INSS_2026);
        fontes.put("pjeCalcOficial", "https://www.csjt.jus.br/web/csjt/pje-calc");
        fontes.put("manualCjf", "https://sicom.cjf.jus.br/arquivos/pdf/manual_de_calculos_2025_vf.pdf");

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
                agora
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
