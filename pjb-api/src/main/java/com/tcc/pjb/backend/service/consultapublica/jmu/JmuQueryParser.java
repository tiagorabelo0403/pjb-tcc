package com.tcc.pjb.backend.service.consultapublica.jmu;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;

public final class JmuQueryParser {

    private static final int MAX_LEN = 512;
    private static final int MAX_TOKENS = 80;
    private static final int MAX_DEPTH = 24;

    public JmuQueryAst parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "q")
                    .addMetadado("motivo", "consulta vazia");
        }
        String trimmed = raw.trim();
        if (trimmed.length() > MAX_LEN) {
            throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "q")
                    .addMetadado("motivo", "consulta muito longa")
                    .addMetadado("limite", MAX_LEN);
        }

        List<Token> tokens = tokenize(trimmed);
        if (tokens.size() > MAX_TOKENS) {
            throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "q")
                    .addMetadado("motivo", "consulta muito complexa")
                    .addMetadado("limite_tokens", MAX_TOKENS);
        }
        Parser p = new Parser(tokens);
        JmuQueryAst ast = p.parseExpression();
        if (p.hasMore()) {
            throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "q")
                    .addMetadado("motivo", "tokens excedentes na consulta")
                    .addMetadado("token", p.peek().kind.name());
        }
        return ast;
    }

    

    private static List<Token> tokenize(String s) {
        List<Token> out = new ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (c == '(') {
                out.add(new Token(Kind.LPAREN, "("));
                i++;
                continue;
            }
            if (c == ')') {
                out.add(new Token(Kind.RPAREN, ")"));
                i++;
                continue;
            }
            if (c == '"') {
                int j = i + 1;
                StringBuilder buf = new StringBuilder();
                boolean escaped = false;
                while (j < s.length()) {
                    char cj = s.charAt(j);
                    if (escaped) {
                        buf.append(cj);
                        escaped = false;
                        j++;
                        continue;
                    }
                    if (cj == '\\') {
                        escaped = true;
                        j++;
                        continue;
                    }
                    if (cj == '"') {
                        break;
                    }
                    buf.append(cj);
                    j++;
                }
                if (j >= s.length() || s.charAt(j) != '"') {
                    throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "q")
                            .addMetadado("motivo", "aspas não balanceadas");
                }
                out.add(new Token(Kind.PHRASE, buf.toString()));
                i = j + 1;

                
                if (i < s.length() && s.charAt(i) == '~') {
                    int k = i + 1;
                    int start = k;
                    while (k < s.length() && Character.isDigit(s.charAt(k))) k++;
                    if (k == start) {
                        throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "q")
                                .addMetadado("motivo", "proximidade exige número após ~");
                    }
                    out.add(new Token(Kind.PROX, s.substring(start, k)));
                    i = k;
                }
                continue;
            }

            
            int j = i;
            while (j < s.length()) {
                char cj = s.charAt(j);
                if (Character.isWhitespace(cj) || cj == '(' || cj == ')' || cj == '"') break;
                j++;
            }
            String raw = s.substring(i, j);
            String norm = normalizeOp(raw);
            if (norm.equals("AND")) {
                out.add(new Token(Kind.AND, raw));
            } else if (norm.equals("OR")) {
                out.add(new Token(Kind.OR, raw));
            } else if (norm.equals("NOT")) {
                out.add(new Token(Kind.NOT, raw));
            } else {
                
                int tilde = raw.indexOf('~');
                if (tilde > 0 && tilde < raw.length() - 1) {
                    String left = raw.substring(0, tilde);
                    int k = tilde + 1;
                    int start = k;
                    while (k < raw.length() && Character.isDigit(raw.charAt(k))) k++;
                    if (k > start && k < raw.length()) {
                        String dist = raw.substring(start, k);
                        String right = raw.substring(k);
                        out.add(new Token(Kind.TERM, left));
                        out.add(new Token(Kind.PROX, dist));
                        out.add(new Token(Kind.TERM, right));
                    } else {
                        out.add(new Token(Kind.TERM, raw));
                    }
                } else {
                    out.add(new Token(Kind.TERM, raw));
                }
            }
            i = j;
        }
        return out;
    }

    private static String normalizeOp(String raw) {
        if (raw == null) return "";
        String s = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toUpperCase();
        return switch (s) {
            case "E", "AND", "&" -> "AND";
            case "OU", "OR", "|" -> "OR";
            case "NAO", "NÃO", "NOT", "!", "-" -> "NOT";
            default -> s;
        };
    }

    

    private static final class Parser {
        private final List<Token> tokens;
        private int pos = 0;
        private int depth = 0;

        Parser(List<Token> tokens) {
            this.tokens = tokens;
        }

        boolean hasMore() {
            return pos < tokens.size();
        }

        Token peek() {
            return tokens.get(pos);
        }

        Token consume() {
            return tokens.get(pos++);
        }

        boolean match(Kind k) {
            if (!hasMore()) return false;
            if (peek().kind == k) {
                pos++;
                return true;
            }
            return false;
        }

        JmuQueryAst parseExpression() {
            return parseOr();
        }

        
        private JmuQueryAst parseOr() {
            JmuQueryAst left = parseAnd();
            while (hasMore() && peek().kind == Kind.OR) {
                consume();
                JmuQueryAst right = parseAnd();
                left = new JmuQueryAst.Or(left, right);
            }
            return left;
        }

        
        private JmuQueryAst parseAnd() {
            JmuQueryAst left = parseNot();
            while (hasMore()) {
                if (peek().kind == Kind.OR || peek().kind == Kind.RPAREN) {
                    break;
                }
                if (peek().kind == Kind.AND) {
                    consume();
                }
                
                if (!hasMore() || !(peek().kind == Kind.TERM || peek().kind == Kind.PHRASE || peek().kind == Kind.LPAREN || peek().kind == Kind.NOT)) {
                    break;
                }
                JmuQueryAst right = parseNot();
                left = new JmuQueryAst.And(left, right);
            }
            return left;
        }

        
        private JmuQueryAst parseNot() {
            boolean negate = false;
            while (hasMore() && peek().kind == Kind.NOT) {
                consume();
                negate = !negate;
            }
            JmuQueryAst node = parsePrimary();
            return negate ? new JmuQueryAst.Not(node) : node;
        }

        private JmuQueryAst parsePrimary() {
            if (!hasMore()) {
                throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "q")
                        .addMetadado("motivo", "consulta incompleta");
            }
            Token t = peek();
            return switch (t.kind) {
                case TERM -> {
                    consume();
                    
                    if (hasMore() && peek().kind == Kind.PROX) {
                        Token dist = consume();
                        if (hasMore() && peek().kind == Kind.TERM) {
                            Token right = consume();
                            yield new JmuQueryAst.Proximity(t.text, right.text, parseDistance(dist.text));
                        } else {
                            
                            yield new JmuQueryAst.Term(t.text);
                        }
                    }
                    yield new JmuQueryAst.Term(t.text);
                }
                case PHRASE -> {
                    consume();
                    if (hasMore() && peek().kind == Kind.PROX) {
                        Token dist = consume();
                        String[] parts = splitPhraseIntoTwoTerms(t.text);
                        yield new JmuQueryAst.Proximity(parts[0], parts[1], parseDistance(dist.text));
                    }
                    yield new JmuQueryAst.Phrase(t.text);
                }
                case LPAREN -> {
                    consume();
                    depth++;
                    if (depth > MAX_DEPTH) {
                        throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "q")
                                .addMetadado("motivo", "consulta muito aninhada")
                                .addMetadado("limite", MAX_DEPTH);
                    }
                    JmuQueryAst inside = parseExpression();
                    if (!match(Kind.RPAREN)) {
                        throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "q")
                                .addMetadado("motivo", "parênteses não balanceados");
                    }
                    depth--;
                    yield inside;
                }
                default -> throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "q")
                        .addMetadado("motivo", "token inesperado")
                        .addMetadado("token", t.kind.name());
            };
        }

        private static int parseDistance(String raw) {
            try {
                int d = Integer.parseInt(raw);
                if (d < 1) return 1;
                return Math.min(d, 50);
            } catch (Exception e) {
                return 3;
            }
        }

        private static String[] splitPhraseIntoTwoTerms(String phrase) {
            if (phrase == null) return new String[]{"", ""};
            String s = phrase.trim().replaceAll("\\s+", " ");
            String[] parts = s.split(" ");
            if (parts.length <= 1) {
                return new String[]{s, s};
            }
            
            return new String[]{parts[0], parts[1]};
        }
    }

    private enum Kind { LPAREN, RPAREN, AND, OR, NOT, TERM, PHRASE, PROX }

    private record Token(Kind kind, String text) {}
}
