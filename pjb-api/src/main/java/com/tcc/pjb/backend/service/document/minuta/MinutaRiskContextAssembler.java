package com.tcc.pjb.backend.service.document.minuta;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MinutaRiskContextAssembler {

    public record RiskContextInput(
            UUID processoId,
            RitoProcessual rito,
            boolean temSigiloAbsoluto,
            boolean partesSemRepresentacao,
            boolean temCustasDevidas,
            boolean competenciaConfirmada
    ) {}

    public record RiskContext(
            UUID processoId,
            List<String> riscos,
            List<String> alertasLegais,
            boolean bloqueioMinuta
    ) {}

    public RiskContext montar(RiskContextInput input) {
        List<String> riscos = new ArrayList<>();
        List<String> alertas = new ArrayList<>();

        if (input.temSigiloAbsoluto()) {
            riscos.add("Processo com sigilo absoluto — conteúdo da minuta protegido.");
        }
        if (input.partesSemRepresentacao()) {
            riscos.add("Parte sem representação válida — risco de nulidade.");
            alertas.add("Verificar regularidade da representação antes de decidir.");
        }
        if (input.temCustasDevidas()) {
            riscos.add("Custas processuais devidas — risco de extinção sem mérito.");
        }
        if (!input.competenciaConfirmada()) {
            riscos.add("Competência não confirmada — verificar antes de sentenciar.");
            alertas.add("Consultar matriz de competência para rito " + input.rito().name() + ".");
        }

        boolean bloqueio = input.partesSemRepresentacao() && !input.competenciaConfirmada();
        return new RiskContext(input.processoId(), riscos, alertas, bloqueio);
    }
}
