package com.tcc.pjb.backend.service.recuperacaojudicial;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RecuperacaoJudicialReadinessService {

    private static final int ANOS_ATIVIDADE_MINIMA = 2;
    private static final int ANOS_CARENCIA_POS_FALENCIA = 5;

    public record RecuperacaoJudicialInput(
            String cnpj,
            LocalDate dataInicioAtividade,
            boolean falenciaAnterior,
            LocalDate dataFalenciaAnterior,
            boolean concordataConcedidaHaMenosDe5Anos,
            boolean recuperacaoJudicialAnteriorHaMenosDe5Anos,
            BigDecimal passivoADescoberto,
            boolean empresarioOuSociedadeEmpresaria
    ) {}

    public record RecuperacaoJudicialResult(
            List<String> requisitosVerificados,
            List<String> pendenciasIdentificadas,
            List<String> documentosNecessarios,
            String sinalizacao
    ) {}

    private static final String SINAL_SEM_PENDENCIAS =
            "Sem pendências formais localizadas — checklist sujeito à validação judicial. Não substitui análise do magistrado.";
    private static final String SINAL_COM_PENDENCIAS =
            "Pendências identificadas — conferir antes de submeter ao magistrado.";

    public RecuperacaoJudicialResult avaliar(RecuperacaoJudicialInput input) {
        List<String> verificados = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();

        if (!input.empresarioOuSociedadeEmpresaria()) {
            pendencias.add("Possível requisito a conferir: legitimidade restrita a empresário ou sociedade empresária (Lei 11.101/2005 art. 1º).");
        } else {
            verificados.add("Sujeito legitimado: empresário ou sociedade empresária — conferido.");
        }

        long anosAtividade = input.dataInicioAtividade() != null
                ? ChronoUnit.YEARS.between(input.dataInicioAtividade(), LocalDate.now()) : 0;
        if (anosAtividade < ANOS_ATIVIDADE_MINIMA) {
            pendencias.add(String.format(
                    "Pendência identificada: atividade inferior a 2 anos — %d ano(s) (art. 48 I Lei 11.101/2005).", anosAtividade));
        } else {
            verificados.add(String.format("Atividade regular há %d anos (mínimo: 2 anos) — conferido.", anosAtividade));
        }

        if (input.falenciaAnterior() && input.dataFalenciaAnterior() != null) {
            long anosPosFalencia = ChronoUnit.YEARS.between(input.dataFalenciaAnterior(), LocalDate.now());
            if (anosPosFalencia < ANOS_CARENCIA_POS_FALENCIA) {
                pendencias.add(String.format(
                        "Pendência identificada: falência anterior há menos de 5 anos — %d ano(s) (art. 48 II Lei 11.101/2005).", anosPosFalencia));
            } else {
                verificados.add("Carência pós-falência de 5 anos cumprida — conferido.");
            }
        }

        if (input.concordataConcedidaHaMenosDe5Anos()) {
            pendencias.add("Pendência identificada: concordata concedida há menos de 5 anos (art. 48 III Lei 11.101/2005).");
        }
        if (input.recuperacaoJudicialAnteriorHaMenosDe5Anos()) {
            pendencias.add("Pendência identificada: recuperação judicial anterior concedida há menos de 5 anos (art. 48 IV Lei 11.101/2005).");
        }
        if (input.passivoADescoberto() != null && input.passivoADescoberto().compareTo(BigDecimal.ZERO) > 0) {
            verificados.add("Passivo a descoberto demonstrado — conferido.");
        } else {
            pendencias.add("Possível requisito a conferir: passivo a descoberto não demonstrado — necessário comprovar crise econômico-financeira.");
        }

        List<String> documentos = List.of(
                "Exposição das causas concretas da crise (art. 51 I)",
                "Demonstrações contábeis últimos 3 exercícios (art. 51 II)",
                "Relação dos credores com classificação (art. 51 III)",
                "Relação dos empregados com salários (art. 51 IV)",
                "Certidão do CNPJ e atos constitutivos (art. 51 V)",
                "Relação dos bens e direitos do ativo (art. 51 VI)",
                "Extratos bancários últimos 90 dias (art. 51 VII)");

        return new RecuperacaoJudicialResult(List.copyOf(verificados), List.copyOf(pendencias), documentos,
                pendencias.isEmpty() ? SINAL_SEM_PENDENCIAS : SINAL_COM_PENDENCIAS);
    }
}
