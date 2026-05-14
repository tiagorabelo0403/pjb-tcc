package com.tcc.pjb.backend.service.certidao;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CertidaoAutoPreparationService {

    public record PreparacaoInput(
            UUID processoId,
            String tipoCertidao,
            boolean aptoParaEmissao,
            List<String> dadosParaCertidao
    ) {}

    public record CertidaoPreparada(
            UUID processoId,
            String tipoCertidao,
            boolean pronta,
            String conteudoMinuta,
            String aviso
    ) {}

    private final CertidaoPendenciaDetector pendenciaDetector;

    public CertidaoAutoPreparationService(CertidaoPendenciaDetector pendenciaDetector) {
        this.pendenciaDetector = pendenciaDetector;
    }

    public CertidaoPreparada preparar(PreparacaoInput input) {
        if (!input.aptoParaEmissao()) {
            return new CertidaoPreparada(input.processoId(), input.tipoCertidao(), false,
                    null, "Processo não apto para certidão — verificar pendências.");
        }
        String minuta = "CERTIDÃO — Tipo: " + input.tipoCertidao()
                + " | Processo: " + input.processoId()
                + " | Dados: " + String.join(", ", input.dadosParaCertidao());
        return new CertidaoPreparada(input.processoId(), input.tipoCertidao(), true, minuta,
                "Certidão preparada — revisar e assinar antes de emitir.");
    }
}
