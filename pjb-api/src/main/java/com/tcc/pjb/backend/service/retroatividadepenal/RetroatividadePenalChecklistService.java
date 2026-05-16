package com.tcc.pjb.backend.service.retroatividadepenal;

import org.springframework.stereotype.Service;

/**
 * Verifica qual lei penal se aplica ao fato conforme o princípio
 * da irretroatividade da lex gravior e retroatividade da lex mitior.
 * CF art. 5°, XL; CP art. 1° e 2°.
 */
@Service
public class RetroatividadePenalChecklistService {

    public enum TipoLeiNova {
        /** Nova lei descriminaliza o fato — CP art. 2°, caput */
        ABOLITIO_CRIMINIS,
        /** Nova lei mantém o crime mas beneficia o réu (pena menor, novo benefício, etc.) */
        LEI_MAIS_BENEFICA,
        /** Nova lei agrava a situação do réu */
        LEI_MAIS_GRAVE,
        /** Nova lei cria crime para conduta antes atípica */
        NOVATIO_LEGIS_INCRIMINADORA
    }

    public enum SituacaoProcessual {
        INVESTIGADO,
        DENUNCIADO,
        CONDENADO_SEM_TRANSITO_EM_JULGADO,
        CONDENADO_TRANSITADO_EM_JULGADO,
        CUMPRINDO_PENA,
        PENA_JA_EXTINTA
    }

    public enum TipoCrimeQuantoAoDurar {
        /** Fato único e instantâneo */
        INSTANTANEO,
        /** Conduta que se prolonga no tempo — sequestro, cárcere privado */
        PERMANENTE,
        /** Série de crimes da mesma espécie — CP art. 71 */
        CONTINUADO
    }

    public enum LeiAplicavel {
        LEI_ANTIGA, LEI_NOVA, AMBAS_SE_BENEFICA
    }

    public record RetroatividadeInput(
            TipoLeiNova tipoLeiNova,
            SituacaoProcessual situacao,
            TipoCrimeQuantoAoDurar tipoCrime,
            /** true quando a lei nova entrou em vigor ANTES da cessação do crime permanente/continuado */
            boolean leiNovaVigenteAntesDataCessacao,
            boolean reuSolicitaCombinacaoDeLeis
    ) {}

    public record RetroatividadeResult(
            LeiAplicavel leiAplicavel,
            boolean retroageImediatamente,
            boolean exigeRevisaoDeOficio,
            String efeito,
            String fundamentoConstitucional,
            String fundamentoLegal,
            String observacao
    ) {}

    public RetroatividadeResult avaliar(RetroatividadeInput input) {
        return switch (input.tipoLeiNova()) {
            case ABOLITIO_CRIMINIS -> avaliarAbolitio(input);
            case LEI_MAIS_BENEFICA -> avaliarLeiMaisBenefica(input);
            case LEI_MAIS_GRAVE -> avaliarLeiMaisGrave(input);
            case NOVATIO_LEGIS_INCRIMINADORA -> avaliarNovatioIncriminadora(input);
        };
    }

    private RetroatividadeResult avaliarAbolitio(RetroatividadeInput input) {
        if (input.situacao() == SituacaoProcessual.PENA_JA_EXTINTA) {
            return new RetroatividadeResult(
                    LeiAplicavel.LEI_NOVA, false, false,
                    "Pena já extinta: abolitio criminis não restaura situação processual já encerrada — efeitos penais cessam, mas efeitos extrapenais (civil, administrativo) podem subsistir",
                    "CF art. 5°, XL",
                    "CP art. 2°, caput",
                    "A extinção da punibilidade retroage mas não desfaz execução já cumprida integralmente.");
        }
        return new RetroatividadeResult(
                LeiAplicavel.LEI_NOVA, true, true,
                "Abolitio criminis: extingue a punibilidade, cessa execução da pena e todos os efeitos penais da condenação",
                "CF art. 5°, XL — a lei penal não retroagirá, salvo para beneficiar o réu",
                "CP art. 2°, caput — lei posterior que deixa de considerar crime cessa a execução e os efeitos penais",
                "Juízo da execução deve extinguir a pena de ofício. Efeitos civis da sentença condenatória subsistem (CP art. 2°, caput in fine).");
    }

