package com.tcc.pjb.backend.model.entity.enums.jurisdicao;

import com.tcc.pjb.backend.model.entity.enums.NivelSegurancaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.PoliticaSeguranca;
import com.tcc.pjb.backend.model.entity.enums.TipoAudiencia;
import java.text.Normalizer;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public enum GrauJurisdicao {

    PRIMEIRO_GRAU(
            1,
            "1º Grau",
            "Juízo singular (Varas, Juizados e unidades originárias).",
            false,
            1,
            Set.of(
                    TipoAudiencia.CONCILIACAO,
                    TipoAudiencia.MEDIACAO,
                    TipoAudiencia.INSTRUCAO,
                    TipoAudiencia.JUSTIFICACAO,
                    TipoAudiencia.CUSTODIA,
                    TipoAudiencia.INTERROGATORIO,
                    TipoAudiencia.OITIVA_TESTEMUNHA,
                    TipoAudiencia.UNA_TRABALHO,
                    TipoAudiencia.PUBLICA_AMBIENTAL
            ),
            Set.of(
                    "PETICAO_INICIAL",
                    "CITACAO",
                    "INTIMACAO",
                    "CONTESTACAO",
                    "REPLICA",
                    "SANEAMENTO",
                    "PRODUCAO_PROVA",
                    "PERICIA",
                    "AUDIENCIA",
                    "SENTENCA"
            ),
            Set.of(
                    "INTIMACAO",
                    "CITACAO",
                    "DESPACHO",
                    "DECISAO_INTERLOCUTORIA",
                    "SENTENCA"
            )
    ) {
        @Override
        public NivelSegurancaInstitucional nivelSeguranca() {
            return NivelSegurancaInstitucional.PADRAO;
        }

        @Override
        public boolean exigeSigilo() {
            return false;
        }

        @Override
        public Set<PoliticaSeguranca> politicasMinimas() {
            return Set.of();
        }
    },

    SEGUNDO_GRAU(
            2,
            "2º Grau",
            "Órgãos revisores colegiados (Câmaras/Turmas/Seções).",
            true,
            3,
            Set.of(
                    TipoAudiencia.SUSTENTACAO_ORAL,
                    TipoAudiencia.JULGAMENTO_COLEGIADO,
                    TipoAudiencia.SESSAO_ADMINISTRATIVA
            ),
            Set.of(
                    "APELACAO",
                    "AGRAVO",
                    "EMBARGOS_DECLARACAO",
                    "CONTRARRAZOES",
                    "PAUTA_JULGAMENTO",
                    "SUSTENTACAO_ORAL",
                    "ACORDAO"
            ),
            Set.of(
                    "INTIMACAO",
                    "PAUTA_JULGAMENTO",
                    "ACORDAO"
            )
    ) {
        @Override
        public NivelSegurancaInstitucional nivelSeguranca() {
            return NivelSegurancaInstitucional.REFORCADO;
        }

        @Override
        public boolean exigeSigilo() {
            return false;
        }

        @Override
        public Set<PoliticaSeguranca> politicasMinimas() {
            return Set.of(
                    PoliticaSeguranca.CONTROLE_ACESSO_RIGOROSO,
                    PoliticaSeguranca.LOG_TEMPORAL,
                    PoliticaSeguranca.VALIDACAO_DUPLA
            );
        }
    },

    SUPERIOR(
            3,
            "Tribunais Superiores",
            "Cúpula infraconstitucional (filtros fortes de admissibilidade e precedentes).",
            true,
            3,
            Set.of(
                    TipoAudiencia.SUSTENTACAO_ORAL,
                    TipoAudiencia.JULGAMENTO_COLEGIADO,
                    TipoAudiencia.SESSAO_ADMINISTRATIVA
            ),
            Set.of(
                    "RECURSO_ESPECIAL",
                    "RECURSO_REVISTA",
                    "RECURSO_ORDINARIO_ELEITORAL",
                    "AGRAVO_RECURSAL",
                    "EMBARGOS_DECLARACAO",
                    "AFETACAO_PRECEDENTE",
                    "JULGAMENTO_COLEGIADO",
                    "ACORDAO"
            ),
            Set.of(
                    "INTIMACAO",
                    "PAUTA_JULGAMENTO",
                    "ACORDAO",
                    "DECISAO_ADMISSIBILIDADE"
            )
    ) {
        @Override
        public NivelSegurancaInstitucional nivelSeguranca() {
            return NivelSegurancaInstitucional.ALTO;
        }

        @Override
        public boolean exigeSigilo() {
            return false;
        }

        @Override
        public Set<PoliticaSeguranca> politicasMinimas() {
            return Set.of(
                    PoliticaSeguranca.CONTROLE_ACESSO_RIGOROSO,
                    PoliticaSeguranca.AUDITORIA_INTEGRAL,
                    PoliticaSeguranca.REGISTRO_IMUTAVEL,
                    PoliticaSeguranca.VALIDACAO_DUPLA,
                    PoliticaSeguranca.RESPONSABILIDADE_FUNCIONAL,
                    PoliticaSeguranca.LOG_TEMPORAL
            );
        }
    },

    CONSTITUCIONAL(
            4,
            "Máximo Constitucional",
            "Cúpula constitucional (guarda da Constituição; impacto sistêmico).",
            true,
            5,
            Set.of(
                    TipoAudiencia.SUSTENTACAO_ORAL,
                    TipoAudiencia.JULGAMENTO_COLEGIADO,
                    TipoAudiencia.SESSAO_ADMINISTRATIVA
            ),
            Set.of(
                    "RECURSO_EXTRAORDINARIO",
                    "AGRAVO_RE",
                    "REPERCUSSAO_GERAL",
                    "PREQUESTIONAMENTO",
                    "OBICE_SUMULAR",
                    "PAUTA_PLENARIO",
                    "PLENARIO",
                    "ACORDAO"
            ),
            Set.of(
                    "INTIMACAO",
                    "PAUTA_PLENARIO",
                    "DECISAO",
                    "ACORDAO"
            )
    ) {
        @Override
        public NivelSegurancaInstitucional nivelSeguranca() {
            return NivelSegurancaInstitucional.MAXIMO_CONSTITUCIONAL;
        }

        @Override
        public boolean exigeSigilo() {
            return true;
        }

        @Override
        public Set<PoliticaSeguranca> politicasMinimas() {
            return Set.of(
                    PoliticaSeguranca.CONTROLE_ACESSO_RIGOROSO,
                    PoliticaSeguranca.AUDITORIA_INTEGRAL,
                    PoliticaSeguranca.REGISTRO_IMUTAVEL,
                    PoliticaSeguranca.VALIDACAO_DUPLA,
                    PoliticaSeguranca.BLOQUEIO_DELEGACAO,
                    PoliticaSeguranca.RESPONSABILIDADE_FUNCIONAL,
                    PoliticaSeguranca.LOG_TEMPORAL
            );
        }
    };

    
    
    
    private final int codigo;
    private final String label;
    private final String descricao;
    private final boolean colegiado;
    private final int quorumMinimoSugerido;

    
    private final Set<TipoAudiencia> audienciasPermitidasNormalizadas;
    private final Set<String> atosRelevantes;
    private final Set<String> publicacoesPermitidas;

    
    private static final Map<Integer, GrauJurisdicao> POR_CODIGO =
            Map.of(
                    1, PRIMEIRO_GRAU,
                    2, SEGUNDO_GRAU,
                    3, SUPERIOR,
                    4, CONSTITUCIONAL
            );

    GrauJurisdicao(
            int codigo,
            String label,
            String descricao,
            boolean colegiado,
            int quorumMinimoSugerido,
            Set<TipoAudiencia> audienciasPermitidas,
            Set<String> atosRelevantes,
            Set<String> publicacoesPermitidas
    ) {
        if (codigo <= 0) throw new IllegalArgumentException("codigo deve ser >= 1");
        this.codigo = codigo;
        this.label = Objects.requireNonNull(label, "label");
        this.descricao = Objects.requireNonNull(descricao, "descricao");
        this.colegiado = colegiado;
        this.quorumMinimoSugerido = Math.max(1, quorumMinimoSugerido);

        
        this.audienciasPermitidasNormalizadas = (audienciasPermitidas == null)
                ? Set.of()
                : audienciasPermitidas.stream()
                .filter(Objects::nonNull)
                .map(TipoAudiencia::normalizar)
                .collect(Collectors.toUnmodifiableSet());

        this.atosRelevantes = normalizeSet(atosRelevantes);
        this.publicacoesPermitidas = normalizeSet(publicacoesPermitidas);
    }

    
    
    
    public int getCodigo() { return codigo; }
    public String getLabel() { return label; }

    public String rotulo() { return label; }
    public String getDescricao() { return descricao; }
    public boolean exigeColegiado() { return colegiado; }
    public int quorumMinimoSugerido() { return quorumMinimoSugerido; }

    
    
    
    public static GrauJurisdicao fromCodigo(int codigo) {
        GrauJurisdicao g = POR_CODIGO.get(codigo);
        if (g == null) throw new IllegalArgumentException("GrauJurisdicao inválido: " + codigo);
        return g;
    }

    
    
    
    public abstract NivelSegurancaInstitucional nivelSeguranca();
    public abstract boolean exigeSigilo();
    public abstract Set<PoliticaSeguranca> politicasMinimas();

    public boolean exigeSigiloPorAto(String atoProcessual) {
        String a = normalizeKey(atoProcessual);
        if (a.isEmpty()) return exigeSigilo();

        if (a.contains("SEGREDO_JUSTICA") || a.contains("SIGILO") || a.contains("DADOS_SENSIVEIS")) return true;
        if (a.contains("MEDIDA_CAUTELAR") || a.contains("INTERCEPTACAO") || a.contains("QUEBRA_SIGILO")) return true;

        if (this == CONSTITUCIONAL) return true;
        return exigeSigilo();
    }

    
    
    
    public Set<TipoAudiencia> audienciasPermitidas() {
        return audienciasPermitidasNormalizadas;
    }

    public boolean permiteAudiencia(TipoAudiencia tipo) {
        if (tipo == null) return false;
        return audienciasPermitidasNormalizadas.contains(tipo.normalizar());
    }

    public boolean exigePautaFormal(TipoAudiencia tipo) {
        if (tipo == null) return false;
        TipoAudiencia t = tipo.normalizar();
        return switch (t) {
            case SUSTENTACAO_ORAL, JULGAMENTO_COLEGIADO, SESSAO_ADMINISTRATIVA -> true;
            default -> false;
        };
    }

    
    
    
    public Set<String> atosRelevantes() {
        return atosRelevantes;
    }

    public static GrauJurisdicao fromString(String texto) {
        String token = normalizeKey(texto);
        if (token.isEmpty()) {
            throw new IllegalArgumentException("Grau de jurisdição inválido: " + texto);
        }
        if (token.equals("1") || token.equals("1_GRAU") || token.equals("PRIMEIRO") || token.equals("PRIMEIRO_GRAU") || token.equals("ORIGINARIO")) {
            return PRIMEIRO_GRAU;
        }
        if (token.equals("2") || token.equals("2_GRAU") || token.equals("SEGUNDO") || token.equals("SEGUNDO_GRAU")) {
            return SEGUNDO_GRAU;
        }
        if (token.equals("SUPERIOR") || token.equals("TRIBUNAIS_SUPERIORES") || token.equals("TRIBUNAL_SUPERIOR")) {
            return SUPERIOR;
        }
        if (token.equals("CONSTITUCIONAL") || token.equals("SUPREMO") || token.equals("STF")) {
            return CONSTITUCIONAL;
        }
        for (GrauJurisdicao value : values()) {
            if (value.name().equals(token) || normalizeKey(value.rotulo()).equals(token)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Grau de jurisdição inválido: " + texto);
    }

    public static String normalizeKey(String input) {
        if (input == null) return "";
        String s = input.trim();
        if (s.isEmpty()) return "";

        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        s = s.toUpperCase(Locale.ROOT);
        s = s.replace(' ', '_').replace('-', '_').replace('/', '_');
        s = s.replaceAll("[^A-Z0-9_]", "");
        s = s.replaceAll("_+", "_");
        s = s.replaceAll("^_+", "").replaceAll("_+$", "");
        return s;
    }

    public String etapaProcessualDe(String atoProcessual) {
        String a = normalizeKey(atoProcessual);
        if (a.isEmpty()) return "INDEFINIDA";

        if (a.contains("PETICAO") || a.contains("INICIAL")) return "PETICAO";
        if (a.contains("CITACAO")) return "CITACAO";
        if (a.contains("CONTESTACAO") || a.contains("REPLICA")) return "POSTULACAO";
        if (a.contains("SANEAMENTO")) return "SANEAMENTO";
        if (a.contains("PROVA") || a.contains("PERICIA") || a.contains("OITIVA")) return "INSTRUCAO";
        if (a.contains("AUDIENCIA")) return "AUDIENCIA";
        if (a.contains("SENTENCA")) return "SENTENCA";
        if (a.contains("ACORDAO")) return "ACORDAO";
        if (a.contains("RECURSO") || a.contains("APELACAO") || a.contains("AGRAVO") || a.contains("EMBARGOS")) return "RECURSAL";
        if (a.contains("EXECUCAO") || a.contains("CUMPRIMENTO")) return "EXECUCAO";
        return "OUTROS";
    }

    public int scoreRisco(String atoProcessual, boolean urgente) {
        int base = switch (this) {
            case PRIMEIRO_GRAU -> 20;
            case SEGUNDO_GRAU -> 35;
            case SUPERIOR -> 55;
            case CONSTITUCIONAL -> 75;
            default -> 20;
        };

        String etapa = etapaProcessualDe(atoProcessual);
        int etapaW = switch (etapa) {
            case "PETICAO", "CITACAO" -> 5;
            case "INSTRUCAO" -> 10;
            case "SENTENCA" -> 15;
            case "RECURSAL" -> 20;
            case "ACORDAO" -> 25;
            default -> 8;
        };

        int urg = urgente ? 20 : 0;
        int sig = exigeSigiloPorAto(atoProcessual) ? 10 : 0;

        return Math.min(100, base + etapaW + urg + sig);
    }

    public boolean permiteAtoProcessual(String atoProcessual) {
        String a = normalizeKey(atoProcessual);
        if (a.isEmpty()) return false;

        if (!atosRelevantes.isEmpty() && atosRelevantes.contains(a)) return true;

        return switch (this) {
            case PRIMEIRO_GRAU -> true;
            case SEGUNDO_GRAU -> a.contains("RECURSO") || a.contains("APELACAO") || a.contains("AGRAVO")
                    || a.contains("EMBARGOS") || a.contains("ACORDAO");
            case SUPERIOR -> a.contains("RECURSO") || a.contains("AGRAVO") || a.contains("EMBARGOS")
                    || a.contains("PRECEDENTE") || a.contains("AFETACAO") || a.contains("ACORDAO");
            case CONSTITUCIONAL -> a.contains("RECURSO_EXTRAORDINARIO") || a.contains("REPERCUSSAO")
                    || a.contains("PREQUESTIONAMENTO") || a.contains("OBICE") || a.contains("PLENARIO")
                    || a.contains("ACORDAO") || a.contains("DECISAO");
            default -> false;
        };
    }

    public Set<String> publicacoesPermitidas() {
        return publicacoesPermitidas;
    }

    public Set<String> checklistMinimo(String classeProcessual) {
        String c = normalizeKey(classeProcessual);
        Set<String> out = new LinkedHashSet<>();

        out.add("COMPETENCIA");
        out.add("ENDERECO");
        out.add("QUALIFICACAO_PARTES");
        out.add("PEDIDO");
        out.add("CAUSA_DE_PEDIR");
        out.add("PROVAS");
        out.add("CAPTURA_LOGS");

        if (this == SEGUNDO_GRAU || this == SUPERIOR || this == CONSTITUCIONAL) {
            out.add("TEMPESTIVIDADE");
            out.add("DIALETICIDADE");
            out.add("PREQUESTIONAMENTO_QUANDO_APLICAVEL");
        }

        if (c.contains("PENAL")) {
            out.add("LEGALIDADE_PRISAO_QUANDO_APLICAVEL");
            out.add("CADEIA_CUSTODIA");
            out.add("GARANTIAS_FUNDAMENTAIS");
        } else if (c.contains("TRABALH")) {
            out.add("RITO");
            out.add("VALOR_CAUSA");
            out.add("AUDIENCIA_UNA_QUANDO_APLICAVEL");
        } else if (c.contains("AMBIENT")) {
            out.add("LEGITIMIDADE_ATIVA");
            out.add("INTERESSE_PUBLICO");
            out.add("PROVA_TECNICA");
        } else {
            out.add("CPC_REQUISITOS_FORMAIS");
        }

        return Collections.unmodifiableSet(out);
    }

    public Set<String> regrasAdmissibilidade(String recurso) {
        String r = normalizeKey(recurso);
        if (r.isEmpty()) return Set.of();

        Set<String> out = new LinkedHashSet<>();
        out.add("TEMPESTIVIDADE");
        out.add("LEGITIMIDADE");
        out.add("INTERESSE_RECURSAL");

        return switch (this) {
            case PRIMEIRO_GRAU -> Collections.unmodifiableSet(out);

            case SEGUNDO_GRAU -> {
                out.add("REGULARIDADE_FORMAL");
                out.add("PREPARO_QUANDO_DEVIDO");
                yield Collections.unmodifiableSet(out);
            }

            case SUPERIOR -> {
                out.add("PREQUESTIONAMENTO");
                out.add("OBICE_SUMULAR_QUANDO_APLICAVEL");
                out.add("DEMONSTRACAO_DIVERGENCIA_QUANDO_CABIVEL");
                out.add("VEDACAO_REEXAME_FATICO_PROBATORIO");
                yield Collections.unmodifiableSet(out);
            }

            case CONSTITUCIONAL -> {
                out.add("PREQUESTIONAMENTO");
                out.add("REPERCUSSAO_GERAL");
                out.add("OBICE_SUMULAR_QUANDO_APLICAVEL");
                out.add("VEDACAO_REEXAME_FATICO_PROBATORIO");
                out.add("RELEVANCIA_CONSTITUCIONAL");
                yield Collections.unmodifiableSet(out);
            }
       

            default -> Collections.unmodifiableSet(out);
        };
    }

    public int slaSugestivoDias(String classeProcessual, boolean urgente) {
        int base = switch (this) {
            case PRIMEIRO_GRAU -> 60;
            case SEGUNDO_GRAU -> 45;
            case SUPERIOR -> 35;
            case CONSTITUCIONAL -> 25;
            default -> 60;
        };

        String c = normalizeKey(classeProcessual);
        int mod = 0;

        if (c.contains("PENAL")) mod -= 10;
        if (c.contains("TRABALH")) mod -= 7;
        if (c.contains("AMBIENT")) mod -= 5;
        if (urgente) mod -= 15;

        return Math.max(5, base + mod);
    }

    public String perfilAtuacaoIA() {
        return switch (this) {
            case PRIMEIRO_GRAU ->
                    "Assistência ampla: triagem, checklist formal, minutas, saneamento e organização probatória.";
            case SEGUNDO_GRAU ->
                    "Foco recursal: dialeticidade, tempestividade, preparo, organização de pauta e suporte ao colegiado.";
            case SUPERIOR ->
                    "Foco em filtros e precedentes: admissibilidade estrita, óbices, divergência, afetação e uniformização.";
            case CONSTITUCIONAL ->
                    "Foco constitucional: repercussão geral, tese e impacto sistêmico (governança reforçada).";
            default -> "Suporte geral: organização, checklist e governança de atos processuais.";
        };
    }

    public Map<String, String> metricTags() {
        return Map.of(
                "grau_codigo", String.valueOf(codigo),
                "grau_nome", name(),
                "colegiado", String.valueOf(colegiado),
                "seguranca", nivelSeguranca().name(),
                "sigilo_padrao", String.valueOf(exigeSigilo())
        );
    }

    public String workflowKey(String classeProcessual, String acao) {
        String c = normalizeKey(classeProcessual);
        String a = normalizeKey(acao);
        if (c.isEmpty()) c = "GERAL";
        if (a.isEmpty()) a = "ACAO";
        return "PJB_" + name() + "_" + c + "_" + a;
    }

    private static Set<String> normalizeSet(Set<String> in) {
        if (in == null || in.isEmpty()) return Set.of();
        Set<String> out = new LinkedHashSet<>();
        for (String s : in) {
            String k = normalizeKey(s);
            if (!k.isEmpty()) out.add(k);
        }
        return Collections.unmodifiableSet(out);
    }
}
