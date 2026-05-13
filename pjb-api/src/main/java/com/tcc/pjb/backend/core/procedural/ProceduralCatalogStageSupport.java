package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport.DefinitionSnapshot;
import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport.DocumentSpec;
import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport.PartyRoleSpec;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.service.rito.model.RitoStage;
import com.tcc.pjb.backend.service.rito.model.WorkTemplate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

final class ProceduralCatalogStageSupport {

    private ProceduralCatalogStageSupport() {
    }

    static List<RitoStage> stagesForWrit(RitoProcessual rito) {
        return List.of(
                stage(FaseProcessual.CONHECIMENTO, List.of(FaseProcessual.RECURSAL), List.of(
                        work("PROTOCOLO_IMPETRACAO_ADV", WorkItemType.MANIFESTACAO, "Protocolar impetração e anexos essenciais", TipoUsuario.ADVOGADO, 1, true, 3),
                        work("EMENDA_DOCUMENTAL_ADV", WorkItemType.JUNTADA, "Complementar documentos exigidos para o writ", TipoUsuario.ADVOGADO, 1, true, 5),
                        work("CONFERENCIA_INICIAL_CARTORIO", WorkItemType.JUNTADA, "Conferir competência, representação e documentos do writ", TipoUsuario.SERVIDOR_FORUM, 2, true, 2),
                        work("DESPACHO_INICIAL_WRIT", WorkItemType.DESPACHO, "Deliberar sobre liminar, notificação e informações", TipoUsuario.JUIZ, 1, true, 3)
                )),
                stage(FaseProcessual.RECURSAL, List.of(), List.of(
                        work("INTERPOSICAO_RECURSO_WRIT", WorkItemType.RECURSO, "Interpor recurso cabível no writ", TipoUsuario.ADVOGADO, 1, true, 5),
                        work("CONTRARRAZOES_WRIT", WorkItemType.MANIFESTACAO, "Apresentar contrarrazões recursais", TipoUsuario.ADVOGADO, 2, false, 5),
                        work("ADMISSIBILIDADE_RECURSAL_WRIT", WorkItemType.DESPACHO, "Examinar admissibilidade recursal", TipoUsuario.JUIZ, 1, true, 2)
                ))
        );
    }

    static List<RitoStage> stagesForHabeasCorpus(RitoProcessual rito) {
        return List.of(
                stage(FaseProcessual.CONHECIMENTO, List.of(FaseProcessual.RECURSAL), List.of(
                        work("IMPETRACAO_HC_ADV", WorkItemType.MANIFESTACAO, "Protocolar habeas corpus com peças essenciais", TipoUsuario.ADVOGADO, 1, true, 1),
                        work("JUNTADA_PECAS_HC", WorkItemType.JUNTADA, "Juntar peças mínimas e prova da coação", TipoUsuario.ADVOGADO, 1, true, 1),
                        work("ANALISE_URGENTE_CARTORIO", WorkItemType.JUNTADA, "Conferir requisitos urgentes do habeas corpus", TipoUsuario.SERVIDOR_FORUM, 1, true, 1),
                        work("DECISAO_LIMINAR_HC", WorkItemType.DECISAO, "Decidir liminarmente sobre a coação alegada", TipoUsuario.JUIZ, 1, true, 1)
                )),
                stage(FaseProcessual.RECURSAL, List.of(), List.of(
                        work("RECURSO_HC_ADV", WorkItemType.RECURSO, "Interpor recurso cabível em habeas corpus", TipoUsuario.ADVOGADO, 1, true, 2),
                        work("ADMISSIBILIDADE_RECURSO_HC", WorkItemType.DESPACHO, "Examinar admissibilidade recursal", TipoUsuario.JUIZ, 1, true, 1)
                ))
        );
    }

    static List<RitoStage> macroStages(RitoProcessual rito, boolean includeCumprimento, boolean includeExecucao, boolean penalFlavor) {
        List<RitoStage> stages = new ArrayList<>();
        stages.add(stage(FaseProcessual.CONHECIMENTO,
                nextList(includeCumprimento ? FaseProcessual.CUMPRIMENTO_SENTENCA : includeExecucao ? FaseProcessual.EXECUCAO : FaseProcessual.RECURSAL),
                conhecimentoTemplates(rito, penalFlavor)));
        if (includeCumprimento) {
            stages.add(stage(FaseProcessual.CUMPRIMENTO_SENTENCA,
                    nextList(includeExecucao ? FaseProcessual.EXECUCAO : FaseProcessual.RECURSAL),
                    cumprimentoTemplates(rito)));
        }
        if (includeExecucao) {
            stages.add(stage(FaseProcessual.EXECUCAO, List.of(FaseProcessual.RECURSAL), execucaoTemplates(rito, penalFlavor)));
        }
        stages.add(stage(FaseProcessual.RECURSAL, List.of(), recursalTemplates(rito)));
        return stages;
    }

