package com.tcc.pjb.backend.service.processual.precedente;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PrecedenteDecisionSupportAssemblerTest {

    private final PrecedenteDecisionSupportAssembler assembler =
            new PrecedenteDecisionSupportAssembler();

    @Test
    void montar_detectaTemaSuspenso_quandoHouver() {
        var precedentes = List.of(
                new PrecedenteDecisionSupportAssembler.PrecedenteItem(
                        "1234", "STJ", "ementa-A", true, false));
        var support = assembler.montar(UUID.randomUUID(), RitoProcessual.PROCEDIMENTO_COMUM, precedentes);
        assertThat(support.haTemaSuspenso()).isTrue();
        assertThat(support.aviso()).contains("sobrestado");
    }

    @Test
    void montar_semSuspensao_quandoTodosNaoSuspensos() {
        var precedentes = List.of(
                new PrecedenteDecisionSupportAssembler.PrecedenteItem(
                        "5678", "STF", "ementa-B", false, true));
        var support = assembler.montar(UUID.randomUUID(), RitoProcessual.PROCEDIMENTO_COMUM, precedentes);
        assertThat(support.haTemaSuspenso()).isFalse();
    }

    @Test
    void montar_listaPrecedentes_noSupport() {
        var precedentes = List.of(
                new PrecedenteDecisionSupportAssembler.PrecedenteItem(
                        "001", "TRT", "ementa-C", false, false),
                new PrecedenteDecisionSupportAssembler.PrecedenteItem(
                        "002", "TST", "ementa-D", false, false));
        var support = assembler.montar(UUID.randomUUID(), RitoProcessual.TRABALHISTA_ORDINARIO, precedentes);
        assertThat(support.precedentes()).hasSize(2);
        assertThat(support.rito()).isEqualTo(RitoProcessual.TRABALHISTA_ORDINARIO);
    }
}
