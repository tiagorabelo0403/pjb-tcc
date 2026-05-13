package com.tcc.pjb.backend.service.processual.recursal.embargos;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.dto.processual.recursal.embargos.EmbargosDeclaracaoFoundationResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.embargos.EmbargosDeclaracaoRequest;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EmbargosDeclaracaoFoundationServiceTest {

    private final EmbargosDeclaracaoFoundationService service = new EmbargosDeclaracaoFoundationService();

    @Test
    void deveDescreverEmbargosDeclaracaoComPrazoCabimentoEFundamentos() {
        EmbargosDeclaracaoFoundationResponse response = service.describe();

        assertThat(response.prazoDiasUteis()).isEqualTo(5);
        assertThat(response.cabivelContraQualquerDecisao()).isTrue();
        assertThat(response.fundamentosCabiveis())
                .contains("Omissão", "Contradição", "Obscuridade", "Erro material");
    }

    @Test
    void deveProjetarContraditorioPrevioQuandoEfeitoInfringenteForPretendido() {
        EmbargosDeclaracaoFoundationResponse response = service.preview(new EmbargosDeclaracaoRequest(
                Set.of("OMISSAO"),
                true,
                false,
                true,
                "Complementar ponto omitido com potencial integrativo"
        ));

        assertThat(response.contraditorioPrevioNecessario()).isTrue();
        assertThat(response.colegiadoNecessario()).isTrue();
        assertThat(response.observacoes()).isEqualTo("Complementar ponto omitido com potencial integrativo");
    }
}
