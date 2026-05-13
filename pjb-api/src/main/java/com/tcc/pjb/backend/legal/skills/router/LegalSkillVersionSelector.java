package com.tcc.pjb.backend.legal.skills.router;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import com.tcc.pjb.backend.legal.skills.contract.LegalSkillRequestContract;
import com.tcc.pjb.backend.legal.skills.contract.LegalSkillResponseContract;
import com.tcc.pjb.backend.legal.skills.v1.LegalSkillRegistryV1;
import com.tcc.pjb.backend.legal.skills.v1.LegalSkillRequestV1;
import com.tcc.pjb.backend.legal.skills.v2.LegalSkillRegistryV2;
import com.tcc.pjb.backend.legal.skills.v2.LegalSkillRequestV2;
import com.tcc.pjb.backend.legal.skills.v3.LegalSkillRegistryV3;
import com.tcc.pjb.backend.legal.skills.v3.LegalSkillRequestV3;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;

public final class LegalSkillVersionSelector {

    private final LegalSkillRegistryV1 v1;
    private final LegalSkillRegistryV2 v2;
    private final LegalSkillRegistryV3 v3;

    public LegalSkillVersionSelector(LegalSkillRegistryV1 v1, LegalSkillRegistryV2 v2, LegalSkillRegistryV3 v3) {
        this.v1 = Objects.requireNonNull(v1, "v1");
        this.v2 = Objects.requireNonNull(v2, "v2");
        this.v3 = Objects.requireNonNull(v3, "v3");
    }

    public ApiVersion resolveVersion(LegalSkillRequestContract request) {
        if (request == null) return ApiVersion.latest();
        ApiVersion fromContract = ApiVersion.tryParse(request.getContractVersion()).orElse(null);
        if (fromContract != null) return fromContract;
        return ApiVersion.inferFromToken(request.getSkill()).orElse(ApiVersion.latest());
    }

    public LegalSkillResponseContract execute(LegalSkillRequestContract request, Map<String, Object> context) {
        ApiVersion version = resolveVersion(request);
        Map<String, Object> ctx = context != null ? context : Collections.emptyMap();

        return switch (version) {
            case V1 -> {
                LegalSkillRequestV1 r = adaptToV1(request);
                yield v1.execute(r, ctx);
            }
            case V2 -> {
                LegalSkillRequestV2 r = adaptToV2(request);
                yield v2.execute(r, ctx);
            }
            case V3 -> {
                LegalSkillRequestV3 r = adaptToV3(request);
                yield v3.execute(r, ctx);
            }
        };
    }

    private static LegalSkillRequestV1 adaptToV1(LegalSkillRequestContract request) {
        if (request instanceof LegalSkillRequestV1 r) {
            return ensureVersionedSkill(r, ApiVersion.V1);
        }
        if (request == null) {
            return LegalSkillRequestV1.builder().withSkill("UNKNOWN_V1").build();
        }
        return ensureVersionedSkill(
                LegalSkillRequestV1.builder()
                        .withRequestId(request.getRequestId())
                        .withCorrelationId(request.getCorrelationId())
                        .withTenantId(request.getTenantId())
                        .withUsuarioId(request.getUsuarioId())
                        .withSkill(request.getSkill())
                        .withContractVersion("v1")
                        .withTimestamp(request.getTimestamp())
                        .withPayload(request.getPayload())
                        .build(),
                ApiVersion.V1
        );
    }

    private static LegalSkillRequestV2 adaptToV2(LegalSkillRequestContract request) {
        if (request instanceof LegalSkillRequestV2 r) {
            return ensureVersionedSkill(r, ApiVersion.V2);
        }
        if (request == null) {
            return LegalSkillRequestV2.builder().withSkill("UNKNOWN_V2").build();
        }
        return ensureVersionedSkill(
                LegalSkillRequestV2.builder()
                        .withRequestId(request.getRequestId())
                        .withCorrelationId(request.getCorrelationId())
                        .withTenantId(request.getTenantId())
                        .withUsuarioId(request.getUsuarioId())
                        .withSkill(request.getSkill())
                        .withContractVersion("v2")
                        .withTimestamp(request.getTimestamp())
                        .withPayload(request.getPayload())
                        .build(),
                ApiVersion.V2
        );
    }

