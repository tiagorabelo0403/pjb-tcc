package com.tcc.pjb.backend.service.processual.recursal;

import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalAliasRegistry;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalDetalhado;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.EmbargosGroundCode;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshSpeciesType;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorTipo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.processual.recursal.workspace.PerfilRecursalDescriptor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

final class RecursalPeticionamentoSupport {

    private final RecursalAliasRegistry recursalAliasRegistry;

    RecursalPeticionamentoSupport() {
        this(new RecursalAliasRegistry());
    }

    RecursalPeticionamentoSupport(RecursalAliasRegistry recursalAliasRegistry) {
        this.recursalAliasRegistry = recursalAliasRegistry;
    }

    PerfilRecursalDescriptor descriptorOf(Usuario usuario) {
        TipoUsuario tipoUsuario = usuario == null ? null : usuario.getTipoUsuario();
        if (tipoUsuario == null) {
            throw new IllegalArgumentException("Usuário sem perfil para peticionamento recursal.");
        }
        if (tipoUsuario.isAdvocacia()) {
            return new PerfilRecursalDescriptor(
                    "ADVOCACIA",
                    "ADVOGADO",
                    new String[]{"ROLE_ADVOGADO", "ROLE_OAB_PRESIDENTE_SECCIONAL"},
                    "ADVOCACIA_PETICAO_RECURSAL",
                    "ADVOCACIA_RECURSO",
                    1,
                    false,
                    false,
                    false
            );
        }
        if (tipoUsuario.isDefensoriaPublica()) {
            return new PerfilRecursalDescriptor(
                    "DEFENSORIA",
                    "DEFENSOR",
                    new String[]{"ROLE_DEFENSOR_PUBLICO", "ROLE_DEFENSOR_PUBLICO_FEDERAL"},
                    "DEFENSORIA_PETICAO_RECURSAL",
                    "DEFENSORIA_RECURSO",
                    2,
                    true,
                    false,
                    true
            );
        }
        if (tipoUsuario.isProcuradoria()) {
            return new PerfilRecursalDescriptor(
                    "PROCURADORIA",
                    "PROCURADORIA",
                    new String[]{"ROLE_PROCURADOR", "ROLE_PROCURADORIA_MUNICIPAL", "ROLE_PROCURADORIA_ESTADUAL", "ROLE_PROCURADORIA_FEDERAL", "ROLE_PROCURADOR_GERAL_REPUBLICA"},
                    "PROCURADORIA_PETICAO_RECURSAL",
                    "PROCURADORIA_RECURSO",
                    2,
                    true,
                    false,
                    true
            );
        }
        if (tipoUsuario.isMinisterioPublico()) {
            return new PerfilRecursalDescriptor(
                    "MINISTERIO_PUBLICO",
                    "MP",
                    new String[]{"ROLE_MEMBRO_MINISTERIO_PUBLICO", "ROLE_PROMOTOR_ELEITORAL", "ROLE_PROMOTOR_TRABALHISTA", "ROLE_PROCURADOR_GERAL_REPUBLICA"},
                    "MINISTERIO_PUBLICO_PETICAO_RECURSAL",
                    "MINISTERIO_PUBLICO_RECURSO",
                    2,
                    true,
                    true,
                    true
            );
        }
        throw new IllegalArgumentException("Perfil sem habilitação recursal ativa no PJB.");
    }

    String resolveCanonicalAlias(String recursoNormalizado) {
        return normalizeNullable(recursalAliasRegistry.resolve(recursoNormalizado));
    }

