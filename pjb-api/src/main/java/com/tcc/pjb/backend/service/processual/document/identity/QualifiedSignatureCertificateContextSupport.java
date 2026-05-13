package com.tcc.pjb.backend.service.processual.document.identity;

import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_ATTRIBUTE_NAMES;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_BRANCH_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_FINGERPRINT_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_ISSUER_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_PEM_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_ROLE_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_SCOPE_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_SERIAL_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_SUBJECT_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.defaultString;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.firstHeader;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.normalizeFingerprint;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.normalizeToNull;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.normalizeUpper;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.resolveCertificateFingerprint;

import com.tcc.pjb.backend.model.entity.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;

final class QualifiedSignatureCertificateContextSupport {

    private QualifiedSignatureCertificateContextSupport() {
    }

    static EntryCertificateContext resolveEntryCertificate(HttpServletRequest request,
                                                           Usuario usuario,
                                                           String papelDetalhado,
                                                           String ramoJustica,
                                                           String instancia,
                                                           String orgaoAssinante,
                                                           String esferaInstitucional,
                                                           Map<String, Object> lotacaoInstitucional,
                                                           String lotacaoAssinante) {
        Optional<X509Certificate> servletCertificate = extractServletCertificate(request);
        Optional<X509Certificate> pemCertificate = servletCertificate.isPresent() ? Optional.empty() : extractPemCertificate(request);
        X509Certificate certificate = servletCertificate.orElseGet(() -> pemCertificate.orElse(null));
        String source = servletCertificate.isPresent() ? "SERVLET_ATTRIBUTE" : pemCertificate.isPresent() ? "HEADER_PEM" : "ABSENT";
        LinkedHashMap<String, String> subjectAttributes = new LinkedHashMap<>();
        LinkedHashSet<String> roleHints = new LinkedHashSet<>();
        LinkedHashSet<String> jurisdictionHints = new LinkedHashSet<>();
        String subjectRfc2253 = "";
        String issuerRfc2253 = normalizeToNull(firstHeader(request, CERTIFICATE_ISSUER_HEADERS));
        String serial = normalizeToNull(firstHeader(request, CERTIFICATE_SERIAL_HEADERS));
        String fingerprint = normalizeFingerprint(firstHeader(request, CERTIFICATE_FINGERPRINT_HEADERS));
        String alias = usuario == null ? null : usuario.getEmail();
        String validityStart = null;
        String validityEnd = null;
        boolean validNow = false;
        if (certificate != null) {
            source = source.equals("ABSENT") ? "CERTIFICATE" : source;
            subjectRfc2253 = certificate.getSubjectX500Principal().getName(X500Principal.RFC2253);
            issuerRfc2253 = certificate.getIssuerX500Principal().getName(X500Principal.RFC2253);
            serial = certificate.getSerialNumber() == null ? serial : certificate.getSerialNumber().toString(16).toUpperCase();
            fingerprint = resolveCertificateFingerprint(certificate, fingerprint);
            subjectAttributes.putAll(parseDn(subjectRfc2253));
            validityStart = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(certificate.getNotBefore().toInstant().atZone(ZoneId.of("UTC")));
            validityEnd = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(certificate.getNotAfter().toInstant().atZone(ZoneId.of("UTC")));
            validNow = certificate.getNotBefore() != null && certificate.getNotAfter() != null
                    && certificate.getNotBefore().toInstant().isBefore(java.time.Instant.now())
                    && certificate.getNotAfter().toInstant().isAfter(java.time.Instant.now());
            collectSanHints(certificate, jurisdictionHints);
        } else {
            subjectRfc2253 = normalizeToNull(firstHeader(request, CERTIFICATE_SUBJECT_HEADERS));
            if (subjectRfc2253 != null) {
                source = "HEADER_DN";
                subjectAttributes.putAll(parseDn(subjectRfc2253));
            } else if (!fingerprint.isBlank() || (serial != null && !serial.isBlank())) {
                source = "HEADER_FINGERPRINT";
            }
        }
        subjectRfc2253 = defaultString(subjectRfc2253);
        issuerRfc2253 = defaultString(issuerRfc2253);
        serial = defaultString(serial);
        fingerprint = defaultString(fingerprint);

        String roleHeader = firstHeader(request, CERTIFICATE_ROLE_HEADERS);
        if (!roleHeader.isBlank()) {
            roleHints.add(normalizeUpper(roleHeader));
        }
        String branchHeader = firstHeader(request, CERTIFICATE_BRANCH_HEADERS);
        if (!branchHeader.isBlank()) {
            jurisdictionHints.add(normalizeUpper(branchHeader));
        }
        String scopeHeader = firstHeader(request, CERTIFICATE_SCOPE_HEADERS);
        if (!scopeHeader.isBlank()) {
            jurisdictionHints.add(normalizeUpper(scopeHeader));
        }
        inferRoleHints(subjectAttributes.values(), roleHints, jurisdictionHints);
        roleHints.add(papelDetalhado);
        jurisdictionHints.add(ramoJustica);
        jurisdictionHints.add(instancia);
        jurisdictionHints.add(orgaoAssinante);
        jurisdictionHints.add(esferaInstitucional);
        jurisdictionHints.add(lotacaoAssinante);
        if (usuario != null && usuario.getTipoUsuario() != null) {
            roleHints.add(usuario.getTipoUsuario().name());
        }
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("presente", certificate != null || !fingerprint.isBlank() || !subjectRfc2253.isBlank() || !serial.isBlank());
        map.put("source", source);
        map.put("alias", alias);
        map.put("subjectRfc2253", normalizeToNull(subjectRfc2253));
        map.put("issuerRfc2253", normalizeToNull(issuerRfc2253));
        map.put("serialNumber", normalizeToNull(serial));
        map.put("fingerprintSha256", normalizeToNull(fingerprint));
        map.put("subjectAttributes", Map.copyOf(subjectAttributes));
        map.put("roleHints", List.copyOf(roleHints));
        map.put("jurisdictionHints", List.copyOf(jurisdictionHints));
        map.put("validityStart", validityStart);
        map.put("validityEnd", validityEnd);
        map.put("validNow", validNow);
        map.put("lotacaoInstitucional", lotacaoInstitucional);
        map.put("lotacaoAssinante", lotacaoAssinante);
        map.put("entryBinding", "CERTIFICADO_ENTRADA_VINCULADO_AO_ATO");
        map.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return new EntryCertificateContext(
                Boolean.TRUE.equals(map.get("presente")),
                source,
                defaultString(fingerprint),
                defaultString(subjectRfc2253),
                defaultString(issuerRfc2253),
                defaultString(serial),
                Map.copyOf(subjectAttributes),
                List.copyOf(roleHints),
                List.copyOf(jurisdictionHints),
                Map.copyOf(map)
        );
    }

