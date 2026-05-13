package com.tcc.pjb.backend.service.consultapublica.jmu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JmuQueryParserTest {

    private final JmuQueryParser parser = new JmuQueryParser();

    @Test
    void parses_and_or_not_precedence() {
        JmuQueryAst ast = parser.parse("a OU b E NAO c");
        
        assertTrue(ast instanceof JmuQueryAst.Or);
        JmuQueryAst.Or or = (JmuQueryAst.Or) ast;
        assertTrue(or.left() instanceof JmuQueryAst.Term);
        assertTrue(or.right() instanceof JmuQueryAst.And);
    }

    @Test
    void implicit_and_is_supported() {
        JmuQueryAst ast = parser.parse("habeas corpus");
        assertTrue(ast instanceof JmuQueryAst.And);
    }

    @Test
    void phrase_and_proximity_are_supported() {
        JmuQueryAst ast = parser.parse("\"habeas corpus\"~5");
        assertTrue(ast instanceof JmuQueryAst.Proximity);
        JmuQueryAst.Proximity p = (JmuQueryAst.Proximity) ast;
        assertEquals(5, p.distance());
    }

    @Test
    void wildcard_term_is_a_term() {
        JmuQueryAst ast = parser.parse("custod*");
        assertTrue(ast instanceof JmuQueryAst.Term);
    }
}
