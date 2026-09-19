package com.tcc.pjb.backend.service.financeiro;

import com.tcc.pjb.backend.model.dto.procuradoria.surface.PrecatorioRpvEnteDevedorTipo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class TetoRpvNacionalService {

    private static final BigDecimal SALARIOS_MINIMOS_FEDERAL = new BigDecimal("60");
    private static final BigDecimal SALARIOS_MINIMOS_ESTADUAL = new BigDecimal("40");
    private static final BigDecimal SALARIOS_MINIMOS_MUNICIPAL = new BigDecimal("30");

    private static final String FUNDAMENTO_FEDERAL = "CF, art. 100, § 3º, c/c Lei 10.259/2001, art. 17, § 1º e art. 3º";
    private static final String FUNDAMENTO_ESTADUAL = "CF, art. 100, § 3º, c/c ADCT, art. 87, I";
    private static final String FUNDAMENTO_MUNICIPAL = "CF, art. 100, § 3º, c/c ADCT, art. 87, II";

    private final SalarioMinimoNacionalService salarioMinimoNacionalService;

    public TetoRpvNacionalService(SalarioMinimoNacionalService salarioMinimoNacionalService) {
        this.salarioMinimoNacionalService = Objects.requireNonNull(salarioMinimoNacionalService,
                "salarioMinimoNacionalService");
    }

    public BigDecimal salariosMinimos(PrecatorioRpvEnteDevedorTipo enteDevedor) {
        return switch (Objects.requireNonNull(enteDevedor, "enteDevedor")) {
            case UNIAO, AUTARQUIA_FEDERAL, FUNDACAO_PUBLICA_FEDERAL -> SALARIOS_MINIMOS_FEDERAL;
            case ESTADO, DISTRITO_FEDERAL, AUTARQUIA_ESTADUAL, AUTARQUIA_DISTRITAL,
                 FUNDACAO_PUBLICA_ESTADUAL, FUNDACAO_PUBLICA_DISTRITAL -> SALARIOS_MINIMOS_ESTADUAL;
            case MUNICIPIO, AUTARQUIA_MUNICIPAL, FUNDACAO_PUBLICA_MUNICIPAL -> SALARIOS_MINIMOS_MUNICIPAL;
        };
    }

    public String fundamentoLegal(PrecatorioRpvEnteDevedorTipo enteDevedor) {
        return switch (Objects.requireNonNull(enteDevedor, "enteDevedor")) {
            case UNIAO, AUTARQUIA_FEDERAL, FUNDACAO_PUBLICA_FEDERAL -> FUNDAMENTO_FEDERAL;
            case ESTADO, DISTRITO_FEDERAL, AUTARQUIA_ESTADUAL, AUTARQUIA_DISTRITAL,
                 FUNDACAO_PUBLICA_ESTADUAL, FUNDACAO_PUBLICA_DISTRITAL -> FUNDAMENTO_ESTADUAL;
            case MUNICIPIO, AUTARQUIA_MUNICIPAL, FUNDACAO_PUBLICA_MUNICIPAL -> FUNDAMENTO_MUNICIPAL;
        };
    }

    public BigDecimal limite(PrecatorioRpvEnteDevedorTipo enteDevedor, LocalDate dataReferencia) {
        Objects.requireNonNull(dataReferencia, "dataReferencia");
        return salarioMinimoNacionalService.multiplicar(salariosMinimos(enteDevedor), dataReferencia);
    }
}
