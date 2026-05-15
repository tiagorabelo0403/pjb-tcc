package com.tcc.pjb.backend.service.acaoconstitucional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MandadoSegurancaReadinessService {

    private static final int PRAZO_DECADENCIAL_DIAS = 120;

    public record MandadoSegurancaInput(
            String processoId,
            LocalDate dataCienciaAto,
            boolean direitoLiquidoCerto,
            boolean provaPreConstituida,
            boolean autoridadeCoatoraPublica,
            boolean atolLeiEmTese,
            boolean atoJudicialComRecursoComEfeitoSuspensivo,
            boolean atoPraticoDeParticularSemPoderDelegado
    ) {}

    public record MandadoSegurancaResult(
            boolean prazoDecadencialVigente,
            int diasRestantes,
            List<String> requisitosVerificados,
            List<String> pendenciasIdentificadas,
            String sinalizacao
    ) {}

    private static final String SINAL_SEM_PENDENCIAS =
            "Sem pendências formais localizadas — checklist sujeito à validação judicial. Não substitui análise do magistrado.";
    private static final String SINAL_COM_PENDENCIAS =
            "Pendências identificadas — conferir antes de submeter ao magistrado.";

    public MandadoSegurancaResult avaliar(MandadoSegurancaInput input) {
        List<String> verificados = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();

        if (input.atolLeiEmTese()) {
            pendencias.add("Pendência identificada: ato é lei em tese — MS possivelmente incabível (Súmula 266 STF).");
        }
        if (input.atoJudicialComRecursoComEfeitoSuspensivo()) {
            pendencias.add("Pendência identificada: ato judicial com recurso de efeito suspensivo — MS possivelmente incabível (Súmula 267 STF).");
        }
        if (input.atoPraticoDeParticularSemPoderDelegado()) {
            pendencias.add("Pendência identificada: ato de particular sem delegação pública — conferir cabimento (Lei 12.016/2009 art. 1º).");
        }
        if (!input.autoridadeCoatoraPublica()) {
            pendencias.add("Possível requisito a conferir: autoridade coatora deve ser agente público (Lei 12.016/2009 art. 1º §1º).");
        }
        if (!input.direitoLiquidoCerto()) {
            pendencias.add("Possível requisito a conferir: direito líquido e certo não demonstrado — dilação probatória incompatível com MS.");
        }
        if (!input.provaPreConstituida()) {
            pendencias.add("Possível requisito a conferir: prova pré-constituída não apresentada (Lei 12.016/2009 art. 6º).");
        }

        if (input.direitoLiquidoCerto()) verificados.add("Direito líquido e certo — conferido.");
        if (input.provaPreConstituida()) verificados.add("Prova pré-constituída — conferida.");
        if (input.autoridadeCoatoraPublica()) verificados.add("Autoridade coatora pública — conferida.");

        long diasDecorridos = input.dataCienciaAto() != null
                ? ChronoUnit.DAYS.between(input.dataCienciaAto(), LocalDate.now()) : 0;
        boolean prazoVigente = diasDecorridos <= PRAZO_DECADENCIAL_DIAS;
        int diasRestantes = (int) Math.max(0, PRAZO_DECADENCIAL_DIAS - diasDecorridos);

        if (!prazoVigente) {
            pendencias.add(String.format(
                    "Pendência identificada: prazo decadencial de 120 dias possivelmente esgotado — %d dias desde a ciência (Lei 12.016/2009 art. 23).",
                    diasDecorridos));
        }

        return new MandadoSegurancaResult(prazoVigente, diasRestantes,
                List.copyOf(verificados), List.copyOf(pendencias),
                pendencias.isEmpty() ? SINAL_SEM_PENDENCIAS : SINAL_COM_PENDENCIAS);
    }
}