    static List<RitoStage> macroStagesEleitorais(RitoProcessual rito) {
        return List.of(
                stage(FaseProcessual.CONHECIMENTO, List.of(FaseProcessual.RECURSAL), List.of(
                        work("PROTOCOLO_INICIAL_ADV", WorkItemType.MANIFESTACAO, "Protocolar petição inicial eleitoral e qualificar corretamente os polos", TipoUsuario.ADVOGADO, 1, true, 3),
                        work("JUNTADA_PROVAS_ELEITORAIS", WorkItemType.JUNTADA, "Juntar prova pré-constituída, mídias e documentos eleitorais", TipoUsuario.ADVOGADO, 1, true, 3),
                        work("RESPOSTA_ELEITORAL_ADV", WorkItemType.MANIFESTACAO, "Apresentar defesa, informações ou impugnação da parte adversa", TipoUsuario.ADVOGADO, 2, false, 5),
                        work("MANIFESTACAO_MP_ELEITORAL", WorkItemType.MANIFESTACAO, "Manifestação do Ministério Público Eleitoral quando cabível", TipoUsuario.MEMBRO_MINISTERIO_PUBLICO, 2, false, 3),
                        work("TRIAGEM_CARTORIO_ELEITORAL", WorkItemType.JUNTADA, "Conferir classe, prazo fatal e requisitos formais", TipoUsuario.SERVIDOR_FORUM, 1, true, 1),
                        work("DESPACHO_INICIAL_ELEITORAL", WorkItemType.DESPACHO, "Deliberar sobre tutela, citação e saneamento inicial", TipoUsuario.JUIZ, 1, true, 2)
                )),
                stage(FaseProcessual.RECURSAL, List.of(), List.of(
                        work("RECURSO_ELEITORAL_ADV", WorkItemType.RECURSO, "Interpor recurso eleitoral cabível", TipoUsuario.ADVOGADO, 1, true, 3),
                        work("CONTRARRAZOES_ELEITORAIS", WorkItemType.MANIFESTACAO, "Apresentar contrarrazões ou resposta recursal", TipoUsuario.ADVOGADO, 2, false, 3),
                        work("ADMISSIBILIDADE_RECURSO_ELEITORAL", WorkItemType.DESPACHO, "Examinar admissibilidade do recurso eleitoral", TipoUsuario.JUIZ, 1, true, 1)
                ))
        );
    }

    static List<RitoStage> macroStagesMilitares(RitoProcessual rito) {
        return List.of(
                stage(FaseProcessual.CONHECIMENTO, List.of(FaseProcessual.RECURSAL), List.of(
                        work("PROTOCOLO_INICIAL_DEFESA_MILITAR", WorkItemType.MANIFESTACAO, "Protocolar petição defensiva militar com qualificação funcional adequada", TipoUsuario.ADVOGADO, 1, true, 3),
                        work("JUNTADA_PECAS_MILITARES", WorkItemType.JUNTADA, "Juntar peças do IPM, portaria e documentos funcionais", TipoUsuario.ADVOGADO, 1, true, 3),
                        work("RESPOSTA_ACUSACAO_MILITAR", WorkItemType.MANIFESTACAO, "Apresentar resposta à acusação, quesitos e rol de testemunhas", TipoUsuario.ADVOGADO, 2, false, 5),
                        work("MANIFESTACAO_MPM", WorkItemType.MANIFESTACAO, "Manifestação do Ministério Público Militar quando cabível", TipoUsuario.MEMBRO_MINISTERIO_PUBLICO, 2, false, 3),
                        work("TRIAGEM_CARTORIO_MILITAR", WorkItemType.JUNTADA, "Conferir competência da auditoria e regularidade formal", TipoUsuario.SERVIDOR_FORUM, 1, true, 1),
                        work("DESPACHO_INICIAL_MILITAR", WorkItemType.DESPACHO, "Deliberar sobre o saneamento e atos militares iniciais", TipoUsuario.JUIZ, 1, true, 2)
                )),
                stage(FaseProcessual.RECURSAL, List.of(), List.of(
                        work("RECURSO_MILITAR_ADV", WorkItemType.RECURSO, "Interpor recurso militar cabível", TipoUsuario.ADVOGADO, 1, true, 3),
                        work("CONTRARRAZOES_MILITARES", WorkItemType.MANIFESTACAO, "Apresentar contrarrazões recursais militares", TipoUsuario.ADVOGADO, 2, false, 3),
                        work("ADMISSIBILIDADE_RECURSO_MILITAR", WorkItemType.DESPACHO, "Examinar admissibilidade do recurso militar", TipoUsuario.JUIZ, 1, true, 1)
                ))
        );
    }

