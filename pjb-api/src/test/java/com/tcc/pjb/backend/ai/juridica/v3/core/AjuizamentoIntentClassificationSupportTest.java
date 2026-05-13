package com.tcc.pjb.backend.ai.juridica.v3.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class AjuizamentoIntentClassificationSupportTest {

    private final AjuizamentoIntentClassificationSupport support = new AjuizamentoIntentClassificationSupport();

    @Test
    void deveInferirFluxoTrabalhistaSumarissimo() {
        Map<String, Object> ctx = Map.of(
                "resumo", "reclamante pede horas extras e fgts contra empregador privado",
                "valor_causa", 20000.0
        );
        String texto = "reclamante pede horas extras e fgts contra empregador privado";

        String esfera = support.inferirEsfera(ctx, texto);
        String ramo = support.inferirRamo(ctx, texto);
        String subRamo = support.inferirSubRamo(ramo, ctx, texto);
        String rito = support.inferirRito(ramo, subRamo, ctx, texto);

        assertThat(esfera).isEqualTo("ESTADUAL");
        assertThat(ramo).isEqualTo("TRABALHISTA");
        assertThat(subRamo).isEqualTo("JORNADA_REMUNERACAO");
        assertThat(rito).isEqualTo("TRABALHISTA_SUMARISSIMO");
    }

    @Test
    void deveInferirFluxoPenalLeiDeDrogas() {
        Map<String, Object> ctx = Map.of(
                "narrativa", "acusado por trafico de drogas com apreensao de entorpecentes"
        );
        String texto = "acusado por trafico de drogas com apreensao de entorpecentes";

        String ramo = support.inferirRamo(ctx, texto);
        String subRamo = support.inferirSubRamo(ramo, ctx, texto);
        String rito = support.inferirRito(ramo, subRamo, ctx, texto);

        assertThat(ramo).isEqualTo("PENAL");
        assertThat(subRamo).isEqualTo("LEI_DROGAS");
        assertThat(rito).isEqualTo("PENAL_LEI_DROGAS");
    }

    @Test
    void deveInferirFluxoDeFamiliaParaAlimentos() {
        Map<String, Object> ctx = Map.of(
                "pedido", "acao de alimentos para menor com pedido de pensao alimenticia"
        );
        String texto = "acao de alimentos para menor com pedido de pensao alimenticia";

        String ramo = support.inferirRamo(ctx, texto);
        String subRamo = support.inferirSubRamo(ramo, ctx, texto);
        String rito = support.inferirRito(ramo, subRamo, ctx, texto);

        assertThat(ramo).isEqualTo("FAMILIA_SUCESSOES");
        assertThat(subRamo).isEqualTo("ALIMENTOS");
        assertThat(rito).isEqualTo("CIVIL_FAMILIA_ALIMENTOS");
    }
}
