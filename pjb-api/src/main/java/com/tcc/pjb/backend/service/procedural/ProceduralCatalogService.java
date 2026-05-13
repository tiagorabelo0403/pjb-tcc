package com.tcc.pjb.backend.service.procedural;

import com.tcc.pjb.backend.core.catalog.TpuClasseCnj;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport;
import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport.DefinitionSnapshot;
import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport.PartyRoleSpec;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.rito.model.RitoDefinition;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ProceduralCatalogService {

    public DefinitionSnapshot snapshot(RitoProcessual rito) {
        return ProceduralCatalogSupport.snapshot(normalizeRito(rito));
    }

    public DefinitionSnapshot snapshot(String ritoName) {
        return snapshot(resolveRito(ritoName, null, null));
    }

    public DefinitionSnapshot snapshot(String ritoRaw, String ramoRaw, String classeTpu) {
        return snapshot(resolveRito(ritoRaw, ramoRaw, classeTpu));
    }

    public RitoDefinition enrichDefinition(RitoProcessual rito, RitoDefinition base) {
        return ProceduralCatalogSupport.enrichDefinition(normalizeRito(rito), base);
    }

    public RitoDefinition enrichDefinition(String ritoRaw, String ramoRaw, String classeTpu, RitoDefinition base) {
        return enrichDefinition(resolveRito(ritoRaw, ramoRaw, classeTpu), base);
    }

    public List<String> requiredDocuments(RitoProcessual rito) {
        return ProceduralCatalogSupport.requiredDocuments(normalizeRito(rito));
    }

    public List<String> requiredDocuments(String ritoName) {
        return requiredDocuments(resolveRito(ritoName, null, null));
    }

    public List<String> requiredDocuments(String ritoRaw, String ramoRaw, String classeTpu) {
        return requiredDocuments(resolveRito(ritoRaw, ramoRaw, classeTpu));
    }

    public List<PartyRoleSpec> requiredParties(RitoProcessual rito) {
        return ProceduralCatalogSupport.requiredParties(normalizeRito(rito));
    }

    public List<PartyRoleSpec> requiredParties(String ritoName) {
        return requiredParties(resolveRito(ritoName, null, null));
    }

    public List<PartyRoleSpec> requiredParties(String ritoRaw, String ramoRaw, String classeTpu) {
        return requiredParties(resolveRito(ritoRaw, ramoRaw, classeTpu));
    }

    public Map<String, Object> describe(RitoProcessual rito) {
        return ProceduralCatalogSupport.describe(normalizeRito(rito));
    }

    public Map<String, Object> describe(String ritoName) {
        return describe(resolveRito(ritoName, null, null));
    }

    public Map<String, Object> describe(String ritoRaw, String ramoRaw, String classeTpu) {
        return describe(resolveRito(ritoRaw, ramoRaw, classeTpu));
    }

    public List<Map<String, Object>> catalog() {
        return ProceduralCatalogSupport.catalog();
    }

    public List<RitoProcessual> catalogDrivenRitos() {
        return ProceduralCatalogSupport.catalogDrivenRitos();
    }

    public List<RitoProcessual> allKnownRitos() {
        List<RitoProcessual> canonical = ProceduralCatalogSupport.allKnownRitos();
        return canonical.stream().sorted((a, b) -> {
            int ia = canonical.indexOf(a);
            int ib = canonical.indexOf(b);
            if (ia >= 0 && ib >= 0) {
                return Integer.compare(ia, ib);
            }
            if (ia >= 0) {
                return -1;
            }
            if (ib >= 0) {
                return 1;
            }
            return a.name().compareToIgnoreCase(b.name());
        }).toList();
    }

    public int totalProcedures() {
        return catalogDrivenRitos().size();
    }

    public Map<String, Object> coverage() {
        List<RitoProcessual> ritos = catalogDrivenRitos();
        List<Map<String, Object>> items = ritos.stream()
                .map(rito -> {
                    DefinitionSnapshot snapshot = snapshot(rito);
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("rito", rito.name());
                    row.put("grupo", rito.group());
                    row.put("ramo", rito.suggestedRamo().name());
                    row.put("requiredParties", snapshot.parties().stream().filter(PartyRoleSpec::required).count());
                    row.put("requiredDocuments", snapshot.documents().stream().filter(doc -> doc.required()).count());
                    row.put("stages", snapshot.stages().size());
                    row.put("hasExternalActor", snapshot.parties().stream().anyMatch(PartyRoleSpec::external));
                    row.put("generated", snapshot.generated());
                    row.put("classesTpu", resolveClasseTpu(null, rito).map(TpuClasseCnj::toMap).stream().toList());
                    return row;
                })
                .toList();
        long total = items.size();
        long withStages = items.stream().filter(item -> ((Number) item.get("stages")).intValue() > 0).count();
        long withParties = items.stream().filter(item -> ((Number) item.get("requiredParties")).intValue() > 0).count();
        long withDocuments = items.stream().filter(item -> ((Number) item.get("requiredDocuments")).intValue() > 0).count();
        long withExternal = items.stream().filter(item -> Boolean.TRUE.equals(item.get("hasExternalActor"))).count();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalRitos", total);
        out.put("withStages", withStages);
        out.put("withRequiredParties", withParties);
        out.put("withRequiredDocuments", withDocuments);
        out.put("withExternalActor", withExternal);
        out.put("totalClassesTpu", TpuClasseCnj.values().length);
        out.put("totalTribunaisNacionais", NationalCompetenceMatrix.values().length);
        out.put("items", items);
        return Collections.unmodifiableMap(out);
    }

    public RitoProcessual resolveRito(String ritoRaw, String ramoRaw, String classeTpu) {
        return normalizeRito(ProceduralCatalogSupport.resolveRito(ritoRaw, ramoRaw, classeTpu));
    }

    public String resolveRitoName(String ritoRaw, String ramoRaw, String classeTpu) {
        return resolveRito(ritoRaw, ramoRaw, classeTpu).name();
    }

    public String resolveRitoName(Map<String, Object> payload) {
        return resolveRito(payload).name();
    }

    public RitoProcessual resolveRito(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return normalizeRito(null);
        }
        String ritoRaw = firstNonBlank(text(payload.get("rito")), text(payload.get("rito_processual")), text(payload.get("ritoProcessual")), text(payload.get("procedimento")), text(payload.get("ritoNome")));
        String ramoRaw = firstNonBlank(text(payload.get("ramoDireito")), text(payload.get("ramo_direito")), text(payload.get("materia")), text(payload.get("ramo")));
        String classeRaw = firstNonBlank(text(payload.get("classeTpu")), text(payload.get("classe_tpu")), text(payload.get("classeProcessual")), text(payload.get("classe")), text(payload.get("tipoAcao")), text(payload.get("classeTpuCodigo")), text(payload.get("classeTpuNome")));
        return resolveRito(ritoRaw, ramoRaw, classeRaw);
    }

    public Optional<TpuClasseCnj> resolveClasseTpu(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return Optional.empty();
        }
        String classeRaw = firstNonBlank(text(payload.get("classeTpu")), text(payload.get("classe_tpu")), text(payload.get("classeProcessual")), text(payload.get("classe")), text(payload.get("tipoAcao")), text(payload.get("classeTpuCodigo")), text(payload.get("classeTpuNome")));
        return resolveClasseTpu(classeRaw, resolveRito(payload));
    }

    public Optional<TpuClasseCnj> resolveClasseTpu(String codigoOuNome, RitoProcessual fallbackRito) {
        if (codigoOuNome != null && !codigoOuNome.isBlank()) {
            Optional<TpuClasseCnj> byName = TpuClasseCnj.porNome(codigoOuNome);
            if (byName.isPresent()) {
                return byName;
            }
            try {
                return TpuClasseCnj.porCodigo(Integer.parseInt(codigoOuNome.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        RitoProcessual rito = normalizeRito(fallbackRito);
        Optional<TpuClasseCnj> byRitoName = TpuClasseCnj.porNome(rito.name());
        if (byRitoName.isPresent()) {
            return byRitoName;
        }
        Optional<TpuClasseCnj> byRitoMapping = inferClasseByRito(rito);
        if (byRitoMapping.isPresent()) {
            return byRitoMapping;
        }
        return catalogBaseForRamo(rito.suggestedRamo()).stream().findFirst();
    }

    public Optional<TpuClasseCnj> resolveClasseTpu(String codigoOuNome, String ritoRaw, String ramoRaw) {
        return resolveClasseTpu(codigoOuNome, resolveRito(ritoRaw, ramoRaw, codigoOuNome));
    }

    public List<Map<String, Object>> listTpuClasses() {
        return TpuClasseCnj.todasEmOrdemCodigo().stream().map(TpuClasseCnj::toMap).toList();
    }

    public List<Map<String, Object>> listTpuClassesByRamo(String ramoRaw) {
        RamoDireito ramo = ramoRaw == null ? null : RamoDireito.fromString(ramoRaw);
        if (ramo == null) {
            return listTpuClasses();
        }
        return catalogBaseForRamo(ramo).stream().map(TpuClasseCnj::toMap).toList();
    }

    public Map<String, Object> describeClasseTpu(String codigoOuNome, RitoProcessual fallbackRito) {
        Optional<TpuClasseCnj> resolved = resolveClasseTpu(codigoOuNome, fallbackRito);
        return resolved.map(TpuClasseCnj::toMap).orElseGet(() -> unresolvedClasse(codigoOuNome, fallbackRito != null ? fallbackRito.name() : null));
    }

    private Optional<TpuClasseCnj> inferClasseByRito(RitoProcessual rito) {
        if (rito == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(switch (rito) {
            case JUIZADO_ESPECIAL_CIVEL, JUIZADO_ESPECIAL -> TpuClasseCnj.JUIZADO_ESPECIAL_CIVEL;
            case JUIZADO_ESPECIAL_FEDERAL -> TpuClasseCnj.JUIZADO_ESPECIAL_FEDERAL;
            case JUIZADO_ESPECIAL_FAZENDA_PUBLICA -> TpuClasseCnj.JUIZADO_ESPECIAL_FAZENDA_PUBLICA;
            case JUIZADO_ESPECIAL_CRIMINAL, PROCEDIMENTO_PENAL_SUMARISSIMO -> TpuClasseCnj.JUIZADO_ESPECIAL_CRIMINAL;
            case CIVIL_ACAO_MONITORIA -> TpuClasseCnj.ACAO_MONITORIA;
            case CIVIL_USUCAPIAO -> TpuClasseCnj.USUCAPIAO;
            case CIVIL_POSSESSORIA -> TpuClasseCnj.REINTEGRACAO_POSSE;
            case CIVIL_INTERDITO_PROIBITORIO -> TpuClasseCnj.INTERDITO_PROIBITORIO;
            case CIVIL_CONSIGNACAO_PAGAMENTO -> TpuClasseCnj.CONSIGNACAO_PAGAMENTO;
            case CIVIL_RETIFICACAO_REGISTRO -> TpuClasseCnj.RETIFICACAO_REGISTRO;
            case CIVIL_NUNCIACAO_OBRA_NOVA -> TpuClasseCnj.NUNCIACAO_OBRA_NOVA;
            case CIVIL_ACAO_CIVIL_PUBLICA, AMBIENTAL_ACP, AGRARIO_ACP_AGRARIA -> TpuClasseCnj.ACAO_CIVIL_PUBLICA;
            case CIVIL_TUTELA_CAUTELAR_ANTECEDENTE -> TpuClasseCnj.TUTELA_CAUTELAR_ANTECEDENTE;
            case CIVIL_TUTELA_ANTECIPADA_ANTECEDENTE -> TpuClasseCnj.TUTELA_ANTECIPADA_ANTECEDENTE;
            case ESPECIAL_MANDADO_SEGURANCA -> TpuClasseCnj.MANDADO_SEGURANCA_INDIVIDUAL;
            case ESPECIAL_MANDADO_SEGURANCA_COLETIVO -> TpuClasseCnj.MANDADO_SEGURANCA_COLETIVO;
            case ESPECIAL_HABEAS_CORPUS, PENAL_HABEAS_CORPUS_PREVENTIVO -> TpuClasseCnj.HABEAS_CORPUS_PENAL;
            case ESPECIAL_HABEAS_DATA -> TpuClasseCnj.HABEAS_DATA;
            case ESPECIAL_ACAO_POPULAR, ADMINISTRATIVO_ACAO_POPULAR -> TpuClasseCnj.ACAO_POPULAR;
            case ESPECIAL_MANDADO_INJUNCAO -> TpuClasseCnj.MANDADO_INJUNCAO;
            case ESPECIAL_MANDADO_INJUNCAO_COLETIVO -> TpuClasseCnj.MANDADO_INJUNCAO_COLETIVO;
            case ESPECIAL_ACAO_DIRETA_INCONSTITUCIONALIDADE -> TpuClasseCnj.ACAO_DIRETA_INCONSTITUCIONALIDADE;
            case ESPECIAL_ACAO_DECLARATORIA_CONSTITUCIONALIDADE -> TpuClasseCnj.ACAO_DECLARATORIA_CONSTITUCIONALIDADE;
            case ESPECIAL_ARGUICAO_DESCUMPRIMENTO_PRECEITO_FUNDAMENTAL -> TpuClasseCnj.ARGUICAO_DESCUMPRIMENTO_PRECEITO;
            case IMPROBIDADE_ADMINISTRATIVA -> TpuClasseCnj.ACAO_IMPROBIDADE_ADMINISTRATIVA;
            case ADMINISTRATIVO_PAD -> TpuClasseCnj.PAD_PROCESSO_ADMINISTRATIVO;
            case ADMINISTRATIVO_CONCURSO_PUBLICO -> TpuClasseCnj.ACAO_CONCURSO_PUBLICO;
            case ADMINISTRATIVO_SERVIDORES -> TpuClasseCnj.MANDADO_SEGURANCA_SERVIDOR;
            case EXECUCAO_FISCAL -> TpuClasseCnj.EXECUCAO_FISCAL;
            case TRIBUTARIO_ANULATORIA_DEBITO -> TpuClasseCnj.ACAO_ANULATORIA_DEBITO_FISCAL;
            case TRIBUTARIO_REPETICAO_INDEBITO -> TpuClasseCnj.ACAO_REPETICAO_INDEBITO;
            case TRIBUTARIO_MANDADO_SEGURANCA -> TpuClasseCnj.MANDADO_SEGURANCA_TRIBUTARIO;
            case TRIBUTARIO_EMBARGOS_EXECUCAO_FISCAL -> TpuClasseCnj.EMBARGOS_EXECUCAO_FISCAL;
            case TRIBUTARIO_DECLARATORIA -> TpuClasseCnj.ACAO_DECLARATORIA_TRIBUTO;
            case TRIBUTARIO_CAUTELAR_FISCAL -> TpuClasseCnj.CAUTELAR_FISCAL;
            case PROCEDIMENTO_PENAL_COMUM -> TpuClasseCnj.DENUNCIA_CRIME_DOLOSO_PENA_RECLUSAO;
            case PROCEDIMENTO_PENAL_SUMARIO -> TpuClasseCnj.DENUNCIA_CRIME_PENA_DETENCAO;
            case TRIBUNAL_JURI -> TpuClasseCnj.DENUNCIA_JURI;
            case PENAL_LEI_DROGAS -> TpuClasseCnj.ACAO_PENAL_LEI_DROGAS;
            case PENAL_MARIA_DA_PENHA -> TpuClasseCnj.ACAO_PENAL_MARIA_PENHA;
            case PENAL_LAVAGEM_DINHEIRO -> TpuClasseCnj.ACAO_PENAL_LAVAGEM;
            case PENAL_ORGANIZACAO_CRIMINOSA -> TpuClasseCnj.ACAO_PENAL_ORGANIZACAO_CRIMINOSA;
            case PENAL_CRIMES_CIBERNETICOS -> TpuClasseCnj.ACAO_PENAL_CRIMES_INFORMATICOS;
            case PENAL_REVISAO_CRIMINAL -> TpuClasseCnj.REVISAO_CRIMINAL;
            case EXECUCAO_PENAL -> TpuClasseCnj.EXECUCAO_PENAL;
            case INFANCIA_JUVENTUDE_ECA -> TpuClasseCnj.GUARDA_ECA;
            case INFANCIA_JUVENTUDE_ADOCAO -> TpuClasseCnj.ADOCAO_NACIONAL;
            case INFANCIA_JUVENTUDE_INFRACIONAL -> TpuClasseCnj.ACAO_PENAL_ECA;
            case INFANCIA_JUVENTUDE_TUTELA_CURATELA_MENOR -> TpuClasseCnj.GUARDA_ECA;
            case AMBIENTAL_CRIMINAL -> TpuClasseCnj.ACAO_PENAL_CRIMES_AMBIENTAIS;
            case TRABALHISTA_ORDINARIO, TRABALHISTA_SUMARISSIMO, TRABALHISTA_SUMARIO_ALCADA, TRABALHISTA_INQUERITO_FALTA_GRAVE -> TpuClasseCnj.RECLAMACAO_TRABALHISTA_INDIVIDUAL;
            case TRABALHISTA_DISSIDIO_COLETIVO -> TpuClasseCnj.DISSIDIO_COLETIVO_ECONOMICO;
            case TRABALHISTA_ACAO_CUMPRIMENTO -> TpuClasseCnj.CUMPRIMENTO_ACORDO_TRABALHISTA;
            case TRABALHISTA_ACAO_RESCISORIA -> TpuClasseCnj.ACAO_RESCISORIA_TRABALHISTA;
            case TRABALHISTA_MANDADO_SEGURANCA -> TpuClasseCnj.MANDADO_SEGURANCA_TRABALHISTA;
            case TRABALHISTA_EXECUCAO -> TpuClasseCnj.EXECUCAO_TRABALHISTA;
            case TRABALHISTA_CUMPRIMENTO_SENTENCA -> TpuClasseCnj.CUMPRIMENTO_ACORDO_TRABALHISTA;
            case TRABALHISTA_ACIDENTE_TRABALHO -> TpuClasseCnj.ACAO_TRABALHISTA_ACIDENTE_TRABALHO;
            case PREVIDENCIARIO_JEF, PREVIDENCIARIO_COMUM, PREVIDENCIARIO_RESTABELECIMENTO -> TpuClasseCnj.CONCESSAO_BENEFICIO_PREVIDENCIARIO;
            case PREVIDENCIARIO_BPC_LOAS -> TpuClasseCnj.BPC_LOAS;
            case PREVIDENCIARIO_AUXILIO_INCAPACIDADE, PREVIDENCIARIO_ACIDENTARIO -> TpuClasseCnj.AUXILIO_INCAPACIDADE;
            case PREVIDENCIARIO_APOSENTADORIA -> TpuClasseCnj.APOSENTADORIA;
            case PREVIDENCIARIO_REVISAO_BENEFICIO -> TpuClasseCnj.REVISAO_BENEFICIO_PREVIDENCIARIO;
            case PREVIDENCIARIO_SALARIO_MATERNIDADE -> TpuClasseCnj.SALARIO_MATERNIDADE;
            case PREVIDENCIARIO_PENSAO_MORTE -> TpuClasseCnj.PENSAO_MORTE_PREVIDENCIARIA;
            case PREVIDENCIARIO_RURAL, PREVIDENCIARIO_ESPECIAL -> TpuClasseCnj.APOSENTADORIA_ESPECIAL;
            case PREVIDENCIARIO_RPPS -> TpuClasseCnj.RPPS_REGIME_PROPRIO;
            case ELEITORAL_REGISTRO_CANDIDATURA -> TpuClasseCnj.REGISTRO_CANDIDATURA;
            case ELEITORAL_AIRC -> TpuClasseCnj.AIRC_IMPUGNACAO_REGISTRO;
            case ELEITORAL_AIJE -> TpuClasseCnj.AIJE_ABUSO_PODER;
            case ELEITORAL_AIME -> TpuClasseCnj.AIME_MANDATO_ELETIVO;
            case ELEITORAL_RCED -> TpuClasseCnj.RCED_RECURSO_DIPLOMA;
            case ELEITORAL_PROPAGANDA -> TpuClasseCnj.REPRESENTACAO_PROPAGANDA;
            case ELEITORAL_DIREITO_RESPOSTA -> TpuClasseCnj.DIREITO_RESPOSTA_ELEITORAL;
            case ELEITORAL_PRESTACAO_CONTAS -> TpuClasseCnj.PRESTACAO_CONTAS_ELEITORAL;
            case ELEITORAL_INELEGIBILIDADE -> TpuClasseCnj.INELEGIBILIDADE_LCP64;
            case ELEITORAL_CAPTACAO_ILICITA_SUFRAGIO -> TpuClasseCnj.CAPTACAO_ILICITA_SUFRAGIO;
            case MILITAR_IPM -> TpuClasseCnj.IPM_INQUERITO_POLICIAL_MILITAR;
            case MILITAR_PROCESSO_PENAL_MILITAR, MILITAR_CONSELHO_JUSTICA -> TpuClasseCnj.ACAO_PENAL_MILITAR;
            case MILITAR_HABEAS_CORPUS_MILITAR -> TpuClasseCnj.HABEAS_CORPUS_MILITAR;
            case MILITAR_PAD -> TpuClasseCnj.PAD_MILITAR;
            case RECUPERACAO_JUDICIAL -> TpuClasseCnj.RECUPERACAO_JUDICIAL;
            case RECUPERACAO_EXTRAJUDICIAL -> TpuClasseCnj.RECUPERACAO_EXTRAJUDICIAL;
            case FALENCIA -> TpuClasseCnj.FALENCIA;
            case INCIDENTE_DESCONSIDERACAO_PERSONALIDADE_JURIDICA -> TpuClasseCnj.INCIDENTE_DESCONSIDERACAO;
            case AGRARIO_DESAPROPRIACAO -> TpuClasseCnj.DESAPROPRIACAO_REFORMA_AGRARIA;
            case AGRARIO_USUCAPIAO_RURAL -> TpuClasseCnj.USUCAPIAO_ESPECIAL_RURAL;
            case AGRARIO_POSSE_TERRA -> TpuClasseCnj.DESAPROPRIACAO_INTERESSE_SOCIAL;
            case HOMOLOGACAO_SENTENCA_ESTRANGEIRA -> TpuClasseCnj.HOMOLOGACAO_SENTENCA_ESTRANGEIRA;
            case CARTA_ROGATORIA -> TpuClasseCnj.CARTA_ROGATORIA;
            case COOPERACAO_JURIDICA_INTERNACIONAL -> TpuClasseCnj.COOPERACAO_JURIDICA_INTERNACIONAL;
            default -> null;
        });
    }

    private List<TpuClasseCnj> catalogBaseForRamo(RamoDireito ramo) {
        if (ramo == null) {
            return TpuClasseCnj.todasEmOrdemCodigo();
        }
        List<TpuClasseCnj> byRamo = TpuClasseCnj.porRamo(ramo);
        return byRamo.isEmpty() ? TpuClasseCnj.todasEmOrdemCodigo() : byRamo;
    }

    public Map<String, Object> describeClasseTpu(String codigoOuNome, String ritoRaw, String ramoRaw) {
        Optional<TpuClasseCnj> resolved = resolveClasseTpu(codigoOuNome, ritoRaw, ramoRaw);
        return resolved.map(TpuClasseCnj::toMap).orElseGet(() -> unresolvedClasse(codigoOuNome, resolveRito(ritoRaw, ramoRaw, codigoOuNome).name()));
    }

    public List<Map<String, Object>> listNationalTribunals() {
        return Arrays.stream(NationalCompetenceMatrix.values())
                .map(NationalCompetenceMatrix::toMap)
                .toList();
    }

    public Optional<NationalCompetenceMatrix> resolveNationalTribunal(String codigo) {
        return NationalCompetenceMatrix.porCodigo(codigo);
    }

    public List<Map<String, Object>> listNationalTribunalsByUf(String uf) {
        if (uf == null || uf.isBlank()) {
            return listNationalTribunals();
        }
        return NationalCompetenceMatrix.porUF(uf.trim().toUpperCase(Locale.ROOT)).stream()
                .map(NationalCompetenceMatrix::toMap)
                .toList();
    }

    private RitoProcessual normalizeRito(RitoProcessual rito) {
        return Objects.requireNonNullElse(rito, RitoProcessual.COMUM_ORDINARIO);
    }

    private static Map<String, Object> unresolvedClasse(String codigoOuNome, String fallbackRito) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("input", codigoOuNome);
        out.put("resolved", false);
        if (fallbackRito != null && !fallbackRito.isBlank()) {
            out.put("fallbackRito", fallbackRito);
        }
        return Collections.unmodifiableMap(out);
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
