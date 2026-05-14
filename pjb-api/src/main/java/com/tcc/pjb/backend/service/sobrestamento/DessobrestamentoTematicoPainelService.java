package com.tcc.pjb.backend.service.sobrestamento;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DessobrestamentoTematicoPainelService {

    public record ProcessoSobrestado(
            UUID processoId,
            String numero,
            String temaNumero,
            LocalDate dataSobrestamento,
            boolean temaJulgado,
            LocalDate dataJulgamentoTema,
            boolean acordaoPublicado,
            boolean prazoAdequacaoEsgotado
    ) {}

    public record DessobrestamentoRecomendacao(
            UUID processoId,
            boolean deveDessobrestar,
            String motivacao,
            String acaoRecomendada
    ) {}

    public record PainelResult(
            List<DessobrestamentoRecomendacao> recomendacoes,
            int totalDessobrestar,
            int totalManter,
            LocalDate dataAnalise
    ) {}

    public PainelResult analisar(List<ProcessoSobrestado> processos) {
        List<DessobrestamentoRecomendacao> recomendacoes = new ArrayList<>();

        for (ProcessoSobrestado p : processos) {
            recomendacoes.add(avaliarProcesso(p));
        }

        long dessobrestar = recomendacoes.stream()
                .filter(DessobrestamentoRecomendacao::deveDessobrestar).count();

        return new PainelResult(
                List.copyOf(recomendacoes),
                (int) dessobrestar,
                processos.size() - (int) dessobrestar,
                LocalDate.now());
    }

    private DessobrestamentoRecomendacao avaliarProcesso(ProcessoSobrestado p) {
        if (!p.temaJulgado()) {
            return new DessobrestamentoRecomendacao(p.processoId(), false,
                    "Tema " + p.temaNumero() + " ainda pendente de julgamento.",
                    "Manter sobrestamento — acompanhar pauta do STF/STJ.");
        }
        if (!p.acordaoPublicado()) {
            return new DessobrestamentoRecomendacao(p.processoId(), false,
                    "Acórdão do Tema " + p.temaNumero() + " não publicado.",
                    "Aguardar publicação do acórdão para adequação (art. 1.040, CPC).");
        }
        return new DessobrestamentoRecomendacao(p.processoId(), true,
                "Tema " + p.temaNumero() + " julgado e acórdão publicado em " + p.dataJulgamentoTema() + ".",
                "Dessobrestar e aplicar precedente vinculante (art. 1.040, CPC).");
    }
}
