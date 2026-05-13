package com.tcc.pjb.backend.core.peticionamento.triagem;

public interface PjbAiTriageSuggestionPort {

    AiTriageSuggestion suggest(AiTriageContext context);
}
