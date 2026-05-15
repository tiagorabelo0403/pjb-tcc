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
            EfeitoMandadoInjuncao efeitoSugerido,
            List<String> requisitosVerificados,
            List<String> pendenciasIdentificadas,
            String sinalizacao
    ) {}

    private static final String SINAL_SEM_PENDENCIAS =
            "Sem pendências formais localizadas — checklist sujeito à validação judicial. Não substitui análise do magistrado.";
    private static final String SINAL_COM_PENDENCIAS =
            "Pendências identificadas — conferir antes de submeter ao magistrado.";

    public MandadoInjuncaoResult avaliar(MandadoInjuncaoInput input) {
        List<String> verificados = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();

        if (!input.normaConstitucionalDependeDeRegulamentacao()) {
            pendencias.add("Possível requisito a conferir: norma constitucional dependente de regulamentação não identificada (CF art. 5º LXXI).");
        } else {
            verificados.add("Norma constitucional que depende de regulamentação — conferida.");
        }
        if (!input.omissaoNormativaConfigurada()) {
            pendencias.add("Possível requisito a conferir: omissão normativa não demonstrada — inércia do legislador a verificar.");
        } else {
            verificados.add("Omissão normativa inconstitucional — conferida.");
        }
        if (!input.faltaRegulamentacaoImpedeDireito()) {
            pendencias.add("Possível requisito a conferir: nexo causal entre omissão e impedimento ao direito não demonstrado.");
        } else {
            verificados.add("Falta de regulamentação impede o exercício do direito constitucional — conferido.");
        }

        EfeitoMandadoInjuncao efeito = input.interesseColetivo()
                ? EfeitoMandadoInjuncao.ERGA_OMNES
                : EfeitoMandadoInjuncao.INTER_PARTES;

        return new MandadoInjuncaoResult(efeito, List.copyOf(verificados), List.copyOf(pendencias),
                pendencias.isEmpty() ? SINAL_SEM_PENDENCIAS : SINAL_COM_PENDENCIAS);
    }
}
