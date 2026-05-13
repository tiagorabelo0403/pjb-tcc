package com.tcc.pjb.backend.service.ui.accessibility;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UiPlainLanguageServiceTest {

    private final UiPlainLanguageService service = new UiPlainLanguageService();

    @Test
    void shouldTranslateJudicialExpressionsToSimplerLanguage() {
        var response = service.preview("Intima-se a parte autora para que se manifeste no prazo de 5 dias e junte-se o documento.");

        assertThat(response.simplifiedText()).contains("Avise-se oficialmente", "quem entrou com o pedido", "apresente sua posição", "dentro do prazo de", "anexe-se");
        assertThat(response.metrics().get("wordCount")).isGreaterThan(0);
    }
}
