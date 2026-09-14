package com.tcc.pjb.backend.service.painel.shared;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Fachada real sobre as 4 camadas de composição de painel (sinal, coleção nativa,
 * superfície de ação, superfície de execução), que sempre são usadas juntas na mesma
 * sequência: derivar sinais -> compor coleções -> montar superfícies -> decorar cada
 * bloco do painel com as 4 camadas.
 *
 * Extraído (F6) de OficialJusticaPainelService, que injetava as 4 diretamente (34
 * dependências de construtor). Reutilizável por qualquer outro *PainelService que siga
 * o mesmo padrão (MinisterioPublicoPainelService, DefensorPublicoPainelService,
 * DelegadoPainelService etc. usam os mesmos 4 colaboradores de painel.shared).
 */
@Component
public class PainelCompositionPipelineService {

    private final PainelSignalReflectionService signalReflectionService;
    private final PainelNativeCollectionCompositionService collectionCompositionService;
    private final PainelActionSurfaceCompositionService actionSurfaceCompositionService;
    private final PainelExecutionSurfaceCompositionService executionSurfaceCompositionService;

    public PainelCompositionPipelineService(
            PainelSignalReflectionService signalReflectionService,
            PainelNativeCollectionCompositionService collectionCompositionService,
            PainelActionSurfaceCompositionService actionSurfaceCompositionService,
            PainelExecutionSurfaceCompositionService executionSurfaceCompositionService) {
        this.signalReflectionService = Objects.requireNonNull(signalReflectionService);
        this.collectionCompositionService = Objects.requireNonNull(collectionCompositionService);
        this.actionSurfaceCompositionService = Objects.requireNonNull(actionSurfaceCompositionService);
        this.executionSurfaceCompositionService = Objects.requireNonNull(executionSurfaceCompositionService);
    }

    public Map<String, Object> deriveSignals(String panelCode,
                                              Map<String, Object> sharedExperience,
                                              int pendingCount,
                                              int urgentDeadlines,
                                              String dominantContext) {
        return signalReflectionService.deriveSignals(panelCode, sharedExperience, pendingCount, urgentDeadlines, dominantContext);
    }

    public Map<String, Object> buildNativeComposition(String panelCode, Map<String, Object> signals) {
        return signalReflectionService.buildNativeComposition(panelCode, signals);
    }

    public <T> List<T> composeList(String panelCode,
                                    String collectionName,
                                    List<T> source,
                                    Map<String, Object> operationalSignals,
                                    Map<String, Object> nativeComposition) {
        return collectionCompositionService.composeList(panelCode, collectionName, source, operationalSignals, nativeComposition);
    }

    public Map<String, Object> buildCollectionComposition(String panelCode,
                                                            Map<String, Object> operationalSignals,
                                                            Map<String, Object> nativeComposition,
                                                            Map<String, ? extends List<?>> collections) {
        return collectionCompositionService.buildCollectionComposition(panelCode, operationalSignals, nativeComposition, collections);
    }

    public Map<String, Object> buildActionSurface(String panelCode,
                                                   Map<String, Object> operationalSignals,
                                                   Map<String, Object> nativeComposition,
                                                   Map<String, Object> collectionComposition) {
        return actionSurfaceCompositionService.buildActionSurface(panelCode, operationalSignals, nativeComposition, collectionComposition);
    }

    public Map<String, Object> buildExecutionSurface(String panelCode,
                                                       Map<String, Object> operationalSignals,
                                                       Map<String, Object> nativeComposition,
                                                       Map<String, Object> collectionComposition,
                                                       Map<String, Object> actionSurface) {
        return executionSurfaceCompositionService.buildExecutionSurface(panelCode, operationalSignals, nativeComposition, collectionComposition, actionSurface);
    }

    /**
     * Pipeline completo de 4 camadas para um bloco: sinal, coleção nativa, superfície de
     * ação, superfície de execução.
     */
    public Map<String, Object> decorate(String panelCode,
                                         String blockCode,
                                         Map<String, Object> rawBlock,
                                         Map<String, Object> operationalSignals,
                                         Map<String, Object> nativeComposition,
                                         Map<String, Object> actionSurface,
                                         Map<String, Object> executionSurface) {
        Map<String, Object> block = signalReflectionService.reflectInBlock(panelCode, blockCode, rawBlock, operationalSignals);
        block = collectionCompositionService.decorateBlock(panelCode, blockCode, block, operationalSignals, nativeComposition);
        block = actionSurfaceCompositionService.decorateBlock(panelCode, blockCode, block, actionSurface, nativeComposition);
        block = executionSurfaceCompositionService.decorateBlock(panelCode, blockCode, block, executionSurface, nativeComposition);
        return block;
    }

    /**
     * Variante sem a camada de coleção nativa -- usada por blocos que não participam
     * de composição de coleção (ex.: identidade visual do painel).
     */
    public Map<String, Object> decorateWithoutCollection(String panelCode,
                                                          String blockCode,
                                                          Map<String, Object> rawBlock,
                                                          Map<String, Object> operationalSignals,
                                                          Map<String, Object> nativeComposition,
                                                          Map<String, Object> actionSurface,
                                                          Map<String, Object> executionSurface) {
        Map<String, Object> block = signalReflectionService.reflectInBlock(panelCode, blockCode, rawBlock, operationalSignals);
        block = actionSurfaceCompositionService.decorateBlock(panelCode, blockCode, block, actionSurface, nativeComposition);
        block = executionSurfaceCompositionService.decorateBlock(panelCode, blockCode, block, executionSurface, nativeComposition);
        return block;
    }
}
