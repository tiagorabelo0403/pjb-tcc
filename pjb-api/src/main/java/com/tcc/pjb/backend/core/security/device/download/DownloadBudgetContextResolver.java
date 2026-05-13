package com.tcc.pjb.backend.core.security.device.download;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DownloadBudgetContextResolver {

    private static final Pattern DOC_PDF = Pattern.compile("^/api/v1/documentos/([0-9a-fA-F-]{36})/pdf$");
    private static final Pattern PUBLIC_DOC_PDF = Pattern.compile("^/api/v1/public/processos/documentos/([0-9a-fA-F-]{36})/pdf$");
    private static final Pattern CIDADAO_PROC_DOC_PDF = Pattern.compile("^/api/v1/cidadao/processos/(\\d+)/documentos/([0-9a-fA-F-]{36})/pdf$");
    private static final Pattern PROC_DOC_PDF = Pattern.compile("^/api/v1/processos/(\\d+)/documentos/([0-9a-fA-F-]{36})/pdf$");
    private static final Pattern MINUTA_JUNTADA = Pattern.compile("^/api/v1/secretariat/processos/(\\d+)/minuta-juntada\\.pdf$");
    private static final Pattern RECURSAL_ATTACHMENT = Pattern.compile("^/api/v1/intelligence/recursal/processo/(\\d+)/attachments/[^/]+$");

    private DownloadBudgetContextResolver() {}

    public static Context resolve(String path) {
        if (path == null) return new Context(null, null);
        String p = path.trim();
        if (p.isEmpty()) return new Context(null, null);

        Matcher m1 = DOC_PDF.matcher(p);
        if (m1.matches()) {
            String doc = m1.group(1);
            return new Context(null, doc.toLowerCase(Locale.ROOT));
        }

        Matcher mPublic = PUBLIC_DOC_PDF.matcher(p);
        if (mPublic.matches()) {
            String doc = mPublic.group(1);
            return new Context(null, doc.toLowerCase(Locale.ROOT));
        }

        Matcher mCid = CIDADAO_PROC_DOC_PDF.matcher(p);
        if (mCid.matches()) {
            Long proc = parseLong(mCid.group(1));
            String doc = mCid.group(2);
            return new Context(proc, doc.toLowerCase(Locale.ROOT));
        }

        Matcher mProcDoc = PROC_DOC_PDF.matcher(p);
        if (mProcDoc.matches()) {
            Long proc = parseLong(mProcDoc.group(1));
            String doc = mProcDoc.group(2);
            return new Context(proc, doc.toLowerCase(Locale.ROOT));
        }

        Matcher m2 = MINUTA_JUNTADA.matcher(p);
        if (m2.matches()) {
            Long proc = parseLong(m2.group(1));
            return new Context(proc, null);
        }

        Matcher m3 = RECURSAL_ATTACHMENT.matcher(p);
        if (m3.matches()) {
            Long proc = parseLong(m3.group(1));
            return new Context(proc, null);
        }

        return new Context(null, null);
    }

    public static Context resolve(HttpServletRequest request) {
        if (request == null) return new Context(null, null);

        Long procAttr = attrLong(request.getAttribute("PJB_PROCESSO_ID"));
        String docAttr = attrString(request.getAttribute("PJB_DOCUMENTO_ID"));
        if (docAttr != null) docAttr = docAttr.toLowerCase(Locale.ROOT);

        String path = request.getRequestURI() != null ? request.getRequestURI() : "";
        Context fromPath = resolve(path);

        Long proc = firstNonNull(procAttr, fromPath.processoId(), parseLong(paramOrNull(request, "processoId")));
        String doc = firstNonBlank(docAttr, fromPath.documentoId(), paramOrNull(request, "documentoId"), paramOrNull(request, "docId"));
        if (doc != null) doc = doc.toLowerCase(Locale.ROOT);

        return new Context(proc, doc);
    }

    private static Long parseLong(String v) {
        try {
            if (v == null) return null;
            String s = v.trim();
            if (s.isEmpty()) return null;
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static String paramOrNull(HttpServletRequest request, String key) {
        try {
            if (request == null || key == null) return null;
            String v = request.getParameter(key);
            if (v == null) return null;
            String s = v.trim();
            return s.isEmpty() ? null : s;
        } catch (Exception e) {
            return null;
        }
    }

    private static Long attrLong(Object v) {
        if (v == null) return null;
        if (v instanceof Long l) return l;
        try {
            String s = String.valueOf(v).trim();
            if (s.isEmpty()) return null;
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static String attrString(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return null;
        if (s.length() > 80) s = s.substring(0, 80);
        return s;
    }

    private static Long firstNonNull(Long a, Long b, Long c) {
        if (a != null) return a;
        if (b != null) return b;
        return c;
    }

    private static String firstNonBlank(String a, String b, String c, String d) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        if (c != null && !c.isBlank()) return c;
        if (d != null && !d.isBlank()) return d;
        return null;
    }

    public record Context(Long processoId, String documentoId) {}
}
