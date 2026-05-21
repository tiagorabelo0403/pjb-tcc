package com.tcc.pjb.backend.core.transito;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PostJudgmentOperationalResolverTest {

    private final PostJudgmentOperationalResolver resolver = new PostJudgmentOperationalResolver();

    @Test
    void deveResolverModoCumprimentoComConstricaoQuandoQualificadorContemPenhora() {
        Processo processo = processoComRito(RitoProcessual.COMUM_ORDINARIO);

        PostJudgmentOperationalProfile profile = resolver.resolve(
                processo, ProcessoLifecycleAction.INICIAR_CUMPRIMENTO, "penhora_sisbajud", 50_000D);

        assertThat(profile.operationMode()).isEqualTo("ABERTURA_EXECUTIVA_COM_CONSTRICAO");
        assertThat(profile.blocking()).isTrue();
    }

    @Test
    void deveResolverModoCumprimentoControladoSemPenhora() {
        Processo processo = processoComRito(RitoProcessual.COMUM_ORDINARIO);

        PostJudgmentOperationalProfile profile = resolver.resolve(
                processo, ProcessoLifecycleAction.INICIAR_CUMPRIMENTO, null, 20_000D);

        assertThat(profile.operationMode()).isEqualTo("ABERTURA_EXECUTIVA_CONTROLADA");
        assertThat(profile.blocking()).isFalse();
    }

    @Test
    void deveAtribuirPrioridade0QuandoCumprimentoDeAltoValor() {
        Processo processo = processoComRito(RitoProcessual.COMUM_ORDINARIO);

        PostJudgmentOperationalProfile profile = resolver.resolve(
                processo, ProcessoLifecycleAction.INICIAR_CUMPRIMENTO, null, 150_000D);

        assertThat(profile.priority()).isEqualTo(0);
    }

    @Test
    void deveUsarBaseLegalCPCArt513ParaCumprimentoDefinitivoCivil() {
        Processo processo = processoComRito(RitoProcessual.COMUM_ORDINARIO);

        PostJudgmentOperationalProfile profile = resolver.resolve(
                processo, ProcessoLifecycleAction.INICIAR_CUMPRIMENTO, null, 10_000D);

        assertThat(profile.baseLegal()).contains("Art. 513 CPC");
        assertThat(profile.satisfactionMode()).isEqualTo("SATISFACAO_PATRIMONIAL_PROGRESSIVA");
    }

    @Test
    void deveUsarBaseLegalProvisionarParaCumprimentoProvisorio() {
        Processo processo = processoComRito(RitoProcessual.COMUM_ORDINARIO);

        PostJudgmentOperationalProfile profile = resolver.resolve(
                processo, ProcessoLifecycleAction.INICIAR_CUMPRIMENTO, "provisorio", 10_000D);

        assertThat(profile.baseLegal()).contains("Arts. 520");
        assertThat(profile.executionTrack()).isEqualTo("CUMPRIMENTO_PROVISORIO_COM_CAUTELA");
        assertThat(profile.blocking()).isTrue();
    }

    @Test
    void deveDirecionarParaExecucaoFazendariaQuandoRitoFiscal() {
        Processo processo = processoComRito(RitoProcessual.EXECUCAO_FISCAL);

        PostJudgmentOperationalProfile profile = resolver.resolve(
                processo, ProcessoLifecycleAction.INICIAR_CUMPRIMENTO, null, 30_000D);

        assertThat(profile.executionTrack()).isEqualTo("EXECUCAO_FAZENDARIA_COM_CONTROLE_DE_GARANTIA");
        assertThat(profile.assignedRole()).isEqualTo(TipoUsuario.SERVIDOR_FORUM);
        assertThat(profile.satisfactionMode()).isEqualTo("SATISFACAO_FAZENDARIA_COM_GARANTIA");
        assertThat(profile.baseLegal()).contains("Lei 6.830");
    }

    @Test
    void deveResolverCertificacaoTransitoComFundamentoCPC() {
        Processo processo = processoComRito(RitoProcessual.COMUM_ORDINARIO);

        PostJudgmentOperationalProfile profile = resolver.resolve(
                processo, ProcessoLifecycleAction.CERTIFICAR_TRANSITO, null, 0D);

        assertThat(profile.operationMode()).isEqualTo("CERTIFICACAO_E_FECHAMENTO_RECURSAL");
        assertThat(profile.baseLegal()).contains("Art. 502 CPC");
        assertThat(profile.assignedRole()).isEqualTo(TipoUsuario.SERVIDOR_FORUM);
    }

    @Test
    void deveResolverArquivamentoDefinitivoDigital() {
        Processo processo = processoComRito(RitoProcessual.COMUM_ORDINARIO);

        PostJudgmentOperationalProfile profile = resolver.resolve(
                processo, ProcessoLifecycleAction.ARQUIVAR, null, 0D);

        assertThat(profile.operationMode()).isEqualTo("BAIXA_E_GUARDA_DEFINITIVA");
        assertThat(profile.retentionMode()).isEqualTo("GUARDA_DEFINITIVA_DIGITAL");
        assertThat(profile.archiveTrack()).isEqualTo("ARQUIVAMENTO_DEFINITIVO_DIGITAL");
    }

    @ParameterizedTest(name = "rito={0} → grupo={1} em track")
    @CsvSource({
            "EXECUCAO_FISCAL,         FAZENDARIA",
            "TRABALHISTA_ORDINARIO,   TRABALHISTA",
            "COMUM_ORDINARIO,         CIVEL"
    })
    void deveRefletirRitoNaTrilhaDeExecucao(String ritoName, String fragmentoEsperado) {
        RitoProcessual rito = RitoProcessual.valueOf(ritoName);
        Processo processo = processoComRito(rito);

        PostJudgmentOperationalProfile profile = resolver.resolve(
                processo, ProcessoLifecycleAction.INICIAR_CUMPRIMENTO, null, 10_000D);

        assertThat(profile.executionTrack().toUpperCase()).contains(fragmentoEsperado.toUpperCase());
    }

    @Test
    void deveAdicionarRevisaoDeGovFazendariaAoChecklistParaRitoFiscalEmCumprimento() {
        Processo processo = processoComRito(RitoProcessual.EXECUCAO_FISCAL);

        PostJudgmentOperationalProfile profile = resolver.resolve(
                processo, ProcessoLifecycleAction.INICIAR_CUMPRIMENTO, null, 10_000D);

        assertThat(profile.reviewChecklist())
                .anyMatch(item -> item.contains("CDA") || item.contains("garantias"));
    }

    @Test
    void deveUsarDueUnitHorasParaCertificacaoDeTransito() {
        Processo processo = processoComRito(RitoProcessual.COMUM_ORDINARIO);

        PostJudgmentOperationalProfile profile = resolver.resolve(
                processo, ProcessoLifecycleAction.CERTIFICAR_TRANSITO, null, 0D);

        assertThat(profile.dueUnit()).isEqualTo(ChronoUnit.HOURS);
        assertThat(profile.dueAmount()).isEqualTo(2L);
    }

    private Processo processoComRito(RitoProcessual rito) {
        Processo processo = new Processo();
        processo.setId(1L);
        processo.setRito(rito);
        return processo;
    }
}
