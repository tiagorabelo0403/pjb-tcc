package com.tcc.pjb.backend.service.processual.precatorio;

import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PrecatorioRadarService {

    private static final BigDecimal SALARIOS_MINIMOS_RPV_FEDERAL = new BigDecimal("60");
    private static final BigDecimal SALARIOS_MINIMOS_RPV_SUBNACIONAL = new BigDecimal("40");

    private final SalarioMinimoNacionalService salarioMinimoService;

    public PrecatorioRadarService(SalarioMinimoNacionalService salarioMinimoService) {
        this.salarioMinimoService = Objects.requireNonNull(salarioMinimoService);
    }

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
        BigDecimal limiteRpv = limiteRpv(input.tipoFazenda(), input.dataTransitoEmJulgado());
        boolean aptaRpv = input.valorCondenacao().compareTo(limiteRpv) <= 0;

        List<String> providencias = new ArrayList<>();
        if (aptaRpv) {
            providencias.add("Expedir Requisição de Pequeno Valor (RPV).");
            providencias.add("Intimar fazenda pública para pagamento em 60 dias.");
        } else {
            providencias.add("Expedir precatório ao TJ/TRF competente.");
            providencias.add("Observar orçamento do ente devedor para inclusão.");
        }
        if (input.naturezaAlimentar() && !aptaRpv) {
            providencias.add("Registrar natureza alimentar para preferência na ordem de pagamento (CF, art. 100, § 1º).");
        }

        return new PrecatorioSnapshot(input.processoId(), !aptaRpv, aptaRpv,
                limiteRpv, List.copyOf(providencias),
                aptaRpv ? "RPV" : "PRECATORIO",
                "CF/88, art. 100; CPC, arts. 534-535.");
    }

    private BigDecimal limiteRpv(TipoObrigacaoFazenda tipoFazenda, LocalDate dataReferencia) {
        BigDecimal salariosMinimos = tipoFazenda == TipoObrigacaoFazenda.FEDERAL
                ? SALARIOS_MINIMOS_RPV_FEDERAL
                : SALARIOS_MINIMOS_RPV_SUBNACIONAL;
        LocalDate referencia = dataReferencia != null ? dataReferencia : LocalDate.now();
        return salarioMinimoService.multiplicar(salariosMinimos, referencia);
    }
}
