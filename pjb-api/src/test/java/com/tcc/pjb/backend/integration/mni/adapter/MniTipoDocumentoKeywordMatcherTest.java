package com.tcc.pjb.backend.integration.mni.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import org.junit.jupiter.api.Test;

class MniTipoDocumentoKeywordMatcherTest {

    private final MniTipoDocumentoKeywordMatcher matcher = new MniTipoDocumentoKeywordMatcher();

    @Test
    void shouldCasarPeticaoInicialIgnorandoAcentoECaixa() {
        assertThat(matcher.match("PETIÇÃO INICIAL.pdf", null)).contains(TipoDocumento.PETICAO_INICIAL);
    }

    @Test
    void shouldCasarPelaDescricaoQuandoNomeNaoAjuda() {
        assertThat(matcher.match("doc001.pdf", "Procuração ad judicia")).contains(TipoDocumento.PROCURACAO);
    }

    @Test
    void shouldRetornarVazioQuandoNaoHaCorrespondenciaInequivoca() {
        assertThat(matcher.match("anexo_diverso.pdf", "Documento diverso enviado pela parte")).isEmpty();
    }

    @Test
    void shouldRetornarVazioQuandoNomeEDescricaoAusentes() {
        assertThat(matcher.match(null, null)).isEmpty();
    }

    @Test
    void shouldCasarCertidaoDeObitoDistintoDeCertidaoDeNascimento() {
        assertThat(matcher.match("certidao_obito.pdf", null)).contains(TipoDocumento.CERTIDAO_OBITO);
        assertThat(matcher.match("certidao_nascimento.pdf", null)).contains(TipoDocumento.CERTIDAO_NASCIMENTO);
    }
}