    static Optional<X509Certificate> extractServletCertificate(HttpServletRequest request) {
        if (request == null) {
            return Optional.empty();
        }
        for (String attributeName : CERTIFICATE_ATTRIBUTE_NAMES) {
            Object raw = request.getAttribute(attributeName);
            if (raw instanceof X509Certificate[] certificates && certificates.length > 0 && certificates[0] != null) {
                return Optional.of(certificates[0]);
            }
            if (raw instanceof X509Certificate certificate) {
                return Optional.of(certificate);
            }
        }
        return Optional.empty();
    }

    static Optional<X509Certificate> extractPemCertificate(HttpServletRequest request) {
        String pem = firstHeader(request, CERTIFICATE_PEM_HEADERS);
        if (pem.isBlank()) {
            return Optional.empty();
        }
        String normalized = pem.replace("%0A", "\n").replace("\\n", "\n").trim();
        try {
            if (normalized.contains("BEGIN CERTIFICATE")) {
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                return Optional.of((X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(normalized.getBytes(StandardCharsets.UTF_8))));
            }
            byte[] der = Base64.getMimeDecoder().decode(normalized);
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            return Optional.of((X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(der)));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    static Map<String, String> parseDn(String dn) {
        if (dn == null || dn.isBlank()) {
            return Map.of();
        }
        try {
            LinkedHashMap<String, String> out = new LinkedHashMap<>();
            LdapName ldapName = new LdapName(dn);
            for (Rdn rdn : ldapName.getRdns()) {
                String type = normalizeUpper(rdn.getType());
                String value = String.valueOf(rdn.getValue()).trim();
                if (!type.isBlank() && !value.isBlank() && !out.containsKey(type)) {
                    out.put(type, value);
                }
            }
            return Collections.unmodifiableMap(out);
        } catch (InvalidNameException ignored) {
            return Map.of();
        }
    }

    private static void inferRoleHints(Collection<String> attributeValues, Set<String> roleHints, Set<String> jurisdictionHints) {
        for (String value : attributeValues) {
            String token = normalizeUpper(value);
            if (token.contains("JUIZ ELEITORAL")) {
                roleHints.add("JUIZ_ELEITORAL");
                jurisdictionHints.add("ELEITORAL");
            }
            if (token.contains("JUIZ FEDERAL")) {
                roleHints.add("JUIZ_FEDERAL");
                jurisdictionHints.add("FEDERAL");
            }
            if (token.contains("JUIZ DO TRABALHO") || token.contains("JUIZ TRABALHISTA")) {
                roleHints.add("JUIZ_TRABALHISTA");
                jurisdictionHints.add("TRABALHO");
            }
            if (token.contains("JUIZADO ESPECIAL")) {
                roleHints.add("JUIZ_JUIZADO_ESPECIAL");
                jurisdictionHints.add("JUIZADO_ESPECIAL");
            }
            if (token.contains("JUIZ") && !token.contains("JUIZADO")) {
                roleHints.add("JUIZ");
            }
            if (token.contains("DESEMBARGADOR")) {
                roleHints.add("DESEMBARGADOR");
                jurisdictionHints.add("SEGUNDO_GRAU");
            }
            if (token.contains("MINISTRO")) {
                roleHints.add("MINISTRO");
            }
            if (token.contains("PROMOTOR")) {
                roleHints.add("PROMOTOR");
            }
            if (token.contains("PROCURADOR")) {
                roleHints.add("PROCURADOR");
            }
            if (token.contains("DEFENSOR")) {
                roleHints.add("DEFENSOR_PUBLICO");
            }
            if (token.contains("DELEGADO")) {
                roleHints.add("DELEGADO");
            }
            if (token.contains("OFICIAL DE JUSTICA") || token.contains("OFICIAL JUSTICA")) {
                roleHints.add("OFICIAL_JUSTICA");
            }
            if (token.contains("SECRETARIA") || token.contains("SECRETARIAT") || token.contains("SEAR")) {
                roleHints.add("SECRETARIA_JUDICIARIA");
            }
            if (token.contains("EMBARGOS")) {
                jurisdictionHints.add("EMBARGOS");
            }
            if (token.contains("ELEITORAL")) {
                jurisdictionHints.add("ELEITORAL");
            }
            if (token.contains("FEDERAL")) {
                jurisdictionHints.add("FEDERAL");
            }
            if (token.contains("ESTADUAL")) {
                jurisdictionHints.add("ESTADUAL");
            }
            if (token.contains("MUNICIPAL")) {
                jurisdictionHints.add("MUNICIPAL");
            }
            if (token.contains("TRABALHO") || token.contains("TRT") || token.contains("TST")) {
                jurisdictionHints.add("TRABALHO");
            }
            if (token.contains("TRE") || token.contains("TSE")) {
                jurisdictionHints.add("ELEITORAL");
            }
            if (token.contains("TRF") || token.contains("STJ")) {
                jurisdictionHints.add("FEDERAL");
            }
            if (token.contains("STF")) {
                jurisdictionHints.add("CONSTITUCIONAL");
            }
        }
    }

    private static void collectSanHints(X509Certificate certificate, Set<String> jurisdictionHints) {
        try {
            Collection<List<?>> san = certificate.getSubjectAlternativeNames();
            if (san == null) {
                return;
            }
            for (List<?> entry : san) {
                if (entry.size() < 2 || entry.get(1) == null) {
                    continue;
                }
                String token = normalizeUpper(String.valueOf(entry.get(1)));
                if (!token.isBlank()) {
                    jurisdictionHints.add(token);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
