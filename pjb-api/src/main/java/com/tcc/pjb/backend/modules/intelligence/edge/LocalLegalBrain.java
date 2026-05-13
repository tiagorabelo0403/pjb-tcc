package com.tcc.pjb.backend.modules.intelligence.edge;


public interface LocalLegalBrain {

    void load();

    String predictDraft(String resumoProcesso);
}
