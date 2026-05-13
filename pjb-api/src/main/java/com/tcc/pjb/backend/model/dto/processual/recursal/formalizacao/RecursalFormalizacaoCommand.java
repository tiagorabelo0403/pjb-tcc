package com.tcc.pjb.backend.model.dto.processual.recursal.formalizacao;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshSpeciesType;
import com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade.RecursalAdmissibilityResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.ia.RecursalIaConferenciaResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import java.util.Map;

public record RecursalFormalizacaoCommand(
        Processo processo,
        Usuario usuario,
        String profileCode,
        WorkItem peticaoRecursal,
        WorkItem recursoPrincipal,
        LegalAppealType appealType,
        RecursalMeshSpeciesType speciesType,
        RecursalFormalizacaoTextos textos,
        RecursalFormalizacaoOpcoes opcoes,
        RecursalAdmissibilityResponse admissibility,
        RecursalIaConferenciaResponse aiReview,
        Map<String, Object> sigiloRecursal) {

    public RecursalFormalizacaoCommand {
        profileCode = normalize(profileCode);
        textos = textos == null ? new RecursalFormalizacaoTextos(null, null, null) : textos;
        opcoes = opcoes == null ? new RecursalFormalizacaoOpcoes(false, false) : opcoes;
        sigiloRecursal = sigiloRecursal == null || sigiloRecursal.isEmpty() ? Map.of() : Map.copyOf(sigiloRecursal);
    }

    public boolean ready() {
        return processo != null && peticaoRecursal != null && recursoPrincipal != null && appealType != null;
    }

    public String razoes() {
        return textos.razoes();
    }

    public String fundamentacao() {
        return textos.fundamentacao();
    }

    public String observacoes() {
        return textos.observacoes();
    }

    public boolean pedidoEfeitoSuspensivo() {
        return opcoes.pedidoEfeitoSuspensivo();
    }

    public boolean preparoDispensado() {
        return opcoes.preparoDispensado();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
