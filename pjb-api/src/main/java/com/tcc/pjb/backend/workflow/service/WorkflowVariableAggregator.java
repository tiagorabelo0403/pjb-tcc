package com.tcc.pjb.backend.workflow.service;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.OrgaoJudiciario;
import com.tcc.pjb.backend.model.repository.JurisdicaoRepository;
import com.tcc.pjb.backend.model.repository.OrgaoJudiciarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkflowVariableAggregator {

    private final JurisdicaoRepository jurisdicaoRepository;
    private final OrgaoJudiciarioRepository orgaoRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> aggregateVariables(Map<String, Object> command) {
        Long jurisdicaoId = (Long) command.get("jurisdicaoId");
        Long orgaoId = (Long) command.get("orgaoJudiciarioId");

        Jurisdicao j = jurisdicaoRepository.findById(jurisdicaoId)
                .orElseThrow(() -> new NoSuchElementException("Jurisdição não encontrada: " + jurisdicaoId));

        OrgaoJudiciario o = orgaoRepository.findById(orgaoId)
                .orElseThrow(() -> new NoSuchElementException("Órgão não encontrado: " + orgaoId));

        Map<String, Object> richVariables = new HashMap<>(command);
        richVariables.put("jurisdicaoCompleta", j);
        richVariables.put("orgaoCompleto", o);

        return richVariables;
    }
}