    static List<WorkTemplate> conhecimentoTemplates(RitoProcessual rito, boolean penalFlavor) {
        List<WorkTemplate> out = new ArrayList<>();
        out.add(work("PROTOCOLO_INICIAL_ADV", WorkItemType.MANIFESTACAO, "Protocolar peça inicial e qualificar adequadamente os polos", TipoUsuario.ADVOGADO, 1, true, 3));
        out.add(work("JUNTADA_DOCUMENTOS_ADV", WorkItemType.JUNTADA, "Juntar documentos essenciais, procuração e prova mínima", TipoUsuario.ADVOGADO, 1, true, 3));
        out.add(work("EMENDA_OU_COMPLEMENTO_ADV", WorkItemType.JUNTADA, "Cumprir eventual emenda, saneamento inicial ou complementação documental", TipoUsuario.ADVOGADO, 2, false, 5));
        if (penalFlavor) {
            out.add(work("MANIFESTACAO_MP", WorkItemType.MANIFESTACAO, "Manifestação ministerial quando cabível", TipoUsuario.MEMBRO_MINISTERIO_PUBLICO, 2, false, 3));
        }
        out.add(work("TRIAGEM_CARTORIO", WorkItemType.JUNTADA, "Conferir competência, custas, documentos e representação", TipoUsuario.SERVIDOR_FORUM, 1, true, 2));
        out.add(work("DESPACHO_INICIAL", WorkItemType.DESPACHO, "Deliberar sobre processamento inicial e saneamento", TipoUsuario.JUIZ, 1, true, 3));
        return out;
    }

    static List<WorkTemplate> cumprimentoTemplates(RitoProcessual rito) {
        return List.of(
                work("REQUERIMENTO_CUMPRIMENTO_ADV", WorkItemType.MANIFESTACAO, "Requerer início do cumprimento com atualização da obrigação", TipoUsuario.ADVOGADO, 1, true, 3),
                work("PLANILHA_ATUALIZADA_ADV", WorkItemType.CALCULO, "Apresentar memória discriminada do débito ou obrigação", TipoUsuario.ADVOGADO, 1, true, 3),
                work("INTIMACAO_CUMPRIMENTO_CARTORIO", WorkItemType.INTIMACAO, "Promover intimação do devedor para cumprimento", TipoUsuario.SERVIDOR_FORUM, 2, true, 2),
                work("DECISAO_CUMPRIMENTO", WorkItemType.DECISAO, "Deliberar sobre multa, honorários e atos executivos", TipoUsuario.JUIZ, 1, true, 3)
        );
    }

    static List<WorkTemplate> execucaoTemplates(RitoProcessual rito, boolean penalFlavor) {
        List<WorkTemplate> out = new ArrayList<>();
        out.add(work("REQUERIMENTO_EXECUCAO_ADV", WorkItemType.MANIFESTACAO, "Requerer atos executivos, bloqueios ou medidas coercitivas", TipoUsuario.ADVOGADO, 1, true, 3));
        out.add(work("INDICACAO_BENS_ADV", WorkItemType.JUNTADA, "Indicar bens, fontes de pesquisa e medidas de constrição", TipoUsuario.ADVOGADO, 2, false, 5));
        out.add(work("PESQUISA_PATRIMONIAL_CARTORIO", WorkItemType.DILIGENCIA, "Executar diligências e pesquisas patrimoniais", TipoUsuario.SERVIDOR_FORUM, 2, true, 2));
        out.add(work("DECISAO_EXECUTIVA", WorkItemType.DECISAO, "Deliberar sobre penhora, bloqueio e atos expropriatórios", TipoUsuario.JUIZ, 1, true, 3));
        return out;
    }

