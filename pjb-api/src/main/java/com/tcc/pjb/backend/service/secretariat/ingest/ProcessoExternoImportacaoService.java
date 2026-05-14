package com.tcc.pjb.backend.service.secretariat.ingest;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProcessoExternoImportacaoService {

    public record ImportacaoRequest(
            String sistemaDeclarado,
            String formatoEnvelope,
            String numeroOrigem,
            String classeOriginal,
            java.util.Map<String, String> camposRaw
    ) {}

    public record ImportacaoResponse(
            UUID processoId,
            ProcessoExternoCargaStatus status,
            SistemaProcessualOrigem origem,
            ProcessoExternalNormalizationService.ProcessoExternoSnapshot snapshot,
            String mensagem
    ) {}

    private final ProcessoExternoOrigemResolver origemResolver;
    private final ProcessoExternalNormalizationService normalizationService;

    public ProcessoExternoImportacaoService(
            ProcessoExternoOrigemResolver origemResolver,
            ProcessoExternalNormalizationService normalizationService) {
        this.origemResolver = origemResolver;
        this.normalizationService = normalizationService;
    }

    public ImportacaoResponse importar(ImportacaoRequest request) {
        SistemaProcessualOrigem origem = origemResolver.resolver(
                request.sistemaDeclarado(), request.formatoEnvelope());

        ProcessoExternalNormalizationService.NormalizacaoInput normInput =
                new ProcessoExternalNormalizationService.NormalizacaoInput(
                        request.numeroOrigem(), origem, request.classeOriginal(), request.camposRaw());

        ProcessoExternalNormalizationService.ProcessoExternoSnapshot snapshot =
                normalizationService.normalizar(normInput);

        ProcessoExternoCargaStatus status = snapshot.normalizacaoConcluida()
                ? ProcessoExternoCargaStatus.IMPORTADO
                : ProcessoExternoCargaStatus.COM_DIVERGENCIA;

        String mensagem = status == ProcessoExternoCargaStatus.IMPORTADO
                ? "Processo importado de " + origem.descricao() + " — conferência recomendada."
                : "Importação com divergência — verificar campos normalizados.";

        return new ImportacaoResponse(snapshot.processoId(), status, origem, snapshot, mensagem);
    }
}
