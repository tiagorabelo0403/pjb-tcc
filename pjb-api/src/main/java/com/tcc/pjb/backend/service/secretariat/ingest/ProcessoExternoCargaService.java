package com.tcc.pjb.backend.service.secretariat.ingest;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProcessoExternoCargaService {

    public record CargaItem(
            String sistemaDeclarado,
            String formatoEnvelope,
            String numeroOrigem,
            String classeOriginal,
            java.util.Map<String, String> camposRaw
    ) {}

    public record CargaResultado(
            int total,
            int importados,
            int comDivergencia,
            int rejeitados,
            List<ProcessoExternoImportacaoService.ImportacaoResponse> detalhes
    ) {}

    private final ProcessoExternoImportacaoService importacaoService;

    public ProcessoExternoCargaService(ProcessoExternoImportacaoService importacaoService) {
        this.importacaoService = importacaoService;
    }

    public CargaResultado processarLote(List<CargaItem> itens) {
        List<ProcessoExternoImportacaoService.ImportacaoResponse> respostas = itens.stream()
                .map(item -> importacaoService.importar(
                        new ProcessoExternoImportacaoService.ImportacaoRequest(
                                item.sistemaDeclarado(), item.formatoEnvelope(),
                                item.numeroOrigem(), item.classeOriginal(), item.camposRaw())))
                .toList();

        long importados = respostas.stream()
                .filter(r -> r.status() == ProcessoExternoCargaStatus.IMPORTADO).count();
        long divergencias = respostas.stream()
                .filter(r -> r.status() == ProcessoExternoCargaStatus.COM_DIVERGENCIA).count();
        long rejeitados = respostas.stream()
                .filter(r -> r.status() == ProcessoExternoCargaStatus.REJEITADO).count();

        return new CargaResultado(itens.size(), (int) importados,
                (int) divergencias, (int) rejeitados, respostas);
    }
}