    static List<WorkTemplate> recursalTemplates(RitoProcessual rito) {
        return List.of(
                work("INTERPOSICAO_RECURSO_ADV", WorkItemType.RECURSO, "Interpor recurso cabível com regularidade formal", TipoUsuario.ADVOGADO, 1, true, 5),
                work("CONTRARRAZOES_ADV", WorkItemType.MANIFESTACAO, "Apresentar contrarrazões ou resposta ao recurso", TipoUsuario.ADVOGADO, 2, false, 5),
                work("RECEBIMENTO_RECURSO_CARTORIO", WorkItemType.JUNTADA, "Conferir tempestividade e preparo do recurso", TipoUsuario.SERVIDOR_FORUM, 1, true, 2),
                work("JUIZO_ADMISSIBILIDADE", WorkItemType.DESPACHO, "Examinar admissibilidade e encaminhamento do recurso", TipoUsuario.JUIZ, 1, true, 2)
        );
    }

    static List<RitoStage> mergeStages(List<RitoStage> baseStages, List<RitoStage> generatedStages) {
        Map<String, RitoStage> base = toStageMap(baseStages);
        Map<String, RitoStage> generated = toStageMap(generatedStages);
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        ordered.addAll(generated.keySet());
        ordered.addAll(base.keySet());
        List<RitoStage> out = new ArrayList<>();
        for (String fase : ordered) {
            RitoStage existing = base.get(fase);
            RitoStage fallback = generated.get(fase);
            RitoStage merged = new RitoStage();
            merged.setFase(firstNonBlank(existing != null ? existing.getFase() : null, fallback != null ? fallback.getFase() : null, fase));
            merged.setAllowedNext(mergeStringLists(existing != null ? existing.getAllowedNext() : null, fallback != null ? fallback.getAllowedNext() : null));
            merged.setWork(mergeWork(existing != null ? existing.getWork() : null, fallback != null ? fallback.getWork() : null));
            out.add(merged);
        }
        return out;
    }

    static Map<String, RitoStage> toStageMap(List<RitoStage> stages) {
        Map<String, RitoStage> map = new LinkedHashMap<>();
        if (stages == null) {
            return map;
        }
        for (RitoStage stage : stages) {
            if (stage == null || stage.getFase() == null || stage.getFase().isBlank()) {
                continue;
            }
            map.putIfAbsent(stage.getFase().trim().toUpperCase(Locale.ROOT), stage);
        }
        return map;
    }

    static List<WorkTemplate> mergeWork(List<WorkTemplate> base, List<WorkTemplate> generated) {
        Map<String, WorkTemplate> merged = new LinkedHashMap<>();
        if (generated != null) {
            for (WorkTemplate item : generated) {
                if (item == null || item.getCode() == null || item.getCode().isBlank()) {
                    continue;
                }
                merged.put(normalizeToken(item.getCode()), cloneWork(item));
            }
        }
        if (base != null) {
            for (WorkTemplate item : base) {
                if (item == null || item.getCode() == null || item.getCode().isBlank()) {
                    continue;
                }
                String key = normalizeToken(item.getCode());
                if (!merged.containsKey(key)) {
                    merged.put(key, cloneWork(item));
                    continue;
                }
                merged.put(key, mergeWorkItem(cloneWork(item), merged.get(key)));
            }
        }
        return List.copyOf(merged.values());
    }

    static WorkTemplate mergeWorkItem(WorkTemplate preferred, WorkTemplate incoming) {
        WorkTemplate merged = cloneWork(preferred);
        merged.setType(firstNonBlank(preferred.getType(), incoming.getType(), WorkItemType.OUTRO.name()));
        merged.setTitle(firstNonBlank(preferred.getTitle(), incoming.getTitle(), humanize(preferred.getCode())));
        merged.setDescription(firstNonBlank(preferred.getDescription(), incoming.getDescription(), incoming.getTitle()));
        merged.setActorRole(firstNonBlank(preferred.getActorRole(), incoming.getActorRole(), TipoUsuario.SERVIDOR_FORUM.name()));
        merged.setPriority(firstNonNull(preferred.getPriority(), incoming.getPriority(), 3));
        merged.setSlaDays(firstNonNull(preferred.getSlaDays(), incoming.getSlaDays(), 3));
        merged.setBlocking(firstNonNull(preferred.getBlocking(), incoming.getBlocking(), Boolean.FALSE));
        merged.setLegalBases(mergeStringLists(preferred.getLegalBases(), incoming.getLegalBases()));
        return merged;
    }

