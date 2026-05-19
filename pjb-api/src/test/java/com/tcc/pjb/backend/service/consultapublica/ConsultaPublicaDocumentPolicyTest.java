package com.tcc.pjb.backend.service.consultapublica;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConsultaPublicaDocumentPolicyTest {

    @Test
    void canExposePublicAct_processoNull_retornaFalse() {
        assertThat(ConsultaPublicaDocumentPolicy.canExposePublicAct(null, new DocumentoProcessual()))
                .isFalse();
    }

    @Test
    void canExposePublicAct_documentoNull_retornaFalse() {
        assertThat(ConsultaPublicaDocumentPolicy.canExposePublicAct(Processo.builder().build(), null))
                .isFalse();
    }

    @Test
    void canExposePublicAct_sigiloProcessoN2_retornaFalse() {
        Processo p = Processo.builder().nivelSigilo(NivelSigilo.SIGILO_N2).build();
        DocumentoProcessual doc = new DocumentoProcessual();
        doc.setTitulo("Sentença");
        assertThat(ConsultaPublicaDocumentPolicy.canExposePublicAct(p, doc)).isFalse();
    }

    @Test
    void canExposePublicAct_sigiloDocumentoN2_retornaFalse() {
        Processo p = Processo.builder().build();
        DocumentoProcessual doc = new DocumentoProcessual();
        doc.setNivelSigilo(NivelSigilo.SIGILO_N2);
        doc.setTitulo("Sentença");
        assertThat(ConsultaPublicaDocumentPolicy.canExposePublicAct(p, doc)).isFalse();
    }

    @Test
    void canExposePublicAct_sentencaPublica_retornaTrue() {
        Processo p = Processo.builder().build();
        DocumentoProcessual doc = new DocumentoProcessual();
        doc.setTitulo("Sentença");
        assertThat(ConsultaPublicaDocumentPolicy.canExposePublicAct(p, doc)).isTrue();
    }

    @Test
    void canExposePublicAct_acordao_retornaTrue() {
        Processo p = Processo.builder().build();
        DocumentoProcessual doc = new DocumentoProcessual();
        doc.setTitulo("ACÓRDÃO");
        assertThat(ConsultaPublicaDocumentPolicy.canExposePublicAct(p, doc)).isTrue();
    }

    @Test
    void canExposePublicAct_tituloNaoReconhecido_retornaFalse() {
        Processo p = Processo.builder().build();
        DocumentoProcessual doc = new DocumentoProcessual();
        doc.setTitulo("Petição inicial");
        assertThat(ConsultaPublicaDocumentPolicy.canExposePublicAct(p, doc)).isFalse();
    }

    @Test
    void resolvePublicActKind_sentenca_retornaSentencaPublica() {
        DocumentoProcessual doc = new DocumentoProcessual();
        doc.setTitulo("SENTENÇA DE MÉRITO");
        assertThat(ConsultaPublicaDocumentPolicy.resolvePublicActKind(doc))
                .isEqualTo("SENTENCA_PUBLICA");
    }

    @Test
    void resolvePublicActKind_acordao_retornaAcordaoPublico() {
        DocumentoProcessual doc = new DocumentoProcessual();
        doc.setTitulo("Acórdão");
        assertThat(ConsultaPublicaDocumentPolicy.resolvePublicActKind(doc))
                .isEqualTo("ACORDAO_PUBLICO");
    }

    @Test
    void resolvePublicActKind_decisao_retornaDecisaoPublica() {
        DocumentoProcessual doc = new DocumentoProcessual();
        doc.setTitulo("DECISÃO INTERLOCUTÓRIA");
        assertThat(ConsultaPublicaDocumentPolicy.resolvePublicActKind(doc))
                .isEqualTo("DECISAO_PUBLICA");
    }

    @Test
    void resolvePublicActKind_despacho_retornaDespachoPublico() {
        DocumentoProcessual doc = new DocumentoProcessual();
        doc.setTitulo("DESPACHO JUDICIAL");
        assertThat(ConsultaPublicaDocumentPolicy.resolvePublicActKind(doc))
                .isEqualTo("DESPACHO_PUBLICO");
    }

    @Test
    void resolvePublicActKind_documentoNull_retornaNull() {
        assertThat(ConsultaPublicaDocumentPolicy.resolvePublicActKind(null)).isNull();
    }

    @Test
    void resolvePublicActKind_tituloVazio_usaNomeOriginal() {
        DocumentoProcessual doc = new DocumentoProcessual();
        doc.setTitulo("");
        doc.setNomeOriginal("sentenca_final.pdf");
        assertThat(ConsultaPublicaDocumentPolicy.resolvePublicActKind(doc))
                .isEqualTo("SENTENCA_PUBLICA");
    }

    @Test
    void resolvePublicActKind_semTituloSemNomeOriginal_retornaNull() {
        DocumentoProcessual doc = new DocumentoProcessual();
        assertThat(ConsultaPublicaDocumentPolicy.resolvePublicActKind(doc)).isNull();
    }
}
