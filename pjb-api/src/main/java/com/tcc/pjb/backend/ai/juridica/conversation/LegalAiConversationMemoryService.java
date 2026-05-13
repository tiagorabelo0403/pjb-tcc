package com.tcc.pjb.backend.ai.juridica.conversation;

import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationApprovalSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationMemorySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationTraceSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiMemoryScopeDescriptor;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class LegalAiConversationMemoryService {

    private static final int DEFAULT_MAX_TURNS = 12;
    private final ConcurrentHashMap<String, ConversationSession> sessions = new ConcurrentHashMap<>();

    public LegalAiConversationMemorySnapshot snapshot(String conversationId,
                                                      LegalAiConversationRequest request,
                                                      LegalAiMemoryScopeDescriptor descriptor) {
        String resolvedConversationId = normalizeConversationId(conversationId);
        ConversationSession session = sessions.compute(resolvedConversationId, (key, current) -> openSession(key, request, descriptor, current));
        return toSnapshot(session, descriptor);
    }

    public LegalAiConversationMemorySnapshot registerTurn(String conversationId,
                                                          LegalAiConversationRequest request,
                                                          String capability,
                                                          String version,
                                                          String answer,
                                                          LegalValidationResponse validation,
                                                          LegalHallucinationGuardResponse guard,
                                                          LegalAiConversationApprovalSnapshot approval,
                                                          LegalAiConversationTraceSnapshot trace,
                                                          List<Map<String, Object>> council,
                                                          LegalAiMemoryScopeDescriptor descriptor) {
        String resolvedConversationId = normalizeConversationId(conversationId);
        ConversationSession session = sessions.compute(resolvedConversationId, (key, current) -> {
            ConversationSession active = openSession(key, request, descriptor, current);
            appendTurn(active, request, capability, version, answer, validation, guard, approval, trace, council, descriptor);
            return active;
        });
        return toSnapshot(session, descriptor);
    }

    private ConversationSession openSession(String conversationId,
                                            LegalAiConversationRequest request,
                                            LegalAiMemoryScopeDescriptor descriptor,
                                            ConversationSession current) {
        Instant now = Instant.now();
        if (current == null || isExpired(current, descriptor, now) || profileChanged(current, request, descriptor)) {
            return newSession(conversationId, request, now);
        }
        current.lastAccessAt = now;
        if (descriptor != null && descriptor.crossCaseReuseBlocked() && processChanged(current, request)) {
            current.processoId = request == null ? null : blankToNull(request.processoId());
            current.processMemory.clear();
            hydrateProcessScope(current.processMemory, request);
            current.turns.removeIf(turn -> turn.processoId() != null && current.processoId != null && !Objects.equals(turn.processoId(), current.processoId));
        }
        hydrateProfileScope(current.profileMemory, request);
        hydrateInstitutionalScope(current.institutionalMemory, request);
        return current;
    }

    private ConversationSession newSession(String conversationId, LegalAiConversationRequest request, Instant now) {
        ConversationSession session = new ConversationSession();
        session.conversationId = conversationId;
        session.processoId = request == null ? null : blankToNull(request.processoId());
        session.userProfile = request == null ? null : blankToNull(request.userProfile());
        session.createdAt = now;
        session.lastAccessAt = now;
        hydrateProfileScope(session.profileMemory, request);
        hydrateProcessScope(session.processMemory, request);
        hydrateInstitutionalScope(session.institutionalMemory, request);
        return session;
    }

    private void appendTurn(ConversationSession session,
                            LegalAiConversationRequest request,
                            String capability,
                            String version,
                            String answer,
                            LegalValidationResponse validation,
                            LegalHallucinationGuardResponse guard,
                            LegalAiConversationApprovalSnapshot approval,
                            LegalAiConversationTraceSnapshot trace,
                            List<Map<String, Object>> council,
                            LegalAiMemoryScopeDescriptor descriptor) {
        session.revision = session.revision + 1;
        session.lastAccessAt = Instant.now();
        hydrateSessionScope(session.sessionMemory, request, capability, version, trace);
        hydrateProcessScope(session.processMemory, request);
        hydrateProfileScope(session.profileMemory, request);
        hydrateInstitutionalScope(session.institutionalMemory, request);
        session.turns.addLast(new StoredTurn(
                trace == null ? null : trace.turnId(),
                trace == null ? null : trace.traceId(),
                blankToNull(request == null ? null : request.processoId()),
                Instant.now(),
                blankToNull(version),
                blankToNull(capability),
                blankToNull(request == null ? null : request.message()),
                excerpt(answer),
                approval == null ? null : approval.status(),
                guard == null ? null : guard.status(),
                validation == null || validation.trace() == null ? null : stringValue(validation.trace().get("symbolicExecutionStatus")),
                request == null || request.attachments() == null ? List.of() : List.copyOf(request.attachments()),
                validation == null || validation.contradictions() == null ? List.of() : List.copyOf(validation.contradictions()),
                validation == null || validation.missingEvidence() == null ? List.of() : List.copyOf(validation.missingEvidence()),
                council == null ? List.of() : council.stream().map(Map::copyOf).toList()
        ));
        trimTurns(session.turns, maxTurns(descriptor));
    }

    private LegalAiConversationMemorySnapshot toSnapshot(ConversationSession session, LegalAiMemoryScopeDescriptor descriptor) {
        LinkedHashMap<String, Object> scopedMemory = new LinkedHashMap<>();
        List<String> enabledScopes = descriptor == null || descriptor.enabledScopes() == null ? List.of() : descriptor.enabledScopes();
        if (enabledScopes.contains("SESSAO")) {
            scopedMemory.put("SESSAO", ImmutableViewSupport.map(session.sessionMemory));
        }
        if (enabledScopes.contains("PROCESSO")) {
            scopedMemory.put("PROCESSO", ImmutableViewSupport.map(session.processMemory));
        }
        if (enabledScopes.contains("PERFIL")) {
            scopedMemory.put("PERFIL", ImmutableViewSupport.map(session.profileMemory));
        }
        if (enabledScopes.contains("INSTITUTIONAL")) {
            scopedMemory.put("INSTITUTIONAL", ImmutableViewSupport.map(session.institutionalMemory));
        }
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("strictIsolation", descriptor != null && descriptor.strictIsolation());
        diagnostics.put("crossCaseReuseBlocked", descriptor != null && descriptor.crossCaseReuseBlocked());
        diagnostics.put("sessionTtlMinutes", sessionTtlMinutes(descriptor));
        diagnostics.put("retainedTurnCount", session.turns.size());
        diagnostics.put("sessionRevision", session.revision);
        diagnostics.put("createdAt", session.createdAt == null ? null : session.createdAt.toString());
        diagnostics.put("lastAccessAt", session.lastAccessAt == null ? null : session.lastAccessAt.toString());
        diagnostics.put("enabledScopes", enabledScopes);
        return new LegalAiConversationMemorySnapshot(
                session.conversationId,
                session.processoId,
                session.userProfile,
                session.turns.stream().map(this::turnAsMap).toList(),
                ImmutableViewSupport.map(scopedMemory),
                ImmutableViewSupport.map(diagnostics)
        );
    }

    private Map<String, Object> turnAsMap(StoredTurn turn) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("turnId", turn.turnId());
        out.put("traceId", turn.traceId());
        out.put("processoId", turn.processoId());
        out.put("createdAt", turn.createdAt() == null ? null : turn.createdAt().toString());
        out.put("version", turn.version());
        out.put("capability", turn.capability());
        out.put("message", turn.message());
        out.put("answerExcerpt", turn.answerExcerpt());
        out.put("approvalStatus", turn.approvalStatus());
        out.put("hallucinationStatus", turn.hallucinationStatus());
        out.put("symbolicExecutionStatus", turn.symbolicExecutionStatus());
        out.put("attachments", turn.attachments());
        out.put("contradictions", turn.contradictions());
        out.put("missingEvidence", turn.missingEvidence());
        out.put("virtualTrends", turn.council().stream().map(item -> item.get("virtualTrend")).toList());
        return ImmutableViewSupport.map(out);
    }

    private void hydrateSessionScope(LinkedHashMap<String, Object> sessionMemory,
                                     LegalAiConversationRequest request,
                                     String capability,
                                     String version,
                                     LegalAiConversationTraceSnapshot trace) {
        if (request != null && request.history() != null && !request.history().isEmpty()) {
            sessionMemory.put("historyWindow", List.copyOf(request.history().stream().limit(6).toList()));
        }
        sessionMemory.put("lastMessage", blankToNull(request == null ? null : request.message()));
        sessionMemory.put("lastCapability", blankToNull(capability));
        sessionMemory.put("lastVersion", blankToNull(version));
        sessionMemory.put("lastTraceId", trace == null ? null : trace.traceId());
        sessionMemory.put("lastTurnId", trace == null ? null : trace.turnId());
    }

    private void hydrateProcessScope(LinkedHashMap<String, Object> processMemory, LegalAiConversationRequest request) {
        processMemory.put("processoId", blankToNull(request == null ? null : request.processoId()));
        processMemory.put("ramo", contextValue(request, "ramo"));
        processMemory.put("rito", contextValue(request, "rito"));
        processMemory.put("classe", contextValue(request, "classe"));
        processMemory.put("sigilo", contextValue(request, "sigilo"));
    }

    private void hydrateProfileScope(LinkedHashMap<String, Object> profileMemory, LegalAiConversationRequest request) {
        profileMemory.put("userProfile", blankToNull(request == null ? null : request.userProfile()));
        profileMemory.put("objetivo", contextValue(request, "objetivo"));
        profileMemory.put("densidade", contextValue(request, "densidade"));
    }

    private void hydrateInstitutionalScope(LinkedHashMap<String, Object> institutionalMemory, LegalAiConversationRequest request) {
        institutionalMemory.put("institutionId", firstContextValue(request, List.of("institutionId", "orgaoId", "unidadeId")));
        institutionalMemory.put("institutionRole", firstContextValue(request, List.of("institutionRole", "orgaoTipo", "lotacao")));
        institutionalMemory.put("competencia", firstContextValue(request, List.of("competencia", "segmento", "tribunal")));
    }

    private boolean isExpired(ConversationSession session, LegalAiMemoryScopeDescriptor descriptor, Instant now) {
        Instant expiresAt = session.lastAccessAt == null ? now : session.lastAccessAt.plus(sessionTtlMinutes(descriptor), ChronoUnit.MINUTES);
        return now.isAfter(expiresAt);
    }

    private boolean profileChanged(ConversationSession session, LegalAiConversationRequest request, LegalAiMemoryScopeDescriptor descriptor) {
        return descriptor != null
                && descriptor.strictIsolation()
                && !Objects.equals(session.userProfile, blankToNull(request == null ? null : request.userProfile()));
    }

    private boolean processChanged(ConversationSession session, LegalAiConversationRequest request) {
        return blankToNull(request == null ? null : request.processoId()) != null
                && !Objects.equals(session.processoId, blankToNull(request == null ? null : request.processoId()));
    }

    private long sessionTtlMinutes(LegalAiMemoryScopeDescriptor descriptor) {
        Object ttl = descriptor == null || descriptor.memoryPolicy() == null ? null : descriptor.memoryPolicy().get("sessionTtlMinutes");
        if (ttl instanceof Number number) {
            return Math.max(5L, number.longValue());
        }
        return 20L;
    }

    private int maxTurns(LegalAiMemoryScopeDescriptor descriptor) {
        Object retainedTurnWindow = descriptor == null || descriptor.memoryPolicy() == null ? null : descriptor.memoryPolicy().get("retainedTurnWindow");
        if (retainedTurnWindow instanceof Number number) {
            return Math.max(4, number.intValue());
        }
        int scopeCount = descriptor == null || descriptor.enabledScopes() == null ? 0 : descriptor.enabledScopes().size();
        return Math.max(DEFAULT_MAX_TURNS, scopeCount * 3);
    }

    private void trimTurns(Deque<StoredTurn> turns, int maxTurns) {
        while (turns.size() > maxTurns) {
            turns.removeFirst();
        }
    }

    private String normalizeConversationId(String conversationId) {
        String value = blankToNull(conversationId);
        return value == null ? java.util.UUID.randomUUID().toString() : value;
    }

    private String contextValue(LegalAiConversationRequest request, String key) {
        return request == null || request.context() == null ? null : stringValue(request.context().get(key));
    }

    private String firstContextValue(LegalAiConversationRequest request, List<String> keys) {
        if (request == null || request.context() == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            String value = stringValue(request.context().get(key));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String excerpt(String value) {
        String text = blankToNull(value);
        if (text == null) {
            return null;
        }
        return text.length() <= 240 ? text : text.substring(0, 240) + "...";
    }

    private static final class ConversationSession {
        private String conversationId;
        private String processoId;
        private String userProfile;
        private Instant createdAt;
        private Instant lastAccessAt;
        private long revision;
        private final LinkedHashMap<String, Object> sessionMemory = new LinkedHashMap<>();
        private final LinkedHashMap<String, Object> processMemory = new LinkedHashMap<>();
        private final LinkedHashMap<String, Object> profileMemory = new LinkedHashMap<>();
        private final LinkedHashMap<String, Object> institutionalMemory = new LinkedHashMap<>();
        private final ArrayDeque<StoredTurn> turns = new ArrayDeque<>();
    }

    private record StoredTurn(String turnId,
                              String traceId,
                              String processoId,
                              Instant createdAt,
                              String version,
                              String capability,
                              String message,
                              String answerExcerpt,
                              String approvalStatus,
                              String hallucinationStatus,
                              String symbolicExecutionStatus,
                              List<String> attachments,
                              List<String> contradictions,
                              List<String> missingEvidence,
                              List<Map<String, Object>> council) {
    }
}
