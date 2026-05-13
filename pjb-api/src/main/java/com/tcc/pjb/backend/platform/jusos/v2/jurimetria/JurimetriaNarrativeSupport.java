package com.tcc.pjb.backend.platform.jusos.v2.jurimetria;

import com.tcc.pjb.backend.ai.jurimetria.model.JurimetriaReport;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.platform.jusos.v2.rules.NationalRulePackEngine;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
class JurimetriaNarrativeSupport {

    List<String> formatarPrecedentes(List<Precedente> precedentes) {
        return precedentes.stream()
                .sorted(Comparator.comparing(Precedente::getDataPublicacao, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .map(precedente -> {
                    StringBuilder sb = new StringBuilder();
                    if (precedente.getFonte() != null) {
                        sb.append(precedente.getFonte().name()).append(' ');
                    }
                    if (precedente.getIdentificador() != null && !precedente.getIdentificador().isBlank()) {
                        sb.append(precedente.getIdentificador()).append(" — ");
                    }
                    sb.append(JurimetriaSupportUtils.firstNonBlank(precedente.getTitulo(), precedente.getTese(), precedente.getEmentaResumo(), "Precedente sem resumo"));
                    if (precedente.getDataPublicacao() != null) {
                        sb.append(" [").append(precedente.getDataPublicacao()).append(']');
                    }
                    return JurimetriaSupportUtils.truncate(sb.toString(), 280);
                })
                .toList();
    }

    String construirMetodologia(NationalRulePackEngine.ResultadoRegras regras,
                                NationalPrazoEngine.PrazoCalculado prazoSensivel,
                                JurimetriaReport relatorioIA,
                                int totalPrecedentes) {
        List<String> blocos = new ArrayList<>();
        blocos.add("Heurística local baseada em estoque, recursividade, encerramento e congestionamento do PJB");
        blocos.add("Fusão com sinais de IA do JurimetriaService");
        blocos.add("Validação normativa via NationalRulePackEngine");
        if (prazoSensivel != null) {
            blocos.add("Sensibilidade temporal via NationalPrazoEngine (" + prazoSensivel.tipo().name() + ")");
        }
        blocos.add("Pesquisa de precedentes indexados: " + totalPrecedentes + " registros relevantes");
        if (relatorioIA != null && relatorioIA.getExplicacao() != null && !relatorioIA.getExplicacao().isBlank()) {
            blocos.add(JurimetriaSupportUtils.truncate(relatorioIA.getExplicacao(), 140));
        }
        return String.join(" | ", blocos);
    }
}
