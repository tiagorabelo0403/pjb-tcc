package com.tcc.pjb.backend.service.improbidadeadministrativa;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ImprobidadeAdministrativaChecklistService {

    public enum TipoAto {
        /** art. 9° — vantagem patrimonial indevida em razão do cargo */
        ENRIQUECIMENTO_ILICITO,
        /** art. 10 — ação ou omissão dolosa que cause prejuízo ao erário */
        DANO_AO_ERARIO,
        /** art. 11 — violação dolosa dos princípios da Administração Pública */
        VIOLACAO_PRINCIPIOS
    }

    public enum TipoSujeito {
        AGENTE_PUBLICO,
        /** Terceiro que induz ou concorre dolosamente — art. 3° */
        TERCEIRO_INDUTOR_OU_CONCORRENTE
    }

    public record SancaoPossivel(
            String descricao,
            String fundamentoLegal
    ) {}

    public record ImprobidadeInput(
            TipoAto tipoAto,
            TipoSujeito tipoSujeito,
            boolean condutaDolosa,
            boolean feitoPorCulpaApenasSemDolo,
            boolean atoAnteriorALei14230,
            boolean ressarcimentoAoErarioPretendido
    ) {}

    public record ImprobidadeResult(
            boolean configuradaImprobidade,
            String motivoNaoConfiguracao,
            List<SancaoPossivel> sancoes,
            String legitimidadeAtiva,
            String prescricao,
            boolean ressarcimentoImprescritivel,
            boolean retroatividadeDoloAplica,
            String fundamentoLegal,
            String observacao
    ) {}

    public ImprobidadeResult avaliar(ImprobidadeInput input) {
        // Lei 14.230/21: TODOS os atos exigem DOLO — culpa não configura improbidade
        if (input.feitoPorCulpaApenasSemDolo() || !input.condutaDolosa()) {
            boolean retroage = input.atoAnteriorALei14230();
            return new ImprobidadeResult(
                    false,
                    "Conduta culposa não configura improbidade administrativa após Lei 14.230/21 — exige-se dolo específico",
                    List.of(),
                    "Ministério Público (exclusivo — art. 17 Lei 14.230/21)",
                    "8 anos da prática do ato (art. 23 Lei 14.230/21)",
                    input.ressarcimentoAoErarioPretendido(),
                    retroage,
                    "Lei 14.230/21 art. 1°, §1° — dolo específico; STF RE 1294133 — retroatividade da exigência de dolo",
                    retroage
                            ? "Ato anterior à Lei 14.230/21: exigência de dolo retroage para beneficiar o réu (lex mitior — STF RE 1294133, Tema 1199)."
                            : "Conduta culposa não gera improbidade. Avaliar responsabilidade civil comum ou administrativa.");
        }

        List<SancaoPossivel> sancoes = buildSancoes(input.tipoAto());
        String obs = buildObservacao(input);

        return new ImprobidadeResult(
                true,
                null,
                List.copyOf(sancoes),
                "Ministério Público (legitimidade exclusiva — art. 17 Lei 14.230/21)",
                "8 anos contados da prática do ato (art. 23 Lei 14.230/21)",
                input.ressarcimentoAoErarioPretendido(),
                false,
                buildFundamento(input.tipoAto()),
                obs);
    }

    private List<SancaoPossivel> buildSancoes(TipoAto tipo) {
        List<SancaoPossivel> sancoes = new ArrayList<>();

        switch (tipo) {
            case ENRIQUECIMENTO_ILICITO -> {
                sancoes.add(new SancaoPossivel("Perda dos bens ou valores acrescidos ilicitamente ao patrimônio", "art. 12, I, Lei 14.230/21"));
                sancoes.add(new SancaoPossivel("Perda da função pública", "art. 12, I, Lei 14.230/21"));
                sancoes.add(new SancaoPossivel("Suspensão dos direitos políticos de 14 a 16 anos", "art. 12, I, Lei 14.230/21"));
                sancoes.add(new SancaoPossivel("Pagamento de multa civil de até 3x o valor do acréscimo patrimonial", "art. 12, I, Lei 14.230/21"));
                sancoes.add(new SancaoPossivel("Proibição de contratar com o Poder Público por 14 anos", "art. 12, I, Lei 14.230/21"));
            }
            case DANO_AO_ERARIO -> {
                sancoes.add(new SancaoPossivel("Ressarcimento integral do dano", "art. 12, II, Lei 14.230/21"));
                sancoes.add(new SancaoPossivel("Perda dos bens ou valores acrescidos ilicitamente", "art. 12, II, Lei 14.230/21"));
                sancoes.add(new SancaoPossivel("Perda da função pública", "art. 12, II, Lei 14.230/21"));
                sancoes.add(new SancaoPossivel("Suspensão dos direitos políticos de 12 a 14 anos", "art. 12, II, Lei 14.230/21"));
                sancoes.add(new SancaoPossivel("Pagamento de multa civil de até 2x o valor do dano", "art. 12, II, Lei 14.230/21"));
                sancoes.add(new SancaoPossivel("Proibição de contratar com o Poder Público por 12 anos", "art. 12, II, Lei 14.230/21"));
            }
            case VIOLACAO_PRINCIPIOS -> {
                sancoes.add(new SancaoPossivel("Perda da função pública", "art. 12, III, Lei 14.230/21"));
                sancoes.add(new SancaoPossivel("Suspensão dos direitos políticos de 6 a 8 anos", "art. 12, III, Lei 14.230/21"));
                sancoes.add(new SancaoPossivel("Pagamento de multa civil de até 24x a remuneração do agente", "art. 12, III, Lei 14.230/21"));
                sancoes.add(new SancaoPossivel("Proibição de contratar com o Poder Público por 6 anos", "art. 12, III, Lei 14.230/21"));
            }
        }

        return sancoes;
    }

    private String buildFundamento(TipoAto tipo) {
        return switch (tipo) {
            case ENRIQUECIMENTO_ILICITO -> "Lei 14.230/21 art. 9° — enriquecimento ilícito em razão do exercício de cargo, mandato, função, emprego ou atividade";
            case DANO_AO_ERARIO -> "Lei 14.230/21 art. 10 — ação ou omissão dolosa que cause lesão ao erário";
            case VIOLACAO_PRINCIPIOS -> "Lei 14.230/21 art. 11 — ato doloso que viole os deveres de honestidade, imparcialidade e legalidade";
        };
    }

    private String buildObservacao(ImprobidadeInput input) {
        StringBuilder obs = new StringBuilder();
        obs.append("Legitimidade ativa exclusiva do MP após Lei 14.230/21 (STF ADI 7236). ");

        if (input.tipoSujeito() == TipoSujeito.TERCEIRO_INDUTOR_OU_CONCORRENTE) {
            obs.append("Terceiro responde apenas se houver agente público como coautor (art. 3° — não há improbidade de particular sozinho). ");
        }

        if (input.ressarcimentoAoErarioPretendido()) {
            obs.append("Ressarcimento ao erário: imprescritível (CF art. 37, §5°; STF Tema 897). ");
        }

        obs.append("Sanções não são automáticas — juiz as aplica motivadamente conforme a gravidade (art. 17-C, §1°).");
        return obs.toString();
    }
}