    static WorkTemplate cloneWork(WorkTemplate original) {
        WorkTemplate copy = new WorkTemplate();
        copy.setCode(original.getCode());
        copy.setType(original.getType());
        copy.setTitle(original.getTitle());
        copy.setDescription(original.getDescription());
        copy.setActorRole(original.getActorRole());
        copy.setPriority(original.getPriority());
        copy.setSlaDays(original.getSlaDays());
        copy.setBlocking(original.getBlocking());
        copy.setLegalBases(original.getLegalBases() == null ? List.of() : List.copyOf(original.getLegalBases()));
        return copy;
    }

    static List<String> mergeStringLists(List<String> primary, List<String> secondary) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (secondary != null) {
            secondary.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).forEach(out::add);
        }
        if (primary != null) {
            primary.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).forEach(out::add);
        }
        return List.copyOf(out);
    }

    static Map<String, Object> stageToMap(RitoStage stage) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("fase", stage.getFase());
        out.put("allowedNext", stage.getAllowedNext() == null ? List.of() : stage.getAllowedNext());
        out.put("work", stage.getWork() == null ? List.of() : stage.getWork().stream().map(w -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", w.getCode());
            item.put("type", w.getType());
            item.put("title", w.getTitle());
            item.put("description", w.getDescription());
            item.put("actorRole", w.getActorRole());
            item.put("priority", w.getPriority());
            item.put("slaDays", w.getSlaDays());
            item.put("blocking", w.getBlocking());
            item.put("legalBases", w.getLegalBases());
            return item;
        }).toList());
        return out;
    }

    static DefinitionSnapshot definition(RitoProcessual rito,
                                                 String title,
                                                 String ramo,
                                                 List<PartyRoleSpec> parties,
                                                 List<DocumentSpec> documents,
                                                 List<RitoStage> stages,
                                                 List<String> competenceHints) {
        return new DefinitionSnapshot(
                rito,
                title,
                ramo,
                List.copyOf(parties),
                List.copyOf(deduplicateDocuments(documents)),
                List.copyOf(stages),
                List.copyOf(new LinkedHashSet<>(competenceHints)),
                true
        );
    }

    static List<DocumentSpec> deduplicateDocuments(List<DocumentSpec> documents) {
        Map<String, DocumentSpec> out = new LinkedHashMap<>();
        for (DocumentSpec item : documents) {
            if (item == null || item.code() == null || item.code().isBlank()) {
                continue;
            }
            String key = normalizeToken(item.code());
            out.merge(key, item, (a, b) -> new DocumentSpec(a.code(), a.required() || b.required(), firstNonBlank(a.rationale(), b.rationale(), a.code())));
        }
        return List.copyOf(out.values());
    }

    static List<PartyRoleSpec> parties(PartyRoleSpec... specs) {
        return List.of(specs);
    }

    static List<DocumentSpec> documents(DocumentSpec... specs) {
        return List.of(specs);
    }

    static PartyRoleSpec role(String code, boolean required, boolean external) {
        List<String> aliases = new ArrayList<>();
        aliases.add(code);
        aliases.addAll(ProceduralCatalogSupport.aliasesForRole(code));
        return new PartyRoleSpec(code, required, external, aliases);
    }

    static DocumentSpec doc(String code, boolean required, String rationale) {
        return new DocumentSpec(code, required, rationale);
    }

    static RitoStage stage(FaseProcessual fase, List<FaseProcessual> allowedNext, List<WorkTemplate> work) {
        RitoStage stage = new RitoStage();
        stage.setFase(fase.name());
        stage.setAllowedNext(allowedNext.stream().map(Enum::name).toList());
        stage.setWork(work);
        return stage;
    }

    static WorkTemplate work(String code,
                                     WorkItemType type,
                                     String title,
                                     TipoUsuario actorRole,
                                     int priority,
                                     boolean blocking,
                                     int slaDays) {
        WorkTemplate work = new WorkTemplate();
        work.setCode(code);
        work.setType(type.name());
        work.setTitle(title);
        work.setDescription(title);
        work.setActorRole(actorRole.name());
        work.setPriority(priority);
        work.setSlaDays(slaDays);
        work.setBlocking(blocking);
        work.setLegalBases(List.of());
        return work;
    }

    static List<FaseProcessual> nextList(FaseProcessual next) {
        return List.of(next);
    }

    static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    @SafeVarargs
    static <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    static String humanize(String value) {
        return Arrays.stream(value.split("_"))
                .filter(s -> !s.isBlank())
                .map(s -> s.substring(0, 1) + s.substring(1).toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));
    }

    static String normalizeToken(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("[^A-Za-z0-9]+", "_").replaceAll("_{2,}", "_").toUpperCase(Locale.ROOT);
    }
}
