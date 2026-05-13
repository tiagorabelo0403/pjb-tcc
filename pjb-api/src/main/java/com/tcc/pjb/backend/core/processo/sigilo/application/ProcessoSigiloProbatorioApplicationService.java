package com.tcc.pjb.backend.core.processo.sigilo.application;

import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionDescriptor;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator;
import com.tcc.pjb.backend.core.processo.prova.application.ProcessoProvaApplicationService;
import com.tcc.pjb.backend.core.processo.prova.domain.ProcessoProvaAggregate;
import com.tcc.pjb.backend.core.processo.prova.domain.ProcessoProvaConsulta;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloProbatorioAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloProbatorioItem;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoSigiloProbatorioApplicationService {

    private static final java.time.Duration ANALISE_PROVA_TIMEOUT = java.time.Duration.ofSeconds(4);

    private final ProcessoRepository processoRepository;
    private final DocumentoProcessualRepository documentoProcessualRepository;
    private final ProcessoProvaApplicationService processoProvaApplicationService;
    private final PjbExecutionOrchestrator executionOrchestrator;

    public ProcessoSigiloProbatorioApplicationService(ProcessoRepository processoRepository,
                                                      DocumentoProcessualRepository documentoProcessualRepository,
                                                      ProcessoProvaApplicationService processoProvaApplicationService,
                                                      PjbExecutionOrchestrator executionOrchestrator) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.documentoProcessualRepository = Objects.requireNonNull(documentoProcessualRepository);
        this.processoProvaApplicationService = Objects.requireNonNull(processoProvaApplicationService);
        this.executionOrchestrator = Objects.requireNonNull(executionOrchestrator);
    }

    @Transactional(readOnly = true)
    public ProcessoSigiloProbatorioAggregate avaliar(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        List<DocumentoProcessual> documentos = documentoProcessualRepository.findTop18ByProcesso_IdOrderByCriadoEmDesc(processoId);
        List<ProcessoProvaAggregate> provas = analisarProvas(processo, documentos);
        ArrayList<ProcessoSigiloProbatorioItem> itens = new ArrayList<>();
        LinkedHashSet<String> alertas = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        NivelSigilo nivelRecomendado = processo.getNivelSigilo() == null ? NivelSigilo.PUBLICO : processo.getNivelSigilo();
        long provasCompartilhadas = 0L;
        for (ProcessoProvaAggregate prova : provas) {
            if (prova.classificacao().nivelSigiloEfetivo().nivel() > nivelRecomendado.nivel()) {
                nivelRecomendado = prova.classificacao().nivelSigiloEfetivo();
            }
            if (prova.evidencia().haCompartilhamentoInterfeitos()) {
                provasCompartilhadas++;
            }
            boolean relevante = prova.classificacao().sensivel()
                    || prova.classificacao().exigeReforcoSigilo()
                    || prova.evidencia().haCompartilhamentoInterfeitos()
                    || prova.integridade().duplicidadeNoMesmoFeito();
            if (!relevante) {
                fundamentos.addAll(prova.fundamentos());
                continue;
            }
            itens.add(new ProcessoSigiloProbatorioItem(
                    prova.identity().documentoId(),
                    prova.identity().tituloDocumento(),
                    prova.classificacao().nivelSigiloEfetivo(),
                    prova.classificacao().naturezaProbatoria(),
                    prova.classificacao().sensivel(),
                    prova.classificacao().exigeReforcoSigilo(),
                    prova.evidencia().haCompartilhamentoInterfeitos(),
                    prova.classificacao().marcadores(),
                    prova.fundamentos()
            ));
            if (prova.classificacao().exigeReforcoSigilo()) {
                alertas.add("Há documento com reforço de sigilo recomendado acima da classificação atual do processo.");
            }
            if (prova.evidencia().haCompartilhamentoInterfeitos()) {
                alertas.add("Há compartilhamento probatório entre feitos com necessidade de governança de acesso e reaproveitamento controlado.");
            }
            if (prova.integridade().duplicidadeNoMesmoFeito()) {
                alertas.add("Há duplicidade intraprocessual de prova com mesmo hash criptográfico.");
            }
            fundamentos.addAll(prova.fundamentos());
        }
        if (nivelRecomendado.nivel() > (processo.getNivelSigilo() == null ? NivelSigilo.PUBLICO : processo.getNivelSigilo()).nivel()) {
            fundamentos.add("A prova elevou o patamar de proteção recomendado do processo acima da classificação atual.");
        }
        itens.sort(Comparator.comparing((ProcessoSigiloProbatorioItem item) -> item.nivelSigiloEfetivo().nivel()).reversed()
                .thenComparing(ProcessoSigiloProbatorioItem::sensivel).reversed()
                .thenComparing(ProcessoSigiloProbatorioItem::tituloDocumento));
        return new ProcessoSigiloProbatorioAggregate(
                processo.getId(),
                processo.getNumero(),
                processo.getNivelSigilo(),
                nivelRecomendado,
                processo.getNivelSigilo() == null || nivelRecomendado.nivel() > processo.getNivelSigilo().nivel(),
                documentos.size(),
                itens.size(),
                provasCompartilhadas,
                List.copyOf(itens),
                List.copyOf(alertas),
                List.copyOf(fundamentos.stream().limit(40).toList()),
                Instant.now()
        );
    }

    private List<ProcessoProvaAggregate> analisarProvas(Processo processo, List<DocumentoProcessual> documentos) {
        if (documentos.isEmpty()) {
            return List.of();
        }
        List<CompletableFuture<ProcessoProvaAggregate>> futures = documentos.stream()
                .map(documento -> executionOrchestrator.supply(
                                PjbExecutionDescriptor.burst("processo.sigilo.prova." + documento.getId(), ANALISE_PROVA_TIMEOUT),
                                () -> processoProvaApplicationService.analisar(new ProcessoProvaConsulta(
                                        processo.getId(),
                                        processo.getNumero(),
                                        documento.getId(),
                                        "SIGILO_PROBATORIO",
                                        "PROCESSO_SIGILO_PROBATORIO"
                                )))
                        .exceptionally(ex -> null))
                .toList();
        awaitAnalises(futures);
        return futures.stream()
                .map(future -> future.getNow(null))
                .filter(Objects::nonNull)
                .toList();
    }

    private void awaitAnalises(List<CompletableFuture<ProcessoProvaAggregate>> futures) {
        if (futures == null || futures.isEmpty()) {
            return;
        }
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(ANALISE_PROVA_TIMEOUT.plusMillis(300).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            cancelarAnalises(futures);
        } catch (java.util.concurrent.TimeoutException | java.util.concurrent.ExecutionException ex) {
            cancelarAnalises(futures);
        }
    }

    private void cancelarAnalises(List<CompletableFuture<ProcessoProvaAggregate>> futures) {
        for (CompletableFuture<ProcessoProvaAggregate> future : futures) {
            if (future != null) {
                future.cancel(true);
            }
        }
    }
}
