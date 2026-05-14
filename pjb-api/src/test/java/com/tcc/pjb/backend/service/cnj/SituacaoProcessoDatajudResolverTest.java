package com.tcc.pjb.backend.service.cnj;

import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SituacaoProcessoDatajudResolverTest {

    private final SituacaoProcessoDatajudResolver resolver = new SituacaoProcessoDatajudResolver();

    @Test
    void resolver_arquivado_encerrado() {
        var s = resolver.resolver(FaseProcessual.ARQUIVAMENTO, null);
        assertThat(s.encerrado()).isTrue();
        assertThat(s.codigo()).isEqualTo("22");
    }

    @Test
    void resolver_autuacao_naoEncerrado() {
        var s = resolver.resolver(FaseProcessual.AUTUACAO, null);
        assertThat(s.encerrado()).isFalse();
        assertThat(s.codigo()).isEqualTo("12056");
    }

    @Test
    void resolver_julgamento_singular_quandoRitoNaoRecursal() {
        var s = resolver.resolver(FaseProcessual.JULGAMENTO, RitoProcessual.COMUM_ORDINARIO);
        assertThat(s.codigo()).isEqualTo("12065");
        assertThat(s.encerrado()).isFalse();
    }

    @Test
    void resolver_julgamento_singular_quandoRitoNull() {
        var s = resolver.resolver(FaseProcessual.JULGAMENTO, null);
        assertThat(s.codigo()).isEqualTo("12065");
    }

    @Test
    void resolver_citacao_codigoCorreto() {
        var s = resolver.resolver(FaseProcessual.CITACAO, null);
        assertThat(s.codigo()).isEqualTo("12058");
        assertThat(s.encerrado()).isFalse();
    }
}
