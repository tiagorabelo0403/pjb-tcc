package com.tcc.pjb.backend.service.processual.acceleration.fazenda;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class FazendaPublicaRemessaNecessariaRadarService {

    private static final BigDecimal LIMITE_UNIAO = new BigDecimal("1000000.00");
    private static final BigDecimal LIMITE_ESTADOS = new BigDecimal("500000.00");
    private static final BigDecimal LIMITE_MUNICIPIOS = new BigDecimal("100000.00");

    public record RemessaInput(
            UUID processoId,
            String numeroProcesso,
            BigDecimal valorCondenacao,
            String enteFazendario,
            boolean sentencaContraMunicipioPequeno
    ) {}

    public record RemessaResultado(
            UUID processoId,
            boolean exigeRemessa,
            String fundamento,
            String mensagem
    ) {}

    public RemessaResultado avaliar(RemessaInput input) {
        BigDecimal limite = resolverLimite(input);
        boolean exige = input.valorCondenacao().compareTo(limite) > 0;
        String fundamento = "CPC art. 496, §3º — limite para " + input.enteFazendario();
        String mensagem = exige
                ? "Remessa necessária obrigatória — valor acima do limite de R$" + limite + "."
                : "Remessa necessária dispensada — valor abaixo do limite (CPC art. 496, §3º).";
        return new RemessaResultado(input.processoId(), exige, fundamento, mensagem);
    }

    public List<RemessaResultado> radar(List<RemessaInput> processos) {
        return processos.stream().map(this::avaliar).filter(RemessaResultado::exigeRemessa).toList();
    }

    private BigDecimal resolverLimite(RemessaInput input) {
        if (input.enteFazendario().contains("UNIAO") || input.enteFazendario().contains("FEDERAL"))
            return LIMITE_UNIAO;
        if (input.sentencaContraMunicipioPequeno()) return LIMITE_MUNICIPIOS;
        return LIMITE_ESTADOS;
    }
}