    private RetroatividadeResult avaliarLeiMaisBenefica(RetroatividadeInput input) {
        if (input.reuSolicitaCombinacaoDeLeis()) {
            return new RetroatividadeResult(
                    LeiAplicavel.AMBAS_SE_BENEFICA, false, false,
                    "Combinação de leis vedada — deve-se aplicar integralmente a lei mais benéfica, não 'partes' de cada lei",
                    "CF art. 5°, XL",
                    "STF Súmula 501; CP art. 2°, parágrafo único",
                    "Juiz deve comparar os regimes legais em sua integralidade e aplicar o mais favorável ao réu.");
        }
        if (input.situacao() == SituacaoProcessual.PENA_JA_EXTINTA) {
            return new RetroatividadeResult(
                    LeiAplicavel.LEI_ANTIGA, false, false,
                    "Pena já extinta: lei penal mais benéfica superveniente não produz efeitos sobre pena já cumprida integralmente",
                    "CF art. 5°, XL",
                    "CP art. 2°, parágrafo único",
                    "Não há interesse processual para revisão — situação já consolidada.");
        }
        boolean revisaoDeOficio = input.situacao() == SituacaoProcessual.CUMPRINDO_PENA
                || input.situacao() == SituacaoProcessual.CONDENADO_TRANSITADO_EM_JULGADO;
        return new RetroatividadeResult(
                LeiAplicavel.LEI_NOVA, true, revisaoDeOficio,
                "Lei penal mais benéfica retroage a fatos anteriores, inclusive após trânsito em julgado",
                "CF art. 5°, XL — salvo para beneficiar o réu",
                "CP art. 2°, parágrafo único — lei posterior que favorecer o agente aplica-se a fatos anteriores",
                revisaoDeOficio
                        ? "Juízo da execução aplica a lex mitior de ofício (STF Súmula 611). STF pode rever em habeas corpus."
                        : "Juiz do processo aplica imediatamente a lei mais benéfica.");
    }

    private RetroatividadeResult avaliarLeiMaisGrave(RetroatividadeInput input) {
        boolean aplicaLeiGrave = (input.tipoCrime() == TipoCrimeQuantoAoDurar.PERMANENTE
                || input.tipoCrime() == TipoCrimeQuantoAoDurar.CONTINUADO)
                && input.leiNovaVigenteAntesDataCessacao();

        if (aplicaLeiGrave) {
            return new RetroatividadeResult(
                    LeiAplicavel.LEI_NOVA, false, false,
                    "Exceção: crime permanente ou continuado — aplica-se a lei mais grave se ela entrou em vigor ANTES da cessação da permanência ou continuidade",
                    "CF art. 5°, XL",
                    "STF Súmula 711 — a lei penal mais grave aplica-se ao crime continuado ou ao crime permanente, se a sua vigência é anterior à cessação da continuidade ou da permanência",
                    "A razão é que o réu optou por continuar praticando o crime já sob a vigência da lei mais severa.");
        }

        return new RetroatividadeResult(
                LeiAplicavel.LEI_ANTIGA, false, false,
                "Lei penal mais grave não retroage — aplica-se a lei vigente na época do fato (tempus regit actum)",
                "CF art. 5°, XL — a lei penal não retroagirá, salvo para beneficiar o réu; CF art. 5°, XXXIX — não há crime sem lei anterior",
                "CP art. 1° — não há crime sem lei anterior que o defina; CP art. 2° a contrario sensu",
                "O fato foi praticado sob a lei anterior: aplica-se a lei vigente na data do crime, mesmo que mais branda.");
    }

    private RetroatividadeResult avaliarNovatioIncriminadora(RetroatividadeInput input) {
        return new RetroatividadeResult(
                LeiAplicavel.LEI_ANTIGA, false, false,
                "Novatio legis incriminadora não retroage — conduta praticada antes da lei é atípica (fato anterior era lícito)",
                "CF art. 5°, XL e XXXIX — legalidade e irretroatividade penal",
                "CP art. 1° — nullum crimen, nulla poena sine lege praevia",
                "Nenhuma pessoa pode ser punida por fato que não era crime quando praticado. Lei nova que cria crime só alcança fatos posteriores à sua vigência.");
    }
}
