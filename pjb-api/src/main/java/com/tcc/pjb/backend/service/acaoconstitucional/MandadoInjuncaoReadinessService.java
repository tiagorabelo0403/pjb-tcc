package com.tcc.pjb.backend.service.acaoconstitucional;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MandadoInjuncaoReadinessService {

    public enum EfeitoMandadoInjuncao {
        INTER_PARTES,
        ERGA_OMNES,
        ULTRA_PARTES
    }

    public record MandadoInjuncaoInput(
            String processoId,
            boolean normaConstitucionalDependeDeRegulamentacao,
            boolean omissaoNormativaConfigurada,
            boolean interesseColetivo,
            boolean faltaRegulamentacaoImpedeDireito
    ) {}

    public record MandadoInjuncaoResult(
            boolean cabivel,
            EfeitoMandadoInjuncao efeitoSugerido,
            List<String> requisitosAtendidos,
            List<String> impeditivos
    ) {}

    public MandadoInjuncaoResult avaliar(MandadoInjuncaoInput input) {
        List<String> atendidos = new ArrayList<>();
        List<String> impeditivos = new ArrayList<>();

        if (!input.normaConstitucionalDependeDeRegulamentacao()) {
            impeditivos.add("MI pressupõe norma constitucional que depende de regulamentação (CF art. 5º LXXI).");
        } else {
            atendidos.add("Norma constitucional carece de regulamentação para ter eficácia plena.");
        }
        if (!input.omissaoNormativaConfigurada()) {
            impeditivos.add("Omissão normativa não configurada — necessário demonstrar inércia do legislador.");
        } else {
            atendidos.add("Omissão normativa inconstitucional configurada.");
        }
        if (!input.faltaRegulamentacaoImpedeDireito()) {
            impeditivos.add("Ausência de nexo causal: falta de regulamentação não impede o exercício do direito.");
        } else {
            atendidos.add("Falta de regulamentação impede o exercício do direito constitucional.");
        }

        EfeitoMandadoInjuncao efeito = input.interesseColetivo()
                ? EfeitoMandadoInjuncao.ERGA_OMNES
                : EfeitoMandadoInjuncao.INTER_PARTES;

        return new MandadoInjuncaoResult(impeditivos.isEmpty(), efeito, atendidos, impeditivos);
    }
}
