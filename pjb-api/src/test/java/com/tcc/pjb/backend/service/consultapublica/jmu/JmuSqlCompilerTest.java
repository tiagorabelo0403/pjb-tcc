package com.tcc.pjb.backend.service.consultapublica.jmu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JmuSqlCompilerTest {

    @Test
    void compiles_term_to_tsquery_and_like_fallback() {
        JmuQueryAst ast = new JmuQueryAst.Term("habeas");
        JmuSqlCompiler.Compiled c = new JmuSqlCompiler().compile(ast, "pg");
        assertTrue(c.sql().contains("plainto_tsquery"));
        assertTrue(c.sql().contains("LIKE"));
        assertTrue(c.usesTsQuery());
        assertFalse(c.params().isEmpty());
    }

    @Test
    void compiles_wildcard_term_to_like_only() {
        JmuQueryAst ast = new JmuQueryAst.Term("custod*");
        JmuSqlCompiler.Compiled c = new JmuSqlCompiler().compile(ast, "pg");
        assertTrue(c.sql().contains("LIKE"));
        assertFalse(c.usesTsQuery());
    }

    @Test
    void compiles_proximity_to_to_tsquery() {
        JmuQueryAst ast = new JmuQueryAst.Proximity("habeas", "corpus", 3);
        JmuSqlCompiler.Compiled c = new JmuSqlCompiler().compile(ast, "pg");
        assertTrue(c.sql().contains("to_tsquery"));
        assertTrue(c.usesTsQuery());
    }
}
