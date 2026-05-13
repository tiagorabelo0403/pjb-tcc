package com.tcc.pjb.backend.service.consultapublica.jmu;

import java.text.Normalizer;
import java.util.*;

public final class JmuSqlCompiler {

    public record Compiled(String sql,
                           Map<String, Object> params,
                           boolean usesTsQuery,
                           List<String> rankTokens,
                           boolean simpleRoot) {}

    private static final int MAX_TOKEN_LEN = 64;

    private final Map<String, Object> params = new LinkedHashMap<>();
    private final List<String> rankTokens = new ArrayList<>();
    private int seq = 0;

    public Compiled compile(JmuQueryAst ast, String pageAlias) {
        boolean simple = (ast instanceof JmuQueryAst.Term) || (ast instanceof JmuQueryAst.Phrase) || (ast instanceof JmuQueryAst.Proximity);
        SqlExpr r = compileNode(ast, pageAlias, false);
        return new Compiled(r.sql, params, r.usesTs, List.copyOf(rankTokens), simple);
    }

    private SqlExpr compileNode(JmuQueryAst node, String pg, boolean insideNot) {
        if (node instanceof JmuQueryAst.And a) {
            SqlExpr l = compileNode(a.left(), pg, insideNot);
            SqlExpr r = compileNode(a.right(), pg, insideNot);
            return new SqlExpr("(" + l.sql + " AND " + r.sql + ")", l.usesTs || r.usesTs);
        }
        if (node instanceof JmuQueryAst.Or o) {
            SqlExpr l = compileNode(o.left(), pg, insideNot);
            SqlExpr r = compileNode(o.right(), pg, insideNot);
            return new SqlExpr("(" + l.sql + " OR " + r.sql + ")", l.usesTs || r.usesTs);
        }
        if (node instanceof JmuQueryAst.Not n) {
            SqlExpr c = compileNode(n.child(), pg, true);
            return new SqlExpr("(NOT (" + c.sql + "))", c.usesTs);
        }
        if (node instanceof JmuQueryAst.Term t) {
            String raw = safeToken(t.raw());
            if (!insideNot && !raw.isBlank()) rankTokens.add(raw);

            if (containsWildcard(raw)) {
                String p = nextParam("w");
                params.put(p, toLikePattern(raw));
                return new SqlExpr("(lower(" + pg + ".texto_extraido) LIKE :" + p + " ESCAPE '\\\\')", false);
            }

            String ts = nextParam("ts");
            params.put(ts, raw);
            String lk = nextParam("lk");
            params.put(lk, "%" + escapeLike(raw.toLowerCase()) + "%");
            String sql = "(" + pg + ".texto_tsv @@ plainto_tsquery('portuguese', :" + ts + ")" +
                    " OR lower(" + pg + ".texto_extraido) LIKE :" + lk + " ESCAPE '\\\\')";
            return new SqlExpr(sql, true);
        }
        if (node instanceof JmuQueryAst.Phrase ph) {
            String raw = safePhrase(ph.raw());
            if (!insideNot && !raw.isBlank()) rankTokens.add(raw);

            String ts = nextParam("ph");
            params.put(ts, raw);
            String lk = nextParam("lk");
            params.put(lk, "%" + escapeLike(raw.toLowerCase()) + "%");
            String sql = "(" + pg + ".texto_tsv @@ phraseto_tsquery('portuguese', :" + ts + ")" +
                    " OR lower(" + pg + ".texto_extraido) LIKE :" + lk + " ESCAPE '\\\\')";
            return new SqlExpr(sql, true);
        }
        if (node instanceof JmuQueryAst.Proximity px) {
            String left = safeToken(px.left());
            String right = safeToken(px.right());
            int d = Math.max(1, Math.min(px.distance(), 50));

            if (!insideNot) {
                if (!left.isBlank()) rankTokens.add(left);
                if (!right.isBlank()) rankTokens.add(right);
            }

            String q = left + " <" + d + "> " + right;
            String ts = nextParam("px");
            params.put(ts, q);
            String sql = "(" + pg + ".texto_tsv @@ to_tsquery('portuguese', :" + ts + "))";
            return new SqlExpr(sql, true);
        }

        
        return new SqlExpr("(1=0)", false);
    }

    private String nextParam(String prefix) {
        seq++;
        return prefix + seq;
    }

    private static boolean containsWildcard(String s) {
        return s != null && (s.indexOf('*') >= 0 || s.indexOf('?') >= 0);
    }

    
    private static String safeToken(String raw) {
        if (raw == null) return "";
        String n = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        n = n.replaceAll("[^0-9A-Za-z_\\*\\?]+", " ");
        n = n.trim().replaceAll("\\s+", " ");
        if (n.length() > MAX_TOKEN_LEN) n = n.substring(0, MAX_TOKEN_LEN);
        return n;
    }

    private static String safePhrase(String raw) {
        if (raw == null) return "";
        String n = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        n = n.replaceAll("[^0-9A-Za-z_\\s]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
        if (n.length() > 240) n = n.substring(0, 240);
        return n;
    }

    private static String toLikePattern(String raw) {
        if (raw == null) return "%";
        String s = raw.toLowerCase();
        s = escapeLike(s);
        s = s.replace("*", "%").replace("?", "_");
        if (!s.startsWith("%")) s = "%" + s;
        if (!s.endsWith("%")) s = s + "%";
        return s;
    }

    private static String escapeLike(String s) {
        if (s == null) return "";
        
        return s.replace("\\", "\\\\")
                .replace("%", "\\\\%")
                .replace("_", "\\\\_");
    }

    private record SqlExpr(String sql, boolean usesTs) {}
}
