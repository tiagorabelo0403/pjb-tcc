package com.tcc.pjb.backend.service.certidao;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CertidaoBatchEmissionService {

    public record CertidaoBatchItem(
            UUID processoId,
            String tipoCertidao,
            boolean aptoParaEmissao
    ) {}

    public record BatchResultado(
            int total,
            int emitidas,
            int bloqueadas,
            List<String> numerosEmitidos,
            String mensagem
    ) {}

    private final CertidaoAutoPreparationService preparationService;

    public CertidaoBatchEmissionService(CertidaoAutoPreparationService preparationService) {
        this.preparationService = preparationService;
    }

    public BatchResultado emitirLote(List<CertidaoBatchItem> itens) {
        List<CertidaoAutoPreparationService.CertidaoPreparada> preparadas = itens.stream()
                .map(item -> preparationService.preparar(
                        new CertidaoAutoPreparationService.PreparacaoInput(
                                item.processoId(), item.tipoCertidao(),
                                item.aptoParaEmissao(), List.of())))
                .toList();

        long emitidas = preparadas.stream().filter(CertidaoAutoPreparationService.CertidaoPreparada::pronta).count();
        long bloqueadas = preparadas.size() - emitidas;

        List<String> numeros = preparadas.stream()
                .filter(CertidaoAutoPreparationService.CertidaoPreparada::pronta)
                .map(p -> p.processoId().toString())
                .toList();

        String mensagem = emitidas + " certidão(ões) preparada(s), " + bloqueadas + " bloqueada(s).";
        return new BatchResultado(itens.size(), (int) emitidas, (int) bloqueadas, numeros, mensagem);
    }
}
