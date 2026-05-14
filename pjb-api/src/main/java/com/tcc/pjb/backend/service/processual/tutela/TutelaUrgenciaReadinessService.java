package com.tcc.pjb.backend.service.processual.tutela;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TutelaUrgenciaReadinessService {

    public enum TipoTutela {
        ANTECIPADA_REQUERIDA, CAUTELAR, ANTECIPADA_ANTECEDENTE, CAUTELAR_ANTECEDENTE,
        TUTELA_EVIDENCIA
    }

    public record TutelaInput(
            UUID processoId,
            TipoTutela tipo,
            boolean probabilidadeDireito,
            boolean perigoNaDemora,
            boolean reversibilidade,
            boolean contraditorioPrevio,
            boolean pedidoFormulado
    ) {}

    public record TutelaReadiness(
            UUID processoId,
            TipoTutela tipo,
            boolean aptaParaApreciacao,
            List<String> requisitosPresentes,
            List<String> requisitosAusentes,
            String orientacaoServidor
    ) {}

    public TutelaReadiness avaliar(TutelaInput input) {
        List<String> presentes = new ArrayList<>();
        List<String> ausentes = new ArrayList<>();
        if (input.pedidoFormulado()) {
            presentes.add("Pedido expressamente formulado.");
        } else {
            ausentes.add("Pedido de tutela não formulado expressamente.");
        }
        if (input.probabilidadeDireito()) {
            presentes.add("Probabilidade do direito indicada.");
        } else {
            ausentes.add("Probabilidade do direito não demonstrada.");
        }
        if (input.perigoNaDemora()) {
            presentes.add("Perigo de dano ou risco ao resultado útil indicado.");
        } else if (input.tipo() != TipoTutela.TUTELA_EVIDENCIA) {
            ausentes.add("Perigo na demora não demonstrado (CPC, art. 300).");
        }
        if (!input.reversibilidade()) {
            presentes.add("ALERTA: medida pode ser irreversível — verificar (CPC, art. 300, §3º).");
        }
        boolean apta = ausentes.isEmpty();
        return new TutelaReadiness(input.processoId(), input.tipo(), apta,
                List.copyOf(presentes), List.copyOf(ausentes),
                apta ? "Tutela aparentemente apta para apreciação pelo magistrado."
                        : "Verificar " + ausentes.size() + " requisito(s) antes de encaminhar.");
    }
}
