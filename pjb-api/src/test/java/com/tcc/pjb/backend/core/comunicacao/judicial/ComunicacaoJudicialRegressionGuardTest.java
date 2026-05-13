package com.tcc.pjb.backend.core.comunicacao.judicial;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ComunicacaoJudicialRegressionGuardTest {

    private static final Path MAIN_JAVA = Path.of("src/main/java");
    private static final Path COMUNICACAO_ROOT = MAIN_JAVA.resolve("com/tcc/pjb/backend/core/comunicacao/judicial");
    private static final Pattern TOP_LEVEL_PUBLIC_TYPE = Pattern.compile("^public\\s+(class|record|interface|enum)\\b");
    private static final Pattern COMMENT_LINE = Pattern.compile("^\\s*(//|/\\*)");
    private static final Set<String> SERVICES_COM_ESTADO = Set.of(
            "WebhookOutboundService.java",
            "GeofencePresencaOficialService.java",
            "PrazoRespostaPosEntregaEngine.java",
            "ReveliaAutomaticaEngine.java",
            "CuradorEspecialAutomaticoService.java",
            "SlaExpedicaoDashboardService.java",
            "QrCodeMandadoService.java",
            "RecusaRecebimentoService.java",
            "CitacaoHoraCertaEngine.java",
            "BnmpIntegracaoService.java",
            "ComunicacaoJudicialAtendimentoRelayService.java"
    );

    @Test
    void naoDeveConterPadraoSetOfComEnumValues() throws IOException {
        assertFalse(scanJavaSources().contains("Set.of(RamoDireito.values())"));
    }

    @Test
    void naoDeveConterTypoDocumentNoFluxoDigital() throws IOException {
        assertFalse(scanJavaSources().contains("document != null && expedicaoRepository.jaFoiServadoDigitalmente(documento)"));
    }

    @Test
    void arquivosSensveisNaoDevemTerMaisDeUmTipoPublicoTopLevel() throws IOException {
        try (Stream<Path> stream = Files.walk(COMUNICACAO_ROOT)) {
            List<Path> violacoes = stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(this::hasMoreThanOneTopLevelPublicType)
                    .toList();
            assertTrue(violacoes.isEmpty(), () -> "Arquivos com mais de um tipo público top-level: " + violacoes);
        }
    }

    @Test
    void moduloComunicacionalNaoDeveConterComentariosDeLinhaOuBloco() throws IOException {
        try (Stream<Path> stream = Files.walk(COMUNICACAO_ROOT)) {
            List<Path> violacoes = stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(this::containsCommentLine)
                    .toList();
            assertTrue(violacoes.isEmpty(), () -> "Arquivos com comentários detectados: " + violacoes);
        }
    }

    @Test
    void servicosComEstadoDevemUsarStateStore() throws IOException {
        try (Stream<Path> stream = Files.walk(COMUNICACAO_ROOT)) {
            List<Path> violacoes = stream
                    .filter(path -> SERVICES_COM_ESTADO.contains(path.getFileName().toString()))
                    .filter(path -> !containsStateStore(path))
                    .toList();
            assertTrue(violacoes.isEmpty(), () -> "Serviços stateful sem ComunicacaoJudicialStateStore: " + violacoes);
        }
    }

    @Test
    void portalNotificationDeveConectarRepresentacaoEAtendimento() throws IOException {
        String portal = Files.readString(COMUNICACAO_ROOT.resolve("ComunicacaoJudicialPortalNotificationService.java"));
        String relay = Files.readString(COMUNICACAO_ROOT.resolve("ComunicacaoJudicialAtendimentoRelayService.java"));
        assertTrue(portal.contains("LaianeProcuracaoRepository"));
        assertTrue(portal.contains("clienteRepository.existsByCpfHashAndAdvogado_Id"));
        assertTrue(portal.contains("ComunicacaoJudicialMensagemInteligenteService"));
        assertTrue(relay.contains("PJB_SISTEMA"));
        assertTrue(relay.contains("ComunicacaoJudicialMensagemInteligenteService"));
        assertTrue(relay.contains("UiHistoryService.EVT_ATENDIMENTO_NEW_MESSAGE"));
    }


    @Test
    void servicoDeMensagemInteligenteDeveRelacionarPrazoRitoEMaterializacao() throws IOException {
        String content = Files.readString(COMUNICACAO_ROOT.resolve("ComunicacaoJudicialMensagemInteligenteService.java"));
        assertTrue(content.contains("MatrizComunicacaoJudicialResolver"));
        assertTrue(content.contains("PrazoRespostaPosEntregaEngine"));
        assertTrue(content.contains("Contexto processual:"));
        assertTrue(content.contains("Forma de ciência priorizada:"));
        assertTrue(content.contains("Prazo identificado:"));
    }

    @Test
    void matrizProcedimentalDeveSinalizarMaterializacao() throws IOException {
        String content = Files.readString(COMUNICACAO_ROOT.resolve("MatrizComunicacaoJudicialResolver.java"));
        assertTrue(content.contains("materializacao="));
        assertTrue(content.contains("materializacao=representante_digital"));
        assertTrue(content.contains("materializacao=oficial_justica"));
        assertTrue(content.contains("microssistema="));
        assertTrue(content.contains("grau="));
    }


    @Test
    void moduloDeveConterResolverNacionalDeMicrossistemas() throws IOException {
        String context = Files.readString(COMUNICACAO_ROOT.resolve("ProceduralCommunicationContext.java"));
        String competencia = Files.readString(COMUNICACAO_ROOT.resolve("ComunicacaoJudicialCompetenciaService.java"));
        assertTrue(Files.exists(COMUNICACAO_ROOT.resolve("ComunicacaoJudicialMicrossistemaResolver.java")));
        assertTrue(context.contains("ComunicacaoJudicialMicrossistema"));
        assertTrue(competencia.contains("ComunicacaoJudicialMicrossistemaResolver"));
        assertTrue(competencia.contains("delegavelSecretaria"));
    }


    @Test
    void moduloDeveConterMalhaDeTribunaisSuperioresECompetenciaRefinada() throws IOException {
        String context = Files.readString(COMUNICACAO_ROOT.resolve("ProceduralCommunicationContext.java"));
        String matriz = Files.readString(COMUNICACAO_ROOT.resolve("MatrizComunicacaoJudicialResolver.java"));
        String controller = Files.readString(COMUNICACAO_ROOT.resolve("CitacaoIntimacaoController.java"));
        String competencia = Files.readString(COMUNICACAO_ROOT.resolve("ComunicacaoJudicialCompetenciaService.java"));
        assertTrue(Files.exists(COMUNICACAO_ROOT.resolve("ComunicacaoJudicialTribunalSuperior.java")));
        assertTrue(Files.exists(COMUNICACAO_ROOT.resolve("ComunicacaoJudicialTribunalSuperiorResolver.java")));
        assertTrue(Files.exists(COMUNICACAO_ROOT.resolve("ComunicacaoJudicialAutoridadeCompetente.java")));
        assertTrue(context.contains("exigeRevisaoRegimentalHumana"));
        assertTrue(matriz.contains("tribunalSuperior="));
        assertTrue(matriz.contains("revisaoRegimentalTribunal"));
        assertTrue(competencia.contains("autoridadeCompetente"));
        assertTrue(competencia.contains("executorPreferencial"));
        assertTrue(competencia.contains("RELATOR_TRIBUNAL"));
        assertTrue(controller.contains("tribunalSuperior"));
        assertTrue(controller.contains("autoridadeCompetente"));
        assertTrue(controller.contains("executorPreferencial"));
        assertTrue(controller.contains("revisaoRegimentalHumana"));
    }


    @Test
    void qrCodeDevePersistirPrimeiraVerificacaoParaEvitarDuplicidadeAposRestart() throws IOException {
        String content = Files.readString(COMUNICACAO_ROOT.resolve("QrCodeMandadoService.java"));
        assertTrue(content.contains("stateStore.exists(DOMAIN_QR_VERIFICACAO, token)"));
        assertTrue(content.contains("marcarPrimeiraVerificacao"));
    }

    @Test
    void webhookDeveHidratarCacheEDeduplicarHistoricoPersistido() throws IOException {
        String content = Files.readString(COMUNICACAO_ROOT.resolve("WebhookOutboundService.java"));
        assertTrue(content.contains("@PostConstruct"));
        assertTrue(content.contains("MAX_HISTORICO_CACHE"));
        assertTrue(content.contains("dispatch.dispatchUuid()"));
        assertTrue(content.contains("appendHistorico"));
    }

    @Test
    void slaDeveInicializarBenchmarksAposConstrucaoDoBean() throws IOException {
        String content = Files.readString(COMUNICACAO_ROOT.resolve("SlaExpedicaoDashboardService.java"));
        assertTrue(content.contains("@PostConstruct"));
        assertTrue(content.contains("bootstrapBenchmarks"));
    }

    @Test
    void moduloDeveConterCanalSefazNfeNaInterceptacaoEmpresarial() throws IOException {
        String modalidades = Files.readString(COMUNICACAO_ROOT.resolve("ModalidadeExpedicaoJudicial.java"));
        String vias = Files.readString(COMUNICACAO_ROOT.resolve("hsm").resolve("ViaInterceptacao.java"));
        String motor = Files.readString(COMUNICACAO_ROOT.resolve("hsm").resolve("MotorInterceptacaoAtiva.java"));
        String engine = Files.readString(COMUNICACAO_ROOT.resolve("CitacaoIntimacaoEngine.java"));
        assertTrue(modalidades.contains("DIGITAL_SEFAZ_NF_EMAIL"));
        assertTrue(vias.contains("SefazNfeEmissor"));
        assertTrue(motor.contains("taticaSefazNfe"));
        assertTrue(engine.contains("SEFAZ_NFE_EMAIL_ICP"));
    }


    private boolean hasMoreThanOneTopLevelPublicType(Path path) {
        try {
            long count = Files.readAllLines(path).stream()
                    .filter(line -> TOP_LEVEL_PUBLIC_TYPE.matcher(line).find())
                    .count();
            return count > 1;
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao inspecionar " + path, e);
        }
    }

    private boolean containsCommentLine(Path path) {
        try {
            return Files.readAllLines(path).stream().anyMatch(line -> COMMENT_LINE.matcher(line).find());
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao inspecionar " + path, e);
        }
    }

    private boolean containsStateStore(Path path) {
        try {
            return Files.readString(path).contains("ComunicacaoJudicialStateStore");
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao inspecionar " + path, e);
        }
    }

    private String scanJavaSources() throws IOException {
        StringBuilder builder = new StringBuilder(1 << 16);
        try (Stream<Path> stream = Files.walk(MAIN_JAVA)) {
            stream.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            builder.append(Files.readString(path)).append('\n');
                        } catch (IOException e) {
                            throw new IllegalStateException("Falha ao ler " + path, e);
                        }
                    });
        }
        return builder.toString();
    }
}
