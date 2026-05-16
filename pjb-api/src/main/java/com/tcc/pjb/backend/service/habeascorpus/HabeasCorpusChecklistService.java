package com.tcc.pjb.backend.service.habeascorpus;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HabeasCorpusChecklistService {

    public enum TipoHC {
        LIBERATORIO,
        PREVENTIVO,
        REPARATORIO
    }

    public enum MotivoIlegalidade {
        SEM_JUSTA_CAUSA,
        INCOMPETENCIA_AUTORIDADE,
        EXCESSO_PRAZO,
        NULIDADE_PROCESSO,
        EXTINCAO_PUNIBILIDADE,
        MEDIDA_SEGURANCA_ILEGAL,
        PRISAO_ALEM_PRAZO
    }

    public enum OrgaoCoator {
        DELEGACIA_ESTADUAL,
        DELEGACIA_FEDERAL,
        JUIZ_ESTADUAL,
        JUIZ_FEDERAL,
        TRIBUNAL_ESTADUAL,
        STJ
    }

    public enum TribunalCompetente {
        TJ, TRF, STJ, STF
    }

    public record HabeasCorpusInput(
            TipoHC tipo,
            OrgaoCoator orgaoCoator,
            List<MotivoIlegalidade> motivos,
            boolean penaExtinta,
            boolean transgressaoDisciplinarMilitar,
            boolean impugnaMultaIsolada,
            boolean impugnaJecrimTurmaRecursal
    ) {}

    public record CompetenciaHC(
            TribunalCompetente tribunal,
            String fundamentoCompetencia
    ) {}

    public record HabeasCorpusResult(
            boolean cabivel,
            String motivoNaoCabimento,
            CompetenciaHC competencia,
            List<String> fundamentosLegais,
            String prazo,
            String observacao
    ) {}

    public HabeasCorpusResult avaliar(HabeasCorpusInput input) {
        String naoCabimento = verificarNaoCabimento(input);
        if (naoCabimento != null) {
            return new HabeasCorpusResult(false, naoCabimento, null, List.of(), "N/A", naoCabimento);
        }

        CompetenciaHC competencia = calcularCompetencia(input.orgaoCoator());
        List<String> fundamentos = buildFundamentos(input);
        String obs = buildObservacao(input);

        return new HabeasCorpusResult(true, null, competencia, List.copyOf(fundamentos), "Sem prazo (writ constitucional)", obs);
    }

    private String verificarNaoCabimento(HabeasCorpusInput input) {
        if (input.penaExtinta()) {
            return "HC incabível: pena privativa de liberdade já extinta (STF Súmula 694)";
        }
        if (input.transgressaoDisciplinarMilitar()) {
            return "HC incabível: transgressão disciplinar militar (STF Súmula 693)";
        }
        if (input.impugnaMultaIsolada()) {
            return "HC incabível: rediscussão de multa sem restrição à liberdade (STF Súmula 695)";
        }
        if (input.impugnaJecrimTurmaRecursal()) {
            return "HC originário incabível no STF para impugnar decisão de turma recursal de JECrim (STF Súmula 606)";
        }
        return null;
    }

    private CompetenciaHC calcularCompetencia(OrgaoCoator coator) {
        return switch (coator) {
            case DELEGACIA_ESTADUAL -> new CompetenciaHC(TribunalCompetente.TJ,
                    "CPP art. 650, II — coator é autoridade estadual");
            case DELEGACIA_FEDERAL -> new CompetenciaHC(TribunalCompetente.TRF,
                    "CPP art. 650, II c/c Lei 7.210/84 — coator é autoridade federal");
            case JUIZ_ESTADUAL -> new CompetenciaHC(TribunalCompetente.TJ,
                    "CPP art. 650, I — tribunal imediatamente superior ao juiz coator");
            case JUIZ_FEDERAL -> new CompetenciaHC(TribunalCompetente.TRF,
                    "CPP art. 650, I — tribunal imediatamente superior ao juiz federal coator");
            case TRIBUNAL_ESTADUAL -> new CompetenciaHC(TribunalCompetente.STJ,
                    "CF art. 105, I, c — STJ julga HC quando coator é tribunal superior estadual");
            case STJ -> new CompetenciaHC(TribunalCompetente.STF,
                    "CF art. 102, I, i — STF julga HC quando coator é o STJ");
        };
    }

    private List<String> buildFundamentos(HabeasCorpusInput input) {
        List<String> fundamentos = new ArrayList<>();
        fundamentos.add("CF art. 5°, LXVIII — garantia constitucional do habeas corpus");
        fundamentos.add("CPP art. 647 — cabe HC sempre que houver violência ou coação ilegal à liberdade de locomoção");

        for (MotivoIlegalidade motivo : input.motivos()) {
            switch (motivo) {
                case SEM_JUSTA_CAUSA ->
                        fundamentos.add("CPP art. 648, I — não há justa causa para a coação");
                case INCOMPETENCIA_AUTORIDADE ->
                        fundamentos.add("CPP art. 648, II — autoridade coatora incompetente");
                case EXCESSO_PRAZO ->
                        fundamentos.add("CPP art. 648, II — prazo superior ao permitido em lei");
                case NULIDADE_PROCESSO ->
                        fundamentos.add("CPP art. 648, VI — processo manifestamente nulo");
                case EXTINCAO_PUNIBILIDADE ->
                        fundamentos.add("CPP art. 648, VII — extinção da punibilidade");
                case MEDIDA_SEGURANCA_ILEGAL ->
                        fundamentos.add("CPP art. 648, VI — medida de segurança por tempo indeterminado");
                case PRISAO_ALEM_PRAZO ->
                        fundamentos.add("CPP art. 648, II — preso além do prazo legal");
            }
        }

        if (input.tipo() == TipoHC.PREVENTIVO) {
            fundamentos.add("CPP art. 660, §1° — salvo-conduto quando não consumada a coação");
        }

        return fundamentos;
    }

    private String buildObservacao(HabeasCorpusInput input) {
        StringBuilder obs = new StringBuilder();
        obs.append("HC não tem prazo (writ de garantia constitucional). ");

        if (input.tipo() == TipoHC.PREVENTIVO) {
            obs.append("Natureza preventiva: impetrar antes da consumação da ameaça de prisão — resultado é salvo-conduto. ");
        } else if (input.tipo() == TipoHC.LIBERATORIO) {
            obs.append("Natureza liberatória: paciente já está preso ilegalmente — resultado é ordem de soltura imediata. ");
        } else {
            obs.append("Natureza reparatória: prisão já cessada, mas ilegalidade deve ser declarada para fins de registro e reparação. ");
        }

        obs.append("Liminar possível nos termos do CPP art. 660, §2°.");
        return obs.toString();
    }
}
