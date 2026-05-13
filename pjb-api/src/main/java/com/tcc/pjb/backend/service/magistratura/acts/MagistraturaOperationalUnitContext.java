package com.tcc.pjb.backend.service.magistratura.acts;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public record MagistraturaOperationalUnitContext(
        String justiceAxis,
        String tribunalAxis,
        String instanciaAxis,
        String regimeAxis,
        String deskAxis,
        String unidadeCodigo,
        String secretariatCode,
        String organizationalPath,
        String varaLabel,
        String orgaoLabel,
        String comarcaLabel,
        String authorityUnitBindingKey,
        String snapshotRoute
) {

    public static MagistraturaOperationalUnitContext resolve(Processo processo,
                                                             Usuario actor,
                                                             SecretariatOperationalRoutingProfile routingProfile,
                                                             MagistraturaAuthorityProjection authorityProjection) {
        boolean preferAuthorityUnit = authorityProjection != null && authorityProjection.scope() != null
                && !"PRIMEIRO_GRAU".equalsIgnoreCase(authorityProjection.scope());
        String unidadeCodigo = preferAuthorityUnit
                ? firstNonBlank(
                authorityProjection == null ? null : authorityProjection.authorityUnitCode(),
                routingProfile == null || routingProfile.metadata() == null ? null : stringValue(routingProfile.metadata().get("unidadeJudiciariaCodigo")),
                processo == null ? null : processo.getUnidadeJudiciariaCodigo(),
                routingProfile == null ? null : routingProfile.secretariatCode(),
                actor == null ? null : actor.getPerfil()
        )
                : firstNonBlank(
                processo == null ? null : processo.getUnidadeJudiciariaCodigo(),
                routingProfile == null || routingProfile.metadata() == null ? null : stringValue(routingProfile.metadata().get("unidadeJudiciariaCodigo")),
                authorityProjection == null ? null : authorityProjection.authorityUnitCode(),
                routingProfile == null ? null : routingProfile.secretariatCode(),
                actor == null ? null : actor.getPerfil()
        );
        String varaLabel = preferAuthorityUnit
                ? firstNonBlank(
                authorityProjection == null ? null : authorityProjection.authorityUnitLabel(),
                routingProfile == null || routingProfile.metadata() == null ? null : stringValue(routingProfile.metadata().get("vara")),
                processo == null ? null : processo.getVara(),
                routingProfile == null ? null : routingProfile.deskAxis()
        )
                : firstNonBlank(
                processo == null ? null : processo.getVara(),
                routingProfile == null || routingProfile.metadata() == null ? null : stringValue(routingProfile.metadata().get("vara")),
                authorityProjection == null ? null : authorityProjection.authorityUnitLabel(),
                routingProfile == null ? null : routingProfile.deskAxis()
        );
        String orgaoLabel = firstNonBlank(
                authorityProjection == null ? null : authorityProjection.orgaoLabel(),
                varaLabel,
                routingProfile == null ? null : routingProfile.deskAxis()
        );
        String comarcaLabel = firstNonBlank(
                processo == null ? null : processo.getComarca(),
                routingProfile == null || routingProfile.metadata() == null ? null : stringValue(routingProfile.metadata().get("comarca")),
                actor == null ? null : actor.getComarca()
        );
        String justiceAxis = firstNonBlank(
                authorityProjection == null ? null : authorityProjection.justiceAxis(),
                inferJusticeAxis(routingProfile == null ? null : routingProfile.tipoJustica()),
                "ESTADUAL"
        );
        String tribunalAxis = firstNonBlank(
                authorityProjection == null ? null : authorityProjection.tribunalAxis(),
                routingProfile == null ? null : routingProfile.tribunalCodigo(),
                processo == null ? null : processo.getTribunalCodigoRoteado(),
                "TJ"
        );
        String instanciaAxis = firstNonBlank(
                routingProfile == null ? null : routingProfile.instanciaAxis(),
                authorityProjection == null ? null : authorityProjection.scope(),
                "PRIMEIRO_GRAU"
        );
        String regimeAxis = routingProfile == null ? null : routingProfile.regimeAxis();
        String deskAxis = firstNonBlank(
                routingProfile == null ? null : routingProfile.deskAxis(),
                authorityProjection == null ? null : authorityProjection.authorityAxis(),
                authorityProjection == null ? null : authorityProjection.orgaoLabel()
        );
        String secretariatCode = firstNonBlank(
                routingProfile == null ? null : routingProfile.secretariatCode(),
                unidadeCodigo,
                deskAxis
        );
        String organizationalPath = firstNonBlank(
                routingProfile == null ? null : routingProfile.organizationalPath(),
                normalize(justiceAxis) + '>' + normalize(tribunalAxis) + '>' + normalize(instanciaAxis) + '>' + normalize(secretariatCode)
        );
        String authorityUnitBindingKey = String.join(">",
                normalize(justiceAxis),
                normalize(tribunalAxis),
                normalize(instanciaAxis),
                normalize(unidadeCodigo),
                normalize(varaLabel),
                normalize(secretariatCode)
        );
        String snapshotRoute = OperationalApiRoutes.withOperationalContext(
                OperationalApiRoutes.secretariatOperationalSnapshot(),
                contextParams(
                        null,
                        "SNAPSHOT",
                        justiceAxis,
                        tribunalAxis,
                        instanciaAxis,
                        deskAxis,
                        unidadeCodigo,
                        secretariatCode,
                        routingProfile == null ? null : routingProfile.receiptInboxKey(),
                        routingProfile == null ? null : routingProfile.receiptQueueCode(),
                        varaLabel,
                        orgaoLabel,
                        comarcaLabel,
                        organizationalPath,
                        authorityUnitBindingKey,
                        stageCellCode(secretariatCode, "SNAPSHOT", routingProfile == null ? null : routingProfile.receiptQueueCode())
                )
        );
        return new MagistraturaOperationalUnitContext(
                justiceAxis,
                tribunalAxis,
                instanciaAxis,
                regimeAxis,
                deskAxis,
                unidadeCodigo,
                secretariatCode,
                organizationalPath,
                varaLabel,
                orgaoLabel,
                comarcaLabel,
                authorityUnitBindingKey,
                snapshotRoute
        );
    }

    public String stageCellCode(String stageToken, String queueCode) {
        return stageCellCode(secretariatCode, stageToken, queueCode);
    }

    public String stageBindingKey(String stageToken, String inboxKey, String queueCode) {
        return authorityUnitBindingKey
                + '>' + normalize(stageToken)
                + '>' + normalize(inboxKey)
                + '>' + normalize(queueCode)
                + '>' + normalize(stageCellCode(stageToken, queueCode));
    }

    public String panelRoute(String baseRoute,
                             Long processoId,
                             String stageToken,
                             String inboxKey,
                             String queueCode) {
        return OperationalApiRoutes.withOperationalContext(
                baseRoute,
                contextParams(
                        processoId,
                        stageToken,
                        justiceAxis,
                        tribunalAxis,
                        instanciaAxis,
                        deskAxis,
                        unidadeCodigo,
                        secretariatCode,
                        inboxKey,
                        queueCode,
                        varaLabel,
                        orgaoLabel,
                        comarcaLabel,
                        organizationalPath,
                        stageBindingKey(stageToken, inboxKey, queueCode),
                        stageCellCode(stageToken, queueCode)
                )
        );
    }

    private static Map<String, Object> contextParams(Long processoId,
                                                     String stageToken,
                                                     String justiceAxis,
                                                     String tribunalAxis,
                                                     String instanciaAxis,
                                                     String deskAxis,
                                                     String unidadeCodigo,
                                                     String secretariatCode,
                                                     String inboxKey,
                                                     String queueCode,
                                                     String varaLabel,
                                                     String orgaoLabel,
                                                     String comarcaLabel,
                                                     String organizationalPath,
                                                     String bindingKey,
                                                     String cellCode) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("processoId", processoId);
        params.put("stage", stageToken);
        params.put("justica", justiceAxis);
        params.put("tribunal", tribunalAxis);
        params.put("instancia", instanciaAxis);
        params.put("desk", deskAxis);
        params.put("unidadeCodigo", unidadeCodigo);
        params.put("secretariaCodigo", secretariatCode);
        params.put("inboxKey", inboxKey);
        params.put("queueCode", queueCode);
        params.put("vara", varaLabel);
        params.put("orgao", orgaoLabel);
        params.put("comarca", comarcaLabel);
        params.put("organizationalPath", organizationalPath);
        params.put("bindingKey", bindingKey);
        params.put("cellCode", cellCode);
        return params;
    }

    private static String stageCellCode(String secretariatCode, String stageToken, String queueCode) {
        String base = normalize(firstNonBlank(secretariatCode, queueCode, "SECRETARIA"));
        String stage = normalize(stageToken);
        if ("AUDIENCIA".equals(stage)) {
            return base + "_PAUTA_AUDIENCIA";
        }
        if ("SANEAMENTO".equals(stage)) {
            return base + "_COMUNICACOES";
        }
        if ("RECEBIMENTO".equals(stage)) {
            return base + "_TRIAGEM";
        }
        if ("ADMISSIBILIDADE".equals(stage)) {
            return base + "_ADMISSIBILIDADE";
        }
        if ("PAUTA".equals(stage)) {
            return base + "_PAUTA_COLEGIADA";
        }
        if ("COLEGIADO".equals(stage)) {
            return base + "_SESSAO_COLEGIADA";
        }
        if ("ACORDAO".equals(stage)) {
            return base + "_ACORDAO_PUBLICACAO";
        }
        if ("EMBARGOS".equals(stage)) {
            return base + "_EMBARGOS_DECLARACAO";
        }
        return base + "_CUMPRIMENTO";
    }

    private static String inferJusticeAxis(String tipoJustica) {
        String normalized = normalize(tipoJustica);
        if (normalized.contains("FEDERAL")) {
            return "FEDERAL";
        }
        if (normalized.contains("SUPERIOR")) {
            return "SUPERIOR";
        }
        if (normalized.contains("ELEITORAL")) {
            return "ELEITORAL";
        }
        if (normalized.contains("TRABALHO")) {
            return "TRABALHO";
        }
        if (normalized.contains("MILITAR")) {
            return "MILITAR";
        }
        return "ESTADUAL";
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "BASE";
        }
        return raw.trim().toUpperCase(Locale.ROOT)
                .replace('Á', 'A').replace('À', 'A').replace('Ã', 'A').replace('Â', 'A')
                .replace('É', 'E').replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O').replace('Õ', 'O').replace('Ô', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C')
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
