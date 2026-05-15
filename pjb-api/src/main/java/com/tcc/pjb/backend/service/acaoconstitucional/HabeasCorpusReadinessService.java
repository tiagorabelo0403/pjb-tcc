package com.tcc.pjb.backend.service.acaoconstitucional;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HabeasCorpusReadinessService {

    public enum CompetenciaHC {
        STF, STJ, TRF, TJ, JUIZ_PRIMEIRO_GRAU
    }

    public record HabeasCorpusInput(
            String processoId,
            boolean liberdadeLocomocaoAmeacada,
            boolean coacaoIlegal,
            boolean abusoDePoderConfigurado,
            String autoridadeCoatora,
            boolean questaoMeramenteFatual,
            boolean condenacaoSomenteAMulta
    ) {}

    public record HabeasCorpusResult(
            boolean cabivel,
            CompetenciaHC competenciaSugerida,
            boolean liminarPossivel,
            List<String> requisitosAtendidos,
            List<String> impeditivos
    ) {}

    public HabeasCorpusResult avaliar(HabeasCorpusInput input) {
        List<String> atendidos = new ArrayList<>();
        List<String> impeditivos = new ArrayList<>();

        if (input.condenacaoSomenteAMulta()) {
            impeditivos.add("HC incabível contra condenação exclusivamente a pena de multa (Súmula 693 STF).");
        }
        if (input.questaoMeramenteFatual()) {
            impeditivos.add("HC não serve para reexame de prova — matéria de fato deve ser discutida em recurso próprio.");
        }
        if (!input.liberdadeLocomocaoAmeacada() && !input.coacaoIlegal()) {
            impeditivos.add("Não há ameaça à liberdade de locomoção nem coação ilegal identificada (CF art. 5º LXVIII).");
        }

        if (input.liberdadeLocomocaoAmeacada()) atendidos.add("Liberdade de locomoção ameaçada ou violada.");
        if (input.coacaoIlegal()) atendidos.add("Coação ilegal configurada.");
        if (input.abusoDePoderConfigurado()) atendidos.add("Abuso de poder identificado.");

        CompetenciaHC competencia = resolverCompetencia(input.autoridadeCoatora());
        boolean cabivel = impeditivos.isEmpty();
        boolean liminar = cabivel && input.liberdadeLocomocaoAmeacada() && input.coacaoIlegal();

        return new HabeasCorpusResult(cabivel, competencia, liminar, atendidos, impeditivos);
    }

    private CompetenciaHC resolverCompetencia(String autoridade) {
        if (autoridade == null) return CompetenciaHC.JUIZ_PRIMEIRO_GRAU;
        String up = autoridade.toUpperCase();
        if (up.contains("STJ") || up.contains("SUPERIOR")) return CompetenciaHC.STF;
        if (up.contains("TRF") || up.contains("TJ") || up.contains("TRIBUNAL")) return CompetenciaHC.STJ;
        if (up.contains("FEDERAL")) return CompetenciaHC.TRF;
        if (up.contains("ESTADUAL") || up.contains("JUIZ")) return CompetenciaHC.TJ;
        return CompetenciaHC.JUIZ_PRIMEIRO_GRAU;
    }
}
