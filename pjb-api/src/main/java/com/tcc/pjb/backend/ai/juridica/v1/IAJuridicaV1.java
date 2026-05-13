package com.tcc.pjb.backend.ai.juridica.v1;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.juridica.mesh.JuridicaUnifiedMeshProfileService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaLegalAiSpineService;
import com.tcc.pjb.backend.ai.core.IAPipelineContext;
import com.tcc.pjb.backend.ai.core.IAService;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class IAJuridicaV1 implements IAService {

    private static final Pattern REGEX_NUMEROS = Pattern.compile("\\D");

    private final RepresentacaoProcessualPolicyService representacaoProcessualPolicyService;
    private final JuridicaUnifiedMeshProfileService juridicaUnifiedMeshProfileService;
    private final JuridicaLegalAiSpineService juridicaLegalAiSpineService;

    public IAJuridicaV1(RepresentacaoProcessualPolicyService representacaoProcessualPolicyService,
                       JuridicaUnifiedMeshProfileService juridicaUnifiedMeshProfileService,
                       JuridicaLegalAiSpineService juridicaLegalAiSpineService) {
        this.representacaoProcessualPolicyService = representacaoProcessualPolicyService;
        this.juridicaUnifiedMeshProfileService = juridicaUnifiedMeshProfileService;
        this.juridicaLegalAiSpineService = juridicaLegalAiSpineService;
    }

    @Override
    public IAResponse processar(IAPipelineContext context) {

        IAResponse resposta = processar(context.getRequestEntrada());

        context.setUltimaResposta(resposta);
        context.memorizar("ultima_ia", getTipo());
        context.memorizar("juridica_v1_executada", true);

        boolean apto = resposta.getStatus() == IAResponse.StatusIA.SUCESSO;
        context.memorizar("processo_apto", apto);

        context.avancarEtapa(
                apto ? "JURIDICA_V2_MERITO" : "TRIAGEM_HUMANA_CORRECAO"
        );

        return resposta;
    }

    

    @Override
    public IAResponse processar(IARequest request) {

        Map<String, Object> payload = request.getPayload();

        List<String> pendencias = new ArrayList<>();
        List<String> alertas = new ArrayList<>();
        List<String> tags = new ArrayList<>();

        Map<String, Object> metadadosTecnicos = new LinkedHashMap<>();

        

        validarPartes(payload, pendencias);

        

        validarRepresentacao(payload, pendencias);

        

        analisarValorDaCausa(payload, pendencias, alertas, metadadosTecnicos);

        

        analisarPrioridades(payload, tags, alertas);

        

        analisarAmbitoDireito(payload, tags, metadadosTecnicos);

        

        gerarMetadadosEspecialistas(payload, metadadosTecnicos);

        boolean apto = pendencias.isEmpty();

        var mesh = juridicaUnifiedMeshProfileService.resolveForIa(
                request,
                com.tcc.pjb.backend.platform.versioning.ApiVersion.V1,
                getTipo(),
                java.util.Map.of(
                        "complexityScore", pendencias.size() * 10 + alertas.size() * 5,
                        "injectionRiskScore", 0,
                        "petitionDetected", false
                ),
                java.util.Map.of(),
                java.util.Map.of("effectiveMode", "READ_ONLY")
        );

        var spine = juridicaLegalAiSpineService.resolveForIa(request, com.tcc.pjb.backend.platform.versioning.ApiVersion.V1, getTipo());

        var meta = new java.util.LinkedHashMap<String, Object>();
        meta.put("pendencias", pendencias);
        meta.put("alertas", alertas);
        meta.put("tags", tags);
        meta.put("analise_tecnica", metadadosTecnicos);
        meta.put("juridica_mesh_profile", mesh.asMap());
        meta.put("juridica_mesh_tools", mesh.tools().stream().map(tool -> tool.id()).toList());
        meta.put("juridica_spine_profile", spine.asMap());
        meta.put("juridica_structured_outputs", spine.structuredOutputs().stream().map(output -> output.schemaId()).toList());
        meta.put("juridica_retrieval_stages", spine.retrieval().stages());
        meta.put("juridica_memory_scopes", spine.memory().enabledScopes());
        meta.put("juridica_symbolic_engines", spine.validation().symbolicEngines());
        meta.put("juridica_graph_enabled", spine.graph().enabled());
        meta.put("juridica_graph_traversals", spine.graph().traversalModes());
        meta.put("juridica_multimodal_modalities", spine.multimodal().enabledModalities());
        meta.put("juridica_eval_suites", spine.evaluation().evalSuites());
        meta.put("juridica_hallucination_guard", spine.hallucinationGuard().asMap());
        meta.put("juridica_unresolved_citation_placeholder", spine.hallucinationGuard().unresolvedCitationPlaceholder());
        meta.put("juridica_citation_emission_mode", spine.hallucinationGuard().citationEmissionMode());
        meta.put("juridica_trace_lane", spine.trace().lane());
        meta.put("juridica_research_capability", com.tcc.pjb.backend.ai.juridica.spine.JuridicaSpineLabels.CAPABILITY_RESEARCH_DOSSIER);
        meta.put("juridica_validation_capability", com.tcc.pjb.backend.ai.juridica.spine.JuridicaSpineLabels.CAPABILITY_VALIDATE_ENVELOPE);
        meta.put("juridica_hallucination_guard_capability", com.tcc.pjb.backend.ai.juridica.spine.JuridicaSpineLabels.CAPABILITY_HALLUCINATION_GUARD);
        meta.put("juridica_conversation_capability", com.tcc.pjb.backend.ai.juridica.spine.JuridicaSpineLabels.CAPABILITY_CONVERSATION);

        return IAResponse.builder()
                .origem(getTipo())
                .status(apto ? IAResponse.StatusIA.SUCESSO : IAResponse.StatusIA.ALERTA)
                .texto(gerarTexto(apto, pendencias, alertas, tags))
                .confianca(1.0)
                .metadados(meta)
                .dataGeracao(Instant.now())
                .build();
    }

    @Override
    public String getTipo() {
        return "JURIDICA_V1_ADMISSIBILIDADE_SUPREMA";
    }

    

    private void validarPartes(Map<String, Object> payload, List<String> pendencias) {

        String cpf = getString(payload, "autor_cpf");
        String cnpj = getString(payload, "autor_cnpj");

        if (cpf.isBlank() && cnpj.isBlank()) {
            pendencias.add("Autor não qualificado (CPF ou CNPJ ausente).");
        }

        if (getString(payload, "reu_nome").isBlank()) {
            pendencias.add("Réu não informado.");
        }
    }

    private void validarRepresentacao(Map<String, Object> payload, List<String> pendencias) {
        String ramo = firstNonBlank(getString(payload, "ramo_direito"), getString(payload, "ramoDireito"), getString(payload, "materia"));
        String rito = firstNonBlank(getString(payload, "rito"), getString(payload, "rito_processual"), getString(payload, "ritoProcessual"));
        String tribunal = firstNonBlank(getString(payload, "tribunal"), getString(payload, "tribunalCodigo"));
        boolean contextoConsensual = getBoolean(payload, "contexto_consensual") || getBoolean(payload, "audiencia_conciliacao") || getBoolean(payload, "audiencia_mediacao");
        boolean poderesEspeciais = getBoolean(payload, "poderes_especiais_transigir") || getBoolean(payload, "poderesEspeciaisTransigir");
        TipoUsuario perfil = inferPerfilAtor(payload);
        String instrumento = firstNonBlank(getString(payload, "tipo_instrumento_representacao"), getString(payload, "tipoInstrumentoRepresentacao"), getString(payload, "tipoInstrumento"));
        var policy = representacaoProcessualPolicyService.resolve(
                ramo,
                rito,
                tribunal,
                perfil,
                instrumento,
                getLong(payload, "audiencia_id"),
                firstNonBlank(getString(payload, "tipo_audiencia"), getString(payload, "tipoAudiencia")),
                contextoConsensual,
                poderesEspeciais,
                getString(payload, "termo_audiencia"),
                getString(payload, "ata_audiencia")
        );

        boolean possuiOab = !getString(payload, "advogado_oab").isBlank();
        boolean possuiProcuracao = getBoolean(payload, "doc_procuracao") || getBoolean(payload, "possui_procuracao") || getBoolean(payload, "possuiProcuracao");

        if (policy.exigeProcuracaoFormal() && !possuiOab) {
            pendencias.add("OAB não informada.");
        }
        if (!representacaoProcessualPolicyService.representacaoSuficiente(policy, possuiProcuracao, possuiOab)) {
            pendencias.add(policy.alertas().isEmpty() ? "Representação processual insuficiente." : policy.alertas().getFirst());
        }
        if (policy.exigePoderesEspeciaisTransigir() && !(getBoolean(payload, "proc_poderes_transigir") || poderesEspeciais)) {
            pendencias.add("Poderes especiais para transigir, confessar ou desistir não foram confirmados para a trilha consensual.");
        }
    }

    

    private TipoUsuario inferPerfilAtor(Map<String, Object> payload) {
        if (getBoolean(payload, "defensoria") || getBoolean(payload, "perfil_defensoria")) {
            return TipoUsuario.DEFENSOR_PUBLICO;
        }
        if (getBoolean(payload, "procuradoria") || getBoolean(payload, "perfil_procuradoria")) {
            return TipoUsuario.PROCURADOR;
        }
        if (getBoolean(payload, "ministerio_publico") || getBoolean(payload, "perfil_mp")) {
            return TipoUsuario.MEMBRO_MINISTERIO_PUBLICO;
        }
        if (!getString(payload, "advogado_oab").isBlank()) {
            return TipoUsuario.ADVOGADO;
        }
        return TipoUsuario.CIDADAO;
    }

    private Long getLong(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private void analisarValorDaCausa(Map<String, Object> payload,
                                      List<String> pendencias,
                                      List<String> alertas,
                                      Map<String, Object> meta) {

        double valor = getDouble(payload, "valor_causa");

        meta.put("analise_matematica", Map.of(
                "valor_causa", valor,
                "criterio", "Art. 291 CPC",
                "consistencia", valor > 0
        ));

        if (valor <= 0) {
            pendencias.add("Valor da causa inválido.");
        }
    }

    private void analisarPrioridades(Map<String, Object> payload,
                                     List<String> tags,
                                     List<String> alertas) {

        String nascimento = getString(payload, "autor_data_nascimento");

        if (!nascimento.isBlank()) {
            try {
                int idade = Period.between(
                        LocalDate.parse(nascimento, DateTimeFormatter.ISO_DATE),
                        LocalDate.now(ZoneOffset.UTC)
                ).getYears();

                if (idade >= 80) tags.add("PRIORIDADE_SUPER_IDOSO");
                else if (idade >= 60) tags.add("PRIORIDADE_IDOSO");

            } catch (Exception e) {
                alertas.add("Data de nascimento inválida.");
            }
        }
    }

    private void analisarAmbitoDireito(Map<String, Object> payload,
                                       List<String> tags,
                                       Map<String, Object> meta) {

        String ambito = getString(payload, "ambito_direito").toUpperCase();

        tags.add("RAMO_" + ambito);

        meta.put("procedimento", switch (ambito) {
            case "PENAL" -> "Código de Processo Penal";
            case "TRABALHISTA" -> "CLT + Rito Trabalhista";
            case "PREVIDENCIARIO" -> "Lei 8.213/91";
            case "ELEITORAL" -> "Código Eleitoral";
            case "MILITAR" -> "CPM + CPPM";
            case "CONSTITUCIONAL" -> "Controle de Constitucionalidade";
            default -> "Código de Processo Civil";
        });
    }

    private void gerarMetadadosEspecialistas(Map<String, Object> payload,
                                             Map<String, Object> meta) {

        meta.put("avaliacao_psicologica", "Não aplicável salvo dano moral ou capacidade civil");
        meta.put("avaliacao_psiquiatrica", "Necessária apenas em interdição, inimputabilidade ou saúde mental");
        meta.put("avaliacao_jurista", "Análise conforme princípios, leis e precedentes");
        meta.put("fontes_normativas", List.of(
                "Constituição Federal",
                "Leis Infraconstitucionais",
                "Pactos Internacionais de Direitos Humanos",
                "Jurisprudência vinculante"
        ));
    }

    

    private String gerarTexto(boolean apto,
                              List<String> pendencias,
                              List<String> alertas,
                              List<String> tags) {

        StringBuilder sb = new StringBuilder();

        sb.append(apto
                ? "Petição formalmente apta."
                : "Petição com impedimentos formais.");

        if (!tags.isEmpty()) sb.append("\nTags: ").append(String.join(", ", tags));
        if (!pendencias.isEmpty()) sb.append("\nPendências: ").append(String.join("; ", pendencias));
        if (!alertas.isEmpty()) sb.append("\nAlertas: ").append(String.join("; ", alertas));

        return sb.toString();
    }

    private String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v == null ? "" : v.toString().trim();
    }

    private boolean getBoolean(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof Boolean ? (Boolean) v : Boolean.parseBoolean(String.valueOf(v));
    }

    private double getDouble(Map<String, Object> map, String key) {
        Object v = map.get(key);
        try {
            return v == null ? 0.0 : Double.parseDouble(v.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }
}
