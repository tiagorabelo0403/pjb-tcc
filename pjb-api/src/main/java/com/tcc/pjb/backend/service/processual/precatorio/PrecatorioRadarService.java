package com.tcc.pjb.backend.service.processual.precatorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PrecatorioRadarService {

    private static final BigDecimal LIMITE_RPV_FEDERAL_2026 = new BigDecimal("66").multiply(
            new BigDecimal("1518"));
    private static final BigDecimal LIMITE_RPV_ESTADUAL_2026 = new BigDecimal("40").multiply(
            new BigDecimal("1518"));

    public enum TipoObrigacaoFazenda {
        FEDERAL, ESTADUAL, MUNICIPAL
    }

    public record PrecatorioInput(
            UUID processoId,
            BigDecimal valorCondenacao,
            TipoObrigacaoFazenda tipoFazenda,
            boolean transitadoEmJulgado,
            boolean naturezaAlimentar,
            LocalDate dataTransitoEmJulgado
    ) {}

    public record PrecatorioSnapshot(
            UUID processoId,
            boolean requerPrecatorio,
            boolean aptaRpv,
            BigDecimal valorLimiteRpv,
            List<String> providencias,
            String tipoExpedicao,
            String fundamentacao
    ) {}

    public PrecatorioSnapshot avaliar(PrecatorioInput input) {
        if (!input.transitadoEmJulgado()) {
            return new PrecatorioSnapshot(input.processoId(), false, false,
                    BigDecimal.ZERO, List.of("Aguardar trânsito em julgado."),
                    "AGUARDANDO_TRANSITO", "CPC, art. 534.");
        }
        BigDecimal limiteRpv = input.tipoFazenda() == TipoObrigacaoFazenda.FEDERAL
                ? LIMITE_RPV_FEDERAL_2026 : LIMITE_RPV_ESTADUAL_2026;
        boolean aptaRpv = input.valorCondenacao().compareTo(limiteRpv) <= 0
                || input.naturezaAlimentar();
        List<String> providencias = new ArrayList<>();
        if (aptaRpv) {
            providencias.add("Expedir Requisição de Pequeno Valor (RPV).");
            providencias.add("Intimar fazenda pública para pagamento em 60 dias.");
        } else {
            providencias.add("Expedir precatório ao TJ/TRF competente.");
            providencias.add("Observar orçamento do ente devedor para inclusão.");
        }
        return new PrecatorioSnapshot(input.processoId(), !aptaRpv, aptaRpv,
                limiteRpv, List.copyOf(providencias),
                aptaRpv ? "RPV" : "PRECATORIO",
                "CF/88, art. 100; CPC, arts. 534-535.");
    }
}
