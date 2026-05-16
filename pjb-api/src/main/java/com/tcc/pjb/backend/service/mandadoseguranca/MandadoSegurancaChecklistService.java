package com.tcc.pjb.backend.service.mandadoseguranca;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MandadoSegurancaChecklistService {

    public enum TipoMS {
        INDIVIDUAL,
        COLETIVO
    }

    public enum AutoridadeCoatora {
        AUTORIDADE_MUNICIPAL,
        AUTORIDADE_ESTADUAL,
        AUTORIDADE_FEDERAL,
        MINISTRO_ESTADO,
        TCU,
        TRIBUNAL_SUPERIOR
    }

    public enum TribunalCompetente {
        JUIZ_ESTADUAL_1GRAU,
        JUIZ_FEDERAL_1GRAU,
        TJ,
        TRF,
        STJ,
        STF
    }

    public record MandadoSegurancaInput(
            TipoMS tipo,
            AutoridadeCoatora autoridade,
            boolean direitoLiquidoCerto,
            boolean provaPreConstituida,
            boolean dentroDosPrazo120dias,
            boolean contraLeiEmTese,
            boolean contraDecisaoTransitadaJulgado,
            boolean existeRecursoComEfeitoSuspensivo,
            boolean atoDeGestaoComercialEmpublicaMista
    ) {}

    public record CompetenciaMS(
            TribunalCompetente tribunal,
            String fundamento
    ) {}

    public record MandadoSegurancaResult(
            boolean cabivel,
            String motivoNaoCabimento,
            CompetenciaMS competencia,
            List<String> fundamentosLegais,
            String prazoImpetração,
            boolean liminarCabivel,
            String observacao
    ) {}

    public MandadoSegurancaResult avaliar(MandadoSegurancaInput input) {
        String naoCabimento = verificarNaoCabimento(input);
        if (naoCabimento != null) {
            return new MandadoSegurancaResult(false, naoCabimento, null, List.of(),
                    "120 dias (art. 23 Lei 12.016/09)", false, naoCabimento);
        }

        CompetenciaMS competencia = calcularCompetencia(input.autoridade());
        List<String> fundamentos = buildFundamentos(input);
        String obs = buildObservacao(input);

        return new MandadoSegurancaResult(
                true, null, competencia, List.copyOf(fundamentos),
                "120 dias contados da ciência do ato coator (art. 23 Lei 12.016/09)",
                true, obs);
    }

    private String verificarNaoCabimento(MandadoSegurancaInput input) {
        if (input.contraLeiEmTese()) {
            return "MS incabível contra lei em tese (STF Súmula 266; art. 5°, I, Lei 12.016/09)";
        }
        if (input.contraDecisaoTransitadaJulgado()) {
            return "MS incabível contra decisão judicial transitada em julgado (art. 5°, III, Lei 12.016/09)";
        }
        if (input.existeRecursoComEfeitoSuspensivo()) {
            return "MS incabível quando cabível recurso com efeito suspensivo (art. 5°, II, Lei 12.016/09)";
        }
        if (input.atoDeGestaoComercialEmpublicaMista()) {
            return "MS incabível contra ato de gestão comercial de empresa pública ou sociedade de economia mista (art. 1°, §2°, Lei 12.016/09)";
        }
        if (!input.direitoLiquidoCerto()) {
            return "MS incabível: ausência de direito líquido e certo — direito deve ser demonstrado de plano por prova pré-constituída (CF art. 5°, LXIX)";
        }
        if (!input.provaPreConstituida()) {
            return "MS incabível: dilação probatória vedada — exige prova pré-constituída (STF Súmula 415)";
        }
        if (!input.dentroDosPrazo120dias()) {
            return "MS incabível: prazo decadencial de 120 dias esgotado (art. 23 Lei 12.016/09)";
        }
        return null;
    }

    private CompetenciaMS calcularCompetencia(AutoridadeCoatora autoridade) {
        return switch (autoridade) {
            case AUTORIDADE_MUNICIPAL -> new CompetenciaMS(TribunalCompetente.JUIZ_ESTADUAL_1GRAU,
                    "CPC art. 50 c/c lei de organização judiciária estadual — 1° grau estadual");
            case AUTORIDADE_ESTADUAL -> new CompetenciaMS(TribunalCompetente.TJ,
                    "CF art. 125 — TJ julga MS contra atos de autoridade estadual quando não for o próprio TJ o coator");
            case AUTORIDADE_FEDERAL -> new CompetenciaMS(TribunalCompetente.JUIZ_FEDERAL_1GRAU,
                    "Lei 12.016/09 art. 2° c/c CF art. 109, VIII — juiz federal de 1° grau");
            case MINISTRO_ESTADO -> new CompetenciaMS(TribunalCompetente.STJ,
                    "CF art. 105, I, b — STJ julga MS contra ato de Ministro de Estado");
            case TCU -> new CompetenciaMS(TribunalCompetente.STJ,
                    "CF art. 105, I, b — STJ julga MS contra ato do TCU");
            case TRIBUNAL_SUPERIOR -> new CompetenciaMS(TribunalCompetente.STF,
                    "CF art. 102, I, d — STF julga MS contra ato de tribunal superior");
        };
    }

    private List<String> buildFundamentos(MandadoSegurancaInput input) {
        List<String> fundamentos = new ArrayList<>();

        if (input.tipo() == TipoMS.INDIVIDUAL) {
            fundamentos.add("CF art. 5°, LXIX — MS individual: direito líquido e certo contra ato ilegal ou abusivo de autoridade pública");
        } else {
            fundamentos.add("CF art. 5°, LXX — MS coletivo: impetrado por partido político, sindicato ou entidade de classe");
            fundamentos.add("Lei 12.016/09 art. 21 — legitimidade ativa para o MS coletivo");
        }

        fundamentos.add("Lei 12.016/09 art. 1° — concessão do mandado de segurança para proteger direito líquido e certo");
        fundamentos.add("Lei 12.016/09 art. 7°, III — liminar: relevância do fundamento e urgência");
        fundamentos.add("Lei 12.016/09 art. 23 — prazo decadencial de 120 dias");

        return fundamentos;
    }

    private String buildObservacao(MandadoSegurancaInput input) {
        StringBuilder obs = new StringBuilder();
        obs.append("Prazo decadencial de 120 dias — não se suspende nem interrompe (STF Súmula 632). ");

        if (input.tipo() == TipoMS.COLETIVO) {
            obs.append("MS coletivo dispensa autorização individual dos associados (STF Súmula 629). ");
        }

        obs.append("Liminar exige relevância do fundamento e urgência (art. 7°, III). ");
        obs.append("Sentença concessiva sujeita a reexame necessário (art. 14, §1°).");
        return obs.toString();
    }
}
