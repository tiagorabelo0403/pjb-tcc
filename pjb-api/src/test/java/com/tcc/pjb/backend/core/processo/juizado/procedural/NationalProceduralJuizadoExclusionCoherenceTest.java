package com.tcc.pjb.backend.core.processo.juizado.procedural;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class NationalProceduralJuizadoExclusionCoherenceTest {

    @Test
    void ritoDeNaturezaExcluidaDosJuizadosNuncaCabeNoJuizadoEspecialFederal() {
        List<RitoProcessual> excluidosPelaNatureza = Arrays.stream(RitoProcessual.values())
                .filter(rito -> NationalProceduralJuizadoExclusionResolver.NATUREZAS_EXCLUIDAS.stream()
                        .anyMatch(natureza -> rito.name().contains(natureza.replace("_DO_", "_"))))
                .toList();

        assertThat(excluidosPelaNatureza).isNotEmpty();
        assertThat(excluidosPelaNatureza).noneMatch(RitoProcessual::cabeNoJuizadoEspecialFederal);
    }

    @Test
    void ritosExcluidosComNomeAbreviadoTambemNaoCabemNoJuizadoEspecialFederal() {
        assertThat(List.of(RitoProcessual.AMBIENTAL_ACP, RitoProcessual.AGRARIO_ACP_AGRARIA, RitoProcessual.FALENCIA,
                RitoProcessual.RECUPERACAO_JUDICIAL, RitoProcessual.RECUPERACAO_EXTRAJUDICIAL, RitoProcessual.TRIBUNAL_JURI,
                RitoProcessual.CIVIL_DIVISAO_DEMARCACAO, RitoProcessual.ESPECIAL_ACAO_POPULAR, RitoProcessual.ADMINISTRATIVO_ACAO_POPULAR,
                RitoProcessual.ESPECIAL_MANDADO_SEGURANCA_COLETIVO, RitoProcessual.ADMINISTRATIVO_PAD, RitoProcessual.EXECUCAO_FISCAL,
                RitoProcessual.TRIBUTARIO_EMBARGOS_EXECUCAO_FISCAL, RitoProcessual.TRIBUTARIO_CAUTELAR_FISCAL))
                .noneMatch(RitoProcessual::cabeNoJuizadoEspecialFederal);
    }

    @Test
    void naturezasDaLei10259Art3Par1IEstaoNaListaDeExclusaoDosJuizados() {
        assertThat(NationalProceduralJuizadoExclusionResolver.NATUREZAS_EXCLUIDAS)
                .contains("MANDADO_SEGURANCA", "DESAPROPRIACAO", "DEMARCACAO", "ACAO_POPULAR", "EXECUCAO_FISCAL",
                        "IMPROBIDADE_ADMINISTRATIVA", "ACAO_CIVIL_PUBLICA");
    }

    @Test
    void ritoDeFamiliaExcluidaDosJuizadosNuncaCabeNoJuizadoEspecialFederal() {
        assertThat(NationalProceduralJuizadoExclusionResolver.FAMILIAS_EXCLUIDAS)
                .containsExactlyInAnyOrder("TRABALHISTA", "ELEITORAL", "MILITAR", "PENAL");
        assertThat(Arrays.stream(RitoProcessual.values())
                .filter(rito -> rito.isTrabalhista() || rito.isEleitoral() || rito.isMilitar() || rito.isPenal()))
                .noneMatch(RitoProcessual::cabeNoJuizadoEspecialFederal);
    }
}
