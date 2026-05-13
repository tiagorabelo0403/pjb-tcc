package com.tcc.pjb.backend.service.processual.peticionamento.triagem;

import com.tcc.pjb.backend.core.peticionamento.triagem.LitigantePerfilPort;
import com.tcc.pjb.backend.core.peticionamento.triagem.LitiganteRiskNivel;
import com.tcc.pjb.backend.core.peticionamento.triagem.LitiganteRiskScore;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LitigantePerfilAdapter implements LitigantePerfilPort {

    private static final int LIMIAR_ALTO = 100;
    private static final int LIMIAR_MEDIO = 30;
    private static final int LIMIAR_BAIXO = 10;
    private static final double TAXA_SUCUMBENCIA_CRITICA = 0.9;
    private static final double TAXA_SUCUMBENCIA_ALTA = 0.75;

    private final LitiganteHistoricoQueryService historicoQueryService;

    public LitigantePerfilAdapter(LitiganteHistoricoQueryService historicoQueryService) {
        this.historicoQueryService = Objects.requireNonNull(historicoQueryService);
    }

    @Override
    public LitiganteRiskScore avaliar(String cpfCnpj) {
        Objects.requireNonNull(cpfCnpj, "cpfCnpj");
        LitiganteHistoricoQueryService.HistoricoLitigante historico = historicoQueryService.consultar(cpfCnpj);
        List<String> padroes = detectarPadroes(historico);
        LitiganteRiskNivel nivel = calcularNivel(historico.totalProcessos(), historico.taxaSucumbencia(), padroes);
        return new LitiganteRiskScore(cpfCnpj, historico.totalProcessos(), historico.taxaSucumbencia(), padroes, nivel);
    }

    private List<String> detectarPadroes(LitiganteHistoricoQueryService.HistoricoLitigante h) {
        List<String> padroes = new ArrayList<>();
        if (h.totalProcessos() >= LIMIAR_ALTO) {
            padroes.add("volume_litigioso_extremo");
        }
        if (h.taxaSucumbencia() >= TAXA_SUCUMBENCIA_CRITICA && h.totalProcessos() > 5) {
            padroes.add("sucumbencia_sistematica");
        }
        if (h.recursosProtelatoriasDetectados() > 3) {
            padroes.add("recurso_protelatório_reiterado");
        }
        if (h.excecoesSistematicas() > 5) {
            padroes.add("excecao_sistematica");
        }
        return List.copyOf(padroes);
    }

    private LitiganteRiskNivel calcularNivel(int total, double taxaSucumbencia, List<String> padroes) {
        if (total >= LIMIAR_ALTO || (taxaSucumbencia >= TAXA_SUCUMBENCIA_CRITICA && total >= 20)) {
            return padroes.size() >= 2 ? LitiganteRiskNivel.CRITICO : LitiganteRiskNivel.ALTO;
        }
        if (total >= LIMIAR_MEDIO || taxaSucumbencia >= TAXA_SUCUMBENCIA_ALTA) {
            return LitiganteRiskNivel.MEDIO;
        }
        if (total >= LIMIAR_BAIXO) {
            return LitiganteRiskNivel.BAIXO;
        }
        return LitiganteRiskNivel.BAIXO;
    }
}
