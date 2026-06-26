package com.tcc.pjb.backend.service.completude;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.dto.Attachment;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.service.completude.CompletudeDocumentalPolicyService.DiagnosticoCompletudeDocumental;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompletudeDocumentalPolicyServiceTest {

    private final CompletudeDocumentalPolicyService service = new CompletudeDocumentalPolicyService();

    @Test
    void ritoNullDefaultsParaComumOrdinario() {
        DiagnosticoCompletudeDocumental d = service.diagnosticar(null, List.of());
        assertThat(d.rito()).isEqualTo(RitoProcessual.COMUM_ORDINARIO);
        assertThat(d.bloqueante()).isTrue();
    }

    @Test
    void anexosNullBlockeiaParaRitoComRequisitos() {
        DiagnosticoCompletudeDocumental d = service.diagnosticar(RitoProcessual.TRABALHISTA_ORDINARIO, null);
        assertThat(d.bloqueante()).isTrue();
        assertThat(d.faltantes()).isNotEmpty();
    }

    @Test
    void tipoDocumentoNullNuncaSatisfazRequisito() {
        Attachment semTipo = Attachment.builder().name("arquivo.pdf").build();
        DiagnosticoCompletudeDocumental d = service.diagnosticar(
                RitoProcessual.TRABALHISTA_ORDINARIO, List.of(semTipo));
        assertThat(d.bloqueante()).isTrue();
        assertThat(d.faltantes()).contains(TipoDocumento.CTPS);
    }

    @Test
    void trabalhistaComTodosObrigatoriosNaoBloqueante() {
        List<Attachment> anexos = List.of(
                tipado(TipoDocumento.PETICAO_INICIAL),
                tipado(TipoDocumento.PROCURACAO),
                tipado(TipoDocumento.CTPS),
                tipado(TipoDocumento.CALCULO_INICIAL)
        );
        DiagnosticoCompletudeDocumental d = service.diagnosticar(RitoProcessual.TRABALHISTA_ORDINARIO, anexos);
        assertThat(d.bloqueante()).isFalse();
        assertThat(d.faltantes()).isEmpty();
    }

    @Test
    void trabalhistaSemCtpsBlockeiaComCtpsNosFaltantes() {
        List<Attachment> anexos = List.of(
                tipado(TipoDocumento.PETICAO_INICIAL),
                tipado(TipoDocumento.PROCURACAO),
                tipado(TipoDocumento.CALCULO_INICIAL)
        );
        DiagnosticoCompletudeDocumental d = service.diagnosticar(RitoProcessual.TRABALHISTA_ORDINARIO, anexos);
        assertThat(d.bloqueante()).isTrue();
        assertThat(d.faltantes()).contains(TipoDocumento.CTPS);
        assertThat(d.faltantes()).doesNotContain(
                TipoDocumento.PETICAO_INICIAL, TipoDocumento.PROCURACAO, TipoDocumento.CALCULO_INICIAL);
    }

    @Test
    void previdenciarioSemCnisBlockeiaComCnisNosFaltantes() {
        List<Attachment> anexos = List.of(
                tipado(TipoDocumento.PETICAO_INICIAL),
                tipado(TipoDocumento.PROCURACAO),
                tipado(TipoDocumento.DOCUMENTO_IDENTIDADE),
                tipado(TipoDocumento.REQUERIMENTO_ADMINISTRATIVO)
        );
        DiagnosticoCompletudeDocumental d = service.diagnosticar(RitoProcessual.PREVIDENCIARIO_JEF, anexos);
        assertThat(d.bloqueante()).isTrue();
        assertThat(d.faltantes()).contains(TipoDocumento.CNIS);
        assertThat(d.faltantes()).doesNotContain(TipoDocumento.PETICAO_INICIAL, TipoDocumento.PROCURACAO);
    }

    @Test
    void toExceptionContemRitoEFaltantesNosMetadados() {
        List<Attachment> anexos = List.of(tipado(TipoDocumento.PETICAO_INICIAL));
        DiagnosticoCompletudeDocumental d = service.diagnosticar(RitoProcessual.TRABALHISTA_ORDINARIO, anexos);
        var ex = service.toException(d);
        assertThat(ex.getMessage()).contains("incompletude documental");
        assertThat(ex.getMetadados()).containsKey("rito");
        assertThat(ex.getMetadados()).containsKey("faltantes");
    }

    private static Attachment tipado(TipoDocumento tipo) {
        return Attachment.builder().name(tipo.name().toLowerCase() + ".pdf").tipoDocumento(tipo).build();
    }
}
