package com.tcc.pjb.backend.service.processual.celerity;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CeleridadeNextBestActionService {

    public record ProximaAcao(
            ProcessoAceleracaoAcaoTipo tipo,
            JudicialIndependenceSafeAccelerationPolicy.ModoAceleracao modo,
            String mensagem,
            boolean exigeConfirmacaoHumana
    ) {}

    public List<ProximaAcao> sugerirProximasAcoes(
            List<CeleridadeBottleneckDiagnosisService.DiagnosticoGargalo> gargalos,
            CeleridadeNivel nivel) {

        return gargalos.stream()
                .filter(g -> g.acaoRecomendada() != null)
                .map(g -> {
                    ProcessoAceleracaoAcaoTipo acao = g.acaoRecomendada();
                    JudicialIndependenceSafeAccelerationPolicy.ModoAceleracao modo =
                            JudicialIndependenceSafeAccelerationPolicy.modoParaAcao(acao);
                    boolean exige = acao.isAtoJurisdicional() || nivel.requerAcaoImediata();
                    return new ProximaAcao(acao, modo, g.solucaoSugerida(), exige);
                })
                .toList();
    }

    public ProximaAcao melhorAcaoUnica(
            List<CeleridadeBottleneckDiagnosisService.DiagnosticoGargalo> gargalos,
            CeleridadeNivel nivel) {

        return gargalos.stream()
                .filter(CeleridadeBottleneckDiagnosisService.DiagnosticoGargalo::acaoImediataCartoraria)
                .map(g -> {
                    ProcessoAceleracaoAcaoTipo acao = g.acaoRecomendada();
                    return new ProximaAcao(acao,
                            JudicialIndependenceSafeAccelerationPolicy.modoParaAcao(acao),
                            g.solucaoSugerida(), false);
                })
                .findFirst()
                .orElseGet(() -> sugerirProximasAcoes(gargalos, nivel).stream()
                        .findFirst()
                        .orElse(null));
    }
}
