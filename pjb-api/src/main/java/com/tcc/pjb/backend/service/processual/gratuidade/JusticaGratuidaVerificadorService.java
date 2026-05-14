package com.tcc.pjb.backend.service.processual.gratuidade;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class JusticaGratuidaVerificadorService {

    public record GratuidadeInput(
            UUID processoId,
            String parteId,
            boolean declaracaoHipossuficiencia,
            BigDecimal rendaMensalDeclarada,
            boolean representadoPorDefensoria,
            boolean beneficioSocial,
            boolean impugnadaPelaParteContraria
    ) {}

    public record GratuidadeSnapshot(
            UUID processoId,
            String parteId,
            boolean presumivelmenteApta,
            List<String> alertas,
            String orientacaoServidor
    ) {}

    private static final BigDecimal TETO_PRESUNCAO_SM = new BigDecimal("5");
    private static final BigDecimal SALARIO_MINIMO_2026 = new BigDecimal("1518");

    public GratuidadeSnapshot avaliar(GratuidadeInput input) {
        List<String> alertas = new ArrayList<>();
        boolean apta = false;
        if (input.representadoPorDefensoria() || input.beneficioSocial()) {
            apta = true;
        } else if (input.declaracaoHipossuficiencia()) {
            BigDecimal teto = SALARIO_MINIMO_2026.multiply(TETO_PRESUNCAO_SM);
            if (input.rendaMensalDeclarada() != null
                    && input.rendaMensalDeclarada().compareTo(teto) <= 0) {
                apta = true;
            } else {
                alertas.add("Renda declarada acima de 5 salários mínimos — gratuidade não presumida.");
            }
        }
        if (input.impugnadaPelaParteContraria()) {
            alertas.add("Gratuidade impugnada — aguardar deliberação judicial (CPC, art. 100).");
        }
        String orientacao = apta && !input.impugnadaPelaParteContraria()
                ? "Gratuidade presumida. Isentar de custas e proceder normalmente."
                : "Aguardar pronunciamento judicial sobre a gratuidade antes de cobrar custas.";
        return new GratuidadeSnapshot(input.processoId(), input.parteId(),
                apta, List.copyOf(alertas), orientacao);
    }
}