    private static LegalSkillRequestV3 adaptToV3(LegalSkillRequestContract request) {
        if (request instanceof LegalSkillRequestV3 r) {
            return ensureVersionedSkill(r, ApiVersion.V3);
        }
        if (request == null) {
            return LegalSkillRequestV3.builder().withSkill("UNKNOWN_V3").build();
        }
        return ensureVersionedSkill(
                LegalSkillRequestV3.builder()
                        .withRequestId(request.getRequestId())
                        .withCorrelationId(request.getCorrelationId())
                        .withTenantId(request.getTenantId())
                        .withUsuarioId(request.getUsuarioId())
                        .withSkill(request.getSkill())
                        .withContractVersion("v3")
                        .withTimestamp(request.getTimestamp())
                        .withPayload(request.getPayload())
                        .build(),
                ApiVersion.V3
        );
    }

    private static LegalSkillRequestV1 ensureVersionedSkill(LegalSkillRequestV1 req, ApiVersion v) {
        String normalized = normalizeSkill(req != null ? req.getSkill() : null, v);
        if (req == null) {
            return LegalSkillRequestV1.builder().withSkill(normalized).build();
        }
        if (normalized.equalsIgnoreCase(req.getSkill())) {
            return req;
        }
        return LegalSkillRequestV1.builder()
                .withRequestId(req.getRequestId())
                .withCorrelationId(req.getCorrelationId())
                .withTenantId(req.getTenantId())
                .withUsuarioId(req.getUsuarioId())
                .withSkill(normalized)
                .withContractVersion(req.getContractVersion())
                .withTimestamp(req.getTimestamp())
                .withPayload(req.getPayload())
                .build();
    }

    private static LegalSkillRequestV2 ensureVersionedSkill(LegalSkillRequestV2 req, ApiVersion v) {
        String normalized = normalizeSkill(req != null ? req.getSkill() : null, v);
        if (req == null) {
            return LegalSkillRequestV2.builder().withSkill(normalized).build();
        }
        if (normalized.equalsIgnoreCase(req.getSkill())) {
            return req;
        }
        return LegalSkillRequestV2.builder()
                .withRequestId(req.getRequestId())
                .withCorrelationId(req.getCorrelationId())
                .withTenantId(req.getTenantId())
                .withUsuarioId(req.getUsuarioId())
                .withSkill(normalized)
                .withContractVersion(req.getContractVersion())
                .withTimestamp(req.getTimestamp())
                .withPayload(req.getPayload())
                .build();
    }

    private static LegalSkillRequestV3 ensureVersionedSkill(LegalSkillRequestV3 req, ApiVersion v) {
        String normalized = normalizeSkill(req != null ? req.getSkill() : null, v);
        if (req == null) {
            return LegalSkillRequestV3.builder().withSkill(normalized).build();
        }
        if (normalized.equalsIgnoreCase(req.getSkill())) {
            return req;
        }
        return LegalSkillRequestV3.builder()
                .withRequestId(req.getRequestId())
                .withCorrelationId(req.getCorrelationId())
                .withTenantId(req.getTenantId())
                .withUsuarioId(req.getUsuarioId())
                .withSkill(normalized)
                .withContractVersion(req.getContractVersion())
                .withTimestamp(req.getTimestamp())
                .withPayload(req.getPayload())
                .build();
    }

    private static String normalizeSkill(String raw, ApiVersion v) {
        String base = (raw == null || raw.isBlank()) ? "UNKNOWN" : raw.trim();
        String up = base.toUpperCase(Locale.ROOT);
        if (ApiVersion.inferFromToken(up).isPresent()) {
            
            return up;
        }
        return up + "_V" + v.major();
    }

    public LegalSkillRegistryV1 registryV1() { return v1; }

    public LegalSkillRegistryV2 registryV2() { return v2; }

    public LegalSkillRegistryV3 registryV3() { return v3; }
}
