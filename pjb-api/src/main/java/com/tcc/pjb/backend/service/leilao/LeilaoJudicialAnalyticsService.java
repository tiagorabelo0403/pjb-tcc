package com.tcc.pjb.backend.service.leilao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class LeilaoJudicialAnalyticsService {

    public AvaliacaoBemResponse avaliarBem(AvaliacaoBemRequest request) {
        Objects.requireNonNull(request);
        BigDecimal baseMercado = positive(request.valorMercadoReferencia());
        BigDecimal fatorConservacao = BigDecimal.valueOf(resolveFatorConservacao(request.estadoConservacao()));
        BigDecimal fatorLiquidez = BigDecimal.valueOf(resolveFatorLiquidez(request.tipoBem()));
        BigDecimal fatorPenalidade = BigDecimal.ONE.subtract(BigDecimal.valueOf(Math.min(0.45d, Math.max(0.0d, request.grauRiscoJuridico()))));
        BigDecimal valorBaseLeilao = baseMercado.multiply(fatorConservacao).multiply(fatorLiquidez).multiply(fatorPenalidade).setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorMinimoPrimeiraPraca = valorBaseLeilao.max(BigDecimal.ZERO);
        BigDecimal valorMinimoSegundaPraca = valorBaseLeilao.multiply(new BigDecimal("0.60")).setScale(2, RoundingMode.HALF_UP);
        Set<String> fatores = new LinkedHashSet<>();
        fatores.add("CONSERVACAO_" + normalize(request.estadoConservacao()));
        fatores.add("TIPO_" + normalize(request.tipoBem()));
        if (request.grauRiscoJuridico() > 0.20d) {
            fatores.add("RISCO_JURIDICO_ELEVADO");
        }
        if (request.ocupado()) {
            fatores.add("BEM_OCUPADO");
        }
        return new AvaliacaoBemResponse(
                request.tipoBem(),
                baseMercado,
                valorBaseLeilao,
                valorMinimoPrimeiraPraca,
                valorMinimoSegundaPraca,
                List.copyOf(fatores),
                Instant.now()
        );
    }

    public AnaliseLanceResponse analisarLances(AnaliseLanceRequest request) {
        Objects.requireNonNull(request);
        List<String> alertas = new ArrayList<>();
        BigDecimal ultimoLance = request.historicoLances().stream()
                .map(LanceHistorico::valor)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        BigDecimal pisoAceitacao = positive(request.valorMinimoAceitacao());
        BigDecimal incrementoMinimo = positive(request.incrementoMinimo());
        BigDecimal valor = positive(request.valorLanceAtual());
        if (valor.compareTo(pisoAceitacao) < 0) {
            alertas.add("LANCE_ABAIXO_DO_PISO");
        }
        if (ultimoLance.signum() > 0 && valor.subtract(ultimoLance).compareTo(incrementoMinimo) < 0) {
            alertas.add("INCREMENTO_INSUFICIENTE");
        }
        long repeticoesMesmoDocumento = request.historicoLances().stream()
                .filter(l -> safe(l.documentoLicitante()).equals(safe(request.documentoLicitante())))
                .count();
        if (repeticoesMesmoDocumento >= 3) {
            alertas.add("CONCENTRACAO_POR_DOCUMENTO");
        }
        long repeticoesMesmoIp = request.historicoLances().stream()
                .filter(l -> safe(l.ipOrigem()).equals(safe(request.ipOrigem())))
                .count();
        if (repeticoesMesmoIp >= 2) {
            alertas.add("IP_RECORRENTE_EM_LANCES");
        }
        boolean aceito = alertas.isEmpty() || alertas.stream().noneMatch(alerta -> alerta.startsWith("LANCE_ABAIXO") || alerta.startsWith("INCREMENTO_"));
        int risco = Math.min(100, alertas.size() * 25 + (request.licitanteHabilitado() ? 0 : 40));
        if (!request.licitanteHabilitado()) {
            alertas.add("LICITANTE_NAO_HABILITADO");
            aceito = false;
        }
        return new AnaliseLanceResponse(
                aceito,
                risco,
                ultimoLance,
                valor,
                List.copyOf(alertas),
                aceito ? "LANCE_VALIDO_PARA_PUBLICACAO" : "LANCE_REQUER_REVISÃO_OPERACIONAL",
                Instant.now()
        );
    }

    private double resolveFatorConservacao(String estadoConservacao) {
        String normalized = normalize(estadoConservacao);
        return switch (normalized) {
            case "EXCELENTE" -> 0.98d;
            case "BOM" -> 0.90d;
            case "REGULAR" -> 0.78d;
            case "RUIM" -> 0.62d;
            default -> 0.70d;
        };
    }

    private double resolveFatorLiquidez(String tipoBem) {
        String normalized = normalize(tipoBem);
        return switch (normalized) {
            case "IMOVEL_URBANO", "VEICULO" -> 0.92d;
            case "IMOVEL_RURAL" -> 0.84d;
            case "MAQUINARIO", "EQUIPAMENTO" -> 0.76d;
            default -> 0.80d;
        };
    }

    private BigDecimal positive(BigDecimal value) {
        return value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public record AvaliacaoBemRequest(
            String tipoBem,
            BigDecimal valorMercadoReferencia,
            String estadoConservacao,
            double grauRiscoJuridico,
            boolean ocupado
    ) {
    }

    public record AvaliacaoBemResponse(
            String tipoBem,
            BigDecimal valorMercadoReferencia,
            BigDecimal valorBaseLeilao,
            BigDecimal valorMinimoPrimeiraPraca,
            BigDecimal valorMinimoSegundaPraca,
            List<String> fatoresAplicados,
            Instant avaliadoEm
    ) {
    }

    public record AnaliseLanceRequest(
            BigDecimal valorLanceAtual,
            BigDecimal valorMinimoAceitacao,
            BigDecimal incrementoMinimo,
            String documentoLicitante,
            String ipOrigem,
            boolean licitanteHabilitado,
            List<LanceHistorico> historicoLances
    ) {
        public AnaliseLanceRequest {
            historicoLances = historicoLances == null ? List.of() : List.copyOf(historicoLances);
        }
    }

    public record LanceHistorico(
            BigDecimal valor,
            String documentoLicitante,
            String ipOrigem,
            Instant criadoEm
    ) {
    }

    public record AnaliseLanceResponse(
            boolean aceito,
            int riscoOperacional,
            BigDecimal ultimoLanceValido,
            BigDecimal valorLanceAtual,
            List<String> alertas,
            String decisao,
            Instant analisadoEm
    ) {
    }
}
