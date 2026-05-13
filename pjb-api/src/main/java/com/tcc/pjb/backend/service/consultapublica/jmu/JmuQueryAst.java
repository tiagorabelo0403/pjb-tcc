package com.tcc.pjb.backend.service.consultapublica.jmu;


public sealed interface JmuQueryAst
        permits JmuQueryAst.And, JmuQueryAst.Or, JmuQueryAst.Not, JmuQueryAst.Term, JmuQueryAst.Phrase, JmuQueryAst.Proximity {

    record And(JmuQueryAst left, JmuQueryAst right) implements JmuQueryAst {}
    record Or(JmuQueryAst left, JmuQueryAst right) implements JmuQueryAst {}
    record Not(JmuQueryAst child) implements JmuQueryAst {}

    
    record Term(String raw) implements JmuQueryAst {}

    
    record Phrase(String raw) implements JmuQueryAst {}

    
    record Proximity(String left, String right, int distance) implements JmuQueryAst {}
}
