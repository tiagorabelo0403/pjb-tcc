package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.ai.juridica.v3.core.LegalDraftingService;
import com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade.RecursalAdmissibilityResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import org.springframework.stereotype.Service;

@Service
public class RecursalDraftPreviewAssembler {

    private final LegalDraftingService legalDraftingService;

    public RecursalDraftPreviewAssembler(LegalDraftingService legalDraftingService) {
        this.legalDraftingService = Objects.requireNonNull(legalDraftingService);
    }

    public String buildDraftPreview(Processo processo,
                                    PerfilRecursalDescriptor descriptor,
                                    String recursoNormalizado,
                                    String razoesNormalizadas,
                                    String fundamentacaoNormalizada,
                                    RecursalAdmissibilityResponse admissibility,
                                    Object plan,
                                    String observacoes) {
        LinkedHashMap<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("numero_processo", safeNumeroProcesso(processo));
        ctx.put("autor", firstNonBlank(processo.getParteAutoraNome(), descriptor.profileCode()));
        ctx.put("reu", firstNonBlank(processo.getParteReuNome(), "Parte adversa"));
        ctx.put("fundamentos", fundamentacaoNormalizada);
        ctx.put("decisao", firstNonBlank(observacoes, processo.getResumoIA(), processo.getObjetoProcessual(), "Decisão recorrida a ser sintetizada pelo operador responsável."));
        ctx.put("tempestividade", buildTempestividadePreview(admissibility));
        ctx.put("pedidos", buildPedidosPreview(recursoNormalizado, admissibility));
        ctx.put("pleading_blueprint", buildBlueprint(plan, admissibility, recursoNormalizado));
        ctx.put("control_points", buildControlPoints(admissibility, observacoes));
        ctx.put("local_data", "[LOCAL], [DATA]");
        ctx.put("assinatura", "[ASSINATURA / IDENTIFICAÇÃO FUNCIONAL]");
        String minuta = legalDraftingService.draftRecurso(ctx);
        if (minuta == null || minuta.isBlank()) {
            return razoesNormalizadas;
        }
        return minuta.replace("[ERRO DE FATO/DIREITO, PRECEDENTES, TESE]", razoesNormalizadas);
    }

    private List<String> buildBlueprint(Object plan, RecursalAdmissibilityResponse admissibility, String recursoNormalizado) {
        LinkedHashSet<String> blueprint = new LinkedHashSet<>();
        blueprint.add("Classificação recursal conectada ao catálogo nacional: " + recursoNormalizado + '.');
        if (plan != null) {
            blueprint.add("Planejamento mesh gerado com rota de admissibilidade, remessa e julgamento vinculadas ao caso.");
        }
        if (admissibility != null) {
            if (admissibility.counterReasonsMode() != null) {
                blueprint.add("Contrarrazões: " + admissibility.counterReasonsMode() + '.');
            }
            if (admissibility.effectMode() != null) {
                blueprint.add("Efeito recursal: " + admissibility.effectMode() + '.');
            }
            if (admissibility.reviewDesk() != null) {
                blueprint.add("Mesa de revisão operacional: " + admissibility.reviewDesk() + '.');
            }
        }
        return List.copyOf(blueprint);
    }

    private List<String> buildControlPoints(RecursalAdmissibilityResponse admissibility, String observacoes) {
        LinkedHashSet<String> points = new LinkedHashSet<>();
        if (admissibility != null) {
            points.add("Tempestividade: " + (admissibility.tempestivo() ? "positiva" : "sensível") + '.');
            points.add("Preparo: " + (admissibility.preparoDispensado() ? "dispensado/isento" : admissibility.preparoSatisfeito() ? "satisfeito" : "exige conferência") + '.');
            if (admissibility.stepUpRequired()) {
                points.add("Submissão com credencial reforçada ou step-up.");
            }
            if (admissibility.connectorWarnings() != null) {
                points.addAll(admissibility.connectorWarnings());
            }
            if (admissibility.alertas() != null) {
                points.addAll(admissibility.alertas().stream().limit(4).toList());
            }
        }
        if (observacoes != null) {
            points.add("Observações operacionais: " + observacoes + '.');
        }
        return List.copyOf(points);
    }

    private String buildTempestividadePreview(RecursalAdmissibilityResponse admissibility) {
        if (admissibility == null) {
            return "Tempestividade inferida em camada automatizada, com validação final assistida no fluxo recursal.";
        }
        StringJoiner joiner = new StringJoiner(" ");
        joiner.add("Data de protocolo considerada:");
        joiner.add(String.valueOf(admissibility.dataProtocolo()) + '.');
        if (admissibility.dataLimite() != null) {
            joiner.add("Data-limite estimada:");
            joiner.add(admissibility.dataLimite() + ".");
        }
        joiner.add("Resultado preliminar:");
        joiner.add(admissibility.tempestivo() ? "tempestivo." : "exige revisão de prazo.");
        return joiner.toString();
    }

    private List<String> buildPedidosPreview(String recursoNormalizado, RecursalAdmissibilityResponse admissibility) {
        ArrayList<String> pedidos = new ArrayList<>();
        pedidos.add("Conhecimento e provimento do " + recursoNormalizado + '.');
        if (admissibility != null && admissibility.effectMode() != null) {
            pedidos.add("Aplicação do regime de efeitos indicado pela malha recursal: " + admissibility.effectMode() + '.');
        }
        if (admissibility != null && admissibility.counterReasonsMode() != null) {
            pedidos.add("Abertura do contraditório recursal conforme modo de contrarrazões: " + admissibility.counterReasonsMode() + '.');
        }
        return List.copyOf(pedidos);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = normalizeNullable(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String safeNumeroProcesso(Processo processo) {
        if (processo == null) {
            return "PROCESSO_SEM_NUMERO";
        }
        String numeroUnificado = normalizeNullable(processo.getNumeroUnificado());
        if (numeroUnificado != null) {
            return numeroUnificado;
        }
        String numeroProcesso = normalizeNullable(processo.getNumeroProcesso());
        if (numeroProcesso != null) {
            return numeroProcesso;
        }
        return "PROCESSO-" + Objects.toString(processo.getId(), "SEM_ID");
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
