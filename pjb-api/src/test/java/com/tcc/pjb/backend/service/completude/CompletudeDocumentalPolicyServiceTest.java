package com.tcc.pjb.backend.service.completude;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.dto.Attachment;
import com.tcc.pjb.backend.model.entity.enums.InstrumentoRepresentacaoProcessual;
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
    void jusPostulandiTrabalhistaDispensaProcuracaoSemDispensarOsDemais() {
        List<Attachment> anexos = List.of(
                tipado(TipoDocumento.PETICAO_INICIAL),
                tipado(TipoDocumento.CTPS),
                tipado(TipoDocumento.CALCULO_INICIAL)
        );

        DiagnosticoCompletudeDocumental comAdvogado = service.diagnosticar(RitoProcessual.TRABALHISTA_ORDINARIO, anexos);
        assertThat(comAdvogado.bloqueante()).isTrue();
        assertThat(comAdvogado.faltantes()).containsExactly(TipoDocumento.PROCURACAO);

        DiagnosticoCompletudeDocumental comJusPostulandi = service.diagnosticar(RitoProcessual.TRABALHISTA_ORDINARIO, anexos,
                InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_TRABALHISTA);
        assertThat(comJusPostulandi.bloqueante()).isFalse();
        assertThat(comJusPostulandi.faltantes()).isEmpty();
    }

    @Test
    void jusPostulandiNaoDispensaDocumentoObrigatorioAlemDaProcuracao() {
        List<Attachment> anexos = List.of(
                tipado(TipoDocumento.PETICAO_INICIAL),
                tipado(TipoDocumento.CALCULO_INICIAL)
        );

        DiagnosticoCompletudeDocumental d = service.diagnosticar(RitoProcessual.TRABALHISTA_ORDINARIO, anexos,
                InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_TRABALHISTA);

        assertThat(d.bloqueante()).isTrue();
        assertThat(d.faltantes()).containsExactly(TipoDocumento.CTPS);
    }

    @Test
    void jusPostulandiDoJuizadoDispensaProcuracaoNoRitoCivel() {
        List<Attachment> anexos = List.of(
                tipado(TipoDocumento.PETICAO_INICIAL),
                tipado(TipoDocumento.DOCUMENTO_IDENTIDADE),
                tipado(TipoDocumento.COMPROVANTE_ENDERECO),
                tipado(TipoDocumento.PROVAS_DOCUMENTAIS_BASICAS)
        );

        DiagnosticoCompletudeDocumental comAdvogado = service.diagnosticar(RitoProcessual.JUIZADO_ESPECIAL_CIVEL, anexos);
        assertThat(comAdvogado.faltantes()).containsExactly(TipoDocumento.PROCURACAO);

        DiagnosticoCompletudeDocumental comJusPostulandi = service.diagnosticar(RitoProcessual.JUIZADO_ESPECIAL_CIVEL, anexos,
                InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_JUIZADO);
        assertThat(comJusPostulandi.bloqueante()).isFalse();

        DiagnosticoCompletudeDocumental comJef = service.diagnosticar(RitoProcessual.JUIZADO_ESPECIAL_CIVEL, anexos,
                InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_JEF);
        assertThat(comJef.bloqueante()).isFalse();
    }

    @Test
    void mandatoAdJudiciaContinuaExigindoProcuracao() {
        List<Attachment> anexos = List.of(
                tipado(TipoDocumento.PETICAO_INICIAL),
                tipado(TipoDocumento.CTPS),
                tipado(TipoDocumento.CALCULO_INICIAL)
        );

        DiagnosticoCompletudeDocumental d = service.diagnosticar(RitoProcessual.TRABALHISTA_ORDINARIO, anexos,
                InstrumentoRepresentacaoProcessual.MANDATO_AD_JUDICIA);

        assertThat(d.bloqueante()).isTrue();
        assertThat(d.faltantes()).containsExactly(TipoDocumento.PROCURACAO);
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

    @Test
    void diagnosticarPorConjuntoDeTiposProduzMesmoResultadoQuePorAnexos() {
        var porAnexos = service.diagnosticar(RitoProcessual.TRABALHISTA_ORDINARIO, List.of(
                tipado(TipoDocumento.PETICAO_INICIAL),
                tipado(TipoDocumento.PROCURACAO),
                tipado(TipoDocumento.CTPS)
        ));

        var porConjunto = service.diagnosticar(RitoProcessual.TRABALHISTA_ORDINARIO,
                java.util.Set.of(TipoDocumento.PETICAO_INICIAL, TipoDocumento.PROCURACAO, TipoDocumento.CTPS),
                null);

        assertThat(porConjunto.bloqueante()).isEqualTo(porAnexos.bloqueante());
        assertThat(porConjunto.faltantes()).containsExactlyInAnyOrderElementsOf(porAnexos.faltantes());
    }

    @Test
    void diagnosticarPorConjuntoVazioDispensaProcuracaoComJusPostulandi() {
        var d = service.diagnosticar(RitoProcessual.TRABALHISTA_ORDINARIO,
                java.util.Set.of(TipoDocumento.PETICAO_INICIAL, TipoDocumento.CTPS, TipoDocumento.CALCULO_INICIAL),
                InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_TRABALHISTA);

        assertThat(d.bloqueante()).isFalse();
    }

    private static Attachment tipado(TipoDocumento tipo) {
        return Attachment.builder().name(tipo.name().toLowerCase() + ".pdf").tipoDocumento(tipo).build();
    }
}
