package com.tcc.pjb.backend.service.recursal.routing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class RecursalCabinetAllocator {

    private final RecursalRoutingProperties properties;

    public RecursalCabinetAllocator(RecursalRoutingProperties properties) {
        this.properties = properties;
    }

    public CabinetSlot allocate(String court, Long caseFileId) {
        return allocate(court, caseFileId, null, null);
    }

    public CabinetSlot allocate(String court, Long caseFileId, String instanceTag, String preventionKey) {
        String normalizedCourt = normalizeCourt(court);
        String normalizedInstance = normalizeToken(instanceTag, "1G");
        String normalizedPrevention = normalizeToken(preventionKey, "SEM_PREVENCAO");
        long base = caseFileId != null ? caseFileId : 0L;
        int slots = properties.resolveCabinetSlots(normalizedCourt, normalizedInstance);
        int chamberBand = stableIndex(normalizedCourt + "|" + normalizedInstance, normalizedPrevention, Math.max(1, Math.min(12, Math.max(1, slots / 4)))) + 1;
        int slot = stableIndex(normalizedCourt + "|" + normalizedInstance, base + "|" + normalizedPrevention, slots) + 1;
        return new CabinetSlot(normalizedCourt, normalizedInstance, chamberBand, slot, slots);
    }

    public String inboxKeyForCabinet(CabinetSlot slot) {
        Objects.requireNonNull(slot, "slot é obrigatório");
        return "REC:INBOX:" + slot.court() + ":" + slot.instanceTag() + ":GAB_PREV:BAND_" + slot.chamberBand() + ":SLOT_" + slot.slot();
    }

    private static int stableIndex(String salt, Object payload, int slots) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest((salt + "|" + payload).getBytes(StandardCharsets.UTF_8));
            int hi = out[0] & 0xFF;
            int lo = out[1] & 0xFF;
            int value = (hi << 8) | lo;
            return Math.floorMod(value, Math.max(1, slots));
        } catch (Exception e) {
            return Math.floorMod(Objects.hash(salt, payload), Math.max(1, slots));
        }
    }

    private static String normalizeCourt(String court) {
        return normalizeToken(court, "UNKNOWN");
    }

    private static String normalizeToken(String raw, String fallback) {
        if (raw == null) {
            return fallback;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        return normalized.isBlank() ? fallback : normalized;
    }

    public record CabinetSlot(String court, String instanceTag, int chamberBand, int slot, int totalSlots) {

        public String descriptor() {
            return court + " " + instanceTag + " banda " + chamberBand + " slot " + slot + "/" + totalSlots;
        }
    }
}
