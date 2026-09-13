package com.tcc.pjb.backend.core.peticionamento.blackbox;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

public final class PjbProtocolBlackBoxService {

    public PjbProtocolBlackBoxEnvelope open(String protocolIntentId, String processNumber, Instant openedAt) {
        PjbProtocolBlackBoxEnvelope envelope = new PjbProtocolBlackBoxEnvelope(protocolIntentId, processNumber, openedAt, List.of(), "", false);
        return append(envelope, new PjbProtocolBlackBoxEntry(PjbProtocolBlackBoxEventType.REQUEST_ACCEPTED, openedAt, "", hash(protocolIntentId + processNumber), "", java.util.Map.of()));
    }

    public PjbProtocolBlackBoxEnvelope append(PjbProtocolBlackBoxEnvelope envelope, PjbProtocolBlackBoxEntry entry) {
        Objects.requireNonNull(envelope, "envelope");
        Objects.requireNonNull(entry, "entry");
        if (envelope.sealed()) {
            return envelope;
        }
        List<PjbProtocolBlackBoxEntry> entries = new ArrayList<>(envelope.entries());
        entries.add(entry);
        String nextHash = hash(envelope.chainHash() + entry.type().name() + entry.occurredAt() + entry.payloadHash() + entry.connectorCode());
        return new PjbProtocolBlackBoxEnvelope(envelope.protocolIntentId(), envelope.processNumber(), envelope.openedAt(), entries, nextHash, false);
    }

    public PjbProtocolBlackBoxEnvelope seal(PjbProtocolBlackBoxEnvelope envelope) {
        Objects.requireNonNull(envelope, "envelope");
        return new PjbProtocolBlackBoxEnvelope(envelope.protocolIntentId(), envelope.processNumber(), envelope.openedAt(), envelope.entries(), envelope.chainHash(), true);
    }

    public String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(Objects.toString(value, "").getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
