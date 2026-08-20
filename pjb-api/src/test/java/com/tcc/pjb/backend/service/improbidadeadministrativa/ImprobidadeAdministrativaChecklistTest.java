package com.tcc.pjb.backend.service.improbidadeadministrativa;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImprobidadeAdministrativaChecklistTest {

    private final ImprobidadeAdministrativaChecklistService svc =
            new ImprobidadeAdministrativaChecklistService();

    private ImprobidadeAdministrativaChecklistService.ImprobidadeInput inputDoloso(
            ImprobidadeAdministrativaChecklistService.TipoAto tipo) {
        return new ImprobidadeAdministrativaChecklistService.ImprobidadeInput(
                tipo,
                ImprobidadeAdministrativaChecklistService.TipoSujeito.AGENTE_PUBLICO,
                true, false, false, false);
    }

    // --- Configuração da improbidade ---

    @Test
    void enriquecimentoIlicito_doloso_configuraImprobidade() {
        var result = svc.avaliar(inputDoloso(
                ImprobidadeAdministrativaChecklistService.TipoAto.ENRIQUECIMENTO_ILICITO));

        assertThat(result.configuradaImprobidade()).isTrue();
        assertThat(result.sancoes()).isNotEmpty();
        assertThat(result.sancoes()).anyMatch(s -> s.descricao().contains("Perda dos bens"));
        assertThat(result.sancoes()).anyMatch(s -> s.descricao().contains("Suspensão dos direitos políticos de 14 a 16 anos"));
        assertThat(result.fundamentoLegal()).contains("art. 9°");
    }

    @Test
    void danoAoErario_doloso_configuraImprobidade() {
        var result = svc.avaliar(inputDoloso(
                ImprobidadeAdministrativaChecklistService.TipoAto.DANO_AO_ERARIO));

        assertThat(result.configuradaImprobidade()).isTrue();
        assertThat(result.sancoes()).anyMatch(s -> s.descricao().contains("Ressarcimento integral"));
        assertThat(result.sancoes()).anyMatch(s -> s.descricao().contains("Suspensão dos direitos políticos de 12 a 14 anos"));
        assertThat(result.fundamentoLegal()).contains("art. 10");
    }

    @Test
    void violacaoPrincipios_dolosa_configuraImprobidade() {
        var result = svc.avaliar(inputDoloso(
                ImprobidadeAdministrativaChecklistService.TipoAto.VIOLACAO_PRINCIPIOS));

        assertThat(result.configuradaImprobidade()).isTrue();
        assertThat(result.sancoes()).anyMatch(s -> s.descricao().contains("Suspensão dos direitos políticos de 6 a 8 anos"));
        assertThat(result.fundamentoLegal()).contains("art. 11");
    }

    // --- Exigência de dolo (Lei 14.230/21) ---

    @Test
    void condutaCulposa_naoConfiguraImprobidade_lei14230() {
        var input = new ImprobidadeAdministrativaChecklistService.ImprobidadeInput(
                ImprobidadeAdministrativaChecklistService.TipoAto.DANO_AO_ERARIO,
                ImprobidadeAdministrativaChecklistService.TipoSujeito.AGENTE_PUBLICO,
                false, true, false, false);

        var result = svc.avaliar(input);

        assertThat(result.configuradaImprobidade()).isFalse();
        assertThat(result.motivoNaoConfiguracao()).contains("dolo específico");
        assertThat(result.sancoes()).isEmpty();
    }

    @Test
    void semDolo_naoConfigura_independenteDoTipo() {
        var input = new ImprobidadeAdministrativaChecklistService.ImprobidadeInput(
                ImprobidadeAdministrativaChecklistService.TipoAto.ENRIQUECIMENTO_ILICITO,
                ImprobidadeAdministrativaChecklistService.TipoSujeito.AGENTE_PUBLICO,
                false, false, false, false);

        var result = svc.avaliar(input);

        assertThat(result.configuradaImprobidade()).isFalse();
        assertThat(result.motivoNaoConfiguracao()).contains("dolo");
    }

    // --- Retroatividade da exigência de dolo (lex mitior) ---

    @Test
    void atoAnteriorALei14230_culposo_retroatividadeDoloAplica() {
        var input = new ImprobidadeAdministrativaChecklistService.ImprobidadeInput(
                ImprobidadeAdministrativaChecklistService.TipoAto.DANO_AO_ERARIO,
                ImprobidadeAdministrativaChecklistService.TipoSujeito.AGENTE_PUBLICO,
                false, true, true, false);

        var result = svc.avaliar(input);

        assertThat(result.configuradaImprobidade()).isFalse();
        assertThat(result.retroatividadeDoloAplica()).isTrue();
        assertThat(result.observacao()).contains("STF RE 1294133").contains("Tema 1199");
    }

    @Test
    void atoAposLei14230_culposo_retroatividadeNaoSeAplica() {
        var input = new ImprobidadeAdministrativaChecklistService.ImprobidadeInput(
                ImprobidadeAdministrativaChecklistService.TipoAto.DANO_AO_ERARIO,
                ImprobidadeAdministrativaChecklistService.TipoSujeito.AGENTE_PUBLICO,
                false, true, false, false);

        var result = svc.avaliar(input);

        assertThat(result.retroatividadeDoloAplica()).isFalse();
    }

    // --- Legitimidade e prescrição ---

    @Test
    void legitimidadeAtiva_exclusivaMP() {
        var result = svc.avaliar(inputDoloso(
                ImprobidadeAdministrativaChecklistService.TipoAto.VIOLACAO_PRINCIPIOS));

        assertThat(result.legitimidadeAtiva()).contains("Ministério Público");
        assertThat(result.legitimidadeAtiva()).contains("exclusiv");
        assertThat(result.legitimidadeAtiva()).contains("art. 17");
    }

    @Test
    void prescricao_8anos() {
        var result = svc.avaliar(inputDoloso(
                ImprobidadeAdministrativaChecklistService.TipoAto.DANO_AO_ERARIO));

        assertThat(result.prescricao()).contains("8 anos");
        assertThat(result.prescricao()).contains("art. 23");
    }

    @Test
    void ressarcimentoErario_imprescritivel() {
        var input = new ImprobidadeAdministrativaChecklistService.ImprobidadeInput(
                ImprobidadeAdministrativaChecklistService.TipoAto.DANO_AO_ERARIO,
                ImprobidadeAdministrativaChecklistService.TipoSujeito.AGENTE_PUBLICO,
                true, false, false, true);

        var result = svc.avaliar(input);

        assertThat(result.ressarcimentoImprescritivel()).isTrue();
        assertThat(result.observacao()).contains("CF art. 37, §5°");
    }

    // --- Terceiro ---

    @Test
    void terceiro_sozinho_naoConfiguraImprobidade_exigeAgentPublico() {
        var input = new ImprobidadeAdministrativaChecklistService.ImprobidadeInput(
                ImprobidadeAdministrativaChecklistService.TipoAto.ENRIQUECIMENTO_ILICITO,
                ImprobidadeAdministrativaChecklistService.TipoSujeito.TERCEIRO_INDUTOR_OU_CONCORRENTE,
                true, false, false, false);

        var result = svc.avaliar(input);

        assertThat(result.configuradaImprobidade()).isTrue();
        assertThat(result.observacao()).contains("art. 3°");
        assertThat(result.observacao()).contains("agente público");
    }

    // --- Sanções por referência legal ---

    @Test
    void enriquecimentoIlicito_sancoes_referenciam_art12_I() {
        var result = svc.avaliar(inputDoloso(
                ImprobidadeAdministrativaChecklistService.TipoAto.ENRIQUECIMENTO_ILICITO));

        assertThat(result.sancoes()).allMatch(s -> s.fundamentoLegal().contains("art. 12, I"));
    }

    @Test
    void danoAoErario_sancoes_referenciam_art12_II() {
        var result = svc.avaliar(inputDoloso(
                ImprobidadeAdministrativaChecklistService.TipoAto.DANO_AO_ERARIO));

        assertThat(result.sancoes()).allMatch(s -> s.fundamentoLegal().contains("art. 12, II"));
    }

    @Test
    void violacaoPrincipios_sancoes_referenciam_art12_III() {
        var result = svc.avaliar(inputDoloso(
                ImprobidadeAdministrativaChecklistService.TipoAto.VIOLACAO_PRINCIPIOS));

        assertThat(result.sancoes()).allMatch(s -> s.fundamentoLegal().contains("art. 12, III"));
    }
}
