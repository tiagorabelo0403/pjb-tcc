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
            boolean cabivel,
            List<String> requisitosAtendidos,
            List<String> impeditivos,
            List<String> documentosNecessarios
    ) {}

    public RecuperacaoJudicialResult avaliar(RecuperacaoJudicialInput input) {
        List<String> atendidos = new ArrayList<>();
        List<String> impeditivos = new ArrayList<>();

        if (!input.empresarioOuSociedadeEmpresaria()) {
            impeditivos.add("Recuperação judicial restrita a empresário ou sociedade empresária (Lei 11.101/2005 art. 1º).");
        } else {
            atendidos.add("Sujeito legitimado: empresário ou sociedade empresária.");
        }

        long anosAtividade = input.dataInicioAtividade() != null
                ? ChronoUnit.YEARS.between(input.dataInicioAtividade(), LocalDate.now()) : 0;
        if (anosAtividade < ANOS_ATIVIDADE_MINIMA) {
            impeditivos.add(String.format(
                    "Atividade inferior a 2 anos: %d ano(s) — não atende art. 48 I da Lei 11.101/2005.", anosAtividade));
        } else {
            atendidos.add(String.format("Atividade regular há %d anos (mínimo: 2 anos).", anosAtividade));
        }

        if (input.falenciaAnterior() && input.dataFalenciaAnterior() != null) {
            long anosPosFalencia = ChronoUnit.YEARS.between(input.dataFalenciaAnterior(), LocalDate.now());
            if (anosPosFalencia < ANOS_CARENCIA_POS_FALENCIA) {
                impeditivos.add(String.format(
                        "Falência anterior há menos de 5 anos: %d ano(s) — art. 48 II Lei 11.101/2005.", anosPosFalencia));
            } else {
                atendidos.add("Falência anterior já superou carência de 5 anos.");
            }
        }

        if (input.concordataConcedidaHaMenosDe5Anos()) {
            impeditivos.add("Concordata concedida há menos de 5 anos — impedimento do art. 48 III Lei 11.101/2005.");
        }
        if (input.recuperacaoJudicialAnteriorHaMenosDe5Anos()) {
            impeditivos.add("Recuperação judicial anterior concedida há menos de 5 anos — art. 48 IV Lei 11.101/2005.");
        }
        if (input.passivoADescoberto() != null && input.passivoADescoberto().compareTo(BigDecimal.ZERO) > 0) {
            atendidos.add("Passivo a descoberto demonstrado.");
        } else {
            impeditivos.add("Passivo a descoberto não demonstrado — necessário comprovar crise econômico-financeira.");
        }

        List<String> documentos = List.of(
                "Exposição das causas concretas da crise (art. 51 I)",
                "Demonstrações contábeis últimos 3 exercícios (art. 51 II)",
                "Relação dos credores com classificação (art. 51 III)",
                "Relação dos empregados com salários (art. 51 IV)",
                "Certidão do CNPJ e atos constitutivos (art. 51 V)",
                "Relação dos bens e direitos do ativo (art. 51 VI)",
                "Extratos bancários últimos 90 dias (art. 51 VII)");

        return new RecuperacaoJudicialResult(impeditivos.isEmpty(), atendidos, impeditivos, documentos);
    }
}
