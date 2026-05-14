package com.tcc.pjb.backend.service.mandados;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CentralMandadosAccelerationService {

    public record MandadoItem(
            UUID mandadoId,
            String processoNumero,
            boolean devolvido,
            boolean semCumprimento,
            boolean prazoVencido,
            boolean reuPreso
    ) {}

    public record MandadoPrioritario(
            UUID mandadoId,
            String processoNumero,
            int prioridade,
            String acao
    ) {}

    private final MandadoReadinessService readinessService;
    private final MandadoReturnDiagnosisService diagnosisService;

    public CentralMandadosAccelerationService(
            MandadoReadinessService readinessService,
            MandadoReturnDiagnosisService diagnosisService) {
        this.readinessService = readinessService;
        this.diagnosisService = diagnosisService;
    }

    public List<MandadoPrioritario> identificarPrioritarios(List<MandadoItem> mandados) {
        return mandados.stream()
                .filter(m -> m.devolvido() || m.prazoVencido() || m.semCumprimento())
                .map(m -> new MandadoPrioritario(
                        m.mandadoId(), m.processoNumero(),
                        calcularPrioridade(m), resolverAcao(m)))
                .sorted(Comparator.comparingInt(MandadoPrioritario::prioridade).reversed())
                .toList();
    }

    private int calcularPrioridade(MandadoItem m) {
        int score = 0;
        if (m.reuPreso()) score += 100;
        if (m.prazoVencido()) score += 50;
        if (m.devolvido()) score += 30;
        if (m.semCumprimento()) score += 20;
        return score;
    }

    private String resolverAcao(MandadoItem m) {
        if (m.devolvido()) return "Diagnosticar devolução e expedir novo mandado.";
        if (m.prazoVencido()) return "Prazo de cumprimento vencido — providenciar urgente.";
        return "Verificar cumprimento do mandado.";
    }
}
