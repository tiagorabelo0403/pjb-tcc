package com.tcc.pjb.backend.service.sentenca;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SentencaTipoTest {

    @Test
    void admiteApelacao_sentencasFinais() {
        assertThat(SentencaTipo.MERITO_PROCEDENTE.admiteApelacao()).isTrue();
        assertThat(SentencaTipo.MERITO_IMPROCEDENTE.admiteApelacao()).isTrue();
        assertThat(SentencaTipo.EXTINCAO_SEM_MERITO.admiteApelacao()).isTrue();
    }

    @Test
    void admiteApelacao_decisaoMonocratica_false() {
        assertThat(SentencaTipo.DECISAO_MONOCRATICA.admiteApelacao()).isFalse();
    }

    @Test
    void transitaEmJulgado_acordaos() {
        assertThat(SentencaTipo.ACORDAO_UNANIME.transitaEmJulgado()).isTrue();
        assertThat(SentencaTipo.ACORDAO_MAIORIA.transitaEmJulgado()).isTrue();
    }

    @Test
    void transitaEmJulgado_liminares_false() {
        assertThat(SentencaTipo.LIMINAR_TUTELA_ANTECIPADA.transitaEmJulgado()).isFalse();
        assertThat(SentencaTipo.DESPACHO_ORDINATORIO.transitaEmJulgado()).isFalse();
    }

    @Test
    void ehAcordao_somentesAcordaos() {
        assertThat(SentencaTipo.ACORDAO_UNANIME.ehAcordao()).isTrue();
        assertThat(SentencaTipo.ACORDAO_MAIORIA.ehAcordao()).isTrue();
        assertThat(SentencaTipo.MERITO_PROCEDENTE.ehAcordao()).isFalse();
    }

    @Test
    void produzCoisaJulgadaMaterial_sentencasMerito() {
        assertThat(SentencaTipo.MERITO_PROCEDENTE.produzCoisaJulgadaMaterial()).isTrue();
        assertThat(SentencaTipo.HOMOLOGATORIA_ACORDO.produzCoisaJulgadaMaterial()).isTrue();
        assertThat(SentencaTipo.EXTINCAO_SEM_MERITO.produzCoisaJulgadaMaterial()).isFalse();
    }

    @Test
    void ehExtincaoSemResolucaoMerito() {
        assertThat(SentencaTipo.EXTINCAO_SEM_MERITO.ehExtincaoSemResolucaoMerito()).isTrue();
        assertThat(SentencaTipo.MERITO_PROCEDENTE.ehExtincaoSemResolucaoMerito()).isFalse();
    }
}
