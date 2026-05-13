package com.tcc.pjb.backend.ai.juridica.mesh;

import com.tcc.pjb.backend.model.dto.ai.legal.mesh.LegalAiToolDescriptor;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class JuridicaLegalToolCatalogService {

    public List<LegalAiToolDescriptor> resolve(String capability, ApiVersion version, boolean strict, boolean petitionDetected) {
        String normalized = capability == null ? "" : capability.toUpperCase(Locale.ROOT);
        ApiVersion effectiveVersion = version == null ? ApiVersion.latest() : version;
        List<LegalAiToolDescriptor> tools = new ArrayList<>();
        tools.add(tool("LEGISLACAO_CANONICA", "Legislação canônica", "LEGISLACAO", true, true, true, false, "MCP_LEGISLACAO"));
        tools.add(tool("JURISPRUDENCIA_PRECEDENTES", "Jurisprudência e precedentes", "JURISPRUDENCIA", true, true, true, false, "MCP_JURISPRUDENCIA"));
        tools.add(tool("TPU_TAXONOMIA_PROCESSUAL", "TPU e taxonomia processual", "PROCESSUAL", true, true, true, false, "MCP_PROCESSUAL"));
        tools.add(tool("RITO_COMPETENCIA", "Rito, competência e cabimento", "PROCESSUAL", true, true, true, false, "MCP_PROCESSUAL"));
        tools.add(tool("PRAZO_E_AGENDA", "Prazo, calendário e audiência", "PRAZOS", true, true, true, false, "MCP_AGENDA_PRAZOS"));
        tools.add(tool("SIGILO_E_VISIBILIDADE", "Sigilo e visibilidade processual", "SEGURANCA", true, true, true, strict, "MCP_PROCESSUAL"));
        tools.add(tool("AUTENTICIDADE_DOCUMENTAL", "Autenticidade documental", "DOCUMENTAL", true, true, true, strict, "MCP_DOCUMENTAL"));
        tools.add(tool("EVIDENCIA_ASSINATURA", "Evidência de assinatura", "DOCUMENTAL", true, true, true, strict, "MCP_DOCUMENTAL"));
        tools.add(tool("DISCOVERY_LEGADO", "Descoberta federada de processos", "INTEROPERABILIDADE", true, true, true, strict, "MCP_INTEROPERABILIDADE"));
        tools.add(tool("ACESSO_LEGADO", "Acesso federado a processo legado", "INTEROPERABILIDADE", true, true, true, true, "MCP_INTEROPERABILIDADE"));
        tools.add(tool("PETICIONAMENTO_BLUEPRINT", "Blueprint de peticionamento", "PETICIONAMENTO", true, false, true, petitionDetected, "PJB_WORKSPACE"));
        tools.add(tool("MINUTA_DIFF_REVISAO", "Diff, revisão e jornada inteligente", "PETICIONAMENTO", true, false, true, petitionDetected, "PJB_WORKSPACE"));
        tools.add(tool("CERTIDOES_E_COMPROVANTES", "Certidões e comprovantes", "DOCUMENTAL", true, false, true, strict, "PJB_DOCUMENTAL"));
        tools.add(tool("ESCRITORIO_E_REPRESENTACAO", "Escritório, assistentes e representação", "REPRESENTACAO", true, false, true, strict, "PJB_WORKSPACE"));
        tools.add(tool("MAGISTRATURA_PREVIEW", "Preview de atos e decisão", "DECISAO", true, false, true, strict, "PJB_JUDICIAL"));
        tools.add(tool("INSTITUTIONAL_WORKBENCH", "Workbench institucional", "INSTITUCIONAL", true, false, true, strict, "PJB_INSTITUTIONAL"));
        tools.add(tool("VALOR_CAUSA_E_CUSTAS", "Valor da causa e custas", "FINANCEIRO", true, false, true, petitionDetected, "PJB_FINANCEIRO"));
        if (petitionDetected || normalized.contains("PETICAO") || normalized.contains("PROTOCOLO")) {
            tools.add(tool("PROTOCOLO_GOVERNADO", "Protocolo governado", "PETICIONAMENTO", false, false, true, true, "PJB_PROTOCOL"));
        }
        if (effectiveVersion.isAtLeast(ApiVersion.V3)) {
            tools.add(tool("MEMORIA_PROCESSUAL", "Memória processual contextual", "MEMORIA", true, false, true, strict, "PJB_MEMORY"));
            tools.add(tool("ANALISE_HERMENEUTICA", "Análise hermenêutica", "RACIOCINIO", true, false, true, false, "PJB_REASONING"));
        }
        return List.copyOf(tools);
    }

    private static LegalAiToolDescriptor tool(String id,
                                              String label,
                                              String category,
                                              boolean readOnly,
                                              boolean mcpEnabled,
                                              boolean ragAware,
                                              boolean requiresStepUp,
                                              String sourceLane) {
        return new LegalAiToolDescriptor(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(label, "label"),
                Objects.requireNonNull(category, "category"),
                readOnly,
                mcpEnabled,
                ragAware,
                requiresStepUp,
                Objects.requireNonNull(sourceLane, "sourceLane")
        );
    }
}
