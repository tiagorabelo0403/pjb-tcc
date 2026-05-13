package com.tcc.pjb.backend.core.quality.codebase.application;

import java.util.Locale;

public final class PjbCodebaseLearningMessages {

    private PjbCodebaseLearningMessages() {
    }

    public static String testDebt(double razaoTeste) {
        return "Razão de testes abaixo do piso recomendado para extração segura: " + formatRatio(razaoTeste);
    }

    public static String incomingPressure(int dependenciasEntrantes) {
        return "Dependências entrantes elevadas: " + dependenciasEntrantes;
    }

    public static String outgoingPressure(int dependenciasSaida) {
        return "Dependências de saída elevadas: " + dependenciasSaida;
    }

    public static String controllerPressure(int consumidoresController) {
        return "Pressão de surface HTTP sobre a fatia: controllers consumidores=" + consumidoresController;
    }

    public static String freezeExpansion() {
        return "Congelar expansão da fatia antes de nova adição funcional.";
    }

    public static String extractPortsAndFacade() {
        return "Extrair fachada canônica, portas internas e contratos explícitos antes da quebra em submódulos.";
    }

    public static String raiseTestDensity() {
        return "Elevar a densidade de testes do hotspot antes de refactors estruturais amplos.";
    }

    public static String prioritizeControllerBoundary() {
        return "Mapear primeiro os pontos consumidos por controllers e estabilizar DTOs de borda.";
    }

    public static String firstWave(String slices) {
        return "Onda 1: iniciar decomposição controlada por " + slices + ".";
    }

    public static String secondWave(String slices) {
        return "Onda 2: preparar extração após estabilizar a primeira onda em " + slices + ".";
    }

    public static String learningMainConcentration(String slices) {
        return "A concentração real do núcleo está em " + slices + ", e não no legado nominal isolado.";
    }

    public static String learningTestDebt(String slices) {
        return "Os maiores hotspots ainda carregam dívida de testes relevante em " + slices + ".";
    }

    public static String learningControllerPressure(String slices) {
        return "A pressão de surface HTTP se concentra em " + slices + ", então a extração deve começar pelos contratos de borda.";
    }

    public static String learningLegacyJudgeIsolation() {
        return "O legado judge permanece isolado e não deve disputar prioridade com os hotspots canônicos do core.";
    }

    public static String learningIntegrationDebt(int testesIntegracao, double razaoIntegracao) {
        return "A malha ainda tem poucos testes de integração reais: " + testesIntegracao + " arquivo(s), razão=" + formatRatio(razaoIntegracao) + ".";
    }

    public static String learningCriticalFlowDebt(String flows) {
        return "Os fluxos institucionais críticos ainda não estão suficientemente cobertos em " + flows + ".";
    }

    public static String learningBlueprintFocus(String blueprints) {
        return "Os blueprints de extração mais maduros neste snapshot são " + blueprints + ".";
    }

    public static String laneCanonicalSurface(String nome) {
        return "Trilha " + nome + " concentra um recorte canônico para extração controlada.";
    }

    public static String laneNeedsCoverage(String nome, double razaoTeste) {
        return "Trilha " + nome + " requer endurecimento de testes antes de separação, razão=" + formatRatio(razaoTeste) + ".";
    }

    public static String laneReady() {
        return "PRONTA";
    }

    public static String lanePrepare() {
        return "PREPARAR";
    }

    public static String laneHarden() {
        return "ENDURECER";
    }

    public static String laneActionStabilizeContracts(String nome) {
        return "Estabilizar contratos de borda e fachada canônica em " + nome + ".";
    }

    public static String laneActionRaiseCoverage(String nome) {
        return "Elevar cobertura direcionada da trilha " + nome + " antes da separação.";
    }

    public static String laneActionSeparateModule(String nome) {
        return "Preparar separação progressiva da trilha " + nome + " em pacote/módulo próprio.";
    }

    public static String blueprintContractStabilization(String alvo) {
        return "Estabilizar DTOs e contratos de borda antes da extração de " + alvo + ".";
    }

    public static String blueprintIntegrationFirst(String contrato) {
        return "Criar teste de integração inicial " + contrato + " para proteger a separação.";
    }

    public static String blueprintModuleAfterCoverage(String alvo) {
        return "Promover " + alvo + " para módulo ou bounded context próprio após endurecimento.";
    }

    public static String criticalFlowMissing(String flow) {
        return "Fluxo crítico ausente de integração real: " + flow + ".";
    }

    public static String criticalFlowPartial(String flow, double coverage) {
        return "Fluxo crítico com cobertura parcial: " + flow + ", cobertura=" + formatRatio(coverage) + ".";
    }

    public static String criticalFlowCovered(String flow, double coverage) {
        return "Fluxo crítico com lastro razoável de integração: " + flow + ", cobertura=" + formatRatio(coverage) + ".";
    }

    public static String criticalFlowActionCreate(String flow) {
        return "Criar IT ponta a ponta para o fluxo " + flow + ".";
    }

    public static String criticalFlowActionExpand(String flow) {
        return "Ampliar ITs existentes para cobrir integralmente o fluxo " + flow + ".";
    }

    public static String criticalFlowActionVerify() {
        return "Executar o fluxo dentro do ciclo verify e quality-gates antes de promover a extração.";
    }

    private static String formatRatio(double ratio) {
        return String.format(Locale.ROOT, "%.3f", ratio);
    }
}
