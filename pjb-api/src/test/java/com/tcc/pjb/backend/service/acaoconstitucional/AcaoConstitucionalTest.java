package com.tcc.pjb.backend.service.acaoconstitucional;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AcaoConstitucionalTest {

    private final HabeasCorpusReadinessService hc = new HabeasCorpusReadinessService();
    private final MandadoSegurancaReadinessService ms = new MandadoSegurancaReadinessService();
    private final HabeasDataReadinessService hd = new HabeasDataReadinessService();
    private final MandadoInjuncaoReadinessService mi = new MandadoInjuncaoReadinessService();
    private final AcaoPopularReadinessService ap = new AcaoPopularReadinessService();

    @Test
    void hcSemPendenciasComCoacaoIlegalELiberdadeAmeacada() {
        var input = new HabeasCorpusReadinessService.HabeasCorpusInput(
                "HC001", true, true, true,
                "JUIZ ESTADUAL", false, false);
        var result = hc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).isEmpty();
        assertThat(result.competenciaSugerida()).isEqualTo(HabeasCorpusReadinessService.CompetenciaHC.TJ);
        assertThat(result.sinalizacao()).contains("validação judicial");
    }

    @Test
    void hcComPendenciaContraMultaSomente() {
        var input = new HabeasCorpusReadinessService.HabeasCorpusInput(
                "HC002", false, false, false,
                "JUIZ", false, true);
        var result = hc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(i -> i.contains("693"));
    }

    @Test
    void hcComPendenciaParaQuestaoFatual() {
        var input = new HabeasCorpusReadinessService.HabeasCorpusInput(
                "HC003", true, true, false,
                "JUIZ ESTADUAL", true, false);
        var result = hc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(i -> i.contains("fato"));
    }

    @Test
    void msSemPendenciasDentroDoPrazo() {
        var input = new MandadoSegurancaReadinessService.MandadoSegurancaInput(
                "MS001", LocalDate.now().minusDays(30),
                true, true, true, false, false, false);
        var result = ms.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).isEmpty();
        assertThat(result.prazoDecadencialVigente()).isTrue();
        assertThat(result.diasRestantes()).isGreaterThan(0);
    }

    @Test
    void msComPendenciaForaDoPrazo120Dias() {
        var input = new MandadoSegurancaReadinessService.MandadoSegurancaInput(
                "MS002", LocalDate.now().minusDays(150),
                true, true, true, false, false, false);
        var result = ms.avaliar(input);
        assertThat(result.prazoDecadencialVigente()).isFalse();
        assertThat(result.pendenciasIdentificadas()).anyMatch(i -> i.contains("120"));
    }

    @Test
    void msComPendenciaSemDireitoLiquidoCerto() {
        var input = new MandadoSegurancaReadinessService.MandadoSegurancaInput(
                "MS003", LocalDate.now().minusDays(10),
                false, false, true, false, false, false);
        var result = ms.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(i -> i.contains("líquido"));
    }

    @Test
    void msComPendenciaContraLeiEmTese() {
        var input = new MandadoSegurancaReadinessService.MandadoSegurancaInput(
                "MS004", LocalDate.now().minusDays(5),
                true, true, true, true, false, false);
        var result = ms.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(i -> i.contains("tese"));
    }

    @Test
    void hdSemPendenciasComRequisitosAtendidos() {
        var input = new HabeasDataReadinessService.HabeasDataInput(
                "HD001", true, true, true,
                HabeasDataReadinessService.FinalidadeHD.RETIFICAR_DADOS);
        var result = hd.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).isEmpty();
        assertThat(result.requisitosVerificados()).hasSize(4);
    }

    @Test
    void hdComPendenciaSemRequisicaoAdministrativaPrevia() {
        var input = new HabeasDataReadinessService.HabeasDataInput(
                "HD002", false, false, true,
                HabeasDataReadinessService.FinalidadeHD.CONHECER_INFORMACOES);
        var result = hd.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(i -> i.contains("administrativo"));
    }

    @Test
    void miSemPendenciasComOmissaoNormativa() {
        var input = new MandadoInjuncaoReadinessService.MandadoInjuncaoInput(
                "MI001", true, true, false, true);
        var result = mi.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).isEmpty();
        assertThat(result.efeitoSugerido()).isEqualTo(MandadoInjuncaoReadinessService.EfeitoMandadoInjuncao.INTER_PARTES);
    }

    @Test
    void miColetivoTemEfeitoErgaOmnes() {
        var input = new MandadoInjuncaoReadinessService.MandadoInjuncaoInput(
                "MI002", true, true, true, true);
        var result = mi.avaliar(input);
        assertThat(result.efeitoSugerido()).isEqualTo(MandadoInjuncaoReadinessService.EfeitoMandadoInjuncao.ERGA_OMNES);
    }

    @Test
    void acaoPopularSemPendenciasCidadaoComTitulo() {
        var input = new AcaoPopularReadinessService.AcaoPopularInput(
                "AP001", true, true, true,
                AcaoPopularReadinessService.ObjetoAcaoPopular.PATRIMONIO_PUBLICO,
                LocalDate.now().minusYears(2));
        var result = ap.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).isEmpty();
        assertThat(result.prazoNaoDecaido()).isTrue();
    }

    @Test
    void acaoPopularComPendenciaPrazo5Anos() {
        var input = new AcaoPopularReadinessService.AcaoPopularInput(
                "AP002", true, true, true,
                AcaoPopularReadinessService.ObjetoAcaoPopular.MORALIDADE_ADMINISTRATIVA,
                LocalDate.now().minusYears(6));
        var result = ap.avaliar(input);
        assertThat(result.prazoNaoDecaido()).isFalse();
        assertThat(result.pendenciasIdentificadas()).anyMatch(i -> i.contains("5 anos"));
    }

    @Test
    void acaoPopularComPendenciaSemTituloEleitor() {
        var input = new AcaoPopularReadinessService.AcaoPopularInput(
                "AP003", true, false, true,
                AcaoPopularReadinessService.ObjetoAcaoPopular.MEIO_AMBIENTE,
                LocalDate.now().minusYears(1));
        var result = ap.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(i -> i.contains("título"));
    }

    @Test
    void acaoConstitucionalTipoTemSiglaCorreta() {
        assertThat(AcaoConstitucionalTipo.HABEAS_CORPUS.sigla()).isEqualTo("HC");
        assertThat(AcaoConstitucionalTipo.MANDADO_SEGURANCA.sigla()).isEqualTo("MS");
        assertThat(AcaoConstitucionalTipo.ACAO_POPULAR.sigla()).isEqualTo("AP");
        assertThat(AcaoConstitucionalTipo.ARGUICAO_DESCUMPRIMENTO_PRECEITO_FUNDAMENTAL.sigla()).isEqualTo("ADPF");
    }
}