    RecursalMeshSpeciesType resolveSpeciesType(String raw,
                                               String canonicalAlias,
                                               LegalAppealType appealType,
                                               Processo processo) {
        if (appealType == LegalAppealType.AGRAVO_INSTRUMENTO && processo != null && processo.getRito() != null && processo.getRito().isTrabalhista()) {
            return RecursalMeshSpeciesType.AGITRAB;
        }
        RecursalMeshSpeciesType fromAlias = RecursalMeshSpeciesType.fromString(raw);
        if (fromAlias != null) {
            return fromAlias;
        }
        if (canonicalAlias != null) {
            fromAlias = RecursalMeshSpeciesType.fromString(canonicalAlias);
            if (fromAlias != null) {
                return fromAlias;
            }
        }
        return switch (appealType) {
            case APELACAO -> RecursalMeshSpeciesType.APCIV;
            case APELACAO_PENAL -> RecursalMeshSpeciesType.APCRIM;
            case AGRAVO_INSTRUMENTO -> processo.getRito() != null && processo.getRito().isTrabalhista()
                    ? RecursalMeshSpeciesType.AGITRAB
                    : RecursalMeshSpeciesType.AGINST;
            case AGRAVO_INTERNO -> RecursalMeshSpeciesType.AGINT;
            case EMBARGOS_DECLARACAO -> RecursalMeshSpeciesType.EDCL;
            case RESP -> RecursalMeshSpeciesType.RESP;
            case RE -> RecursalMeshSpeciesType.RE;
            case RECURSO_INOMINADO -> RecursalMeshSpeciesType.RINOM;
            case PEDIDO_UNIFORMIZACAO -> RecursalMeshSpeciesType.PUILF;
            case AGRAVO_REGIMENTAL -> RecursalMeshSpeciesType.AGREG;
            case RECURSO_ORDINARIO_CONSTITUCIONAL -> RecursalMeshSpeciesType.ROC;
            case RECURSO_ORDINARIO_TRABALHISTA -> RecursalMeshSpeciesType.ROT;
            case RECURSO_REVISTA -> RecursalMeshSpeciesType.RR;
            case AGRAVO_RECURSO_REVISTA -> RecursalMeshSpeciesType.AIRR;
            case AGRAVO_PETICAO -> RecursalMeshSpeciesType.AGPET;
            case EMBARGOS_EXECUCAO -> RecursalMeshSpeciesType.EEXEC;
            case EMBARGOS_EXECUCAO_FISCAL -> RecursalMeshSpeciesType.EEFISC;
            case EMBARGOS_TERCEIRO -> RecursalMeshSpeciesType.ETERC;
            case RECLAMACAO_CONSTITUCIONAL -> RecursalMeshSpeciesType.RCL;
            case CONFLITO_COMPETENCIA -> RecursalMeshSpeciesType.CC;
            case CORREICAO_PARCIAL -> RecursalMeshSpeciesType.CPARCIAL;
            case AGRAVO_RESP_RE -> raw != null && raw.toUpperCase(Locale.ROOT).contains("RESP")
                    ? RecursalMeshSpeciesType.ARESP
                    : RecursalMeshSpeciesType.ARE;
            default -> null;
        };
    }

    String resourceKeyForCorrelation(String recursoNormalizado,
                                     String canonicalAlias,
                                     LegalAppealType appealType,
                                     RecursalMeshSpeciesType speciesType) {
        if (speciesType != null) {
            return speciesType.name();
        }
        if (canonicalAlias != null) {
            return canonicalAlias;
        }
        if (appealType != null) {
            return appealType.name();
        }
        return recursoNormalizado.toUpperCase(Locale.ROOT);
    }

    int priorityFor(LegalAppealType appealType, boolean pedidoEfeitoSuspensivo) {
        if (pedidoEfeitoSuspensivo || appealType.isExceptional()) {
            return 1;
        }
        if (appealType.isInternalReview() || appealType.isExecutoryIncident()) {
            return 2;
        }
        return 1;
    }

    String buildPeticaoDescricao(String razoes,
                                 String observacoes,
                                 boolean pedidoEfeitoSuspensivo,
                                 boolean preparoDispensado) {
        StringJoiner joiner = new StringJoiner("\n\n");
        joiner.add(razoes);
        if (observacoes != null) {
            joiner.add("Observações recursais: " + observacoes);
        }
        StringJoiner qualificadores = new StringJoiner("; ");
        if (pedidoEfeitoSuspensivo) {
            qualificadores.add("pedido de efeito suspensivo");
        }
        if (preparoDispensado) {
            qualificadores.add("preparo dispensado/isento");
        }
        String tags = qualificadores.toString();
        if (!tags.isBlank()) {
            joiner.add("Qualificadores: " + tags);
        }
        return joiner.toString();
    }

    String buildFundamentacao(String fundamentacao,
                              boolean pedidoEfeitoSuspensivo,
                              boolean preparoDispensado) {
        StringJoiner joiner = new StringJoiner(" | ");
        joiner.add(fundamentacao);
        if (pedidoEfeitoSuspensivo) {
            joiner.add("efeito_suspensivo_requerido");
        }
        if (preparoDispensado) {
            joiner.add("preparo_dispensado_ou_isento");
        }
        return joiner.toString();
    }

