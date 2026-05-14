package com.tcc.pjb.backend.service.processual.extincao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ExtincaoSemMeritoDetectorService {

    public enum HipoteseExtincao {
        AUSENCIA_PRESSUPOSTO_PROCESSUAL,
        ILEGITIMIDADE_PARTES,
        FALTA_INTERESSE_AGIR,
        PERDA_OBJETO,
        DESISTENCIA,
        RECONHECIMENTO_JURIDICO,
        TRANSACAO,
        ABANDONO_CAUSA_BILATERAL,
        LITISPENDENCIA,
        COISA_JULGADA
    }

    public record ExtincaoInput(
            UUID processoId,
            boolean pressupostosProcessuaisPresentes,
            boolean partesLegitimas,
            boolean interesseDeAgir,
            boolean objetoSubsistente,
            boolean desistenciaFormulada,
            boolean reconhecimentoJuridico,
            boolean transacaoHomologada,
            boolean abandonoCausaBilateral,
            boolean litispendenciaIdentificada,
            boolean coisaJulgadaIdentificada
    ) {}

    public record ExtincaoSnapshot(
            UUID processoId,
            boolean aptaParaExtincaoSemMerito,
            List<HipoteseExtincao> hipoteses,
            String recomendacao
    ) {}

    public ExtincaoSnapshot detectar(ExtincaoInput input) {
        List<HipoteseExtincao> hipoteses = new ArrayList<>();
        if (!input.pressupostosProcessuaisPresentes()) {
            hipoteses.add(HipoteseExtincao.AUSENCIA_PRESSUPOSTO_PROCESSUAL);
        }
        if (!input.partesLegitimas()) {
            hipoteses.add(HipoteseExtincao.ILEGITIMIDADE_PARTES);
        }
        if (!input.interesseDeAgir()) {
            hipoteses.add(HipoteseExtincao.FALTA_INTERESSE_AGIR);
        }
        if (!input.objetoSubsistente()) {
            hipoteses.add(HipoteseExtincao.PERDA_OBJETO);
        }
        if (input.desistenciaFormulada()) {
            hipoteses.add(HipoteseExtincao.DESISTENCIA);
        }
        if (input.reconhecimentoJuridico()) {
            hipoteses.add(HipoteseExtincao.RECONHECIMENTO_JURIDICO);
        }
        if (input.transacaoHomologada()) {
            hipoteses.add(HipoteseExtincao.TRANSACAO);
        }
        if (input.abandonoCausaBilateral()) {
            hipoteses.add(HipoteseExtincao.ABANDONO_CAUSA_BILATERAL);
        }
        if (input.litispendenciaIdentificada()) {
            hipoteses.add(HipoteseExtincao.LITISPENDENCIA);
        }
        if (input.coisaJulgadaIdentificada()) {
            hipoteses.add(HipoteseExtincao.COISA_JULGADA);
        }
        boolean apta = !hipoteses.isEmpty();
        String recomendacao = apta
                ? hipoteses.size() + " hipótese(s) de extinção sem mérito detectada(s) — apreciar."
                : "Nenhuma hipótese de extinção sem mérito detectada — prosseguir normalmente.";
        return new ExtincaoSnapshot(input.processoId(), apta,
                List.copyOf(hipoteses), recomendacao);
    }
}
