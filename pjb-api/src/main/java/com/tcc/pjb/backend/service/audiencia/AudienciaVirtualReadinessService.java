package com.tcc.pjb.backend.service.audiencia;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AudienciaVirtualReadinessService {

    public record VirtualReadinessInput(
            UUID processoId,
            AudienciaTipo tipo,
            boolean plataformaHabilitada,
            boolean linkGerado,
            boolean partesNotificadas,
            boolean advogadosConfirmados,
            boolean gravidadeConexaoVerificada,
            boolean menorEnvolvido,
            boolean acordoTodosParticipantes
    ) {}

    public record VirtualReadinessResult(
            boolean apto,
            List<String> pendencias,
            String fundamentacao
    ) {}

    public VirtualReadinessResult avaliar(VirtualReadinessInput input) {
        List<String> pendencias = new ArrayList<>();

        if (!input.tipo().admiteVirtual()) {
            return new VirtualReadinessResult(false,
                    List.of("Tipo " + input.tipo() + " não admite audiência virtual."),
                    "Audiência virtual inviável para este tipo processual.");
        }
        if (input.menorEnvolvido()) {
            pendencias.add("Menor envolvido — verificar autorização expressa do juízo para modalidade virtual.");
        }
        if (!input.plataformaHabilitada()) {
            pendencias.add("Plataforma de videoconferência não habilitada na vara.");
        }
        if (!input.linkGerado()) {
            pendencias.add("Link de acesso à audiência não gerado.");
        }
        if (!input.partesNotificadas()) {
            pendencias.add("Partes não notificadas com antecedência mínima.");
        }
        if (!input.advogadosConfirmados()) {
            pendencias.add("Confirmação dos advogados pendente.");
        }
        if (!input.gravidadeConexaoVerificada()) {
            pendencias.add("Qualidade de conexão dos participantes não verificada.");
        }
        if (!input.acordoTodosParticipantes()) {
            pendencias.add("Ausência de concordância expressa de todos os participantes (art. 236, §3º, CPC).");
        }

        boolean apto = pendencias.isEmpty();
        return new VirtualReadinessResult(apto, List.copyOf(pendencias),
                apto ? "Audiência virtual apta para realização."
                     : pendencias.size() + " pendência(s) para audiência virtual.");
    }
}