    String buildHistoryMessage(String tipoRecurso,
                               boolean pedidoEfeitoSuspensivo,
                               boolean preparoDispensado) {
        StringJoiner joiner = new StringJoiner("; ");
        joiner.add(tipoRecurso + " interposto com petição recursal vinculada, mesh recursal conectada e superfície de leitura especializada pronta");
        if (pedidoEfeitoSuspensivo) {
            joiner.add("pedido de efeito suspensivo registrado");
        }
        if (preparoDispensado) {
            joiner.add("preparo dispensado/isento informado");
        }
        return joiner.toString() + '.';
    }

    boolean preparoDispensadoPorPerfil(PerfilRecursalDescriptor descriptor) {
        return descriptor.autoIsencaoBase();
    }

    Set<EmbargosGroundCode> resolveEmbargosGrounds(String observacoes, LegalAppealType appealType) {
        if (appealType != LegalAppealType.EMBARGOS_DECLARACAO) {
            return Set.of();
        }
        LinkedHashSet<EmbargosGroundCode> grounds = new LinkedHashSet<>();
        if (containsAny(observacoes, "omissao", "omissão")) {
            grounds.add(EmbargosGroundCode.OMISSAO);
        }
        if (containsAny(observacoes, "contradicao", "contradição")) {
            grounds.add(EmbargosGroundCode.CONTRADICAO);
        }
        if (containsAny(observacoes, "obscuridade")) {
            grounds.add(EmbargosGroundCode.OBSCURIDADE);
        }
        if (containsAny(observacoes, "erro material", "erro aritmetico", "erro aritmético")) {
            grounds.add(EmbargosGroundCode.ERRO_MATERIAL);
        }
        if (grounds.isEmpty()) {
            grounds.add(EmbargosGroundCode.OMISSAO);
        }
        return Set.copyOf(grounds);
    }

    String buildPedidoUsuarioIa(LegalAppealType appealType, String observacoes) {
        StringJoiner joiner = new StringJoiner(" ");
        joiner.add("Conferir admissibilidade real, tempestividade, preparo, prevenção, contrarrazões e risco operacional do recurso");
        joiner.add(appealType.name());
        if (observacoes != null) {
            joiner.add("com observações:");
            joiner.add(observacoes);
        }
        return joiner.toString();
    }

    String stableActorKey(Usuario usuario) {
        if (usuario == null) {
            return "anonimo";
        }
        String cpf = normalizeNullable(usuario.getCpf());
        if (cpf != null) {
            String digits = cpf.replaceAll("[^0-9]", "");
            if (!digits.isBlank()) {
                return digits;
            }
        }
        if (usuario.getId() != null) {
            return String.valueOf(usuario.getId());
        }
        TipoUsuario tipoUsuario = usuario.getTipoUsuario();
        return tipoUsuario == null ? "perfil_indefinido" : tipoUsuario.name();
    }

    String safeNumeroProcesso(Processo processo) {
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
        String numero = normalizeNullable(processo.getNumero());
        if (numero != null) {
            return numero;
        }
        return processo.getId() == null ? "PROCESSO_SEM_NUMERO" : "PROCESSO-" + processo.getId();
    }

    TipoJustica inferTipoJustica(Processo processo) {
        if (processo.getTipoJustica() != null) {
            return processo.getTipoJustica();
        }
        if (processo.getRito() != null) {
            if (processo.getRito().isTrabalhista()) {
                return TipoJustica.TRABALHO;
            }
            if (processo.getRito().isEleitoral()) {
                return TipoJustica.ELEITORAL;
            }
            if (processo.getRito().isMilitar()) {
                return TipoJustica.MILITAR_ESTADUAL;
            }
            if (processo.getRito().isEspecialConstitucional()) {
                return TipoJustica.SUPERIOR;
            }
        }
        if (processo.getRamoDireito() == RamoDireito.TRABALHISTA) {
            return TipoJustica.TRABALHO;
        }
        if (processo.getRamoDireito() == RamoDireito.ELEITORAL) {
            return TipoJustica.ELEITORAL;
        }
        if (processo.getRamoDireito() == RamoDireito.MILITAR) {
            return TipoJustica.MILITAR_ESTADUAL;
        }
        String tribunal = firstNonBlank(processo.getTribunalCodigoRoteado(), processo.getTribunal());
        TipoJustica parsed = TipoJustica.fromString(tribunal);
        return parsed == null ? TipoJustica.ESTADUAL : parsed;
    }

