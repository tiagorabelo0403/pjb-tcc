
package com.tcc.pjb.backend.service.processual.peticionamento;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PeticionamentoProcedureSpecificVerifierService {

    public VerificationReport analyze(ResolveRequest request) {
        ResolveRequest safe = request == null ? ResolveRequest.empty() : request;
        ProcedureTrack track = resolveTrack(safe);
        Specification specification = buildSpecification(track, safe);

        ArrayList<String> blockers = new ArrayList<>();
        ArrayList<String> alerts = new ArrayList<>(specification.alerts());
        ArrayList<String> coveredDocuments = new ArrayList<>();
        ArrayList<String> missingDocuments = new ArrayList<>();

        String corpus = normalize(join(
                safe.tituloCaso(),
                safe.ramoDireito(),
                safe.ritoProcessual(),
                safe.classeProcessual(),
                safe.assuntoTpu(),
                safe.materiaPrincipal(),
                safe.naturezaJuridica(),
                safe.textoBase(),
                String.join(" ", safe.fatos()),
                String.join(" ", safe.pedidos()),
                String.join(" ", safe.documentosAnexados())
        ));

        if (!safe.representacaoRegular()) {
            blockers.add("A representação processual ainda não está regular para o trilho procedimental escolhido.");
        }

        for (RequiredDocument required : specification.requiredDocuments()) {
            boolean present = containsAny(corpus, required.matchers());
            if (present) {
                coveredDocuments.add(required.code());
            } else {
                missingDocuments.add(required.code());
                if (required.blocking() && (safe.prepararPacoteProtocolo() || specification.strict())) {
                    blockers.add(required.blockingMessage());
                } else {
                    alerts.add(required.alertMessage());
                }
            }
        }

        if (safe.sigiloReforcado()) {
            alerts.add("O caso opera em trilha de sigilo reforçado e exige redobrada revisão de visibilidade, mascaramento e need-to-know.");
        }
        if ((safe.casoUrgente() || safe.tutelaUrgencia()) && !containsAny(corpus, "liminar", "urgencia", "tutela", "plantao")) {
            alerts.add("Há marcação de urgência, mas o texto-base ainda não evidencia com clareza o fundamento urgente.");
        }
        if (safe.tipoUsuario() != null && safe.tipoUsuario().isInstitucional()) {
            alerts.add("Fluxo institucional detectado: revisar competência, legitimidade e cadeia formal de autorização antes da assinatura.");
        }
        if (track == ProcedureTrack.MANDADO_DE_SEGURANCA && !containsAny(corpus, "120", "cento_e_vinte", "prazo", "decadencia")) {
            blockers.add("Mandado de segurança sem tratamento explícito do prazo decadencial e da tempestividade aparente.");
        }
        if (track == ProcedureTrack.EXECUCAO_FISCAL && !containsAny(corpus, "cda", "certidao_de_divida_ativa", "divida_ativa")) {
            blockers.add("Execução fiscal sem CDA ou referência documental equivalente para aparelhar o protocolo.");
        }
        if (track == ProcedureTrack.PREVIDENCIARIO_BPC && !containsAny(corpus, "miserabilidade", "vulnerabilidade", "deficiencia", "impedimento")) {
            alerts.add("BPC sem demonstração material suficiente de vulnerabilidade ou impedimento de longo prazo.");
        }

        ArrayList<String> finalGates = new ArrayList<>();
        finalGates.add(safe.representacaoRegular() ? "REPRESENTACAO_OK" : "REPRESENTACAO_PENDENTE");
        finalGates.add(missingDocuments.isEmpty() ? "COBERTURA_DOCUMENTAL_OK" : "COBERTURA_DOCUMENTAL_EM_AJUSTE");
        finalGates.add(blockers.isEmpty() ? "VERIFICADOR_SUBESPECIE_OK" : "VERIFICADOR_SUBESPECIE_BLOQUEADO");
        if (safe.prepararPacoteProtocolo()) {
            finalGates.add(blockers.isEmpty() ? "PROTOCOLO_SUBESPECIE_APTO" : "PROTOCOLO_SUBESPECIE_RETIDO");
        }
        if (safe.sigiloReforcado()) {
            finalGates.add("SIGILO_REFORCADO_ATIVO");
        }

        int score = resolveScore(blockers, alerts, coveredDocuments, specification.requiredDocuments());
        LinkedHashMap<String, Object> workspace = new LinkedHashMap<>();
        workspace.put("profile", specification.profile());
        workspace.put("resolvedProcedureTrack", track.name());
        workspace.put("resolvedProcedureLabel", track.label());
        workspace.put("verifierMode", specification.strict() ? "STRICT" : "BALANCED");
        workspace.put("mandatoryChecks", specification.mandatoryChecks());
        workspace.put("requiredDocuments", specification.requiredDocuments().stream().map(RequiredDocument::code).toList());
        workspace.put("coveredDocuments", List.copyOf(new LinkedHashSet<>(coveredDocuments)));
        workspace.put("missingDocuments", List.copyOf(new LinkedHashSet<>(missingDocuments)));
        workspace.put("alerts", List.copyOf(new LinkedHashSet<>(alerts)));
        workspace.put("blockers", List.copyOf(new LinkedHashSet<>(blockers)));
        workspace.put("finalGates", List.copyOf(new LinkedHashSet<>(finalGates)));
        workspace.put("readinessScore", score);
        workspace.put("requiresHumanReview", !blockers.isEmpty() || safe.sigiloReforcado());

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("profile", specification.profile());
        metadata.put("track", track.name());
        metadata.put("strict", specification.strict());
        metadata.put("score", score);

        return new VerificationReport(
                specification.profile(),
                track.name(),
                track.label(),
                specification.strict(),
                specification.mandatoryChecks(),
                specification.requiredDocuments().stream().map(RequiredDocument::code).toList(),
                List.copyOf(new LinkedHashSet<>(coveredDocuments)),
                List.copyOf(new LinkedHashSet<>(missingDocuments)),
                List.copyOf(new LinkedHashSet<>(alerts)),
                List.copyOf(new LinkedHashSet<>(blockers)),
                List.copyOf(new LinkedHashSet<>(finalGates)),
                Collections.unmodifiableMap(metadata),
                Map.copyOf(workspace)
        );
    }

    private ProcedureTrack resolveTrack(ResolveRequest request) {
        String corpus = normalize(join(
                request.ramoDireito(),
                request.ritoProcessual(),
                request.classeProcessual(),
                request.assuntoTpu(),
                request.materiaPrincipal(),
                request.naturezaJuridica(),
                request.tituloCaso(),
                request.textoBase(),
                String.join(" ", request.documentosAnexados())
        ));
        String ramo = normalize(request.ramoDireito());

        if (containsAny(corpus, "mandado_de_seguranca", "mandamus")) return ProcedureTrack.MANDADO_DE_SEGURANCA;
        if (containsAny(corpus, "execucao_fiscal", "cda", "divida_ativa")) return ProcedureTrack.EXECUCAO_FISCAL;
        if (containsAny(corpus, "habeas_corpus", "constrangimento_ilegal")) return ProcedureTrack.PENAL_HABEAS_CORPUS;
        if (containsAny(corpus, "audiencia_de_custodia", "custodia", "prisao_em_flagrante", "flagrante")) return ProcedureTrack.PENAL_CUSTODIA;
        if (containsAny(corpus, "juri", "homicidio", "doloso_contra_a_vida")) return ProcedureTrack.PENAL_JURI;
        if (containsAny(ramo, "trabalh", "clt")) return ProcedureTrack.TRABALHISTA;
        if (containsAny(ramo, "eleitoral")) return ProcedureTrack.ELEITORAL;
        if (containsAny(ramo, "previdenci")) {
            if (containsAny(corpus, "bpc", "loas", "beneficio_de_prestacao_continuada")) return ProcedureTrack.PREVIDENCIARIO_BPC;
            return ProcedureTrack.PREVIDENCIARIO_BENEFICIO;
        }
        if (containsAny(ramo, "famil", "infancia")) {
            if (containsAny(corpus, "alimentos", "alimenticia", "pensionamento")) return ProcedureTrack.FAMILIA_ALIMENTOS;
            if (containsAny(corpus, "guarda", "convivencia", "visitas", "regulamentacao_de_visitas")) return ProcedureTrack.FAMILIA_GUARDA_CONVIVENCIA;
            if (containsAny(corpus, "divorcio", "dissolucao", "partilha", "uniao_estavel")) return ProcedureTrack.FAMILIA_DIVORCIO;
        }
        if (containsAny(corpus, "revisional", "revisao")) return ProcedureTrack.CIVEL_REVISIONAL;
        if (containsAny(corpus, "obrigacao_de_fazer", "obrigacao", "cumprimento_especifico")) return ProcedureTrack.CIVEL_OBRIGACAO_FAZER;
        if (containsAny(corpus, "indeniz", "dano_moral", "dano_material", "responsabilidade_civil")) return ProcedureTrack.CIVEL_INDENIZATORIA;
        return ProcedureTrack.GERAL;
    }

    private Specification buildSpecification(ProcedureTrack track, ResolveRequest request) {
        return switch (track) {
            case FAMILIA_ALIMENTOS -> new Specification(
                    "VERIFICADOR_ALIMENTOS_V2",
                    false,
                    List.of(
                            "Verificar binômio necessidade-possibilidade.",
                            "Conferir legitimidade ativa e qualificação das partes.",
                            "Revisar prova mínima de vínculo familiar, dependência ou guarda."
                    ),
                    List.of(
                            new RequiredDocument("VINCULO_FAMILIAR_OU_GUARDA", false, "Faltam indícios claros de vínculo ou guarda para alimentos.", "Anexar certidão, documento de guarda ou prova equivalente do vínculo."),
                            new RequiredDocument("COMPROVACAO_RENDA_OU_CAPACIDADE", false, "A capacidade econômica do alimentante ainda não está bem documentada.", "Adicionar contracheques, extratos ou sinais concretos de capacidade econômica.")
                    ),
                    List.of("Conferir pedido imediato, alimentos provisórios e termo inicial.")
            );
            case FAMILIA_GUARDA_CONVIVENCIA -> new Specification(
                    "VERIFICADOR_GUARDA_CONVIVENCIA_V2",
                    true,
                    List.of(
                            "Conferir interesse superior da criança ou adolescente.",
                            "Revisar rotina, residência, convivência e eventual urgência protetiva.",
                            "Checar se a narrativa define com precisão o regime pedido."
                    ),
                    List.of(
                            new RequiredDocument("CERTIDAO_NASCIMENTO_OU_IDENTIFICACAO_DO_MENOR", true, "Falta documentação mínima da criança ou adolescente para o pedido de guarda/convivência.", "Anexar certidão de nascimento ou documento equivalente do menor."),
                            new RequiredDocument("SUPORTE_FATICO_DE_ROTINA_E_CONVIVENCIA", false, "A rotina de convivência ainda está pouco demonstrada.", "Adicionar comprovantes de escola, residência, saúde ou convivência.")
                    ),
                    List.of("Validar se há pedido específico de guarda unilateral, compartilhada ou convivência.")
            );
            case FAMILIA_DIVORCIO -> new Specification(
                    "VERIFICADOR_DIVORCIO_V2",
                    false,
                    List.of(
                            "Conferir certidão de casamento ou união estável.",
                            "Revisar existência de filhos, alimentos, guarda e partilha.",
                            "Separar pedidos principais e acessórios para evitar contradição."
                    ),
                    List.of(
                            new RequiredDocument("CERTIDAO_CASAMENTO_OU_UNIAO_ESTAVEL", true, "Divórcio sem certidão de casamento ou documento equivalente.", "Anexar certidão de casamento ou instrumento equivalente."),
                            new RequiredDocument("QUADRO_PARTILHA_OU_DECLARACAO_SEM_BENS", false, "A petição ainda não esclarece bens, dívidas ou inexistência de partilha.", "Adicionar quadro de partilha ou declaração de inexistência de bens.")
                    ),
                    List.of("Checar coerência entre dissolução, nome, guarda, visitas e alimentos.")
            );
            case EXECUCAO_FISCAL -> new Specification(
                    "VERIFICADOR_EXECUCAO_FISCAL_V3",
                    true,
                    List.of(
                            "Conferir CDA, presunção de liquidez e identificação do executado.",
                            "Revisar prescrição, legitimidade ativa e competência fazendária.",
                            "Verificar memória mínima do débito e lastro normativo."
                    ),
                    List.of(
                            new RequiredDocument("CDA_OU_LASTRO_EXECUTIVO_EQUIVALENTE", true, "Execução fiscal sem CDA ou lastro executivo equivalente.", "Anexar CDA ou documento executivo equivalente."),
                            new RequiredDocument("DEMONSTRATIVO_MINIMO_DO_DEBITO", true, "Execução fiscal sem demonstrativo mínimo do débito.", "Adicionar demonstrativo de débito, planilha ou memória compatível.")
                    ),
                    List.of("Checar órgão fazendário, competência, prescrição e pedido executivo.")
            );
            case MANDADO_DE_SEGURANCA -> new Specification(
                    "VERIFICADOR_MANDADO_SEGURANCA_V3",
                    true,
                    List.of(
                            "Conferir direito líquido e certo demonstrado documentalmente.",
                            "Identificar autoridade coatora e ato coator com precisão.",
                            "Validar competência, tempestividade e pedido liminar quando houver."
                    ),
                    List.of(
                            new RequiredDocument("ATO_COATOR_DOCUMENTADO", true, "Mandado de segurança sem ato coator documentalmente identificável.", "Anexar ato coator, decisão ou comunicação formal equivalente."),
                            new RequiredDocument("PROVA_PRECONSTITUIDA", true, "Mandado de segurança sem prova pré-constituída suficiente.", "Adicionar documentos pré-constituídos que sustentem o direito alegado.")
                    ),
                    List.of("Revisar prazo decadencial, autoridade coatora e prova pré-constituída.")
            );
            case PENAL_CUSTODIA -> new Specification(
                    "VERIFICADOR_CUSTODIA_V3",
                    true,
                    List.of(
                            "Conferir auto de prisão, integridade física e fundamentos cautelares.",
                            "Revisar eventual ilegalidade do flagrante, condições pessoais e medidas alternativas.",
                            "Identificar necessidade de tutela imediata, saúde ou vulnerabilidade."
                    ),
                    List.of(
                            new RequiredDocument("AUTO_DE_PRISAO_OU_FLAGRANTE", true, "Audiência de custódia sem auto de prisão ou peça equivalente.", "Anexar APF, auto de prisão ou documento equivalente."),
                            new RequiredDocument("DADOS_PESSOAIS_E_CONDICOES_DO_CUSTODIADO", false, "A peça ainda não demonstra de forma suficiente as condições pessoais do custodiado.", "Adicionar antecedentes, residência, trabalho, saúde ou outras condições pessoais.")
                    ),
                    List.of("Checar fundamentos cautelares, tortura, integridade e medidas alternativas.")
            );
            case PENAL_HABEAS_CORPUS -> new Specification(
                    "VERIFICADOR_HABEAS_CORPUS_V3",
                    true,
                    List.of(
                            "Conferir paciente, autoridade coatora e constrangimento ilegal.",
                            "Revisar urgência e pedido liminar.",
                            "Checar elementos documentais mínimos do ato impugnado."
                    ),
                    List.of(
                            new RequiredDocument("ATO_RESTRITIVO_OU_DECISAO_IMPUGNADA", true, "Habeas corpus sem decisão ou ato restritivo identificável.", "Anexar decisão, mandado ou ato que origine o constrangimento."),
                            new RequiredDocument("QUALIFICACAO_DO_PACIENTE", false, "A qualificação do paciente ainda está insuficiente.", "Complementar dados do paciente e do ato constritivo.")
                    ),
                    List.of("Revisar competência, urgência e ilegalidade manifesta.")
            );
            case PENAL_JURI -> new Specification(
                    "VERIFICADOR_TRIBUNAL_JURI_V2",
                    true,
                    List.of(
                            "Conferir materialidade e indícios mínimos sobre crime doloso contra a vida.",
                            "Revisar narrativa, qualificadoras e coerência probatória.",
                            "Separar pedidos compatíveis com a fase processual."
                    ),
                    List.of(
                            new RequiredDocument("LAUDO_OU_PROVA_DE_MATERIALIDADE", true, "Caso de júri sem prova mínima de materialidade.", "Anexar laudo, certidão ou outro documento que comprove materialidade."),
                            new RequiredDocument("PROVA_INDICIARIA_OU_TESTEMUNHAL_REFERENCIADA", false, "A petição ainda não evidencia lastro indiciário mínimo.", "Adicionar referências probatórias essenciais para a fase.")
                    ),
                    List.of("Revisar fase procedimental e compatibilidade do pedido com o rito do júri.")
            );
            case TRABALHISTA -> new Specification(
                    "VERIFICADOR_TRABALHISTA_V3",
                    false,
                    List.of(
                            "Conferir vínculo, período contratual e verbas postuladas.",
                            "Revisar liquidação, cálculos e documentos laborais básicos.",
                            "Checar competência, rito e eventual urgência."
                    ),
                    List.of(
                            new RequiredDocument("PROVA_MINIMA_DO_VINCULO", false, "A prova do vínculo empregatício ainda está fraca.", "Adicionar CTPS, contracheques, crachá, mensagens ou outros sinais do vínculo."),
                            new RequiredDocument("PLANILHA_OU_MEMORIA_DAS_VERBAS", false, "As verbas pleiteadas ainda carecem de memória de cálculo mínima.", "Adicionar planilha, memória ou racional de cálculo das verbas.")
                    ),
                    List.of("Revisar liquidação, jornada, verbas e reflexos.")
            );
            case PREVIDENCIARIO_BPC -> new Specification(
                    "VERIFICADOR_PREVIDENCIARIO_BPC_V3",
                    true,
                    List.of(
                            "Conferir miserabilidade, deficiência ou impedimento de longo prazo.",
                            "Revisar prova socioeconômica e documentação clínica.",
                            "Checar histórico administrativo e competência federal."
                    ),
                    List.of(
                            new RequiredDocument("PROVA_SOCIOECONOMICA_MINIMA", true, "BPC sem prova socioeconômica mínima.", "Anexar CadÚnico, extratos, composição familiar ou equivalente."),
                            new RequiredDocument("LAUDO_OU_RELATORIO_DE_IMPEDIMENTO", true, "BPC sem laudo ou relatório de impedimento/deficiência.", "Adicionar laudo, relatório médico ou documento equivalente.")
                    ),
                    List.of("Revisar miserabilidade, impedimento de longo prazo e histórico administrativo.")
            );
            case PREVIDENCIARIO_BENEFICIO -> new Specification(
                    "VERIFICADOR_PREVIDENCIARIO_BENEFICIO_V3",
                    true,
                    List.of(
                            "Conferir histórico contributivo, indeferimento administrativo e prova técnica.",
                            "Revisar CNIS, DER, laudos e incapacidade quando cabíveis.",
                            "Checar competência e adequação do benefício pretendido."
                    ),
                    List.of(
                            new RequiredDocument("CNIS_OU_HISTORICO_CONTRIBUTIVO", true, "Benefício previdenciário sem histórico contributivo identificável.", "Anexar CNIS ou histórico contributivo equivalente."),
                            new RequiredDocument("INDEFERIMENTO_ADMINISTRATIVO_OU_PROTOCOLO", false, "A petição ainda não documenta claramente o histórico administrativo.", "Adicionar indeferimento administrativo, protocolo ou andamento do pedido.")
                    ),
                    List.of("Revisar DER, qualidade de segurado, carência e incapacidade quando couber.")
            );
            case ELEITORAL -> new Specification(
                    "VERIFICADOR_ELEITORAL_V3",
                    true,
                    List.of(
                            "Conferir legitimidade ativa, rito e tempestividade eleitoral.",
                            "Revisar calendário, prova mínima e risco de perecimento do objeto.",
                            "Checar competência da justiça eleitoral e do órgão julgador."
                    ),
                    List.of(
                            new RequiredDocument("PROVA_MINIMA_DOS_FATOS_ELEITORAIS", true, "Caso eleitoral sem prova mínima dos fatos narrados.", "Anexar mídia, documento, decisão ou prova material mínima."),
                            new RequiredDocument("MARCO_TEMPORAL_OU_REFERENCIA_AO_CALENDARIO", false, "A peça ainda não explicita com nitidez o marco temporal eleitoral.", "Incluir datas, calendário ou eventos eleitorais relevantes.")
                    ),
                    List.of("Revisar competência, legitimidade, calendário e prova mínima.")
            );
            case CIVEL_INDENIZATORIA -> new Specification(
                    "VERIFICADOR_CIVEL_INDENIZATORIA_V2",
                    false,
                    List.of(
                            "Conferir dano, nexo causal e responsabilidade.",
                            "Revisar pedido de dano moral, material ou ambos.",
                            "Checar lastro documental mínimo do fato gerador."
                    ),
                    List.of(
                            new RequiredDocument("PROVA_DO_FATO_GERADOR", false, "O fato gerador ainda está pouco documentado.", "Adicionar boletim, contrato, conversa, foto, laudo ou documento equivalente."),
                            new RequiredDocument("INDICIOS_DE_DANO", false, "A extensão do dano ainda não está bem demonstrada.", "Anexar laudos, recibos, comprovantes ou elementos que mostrem o dano.")
                    ),
                    List.of("Revisar dano, nexo, culpa/responsabilidade objetiva e quantum.")
            );
            case CIVEL_OBRIGACAO_FAZER -> new Specification(
                    "VERIFICADOR_OBRIGACAO_FAZER_V2",
                    false,
                    List.of(
                            "Conferir obrigação específica, inadimplemento e utilidade prática do provimento.",
                            "Revisar urgência, prazo de cumprimento e coercitividade.",
                            "Checar documentos que mostrem recusa, mora ou descumprimento."
                    ),
                    List.of(
                            new RequiredDocument("DOCUMENTO_DA_OBRIGACAO", false, "A obrigação principal ainda não está bem demonstrada.", "Anexar contrato, solicitação, negativa ou documento equivalente."),
                            new RequiredDocument("PROVA_DO_DESCUMPRIMENTO", false, "O descumprimento ainda carece de demonstração documental.", "Adicionar negativa, aviso, protocolo ou registro de mora.")
                    ),
                    List.of("Revisar obrigação, descumprimento, tutela e multa coercitiva.")
            );
            case CIVEL_REVISIONAL -> new Specification(
                    "VERIFICADOR_REVISIONAL_V2",
                    false,
                    List.of(
                            "Conferir contrato, cláusulas questionadas e base de revisão.",
                            "Revisar memória comparativa, índices ou excesso alegado.",
                            "Checar prova documental do pacto e da cobrança."
                    ),
                    List.of(
                            new RequiredDocument("CONTRATO_OU_INSTRUMENTO_BASE", true, "Ação revisional sem contrato ou instrumento-base identificável.", "Anexar contrato, instrumento ou documento equivalente."),
                            new RequiredDocument("PLANILHA_COMPARATIVA_OU_CRITERIO_REVISIONAL", false, "A tese revisional ainda não está acompanhada de critério comparativo suficiente.", "Adicionar planilha, índice ou racional técnico da revisão.")
                    ),
                    List.of("Revisar cláusulas, critério revisional e prova do pacto.")
            );
            case GERAL -> new Specification(
                    "VERIFICADOR_GERAL_PETICIONAMENTO_V1",
                    false,
                    List.of(
                            "Conferir competência, partes, narrativa, pedidos e prova mínima.",
                            "Revisar representação, sigilo e preparo do protocolo.",
                            "Separar núcleo do pedido e anexos essenciais."
                    ),
                    List.of(
                            new RequiredDocument("PECA_BASE", false, "A peça-base ainda não foi identificada de forma segura.", "Adicionar ou consolidar a petição-base."),
                            new RequiredDocument("PROVA_MINIMA", false, "O conjunto documental ainda não demonstra prova mínima organizada.", "Adicionar prova mínima coerente com o pedido.")
                    ),
                    List.of("Revisar coerência procedimental e conjunto documental mínimo.")
            );
        };
    }

    private int resolveScore(List<String> blockers,
                             List<String> alerts,
                             List<String> coveredDocuments,
                             List<RequiredDocument> requiredDocuments) {
        int score = 72;
        score += Math.min(18, coveredDocuments.size() * 8);
        score -= blockers.size() * 20;
        score -= alerts.size() * 6;
        if (requiredDocuments.isEmpty()) {
            score += 4;
        }
        return Math.max(0, Math.min(100, score));
    }

    private static boolean containsAny(String value, String... terms) {
        if (value == null || value.isBlank() || terms == null || terms.length == 0) {
            return false;
        }
        for (String term : terms) {
            String normalized = normalize(term);
            if (!normalized.isBlank() && value.contains(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static String join(String... values) {
        StringBuilder sb = new StringBuilder();
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(value.trim());
        }
        return sb.toString();
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9]+", "_");
        normalized = normalized.replaceAll("_+", "_");
        return normalized.replaceAll("^_|_$", "");
    }

    public record ResolveRequest(String tituloCaso,
                                 String ramoDireito,
                                 String ritoProcessual,
                                 String classeProcessual,
                                 String assuntoTpu,
                                 String materiaPrincipal,
                                 String naturezaJuridica,
                                 String tipoJustica,
                                 String textoBase,
                                 List<String> fatos,
                                 List<String> pedidos,
                                 List<String> documentosAnexados,
                                 boolean tutelaUrgencia,
                                 boolean casoUrgente,
                                 boolean prepararPacoteProtocolo,
                                 boolean representacaoRegular,
                                 boolean sigiloReforcado,
                                 TipoUsuario tipoUsuario) {

        public ResolveRequest {
            fatos = immutableList(fatos);
            pedidos = immutableList(pedidos);
            documentosAnexados = immutableList(documentosAnexados);
        }

        static ResolveRequest empty() {
            return new ResolveRequest(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    false,
                    false,
                    false,
                    true,
                    false,
                    null
            );
        }

        private static List<String> immutableList(List<String> values) {
            if (values == null || values.isEmpty()) {
                return List.of();
            }
            ArrayList<String> out = new ArrayList<>();
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    String trimmed = value.trim();
                    if (!trimmed.isEmpty() && !out.contains(trimmed)) {
                        out.add(trimmed);
                    }
                }
            }
            return out.isEmpty() ? List.of() : List.copyOf(out);
        }
    }

    public record VerificationReport(String profile,
                                     String resolvedTrack,
                                     String resolvedLabel,
                                     boolean strict,
                                     List<String> mandatoryChecks,
                                     List<String> requiredDocuments,
                                     List<String> coveredDocuments,
                                     List<String> missingDocuments,
                                     List<String> alerts,
                                     List<String> blockers,
                                     List<String> finalGates,
                                     Map<String, Object> metadata,
                                     Map<String, Object> workspace) {
        public boolean blocking() {
            return !blockers.isEmpty();
        }
    }

    private record Specification(String profile,
                                 boolean strict,
                                 List<String> mandatoryChecks,
                                 List<RequiredDocument> requiredDocuments,
                                 List<String> alerts) {
    }

    private record RequiredDocument(String code,
                                    boolean blocking,
                                    String blockingMessage,
                                    String alertMessage) {
        String[] matchers() {
            return switch (code) {
                case "VINCULO_FAMILIAR_OU_GUARDA" -> new String[]{"certidao_nascimento", "guarda", "filiacao", "filho", "menor"};
                case "COMPROVACAO_RENDA_OU_CAPACIDADE" -> new String[]{"contracheque", "holerite", "extrato", "renda", "salario"};
                case "CERTIDAO_NASCIMENTO_OU_IDENTIFICACAO_DO_MENOR" -> new String[]{"certidao_nascimento", "identidade", "cpf", "menor"};
                case "SUPORTE_FATICO_DE_ROTINA_E_CONVIVENCIA" -> new String[]{"escola", "residencia", "convivencia", "visita", "saude"};
                case "CERTIDAO_CASAMENTO_OU_UNIAO_ESTAVEL" -> new String[]{"certidao_casamento", "casamento", "uniao_estavel"};
                case "QUADRO_PARTILHA_OU_DECLARACAO_SEM_BENS" -> new String[]{"partilha", "bens", "sem_bens", "patrimonio"};
                case "CDA_OU_LASTRO_EXECUTIVO_EQUIVALENTE" -> new String[]{"cda", "certidao_de_divida_ativa", "divida_ativa"};
                case "DEMONSTRATIVO_MINIMO_DO_DEBITO" -> new String[]{"demonstrativo", "planilha", "debito", "memoria_de_calculo"};
                case "ATO_COATOR_DOCUMENTADO" -> new String[]{"ato_coator", "autoridade_coatora", "decisao", "indeferimento"};
                case "PROVA_PRECONSTITUIDA" -> new String[]{"prova_pre_constituida", "prova_preconstituida", "certidao", "comprovante_especifico", "prova_documental_previa"};
                case "AUTO_DE_PRISAO_OU_FLAGRANTE" -> new String[]{"auto_de_prisao", "flagrante", "apf", "prisao"};
                case "DADOS_PESSOAIS_E_CONDICOES_DO_CUSTODIADO" -> new String[]{"residencia", "trabalho", "familia", "saude"};
                case "ATO_RESTRITIVO_OU_DECISAO_IMPUGNADA" -> new String[]{"decisao", "mandado", "ato", "constrangimento"};
                case "QUALIFICACAO_DO_PACIENTE" -> new String[]{"paciente", "rg", "cpf", "qualificacao"};
                case "LAUDO_OU_PROVA_DE_MATERIALIDADE" -> new String[]{"laudo", "materialidade", "necropsia", "exame"};
                case "PROVA_INDICIARIA_OU_TESTEMUNHAL_REFERENCIADA" -> new String[]{"testemunha", "indicio", "depoimento"};
                case "PROVA_MINIMA_DO_VINCULO" -> new String[]{"ctps", "carteira", "contracheque", "escala", "mensagem"};
                case "PLANILHA_OU_MEMORIA_DAS_VERBAS" -> new String[]{"planilha", "memoria", "verbas", "calculo"};
                case "PROVA_SOCIOECONOMICA_MINIMA" -> new String[]{"cadunico", "cad_unico", "composicao_familiar", "renda"};
                case "LAUDO_OU_RELATORIO_DE_IMPEDIMENTO" -> new String[]{"laudo", "relatorio", "deficiencia", "impedimento"};
                case "CNIS_OU_HISTORICO_CONTRIBUTIVO" -> new String[]{"cnis", "historico_contributivo", "carencia"};
                case "INDEFERIMENTO_ADMINISTRATIVO_OU_PROTOCOLO" -> new String[]{"indeferimento", "protocolo", "der", "administrativo"};
                case "PROVA_MINIMA_DOS_FATOS_ELEITORAIS" -> new String[]{"video", "audio", "midia", "ata", "documento", "representacao"};
                case "MARCO_TEMPORAL_OU_REFERENCIA_AO_CALENDARIO" -> new String[]{"data", "calendario", "prazo", "eleicao"};
                case "PROVA_DO_FATO_GERADOR" -> new String[]{"boletim", "contrato", "foto", "video", "email", "whatsapp", "laudo"};
                case "INDICIOS_DE_DANO" -> new String[]{"recibo", "laudo", "despesa", "dano", "prejuizo"};
                case "DOCUMENTO_DA_OBRIGACAO" -> new String[]{"contrato", "solicitacao", "protocolo", "oficio", "negativa"};
                case "PROVA_DO_DESCUMPRIMENTO" -> new String[]{"mora", "negativa", "descumprimento", "nao_cumpriu", "atraso"};
                case "CONTRATO_OU_INSTRUMENTO_BASE" -> new String[]{"contrato", "instrumento", "aditivo", "clausula"};
                case "PLANILHA_COMPARATIVA_OU_CRITERIO_REVISIONAL" -> new String[]{"planilha", "indice", "tabela", "revisao", "comparativo"};
                case "PECA_BASE" -> new String[]{"peticao", "inicial", "minuta", "manifestacao"};
                case "PROVA_MINIMA" -> new String[]{"documento", "comprovante", "laudo", "contrato", "foto"};
                default -> new String[]{normalize(code)};
            };
        }
    }

    private enum ProcedureTrack {
        FAMILIA_ALIMENTOS("Alimentos"),
        FAMILIA_GUARDA_CONVIVENCIA("Guarda e convivência"),
        FAMILIA_DIVORCIO("Divórcio e dissolução"),
        EXECUCAO_FISCAL("Execução fiscal"),
        MANDADO_DE_SEGURANCA("Mandado de segurança"),
        PENAL_CUSTODIA("Audiência de custódia"),
        PENAL_HABEAS_CORPUS("Habeas corpus"),
        PENAL_JURI("Tribunal do júri"),
        TRABALHISTA("Trabalhista"),
        PREVIDENCIARIO_BPC("Previdenciário BPC"),
        PREVIDENCIARIO_BENEFICIO("Previdenciário benefício"),
        ELEITORAL("Eleitoral"),
        CIVEL_INDENIZATORIA("Cível indenizatória"),
        CIVEL_OBRIGACAO_FAZER("Cível obrigação de fazer"),
        CIVEL_REVISIONAL("Cível revisional"),
        GERAL("Geral");

        private final String label;

        ProcedureTrack(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
