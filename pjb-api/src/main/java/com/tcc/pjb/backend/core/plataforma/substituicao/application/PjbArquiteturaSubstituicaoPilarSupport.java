package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoCapacidade;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoPilar;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbFechamentoStatus;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Construção compartilhada de pilares/capacidades usada pelos avaliadores de pilar.
 * Extraída de PjbArquiteturaSubstituicaoNacionalApplicationService (F6) para eliminar
 * duplicação entre os 4 avaliadores sem reintroduzir o god service.
 */
final class PjbArquiteturaSubstituicaoPilarSupport {

    private PjbArquiteturaSubstituicaoPilarSupport() {
    }

    static boolean available(ObjectProvider<?> provider) {
        return provider.getIfAvailable() != null;
    }

    static boolean available(Object value) {
        return value != null;
    }

    static int score(double value) {
        return Math.max(0, Math.min(100, (int) Math.round(value)));
    }

    static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    static PjbArquiteturaSubstituicaoCapacidade capacidade(String codigo,
                                                            String titulo,
                                                            boolean concluida,
                                                            int scoreConcluida,
                                                            List<String> evidencias,
                                                            List<String> pendencias) {
        int score = concluida ? Math.max(scoreConcluida, 85) : Math.max(45, Math.min(84, scoreConcluida - 12));
        PjbFechamentoStatus status = concluida ? PjbFechamentoStatus.CONCLUIDA : score >= 70 ? PjbFechamentoStatus.PARCIAL : PjbFechamentoStatus.PENDENTE;
        String conclusao = concluida
                ? "Capacidade materializada com base concreta no herdeiro atual."
                : "Capacidade parcialmente presente, ainda exigindo fechamento fino para substituição nacional imediata.";
        return new PjbArquiteturaSubstituicaoCapacidade(codigo, titulo, status, score, conclusao, evidencias, pendencias);
    }

    static PjbArquiteturaSubstituicaoPilar pilar(String codigo,
                                                  String titulo,
                                                  List<PjbArquiteturaSubstituicaoCapacidade> capacidades,
                                                  List<String> proximasAcoes) {
        int score = score(capacidades.stream().mapToInt(PjbArquiteturaSubstituicaoCapacidade::score).average().orElse(0));
        long concluidas = capacidades.stream().filter(PjbArquiteturaSubstituicaoCapacidade::concluida).count();
        boolean pronto = concluidas == capacidades.size() && score >= 85;
        PjbFechamentoStatus status = pronto ? PjbFechamentoStatus.CONCLUIDA : score >= 75 ? PjbFechamentoStatus.PARCIAL : PjbFechamentoStatus.BLOQUEADA;
        return new PjbArquiteturaSubstituicaoPilar(codigo, titulo, status, score, pronto, capacidades, proximasAcoes);
    }
}