    RecursalTribunalDetalhado inferTribunalDetalhado(Processo processo, RecursalTribunal tribunalOrigem) {
        String code = firstNonBlank(processo.getTribunalCodigoRoteado(), processo.getTribunal());
        RecursalTribunalDetalhado detalhado = RecursalTribunalDetalhado.fromString(code);
        if (detalhado != null) {
            return detalhado;
        }
        String uf = normalizeNullable(processo.getUf());
        if (uf != null && tribunalOrigem == RecursalTribunal.TJ) {
            RecursalTribunalDetalhado byUf = RecursalTribunalDetalhado.fromString("TJ" + uf.toUpperCase(Locale.ROOT));
            if (byUf != null) {
                return byUf;
            }
        }
        if (uf != null && tribunalOrigem == RecursalTribunal.TRE) {
            RecursalTribunalDetalhado byUf = RecursalTribunalDetalhado.fromString("TRE" + uf.toUpperCase(Locale.ROOT));
            if (byUf != null) {
                return byUf;
            }
        }
        return RecursalTribunalDetalhado.fromFamily(tribunalOrigem);
    }

    InstanceLevel inferInstanceLevel(Processo processo, RecursalTribunal tribunalOrigem) {
        if (tribunalOrigem == RecursalTribunal.STF) {
            return InstanceLevel.EXTRAORDINARY;
        }
        if (tribunalOrigem == RecursalTribunal.STJ || tribunalOrigem == RecursalTribunal.TST) {
            return InstanceLevel.SUPERIOR;
        }
        if (processo.getFaseAtual() == FaseProcessual.RECURSAL
                || processo.getStatusProcesso() != null && processo.getStatusProcesso().name().contains("RECURSO")) {
            return tribunalOrigem.secondInstanceCourt() ? InstanceLevel.SECOND_INSTANCE : tribunalOrigem.instanceLevel();
        }
        return InstanceLevel.FIRST_INSTANCE;
    }

    OrgaoJulgadorTipo inferOrgaoProlator(Processo processo,
                                         LegalAppealType appealType,
                                         InstanceLevel instanciaAtual) {
        if (appealType == LegalAppealType.AGRAVO_INTERNO || appealType == LegalAppealType.AGRAVO_REGIMENTAL) {
            return OrgaoJulgadorTipo.RELATOR;
        }
        if (processo.getFaseAtual() == FaseProcessual.RECURSAL || instanciaAtual != InstanceLevel.FIRST_INSTANCE) {
            return OrgaoJulgadorTipo.COLEGIADO;
        }
        return OrgaoJulgadorTipo.MONOCRATICO;
    }

    LocalDate inferDataIntimacao(Processo processo) {
        LocalDateTime base = processo.getDataUltimaMovimentacao();
        if (base == null) {
            base = processo.getDataAtualizacao();
        }
        if (base == null) {
            base = processo.getDataCriacao();
        }
        if (base == null) {
            return LocalDate.now(ZoneOffset.UTC);
        }
        return base.toLocalDate();
    }

    String inferTribunalCodigo(Processo processo,
                               RecursalTribunal tribunalOrigem,
                               RecursalTribunalDetalhado detalhado) {
        String code = normalizeNullable(processo.getTribunalCodigoRoteado());
        if (code != null) {
            return code;
        }
        code = normalizeNullable(processo.getTribunal());
        if (code != null) {
            return code;
        }
        if (detalhado != null) {
            return detalhado.name();
        }
        return tribunalOrigem == null ? "TJSP" : tribunalOrigem.name();
    }

    String requiredText(String value, String fieldName) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new IllegalArgumentException("Campo obrigatório ausente: " + fieldName);
        }
        return normalized;
    }

    String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    boolean containsAny(String text, String... needles) {
        String normalized = normalizedSearchText(text);
        if (normalized == null || needles == null) {
            return false;
        }
        for (String needle : needles) {
            String normalizedNeedle = normalizedSearchText(needle);
            if (normalizedNeedle != null && normalized.contains(normalizedNeedle)) {
                return true;
            }
        }
        return false;
    }

    private String normalizedSearchText(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        String ascii = Normalizer.normalize(normalized, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return ascii.toLowerCase(Locale.ROOT);
    }

    String firstNonBlank(String... values) {
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

    void putIfNotNull(Map<String, Object> target, String key, Object value) {
        if (target != null && key != null && value != null) {
            target.put(key, value);
        }
    }

    String safeMessage(Throwable throwable) {
        if (throwable == null) {
            return "Falha recursal não especificada.";
        }
        String message = normalizeNullable(throwable.getMessage());
        return message == null ? throwable.getClass().getSimpleName() : message;
    }
}
