package com.tcc.pjb.backend.ai.skills.v1;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.skills.IASkill;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityPreset;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiUsageMetricsDto;
import com.tcc.pjb.backend.service.ui.accessibility.engine.AccessibilityEvaluation;
import com.tcc.pjb.backend.service.ui.accessibility.engine.AccessibilityEvaluator;

@Component
public class SkillUiAccessibilitySuggestV1 implements IASkill {

  public static final String ACTION = "SKILL_UI_ACCESSIBILITY_SUGGEST_V1";

  private final AccessibilityEvaluator evaluator;
  private final ObjectMapper mapper;

  public SkillUiAccessibilitySuggestV1(AccessibilityEvaluator evaluator, ObjectMapper mapper) {
    this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
    this.mapper = Objects.requireNonNull(mapper, "mapper");
  }

  @Override
  public boolean suporta(IARequest request) {
    return request != null && ACTION.equalsIgnoreCase(request.getAcao());
  }

  @Override
  public IAResponse executar(IARequest request, Map<String, Object> contexto) {
    Objects.requireNonNull(request, "request");

    AccessibilityEvaluation eval;
    try {
      Object m = request.getPayload().get("metrics");
      UiUsageMetricsDto metrics;
      if (m instanceof UiUsageMetricsDto dto) {
        metrics = dto;
      } else {
        metrics = mapper.convertValue(m == null ? Map.of() : m, UiUsageMetricsDto.class);
      }
      eval = evaluator.evaluate(metrics);
    } catch (Exception ex) {
      return IAResponse.builder()
          .origem(getNome())
          .status(IAResponse.StatusIA.ERRO)
          .texto("Falha interna ao avaliar sugestão de acessibilidade.")
          .confianca(0.2)
          .dataGeracao(Instant.now())
          .metadado("error", ex.getClass().getSimpleName())
          .build();
    }

    Map<String, Object> essence = new LinkedHashMap<>();
    essence.put("preset", eval.preset().name());
    essence.put("score", eval.score());
    essence.put("probability", eval.probability());
    essence.put("confidence", eval.confidence());
    essence.put("reasonCodes", eval.reasonCodes());
    essence.put("reasons", eval.reasons());
    essence.put("suggestionHash", eval.suggestionHash());

    
    String msg = (eval.preset() == UiAccessibilityPreset.DEFAULT)
        ? "Nenhuma adaptação forte identificada; mantendo preset padrão (apenas sugestão, nunca forçada)."
        : "Sugestão de acessibilidade gerada (opcional e explicável).";

    return IAResponse.builder()
        .origem(getNome())
        .status(IAResponse.StatusIA.SUCESSO)
        .texto(msg)
        .confianca(Math.max(0.1, Math.min(1.0, eval.confidence())))
        .dataGeracao(Instant.now())
        .essence(essence)
        .build();
  }

  @Override
  public String getNome() {
    return ACTION;
  }
}
