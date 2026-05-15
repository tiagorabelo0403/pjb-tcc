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
            CompetenciaHC competenciaSugerida,
            List<String> requisitosVerificados,
            List<String> pendenciasIdentificadas,
            String sinalizacao
    ) {}

    private static final String SINAL_SEM_PENDENCIAS =
            "Sem pendências formais localizadas — sugestão sujeita à validação judicial. Não substitui análise do magistrado.";
    private static final String SINAL_COM_PENDENCIAS =
            "Pendências identificadas — conferir antes de submeter ao magistrado.";

    public HabeasCorpusResult avaliar(HabeasCorpusInput input) {
        List<String> verificados = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();

        if (input.condenacaoSomenteAMulta()) {
            pendencias.add("Pendência identificada: condenação exclusivamente a multa — HC possivelmente incabível (Súmula 693 STF).");
        }
        if (input.questaoMeramenteFatual()) {
            pendencias.add("Pendência identificada: matéria de fato — HC não serve para reexame de prova; conferir recurso adequado.");
        }
        if (!input.liberdadeLocomocaoAmeacada() && !input.coacaoIlegal()) {
            pendencias.add("Possível requisito a conferir: ameaça à liberdade de locomoção ou coação ilegal não identificada (CF art. 5º LXVIII).");
        }

        if (input.liberdadeLocomocaoAmeacada()) verificados.add("Liberdade de locomoção ameaçada ou violada — conferido.");
        if (input.coacaoIlegal()) verificados.add("Coação ilegal — conferido.");
        if (input.abusoDePoderConfigurado()) verificados.add("Abuso de poder — conferido.");
        if (input.liberdadeLocomocaoAmeacada() && input.coacaoIlegal() && pendencias.isEmpty()) {
            verificados.add("Possível requisito a conferir: tutela de urgência (liminar) — sujeito à avaliação do magistrado.");
        }

        CompetenciaHC competencia = resolverCompetencia(input.autoridadeCoatora());

        return new HabeasCorpusResult(competencia, List.copyOf(verificados), List.copyOf(pendencias),
                pendencias.isEmpty() ? SINAL_SEM_PENDENCIAS : SINAL_COM_PENDENCIAS);
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
