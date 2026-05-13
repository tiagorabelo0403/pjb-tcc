
package com.tcc.pjb.backend.service.processual.peticionamento;

import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.modules.laiane.dto.protocol.LaianeProtocolPackageDto;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PeticionamentoProtocolEnvelopeHardeningService {

    public EnvelopeReport harden(ResolveRequest request) {
        ResolveRequest safe = request == null ? ResolveRequest.empty() : request;
        String signatureMode = resolveSignatureMode(safe.tipoUsuario(), safe.resolvedInstrument(), safe.sigilo());
        String submissionMode = resolveSubmissionMode(safe.prepararPacoteProtocolo(), safe.protocolPackage(), safe.sigilo());
        ArrayList<GateDecision> gateDecisions = new ArrayList<>();
        gateDecisions.add(new GateDecision(
                "REPRESENTACAO_VALIDADA",
                safe.representacaoRegular(),
                !safe.representacaoRegular(),
                safe.representacaoRegular() ? "Representação processual apta." : "Representação processual ainda pendente."
        ));
        gateDecisions.add(new GateDecision(
                "LOTE_DOCUMENTAL_MINIMO",
                safe.batchBlockingIssues().isEmpty(),
                !safe.batchBlockingIssues().isEmpty(),
                safe.batchBlockingIssues().isEmpty() ? "Leitura em lote contém o mínimo necessário." : String.join(" ", safe.batchBlockingIssues())
        ));
        gateDecisions.add(new GateDecision(
                "VERIFICADOR_SUBESPECIE_FINAL",
                safe.verifierBlockers().isEmpty(),
                !safe.verifierBlockers().isEmpty(),
                safe.verifierBlockers().isEmpty() ? "Verificador por subespécie liberou o fluxo." : String.join(" ", safe.verifierBlockers())
        ));
        gateDecisions.add(new GateDecision(
                "PREFLIGHT_ASSISTIDO",
                !safe.prepararPacoteProtocolo() || safe.assistiveReady(),
                safe.prepararPacoteProtocolo() && !safe.assistiveReady(),
                !safe.prepararPacoteProtocolo() || safe.assistiveReady()
                        ? "Preflight compatível com a etapa atual."
                        : "O pacote final exige preflight assistido apto."
        ));
        gateDecisions.add(new GateDecision(
                "PACOTE_PROTOCOLO_BASE",
                !safe.prepararPacoteProtocolo() || safe.protocolPackage() != null,
                safe.prepararPacoteProtocolo() && safe.protocolPackage() == null,
                !safe.prepararPacoteProtocolo() || safe.protocolPackage() != null
                        ? "Pacote base disponível para a etapa de submissão."
                        : "Ainda não existe pacote base de protocolo para esta sessão."
        ));
        gateDecisions.add(new GateDecision(
                "SIGILO_E_AUDITORIA",
                true,
                false,
                NivelSigilo.fromString(safe.sigilo()).exigeCredencial()
                        ? "Fluxo com trilha restrita de auditoria e need-to-know."
                        : "Fluxo em trilha padrão de auditoria."
        ));

        ArrayList<String> finalGates = new ArrayList<>();
        ArrayList<String> blocking = new ArrayList<>();
        ArrayList<Map<String, Object>> gates = new ArrayList<>();
        for (GateDecision gate : gateDecisions) {
            finalGates.add(gate.passed() ? gate.code() + "_OK" : gate.code() + "_PENDENTE");
            if (gate.blocking() && !gate.passed()) {
                blocking.add(gate.code());
            }
            LinkedHashMap<String, Object> item = new LinkedHashMap<>();
            item.put("code", gate.code());
            item.put("passed", gate.passed());
            item.put("blocking", gate.blocking());
            item.put("message", gate.message());
            gates.add(Map.copyOf(item));
        }

        LinkedHashMap<String, Object> manifest = new LinkedHashMap<>();
        put(manifest, "sessionKey", safe.sessionKey());
        put(manifest, "processoId", safe.processoId());
        put(manifest, "tituloCaso", safe.tituloCaso());
        put(manifest, "ramoDireito", safe.ramoDireito());
        put(manifest, "ritoProcessual", safe.ritoProcessual());
        put(manifest, "classeProcessual", safe.classeProcessual());
        put(manifest, "tipoJustica", safe.tipoJustica());
        put(manifest, "documentos", List.copyOf(safe.documentosAnexados()));
        put(manifest, "resolvedTrack", safe.verifierResolvedTrack());
        put(manifest, "batchProfile", safe.batchProfile());
        put(manifest, "verifierProfile", safe.verifierProfile());
        put(manifest, "payloadFingerprint", safe.payloadFingerprint());
        if (safe.protocolPackage() != null) {
            put(manifest, "protocolTitle", safe.protocolPackage().getTitle());
            put(manifest, "protocolIntegrityHash", safe.protocolPackage().getIntegrityHash());
            put(manifest, "protocolStatus", safe.protocolPackage().getStatus());
            put(manifest, "externalProtocolRef", safe.protocolPackage().getExternalProtocolRef());
        }

        ArrayList<Map<String, Object>> auditTrail = new ArrayList<>();
        auditTrail.add(audit("PAYLOAD_HARDENING", safe.payloadFingerprint() == null ? "PENDING" : "OK", "Fingerprint do payload incorporado ao envelope."));
        auditTrail.add(audit("BATCH_READING", safe.batchBlockingIssues().isEmpty() ? "OK" : "ATTENTION", safe.batchBlockingIssues().isEmpty()
                ? "Leitura em lote consolidada."
                : String.join(" ", safe.batchBlockingIssues())));
        auditTrail.add(audit("PROCEDURE_VERIFIER", safe.verifierBlockers().isEmpty() ? "OK" : "BLOCKED", safe.verifierBlockers().isEmpty()
                ? "Verificador procedimental finalizado."
                : String.join(" ", safe.verifierBlockers())));
        auditTrail.add(audit("SIGNATURE_AND_SUBMISSION", blocking.isEmpty() ? "READY" : "HELD", blocking.isEmpty()
                ? "Envelope final apto para assinatura/submissão."
                : "Envelope retido pelos gates: " + String.join(", ", blocking)));

        String deterministicHash = computeDeterministicHash(
                safe.sessionKey(),
                safe.processoId(),
                safe.tituloCaso(),
                safe.ramoDireito(),
                safe.ritoProcessual(),
                safe.classeProcessual(),
                safe.tipoJustica(),
                signatureMode,
                submissionMode,
                safe.resolvedInstrument(),
                safe.sigilo(),
                safe.payloadFingerprint(),
                safe.documentosAnexados(),
                safe.batchProfile(),
                safe.verifierProfile(),
                safe.verifierResolvedTrack(),
                finalGates,
                safe.protocolPackage()
        );

        LinkedHashMap<String, Object> strategicEnvelope = new LinkedHashMap<>();
        put(strategicEnvelope, "profile", blocking.isEmpty()
                ? "PETICIONAMENTO_PROTOCOL_ENVELOPE_HARDENING_V3"
                : "PETICIONAMENTO_PROTOCOL_ENVELOPE_HARDENING_HELD_V3");
        put(strategicEnvelope, "signatureMode", signatureMode);
        put(strategicEnvelope, "submissionMode", submissionMode);
        put(strategicEnvelope, "resolvedInstrument", safe.resolvedInstrument());
        put(strategicEnvelope, "sigilo", safe.sigilo());
        put(strategicEnvelope, "deterministicHash", deterministicHash);
        put(strategicEnvelope, "blocking", !blocking.isEmpty());
        put(strategicEnvelope, "blockingCodes", List.copyOf(new LinkedHashSet<>(blocking)));
        put(strategicEnvelope, "gates", List.copyOf(gates));
        put(strategicEnvelope, "manifest", Map.copyOf(manifest));
        put(strategicEnvelope, "auditTrail", List.copyOf(auditTrail));
        put(strategicEnvelope, "issuedAt", Instant.now().toString());

        return new EnvelopeReport(
                Map.copyOf(strategicEnvelope),
                List.copyOf(new LinkedHashSet<>(finalGates)),
                deterministicHash,
                !blocking.isEmpty()
        );
    }

    private String resolveSignatureMode(TipoUsuario tipoUsuario, String resolvedInstrument, String sigilo) {
        String base;
        if (tipoUsuario != null && (tipoUsuario.isDefensoriaPublica() || tipoUsuario.isProcuradoria() || tipoUsuario.isMinisterioPublico())) {
            base = "ASSINATURA_INSTITUCIONAL_CERTIFICADA";
        } else if (tipoUsuario != null && tipoUsuario.isAdvocacia()) {
            base = "ASSINATURA_ADVOCACIA_OAB";
        } else {
            base = "ASSINATURA_JURIDICA_QUALIFICADA";
        }
        if (resolvedInstrument != null && !resolvedInstrument.isBlank()) {
            base = base + "_" + normalize(resolvedInstrument).toUpperCase(Locale.ROOT);
        }
        if (NivelSigilo.fromString(sigilo).exigeCredencial()) {
            base = base + "_TRILHA_RESTRITA";
        }
        return base;
    }

    private String resolveSubmissionMode(boolean prepararPacoteProtocolo,
                                         LaianeProtocolPackageDto protocolPackage,
                                         String sigilo) {
        boolean restrito = NivelSigilo.fromString(sigilo).exigeCredencial();
        if (prepararPacoteProtocolo && protocolPackage != null) {
            return restrito ? "CONECTOR_JUDICIAL_RESTRITO_AUDITADO" : "CONECTOR_JUDICIAL_ASSISTIDO";
        }
        if (prepararPacoteProtocolo) {
            return restrito ? "PACOTE_PROTOCOLO_RESTRITO_PENDENTE" : "PACOTE_PROTOCOLO_PRONTO_PARA_SUBMISSAO";
        }
        return restrito ? "PREPARACAO_ASSINAVEL_RESTRITA" : "PREPARACAO_ASSINAVEL";
    }

    private Map<String, Object> audit(String stage, String status, String message) {
        LinkedHashMap<String, Object> item = new LinkedHashMap<>();
        item.put("stage", stage);
        item.put("status", status);
        item.put("message", message);
        return Map.copyOf(item);
    }

    private String computeDeterministicHash(String sessionKey,
                                            Long processoId,
                                            String tituloCaso,
                                            String ramoDireito,
                                            String ritoProcessual,
                                            String classeProcessual,
                                            String tipoJustica,
                                            String signatureMode,
                                            String submissionMode,
                                            String resolvedInstrument,
                                            String sigilo,
                                            String payloadFingerprint,
                                            List<String> documentos,
                                            String batchProfile,
                                            String verifierProfile,
                                            String verifierTrack,
                                            List<String> finalGates,
                                            LaianeProtocolPackageDto protocolPackage) {
        StringBuilder sb = new StringBuilder();
        append(sb, sessionKey);
        append(sb, processoId);
        append(sb, tituloCaso);
        append(sb, ramoDireito);
        append(sb, ritoProcessual);
        append(sb, classeProcessual);
        append(sb, tipoJustica);
        append(sb, signatureMode);
        append(sb, submissionMode);
        append(sb, resolvedInstrument);
        append(sb, sigilo);
        append(sb, payloadFingerprint);
        append(sb, batchProfile);
        append(sb, verifierProfile);
        append(sb, verifierTrack);
        if (documentos != null) {
            for (String documento : documentos) {
                append(sb, documento);
            }
        }
        if (finalGates != null) {
            for (String gate : finalGates) {
                append(sb, gate);
            }
        }
        if (protocolPackage != null) {
            append(sb, protocolPackage.getIntegrityHash());
            append(sb, protocolPackage.getExternalProtocolRef());
            append(sb, protocolPackage.getStatus());
            append(sb, protocolPackage.getTitle());
        }
        return Hashes.sha256Hex(sb.toString());
    }

    private static void put(Map<String, Object> target, String key, Object value) {
        if (target == null || key == null || key.isBlank() || value == null) {
            return;
        }
        if (value instanceof String text && text.isBlank()) {
            return;
        }
        if (value instanceof List<?> list && list.isEmpty()) {
            return;
        }
        target.put(key, value);
    }

    private static void append(StringBuilder sb, Object value) {
        if (value == null) {
            return;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append('|');
        }
        sb.append(text);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_");
        normalized = normalized.replaceAll("_+", "_");
        return normalized.replaceAll("^_|_$", "");
    }

    public record ResolveRequest(String sessionKey,
                                 Long processoId,
                                 String tituloCaso,
                                 String ramoDireito,
                                 String ritoProcessual,
                                 String classeProcessual,
                                 String tipoJustica,
                                 TipoUsuario tipoUsuario,
                                 String resolvedInstrument,
                                 String sigilo,
                                 boolean representacaoRegular,
                                 boolean prepararPacoteProtocolo,
                                 boolean assistiveReady,
                                 String payloadFingerprint,
                                 List<String> documentosAnexados,
                                 String batchProfile,
                                 List<String> batchBlockingIssues,
                                 String verifierProfile,
                                 String verifierResolvedTrack,
                                 List<String> verifierBlockers,
                                 LaianeProtocolPackageDto protocolPackage) {

        public ResolveRequest {
            documentosAnexados = immutableList(documentosAnexados);
            batchBlockingIssues = immutableList(batchBlockingIssues);
            verifierBlockers = immutableList(verifierBlockers);
        }

        static ResolveRequest empty() {
            return new ResolveRequest(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    NivelSigilo.PUBLICO.name(),
                    true,
                    false,
                    false,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    null,
                    null,
                    List.of(),
                    null
            );
        }

        private static List<String> immutableList(List<String> values) {
            if (values == null || values.isEmpty()) {
                return List.of();
            }
            ArrayList<String> out = new ArrayList<>();
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    String trimmed = value.trim();
                    if (!trimmed.isEmpty() && !out.contains(trimmed)) {
                        out.add(trimmed);
                    }
                }
            }
            return out.isEmpty() ? List.of() : List.copyOf(out);
        }
    }

    public record EnvelopeReport(Map<String, Object> strategicEnvelope,
                                 List<String> finalGates,
                                 String deterministicHash,
                                 boolean blocking) {
    }

    private record GateDecision(String code, boolean passed, boolean blocking, String message) {
    }
}
