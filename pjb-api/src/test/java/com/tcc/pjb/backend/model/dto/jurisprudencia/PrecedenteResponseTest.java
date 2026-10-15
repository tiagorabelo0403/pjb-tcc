package com.tcc.pjb.backend.model.dto.jurisprudencia;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoPrecedente;
import com.tcc.pjb.backend.model.entity.enums.TribunalFonte;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PrecedenteResponseTest {

    private final ObjectMapper json = JsonMapper.builder().findAndAddModules().build();

    @Test
    void respostaSerializaExatamenteComoAEntidadeSerializavaNoContratoDaBusca() throws Exception {
        Precedente precedente = Precedente.builder()
                .id(42L)
                .fonte(TribunalFonte.values()[0])
                .tipo(TipoPrecedente.values()[0])
                .identificador("Tema 1.234")
                .titulo("Repetitivo sobre prescricao")
                .tese("A pretensao prescreve em cinco anos.")
                .ementaResumo("Ementa resumida.")
                .urlReferencia("https://precedentes.exemplo/tema-1234")
                .dataPublicacao(LocalDate.of(2025, 3, 14))
                .ramoSugerido(RamoDireito.CIVIL)
                .ritoSugerido(RitoProcessual.COMUM_ORDINARIO)
                .createdAt(LocalDateTime.of(2026, 1, 2, 3, 4, 5))
                .build();

        assertThat(json.readTree(json.writeValueAsString(PrecedenteResponse.de(precedente))))
                .isEqualTo(json.readTree(json.writeValueAsString(precedente)));
    }
}
