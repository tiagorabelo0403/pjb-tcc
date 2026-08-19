package com.tcc.pjb.backend.core.security.access;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import org.junit.jupiter.api.Test;

class ProcessoPartyCpfMatcherTest {

    private final ProcessoPartyCpfMatcher matcher = new ProcessoPartyCpfMatcher();

    @Test
    void casaComParteAutora() {
        Processo processo = Processo.builder().parteAutoraCpf("11111111111").parteReuCpf("22222222222").build();

        PartyMatchResult result = matcher.match("11111111111", processo);

        assertThat(result).isEqualTo(new PartyMatchResult.Matched(PartyRole.AUTOR));
    }

    @Test
    void casaComParteRe() {
        Processo processo = Processo.builder().parteAutoraCpf("11111111111").parteReuCpf("22222222222").build();

        PartyMatchResult result = matcher.match("22222222222", processo);

        assertThat(result).isEqualTo(new PartyMatchResult.Matched(PartyRole.REU));
    }

    @Test
    void casaComUsuarioVinculadoQuandoAutorEReuNaoBatem() {
        Usuario usuarioVinculado = new Usuario();
        usuarioVinculado.setCpf("33333333333");
        Processo processo = Processo.builder()
                .parteAutoraCpf("11111111111")
                .parteReuCpf("22222222222")
                .usuario(usuarioVinculado)
                .build();

        PartyMatchResult result = matcher.match("33333333333", processo);

        assertThat(result).isEqualTo(new PartyMatchResult.Matched(PartyRole.USUARIO_VINCULADO));
    }

    @Test
    void naoCasaQuandoNenhumCpfBate() {
        Processo processo = Processo.builder().parteAutoraCpf("11111111111").parteReuCpf("22222222222").build();

        PartyMatchResult result = matcher.match("99999999999", processo);

        assertThat(result).isEqualTo(new PartyMatchResult.NotMatched());
    }

    @Test
    void naoCasaQuandoCpfNulo() {
        Processo processo = Processo.builder().parteAutoraCpf("11111111111").build();

        PartyMatchResult result = matcher.match(null, processo);

        assertThat(result).isEqualTo(new PartyMatchResult.NotMatched());
    }

    @Test
    void naoCasaQuandoCpfEmBranco() {
        Processo processo = Processo.builder().parteAutoraCpf("11111111111").build();

        PartyMatchResult result = matcher.match("   ", processo);

        assertThat(result).isEqualTo(new PartyMatchResult.NotMatched());
    }

    @Test
    void naoCasaQuandoProcessoNulo() {
        PartyMatchResult result = matcher.match("11111111111", null);

        assertThat(result).isEqualTo(new PartyMatchResult.NotMatched());
    }
}
