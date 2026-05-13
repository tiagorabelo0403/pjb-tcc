package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralActionProfileEconomicRitoResolver {

    private final TetoProcessualService tetoProcessualService;
    private final SalarioMinimoNacionalService salarioMinimoNacionalService;

    public NationalProceduralActionProfileEconomicRitoResolver(TetoProcessualService tetoProcessualService,
                                                               SalarioMinimoNacionalService salarioMinimoNacionalService) {
        this.tetoProcessualService = Objects.requireNonNull(tetoProcessualService);
        this.salarioMinimoNacionalService = Objects.requireNonNull(salarioMinimoNacionalService);
    }

    String inferTrabalhistaDefaultRito(Map<String, Object> payload) {
        return inferTrabalhistaDefaultRito(payload, null, null);
    }

    String inferTrabalhistaDefaultRito(Map<String, Object> payload,
                                       String corpus,
                                       NationalProceduralPartyProfile partyProfile) {
        String normalizedCorpus = NationalProceduralActionProfileSupport.normalize(corpus);
        if (NationalProceduralActionProfileSupport.containsAny(normalizedCorpus, "INQUERITO JUDICIAL", "FALTA GRAVE", "ART 853", "ART. 853")) {
            return "TRABALHISTA_INQUERITO_FALTA_GRAVE";
        }
        if (NationalProceduralActionProfileSupport.containsAny(normalizedCorpus, "ACAO DE CUMPRIMENTO", "AÇÃO DE CUMPRIMENTO", "ART 872", "ART. 872")) {
            return "TRABALHISTA_ACAO_CUMPRIMENTO";
        }
        if (NationalProceduralActionProfileSupport.containsAny(normalizedCorpus, "DISSIDIO COLETIVO", "DISSÍDIO COLETIVO")) {
            return "TRABALHISTA_DISSIDIO_COLETIVO";
        }
        if (NationalProceduralActionProfileSupport.containsAny(normalizedCorpus, "ACAO RESCISORIA", "AÇÃO RESCISÓRIA")) {
            return "TRABALHISTA_ACAO_RESCISORIA";
        }
        if (NationalProceduralActionProfileSupport.containsAny(normalizedCorpus, "MANDADO DE SEGURANCA TRABALHISTA", "MANDADO SEGURANÇA TRABALHISTA")) {
            return "TRABALHISTA_MANDADO_SEGURANCA";
        }
        if (NationalProceduralActionProfileSupport.containsAny(normalizedCorpus, "ACIDENTE DO TRABALHO", "DOENCA OCUPACIONAL", "DOENÇA OCUPACIONAL")) {
            return "TRABALHISTA_ACIDENTE_TRABALHO";
        }
        if (NationalProceduralActionProfileSupport.containsAny(normalizedCorpus, "SUMARIO", "ALCADA", "ALÇADA", "LEI 5.584", "LEI 5584")) {
            return "TRABALHISTA_SUMARIO_ALCADA";
        }
        boolean publicParty = isTrabalhistaPublicParty(normalizedCorpus, partyProfile);
        BigDecimal valorCausa = NationalProceduralActionProfileSupport.decimal(payload == null ? null : payload.get("valorCausa"));
        LocalDate dataReferencia = LocalDate.now();
        BigDecimal salarioMinimo = salarioMinimoNacionalService.valorEm(dataReferencia);
        if (!publicParty && valorCausa.compareTo(BigDecimal.ZERO) > 0 && valorCausa.compareTo(salarioMinimo.multiply(new BigDecimal("2"))) <= 0) {
            return "TRABALHISTA_SUMARIO_ALCADA";
        }
        TetoProcessualService.DiagnosticoTetoProcessual diagnostico = tetoProcessualService.diagnosticar(
                valorCausa,
                TipoJustica.TRABALHO,
                RamoDireito.TRABALHISTA,
                RitoProcessual.TRABALHISTA_SUMARISSIMO,
                null,
                dataReferencia
        );
        return !publicParty && !diagnostico.violacao() ? "TRABALHISTA_SUMARISSIMO" : "TRABALHISTA_ORDINARIO";
    }

    String inferPrevidenciarioDefaultRito(Map<String, Object> payload) {
        TetoProcessualService.DiagnosticoTetoProcessual diagnostico = tetoProcessualService.diagnosticar(
                NationalProceduralActionProfileSupport.decimal(payload == null ? null : payload.get("valorCausa")),
                TipoJustica.FEDERAL,
                RamoDireito.PREVIDENCIARIO,
                RitoProcessual.PREVIDENCIARIO_JEF,
                null,
                LocalDate.now()
        );
        return diagnostico.violacao() ? "PREVIDENCIARIO_COMUM" : "PREVIDENCIARIO_JEF";
    }

    private boolean isTrabalhistaPublicParty(String corpus,
                                             NationalProceduralPartyProfile partyProfile) {
        return NationalProceduralActionProfileSupport.containsAny(corpus,
                "ADMINISTRACAO PUBLICA",
                "ADMINISTRAÇÃO PÚBLICA",
                "AUTARQUIA",
                "FUNDACAO PUBLICA",
                "FUNDAÇÃO PÚBLICA",
                "MUNICIPIO",
                "PREFEITURA",
                "ESTADO",
                "UNIAO",
                "UNIÃO");
    }
}
