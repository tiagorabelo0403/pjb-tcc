package com.tcc.pjb.backend.service.processual.peticionamento;

import com.tcc.pjb.backend.core.storage.ObjectStorageProperties;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PeticionamentoPericiaEvidenceIntelligenceService {

    private final ObjectStorageProperties.Upload uploadProperties;

    public PeticionamentoPericiaEvidenceIntelligenceService() {
        this(new ObjectStorageProperties());
    }

    public PeticionamentoPericiaEvidenceIntelligenceService(ObjectStorageProperties props) {
        ObjectStorageProperties properties = props == null ? new ObjectStorageProperties() : props;
        this.uploadProperties = properties.getUpload() == null ? new ObjectStorageProperties.Upload() : properties.getUpload();
    }

    public PericiaEvidenceReport analyze(ResolveRequest request) {
        ResolveRequest safe = request == null ? ResolveRequest.empty() : request;
        ArrayList<String> alerts = new ArrayList<>();
        LinkedHashSet<String> especialidades = new LinkedHashSet<>();
        LinkedHashSet<String> diligencias = new LinkedHashSet<>();
        ArrayList<Map<String, Object>> evidenceMap = new ArrayList<>();

        int imageCount = 0;
        int audioCount = 0;
        int videoCount = 0;
        boolean cadeiaCustodiaReforcada = false;
        boolean transcricaoObrigatoria = false;
        boolean keyframesObrigatorios = false;

        for (PeticionamentoMediaBlocoRequest block : safe.inlineMediaBlocks()) {
            if (block == null) {
                continue;
            }
            String tipo = block.tipoResolvido();
            long tamanho = block.getTamanhoBytes() == null ? 0L : Math.max(0L, block.getTamanhoBytes());
            long duracao = block.getDuracaoMs() == null ? 0L : Math.max(0L, block.getDuracaoMs());
            LinkedHashMap<String, Object> item = new LinkedHashMap<>(block.toMap());
            boolean custodiaItemReforcada = block.blurInicialObrigatorio();

            switch (tipo) {
                case "IMAGEM" -> {
                    imageCount++;
                    especialidades.add("DOCUMENTOSCOPIA_IMAGEM");
                    diligencias.add("PRESERVAR_ORIGINAL_SEM_RECOMPRESSAO");
                    if (block.blurInicialObrigatorio()) {
                        cadeiaCustodiaReforcada = true;
                        custodiaItemReforcada = true;
                    }
                }
                case "AUDIO" -> {
                    audioCount++;
                    especialidades.add("FONETICA_FORENSE");
                    especialidades.add("COMPUTACAO_FORENSE");
                    diligencias.add("TRANSCRICAO_COM_MARCA_TEMPORAL");
                    diligencias.add("PRESERVAR_ARQUIVO_ORIGINAL_E_DERIVADO_CANONICO");
                    transcricaoObrigatoria = true;
                    cadeiaCustodiaReforcada = true;
                    custodiaItemReforcada = true;
                    if (duracao > positiveOr(uploadProperties.getMaxAudioDurationMs(), 900_000L) / 2L) {
                        alerts.add("Áudio extenso detectado; sugerir recortes temáticos e índice temporal para facilitar perícia e contraditório.");
                    }
                }
                case "VIDEO" -> {
                    videoCount++;
                    especialidades.add("AUDIOVISUAL_FORENSE");
                    especialidades.add("COMPUTACAO_FORENSE");
                    diligencias.add("EXTRACAO_KEYFRAMES_COM_HASH");
                    diligencias.add("TRANSCRICAO_MULTIMODAL_COM_MARCA_TEMPORAL");
                    diligencias.add("PRESERVAR_STREAM_SEGURO_E_ORIGINAL_IMUTAVEL");
                    transcricaoObrigatoria = true;
                    keyframesObrigatorios = true;
                    cadeiaCustodiaReforcada = true;
                    custodiaItemReforcada = true;
                    if (duracao > positiveOr(uploadProperties.getMaxVideoDurationMs(), 900_000L) / 2L) {
                        alerts.add("Vídeo extenso detectado; gerar keyframes, resumo temporal e pontos de interesse reduz o custo de leitura pericial e judicial.");
                    }
                    if (tamanho > positiveOr(uploadProperties.getMaxInlineVideoBytes(), 100_663_296L) / 2L) {
                        alerts.add("Vídeo pesado detectado; preferir streaming seguro, keyframes e prova narrativa resumida no corpo da peça.");
                    }
                }
                default -> {
                    especialidades.add("DOCUMENTOSCOPIA");
                    diligencias.add("VERIFICAR_ORIGINALIDADE_DOCUMENTAL");
                }
            }

            if (block.blurInicialObrigatorio()) {
                especialidades.add("ANALISE_CONTEUDO_SENSIVEL");
                diligencias.add("TRILHA_RESTRITA_DE_VISUALIZACAO");
                custodiaItemReforcada = true;
            }

            item.put("forensicTrack", resolveTrack(tipo));
            item.put("chainOfCustody", custodiaItemReforcada ? "REFORCADA" : "PADRAO_CONTROLADA");
            item.put("recommendedActions", List.copyOf(resolveActions(tipo, block.blurInicialObrigatorio())));
            evidenceMap.add(Map.copyOf(item));
        }

        if (!safe.provasDocumentais().isEmpty()) {
            especialidades.add("DOCUMENTOSCOPIA");
            diligencias.add("CONSOLIDAR_REFERENCIAS_DOCUMENTAIS_POS_PETICAO");
        }
        if (safe.sigiloSensivel()) {
            cadeiaCustodiaReforcada = true;
            diligencias.add("RESTRINGIR_ACESSO_A_PERITOS_HABILITADOS");
        }

        LinkedHashMap<String, Object> workspace = new LinkedHashMap<>();
        workspace.put("profile", resolveProfile(audioCount, videoCount, imageCount, cadeiaCustodiaReforcada));
        workspace.put("specialties", List.copyOf(especialidades));
        workspace.put("recommendedActions", List.copyOf(diligencias));
        workspace.put("inlineEvidenceMap", List.copyOf(evidenceMap));
        workspace.put("chainOfCustodyMode", cadeiaCustodiaReforcada ? "REFORCADA" : "PADRAO_CONTROLADA");
        workspace.put("transcriptionRequired", transcricaoObrigatoria);
        workspace.put("keyframesRequired", keyframesObrigatorios);
        workspace.put("sensitiveSealedReview", safe.sigiloSensivel());
        workspace.put("pericialReadiness", Map.of(
                "imageCount", imageCount,
                "audioCount", audioCount,
                "videoCount", videoCount,
                "documentaryReferenceCount", safe.provasDocumentais().size()
        ));

        return new PericiaEvidenceReport(
                resolveProfile(audioCount, videoCount, imageCount, cadeiaCustodiaReforcada),
                List.copyOf(alerts),
                Map.copyOf(workspace),
                Map.of(
                        "specialties", workspace.get("specialties"),
                        "recommendedActions", workspace.get("recommendedActions"),
                        "chainOfCustodyMode", workspace.get("chainOfCustodyMode"),
                        "transcriptionRequired", workspace.get("transcriptionRequired"),
                        "keyframesRequired", workspace.get("keyframesRequired")
                )
        );
    }

    private static String resolveTrack(String tipo) {
        return switch (Objects.requireNonNullElse(tipo, "DOCUMENTO")) {
            case "IMAGEM" -> "IMAGEM_FORENSE";
            case "AUDIO" -> "AUDIO_FORENSE";
            case "VIDEO" -> "VIDEO_FORENSE";
            default -> "DOCUMENTO_FORENSE";
        };
    }

    private static List<String> resolveActions(String tipo, boolean sensivel) {
        ArrayList<String> actions = new ArrayList<>();
        switch (Objects.requireNonNullElse(tipo, "DOCUMENTO")) {
            case "IMAGEM" -> {
                actions.add("GERAR_DERIVADO_DE_VISUALIZACAO_SEGURO");
                actions.add("REMOVER_METADADOS_SUPERFLUOS_DO_PREVIEW");
            }
            case "AUDIO" -> {
                actions.add("GERAR_TRANSCRICAO_TEMPORAL");
                actions.add("PRESERVAR_FAIXA_ORIGINAL");
            }
            case "VIDEO" -> {
                actions.add("GERAR_KEYFRAMES_INDEXADOS");
                actions.add("GERAR_TRANSCRICAO_MULTIMODAL");
            }
            default -> actions.add("CONSOLIDAR_NO_BLOCO_POS_PETICAO");
        }
        if (sensivel) {
            actions.add("RESTRICAO_DE_VISUALIZACAO_COM_BLUR_INICIAL");
        }
        return List.copyOf(actions);
    }

    private static long positiveOr(long value, long fallback) {
        return value > 0L ? value : fallback;
    }

    private static String resolveProfile(int audioCount, int videoCount, int imageCount, boolean cadeiaCustodiaReforcada) {
        if (videoCount > 0) {
            return cadeiaCustodiaReforcada ? "PERICIA_AUDIOVISUAL_CUSTODIA_REFORCADA" : "PERICIA_AUDIOVISUAL_GUIADA";
        }
        if (audioCount > 0) {
            return cadeiaCustodiaReforcada ? "PERICIA_AUDIO_CUSTODIA_REFORCADA" : "PERICIA_AUDIO_GUIADA";
        }
        if (imageCount > 0) {
            return cadeiaCustodiaReforcada ? "PERICIA_IMAGEM_CUSTODIA_REFORCADA" : "PERICIA_IMAGEM_GUIADA";
        }
        return "PERICIA_DOCUMENTAL_PADRAO";
    }

    public record ResolveRequest(List<PeticionamentoMediaBlocoRequest> inlineMediaBlocks,
                                 List<String> provasDocumentais,
                                 boolean sigiloSensivel) {
        public ResolveRequest {
            inlineMediaBlocks = inlineMediaBlocks == null ? List.of() : List.copyOf(inlineMediaBlocks);
            provasDocumentais = provasDocumentais == null ? List.of() : List.copyOf(provasDocumentais);
        }

        public static ResolveRequest empty() {
            return new ResolveRequest(List.of(), List.of(), false);
        }
    }

    public record PericiaEvidenceReport(String profile,
                                        List<String> alerts,
                                        Map<String, Object> workspace,
                                        Map<String, Object> protocolSection) {
        public PericiaEvidenceReport {
            profile = Objects.requireNonNullElse(profile, "PERICIA_DOCUMENTAL_PADRAO");
            alerts = alerts == null ? List.of() : List.copyOf(alerts);
            workspace = workspace == null ? Map.of() : Map.copyOf(workspace);
            protocolSection = protocolSection == null ? Map.of() : Map.copyOf(protocolSection);
        }
    }
}
