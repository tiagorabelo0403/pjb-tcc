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
            boolean cabivel,
            boolean dentroDosPrazo,
            int diasRestantes,
            List<String> requisitosAtendidos,
            List<String> impeditivos
    ) {}

    public MandadoSegurancaResult avaliar(MandadoSegurancaInput input) {
        List<String> atendidos = new ArrayList<>();
        List<String> impeditivos = new ArrayList<>();

        if (input.atolLeiEmTese()) {
            impeditivos.add("MS não cabe contra lei em tese — ausência de lesividade concreta (Súmula 266 STF).");
        }
        if (input.atoJudicialComRecursoComEfeitoSuspensivo()) {
            impeditivos.add("MS não cabe contra ato judicial que admite recurso com efeito suspensivo (Súmula 267 STF).");
        }
        if (input.atoPraticoDeParticularSemPoderDelegado()) {
            impeditivos.add("MS não cabe contra ato de particular sem delegação de poder público (Lei 12.016/2009 art. 1º).");
        }
        if (!input.autoridadeCoatoraPublica()) {
            impeditivos.add("Autoridade coatora deve ser agente público (Lei 12.016/2009 art. 1º §1º).");
        }
        if (!input.direitoLiquidoCerto()) {
            impeditivos.add("Ausência de direito líquido e certo — demanda dilação probatória incompatível com MS.");
        }
        if (!input.provaPreConstituida()) {
            impeditivos.add("MS exige prova pré-constituída — documentos devem acompanhar a petição inicial (Lei 12.016/2009 art. 6º).");
        }

        if (input.direitoLiquidoCerto()) atendidos.add("Direito líquido e certo evidenciado.");
        if (input.provaPreConstituida()) atendidos.add("Prova pré-constituída disponível.");
        if (input.autoridadeCoatoraPublica()) atendidos.add("Autoridade coatora é agente público.");

        long diasDecorridos = input.dataCienciaAto() != null
                ? ChronoUnit.DAYS.between(input.dataCienciaAto(), LocalDate.now()) : 0;
        boolean dentroDoPrazo = diasDecorridos <= PRAZO_DECADENCIAL_DIAS;
        int diasRestantes = (int) Math.max(0, PRAZO_DECADENCIAL_DIAS - diasDecorridos);

        if (!dentroDoPrazo) {
            impeditivos.add(String.format(
                    "Prazo decadencial de 120 dias esgotado: %d dias desde a ciência do ato (Lei 12.016/2009 art. 23).",
                    diasDecorridos));
        }

        return new MandadoSegurancaResult(
                impeditivos.isEmpty(), dentroDoPrazo, diasRestantes, atendidos, impeditivos);
    }
}
