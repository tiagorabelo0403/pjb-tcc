package com.tcc.pjb.backend.service.processual.arquivamento;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ArquivamentoReadinessService {

    public record ReadinessSnapshot(
            UUID processoId,
            boolean apto,
            List<ArquivamentoPendenciaChecker.PendenciaArquivamento> pendencias,
            String mensagem
    ) {}

    private final ArquivamentoPendenciaChecker checker;

    public ArquivamentoReadinessService(ArquivamentoPendenciaChecker checker) {
        this.checker = checker;
    }

    public ReadinessSnapshot avaliar(UUID processoId,
            ArquivamentoPendenciaChecker.ArquivamentoInput input) {
        List<ArquivamentoPendenciaChecker.PendenciaArquivamento> pendencias = checker.verificar(input);
        boolean bloqueante = pendencias.stream()
                .anyMatch(ArquivamentoPendenciaChecker.PendenciaArquivamento::bloqueante);
        boolean apto = !bloqueante;
        String mensagem = apto
                ? "Processo aparentemente apto para baixa e arquivamento — confirmar com servidor."
                : pendencias.stream().filter(
                        ArquivamentoPendenciaChecker.PendenciaArquivamento::bloqueante).count()
                        + " pendência(s) bloqueante(s) — regularizar antes de arquivar.";
        return new ReadinessSnapshot(processoId, apto, pendencias, mensagem);
    }
}
