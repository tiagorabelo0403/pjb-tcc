package com.tcc.pjb.backend.service.processual.peticionamento.media;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PeticionamentoThreatSentinelService {

    public ThreatSentinelReport plan(ResolveRequest request) {
        ResolveRequest safe = request == null ? ResolveRequest.empty() : request;
        ArrayList<String> watchSignals = new ArrayList<>();
        ArrayList<String> hardening = new ArrayList<>();
        int sensitiveCount = 0;
        int videoCount = 0;

        for (PeticionamentoMediaBlocoRequest block : safe.inlineMediaBlocks()) {
            if (block == null) {
                continue;
            }
            if (block.blurInicialObrigatorio()) {
                sensitiveCount++;
            }
            if ("VIDEO".equals(block.tipoResolvido())) {
                videoCount++;
            }
        }

        watchSignals.add("SURTO_UPLOADS_POR_ATOR_OU_IP");
        watchSignals.add("MISMATCH_MIME_MAGIC_BYTES");
        watchSignals.add("ERROS_REPETIDOS_EM_QUARENTENA");
        watchSignals.add("EXPLOSAO_TRANSCODIFICACAO_OU_THUMBNAIL");
        watchSignals.add("LEITURA_ANOMALA_DE_STORAGE_SENSIVEL");
        if (sensitiveCount > 0) {
            watchSignals.add("TENTATIVA_DE_VISUALIZACAO_DE_MIDIA_BORRADA_FORA_DA_ALCADA");
        }
        if (videoCount > 0) {
            watchSignals.add("CONSUMO_ANOMALO_DE_CPU_EM_PIPELINE_DE_VIDEO");
        }

        hardening.add("FALCO_RUNTIME_ALERTING");
        hardening.add("CLAMAV_QUARENTENA");
        hardening.add("YARA_HUNTING_RULES");
        hardening.add("RATELIMIT_DE_UPLOAD_E_PREVIEW");
        hardening.add("HASH_E_AUDITORIA_IMUTAVEL");
        if (safe.sigiloSensivel()) {
            hardening.add("TRILHA_RESTRITA_PARA_SIGILO_SENSIVEL");
        }

        LinkedHashMap<String, Object> workspace = new LinkedHashMap<>();
        workspace.put("profile", resolveProfile(safe, sensitiveCount, videoCount));
        workspace.put("watchSignals", List.copyOf(watchSignals));
        workspace.put("hardening", List.copyOf(hardening));
        workspace.put("engines", List.of("FALCO", "CLAMAV", "YARA", "AI_ANOMALY_COMPANION"));
        workspace.put("sensitiveMediaCount", sensitiveCount);
        workspace.put("videoCount", videoCount);
        workspace.put("sessionKey", safe.sessionKey());

        return new ThreatSentinelReport(
                resolveProfile(safe, sensitiveCount, videoCount),
                List.copyOf(watchSignals),
                List.copyOf(hardening),
                Map.copyOf(workspace)
        );
    }

    private static String resolveProfile(ResolveRequest request, int sensitiveCount, int videoCount) {
        if (request.inlineMediaBlocks().isEmpty()) {
            return "SENTINEL_PADRAO";
        }
        if (sensitiveCount > 0) {
            return "SENTINEL_SENSIVEL_REFORCADO";
        }
        if (videoCount > 0) {
            return "SENTINEL_VIDEO_EXPANDIDO";
        }
        return "SENTINEL_MULTIMIDIA";
    }

    public record ResolveRequest(String sessionKey,
                                 List<PeticionamentoMediaBlocoRequest> inlineMediaBlocks,
                                 boolean sigiloSensivel) {
        public ResolveRequest {
            sessionKey = Objects.requireNonNullElse(sessionKey, "peticao-session");
            inlineMediaBlocks = inlineMediaBlocks == null ? List.of() : List.copyOf(inlineMediaBlocks);
        }

        public static ResolveRequest empty() {
            return new ResolveRequest("peticao-session", List.of(), false);
        }
    }

    public record ThreatSentinelReport(String profile,
                                       List<String> watchSignals,
                                       List<String> hardening,
                                       Map<String, Object> workspace) {
        public ThreatSentinelReport {
            profile = Objects.requireNonNullElse(profile, "SENTINEL_PADRAO");
            watchSignals = watchSignals == null ? List.of() : List.copyOf(watchSignals);
            hardening = hardening == null ? List.of() : List.copyOf(hardening);
            workspace = workspace == null ? Map.of() : Map.copyOf(workspace);
        }
    }
}
