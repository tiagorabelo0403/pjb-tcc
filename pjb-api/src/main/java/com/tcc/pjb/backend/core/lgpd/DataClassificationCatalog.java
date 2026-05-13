package com.tcc.pjb.backend.core.lgpd;

import com.tcc.pjb.backend.core.security.sigilo.SigiloAccessRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.cidadao.CidadaoProcessoNacionalProjection;
import com.tcc.pjb.backend.model.entity.criminal.InqueritoPolicialDigital;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.judicial.DjePublicacao;
import com.tcc.pjb.backend.model.entity.security.SigiloProcessoProofChallenge;
import java.util.EnumSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class DataClassificationCatalog {

    private final List<DataClassificationEntry> entries;
    private final Map<String, DataClassificationEntry> byEntityClassName;
    private final Map<String, DataClassificationEntry> byTableName;

    public DataClassificationCatalog() {
        this.entries = List.of(
                entry(
                        Processo.class,
                        "tb_processo",
                        EnumSet.of(DataClassificationCategory.DADOS_PESSOAIS, DataClassificationCategory.DADOS_JUDICIAIS, DataClassificationCategory.DADOS_SENSIVEIS),
                        "ART_7_II__ART_11_II_A__ART_23",
                        true,
                        true,
                        "TEMPORALIDADE_PROCESSUAL_CNJ",
                        List.of("ABAC_INSTITUCIONAL", "RLS_POR_AFILIACAO_UNIDADE", "STEP_UP_PARA_SIGILO", "AUDITORIA_IMUTAVEL")
                ),
                entry(
                        DocumentoProcessual.class,
                        "tb_documento_processual",
                        EnumSet.of(DataClassificationCategory.DADOS_PESSOAIS, DataClassificationCategory.DADOS_JUDICIAIS, DataClassificationCategory.DADOS_SENSIVEIS),
                        "ART_7_II__ART_11_II_A__ART_23",
                        true,
                        true,
                        "TEMPORALIDADE_DOCUMENTAL_PROCESSUAL",
                        List.of("RLS_POR_PROCESSO", "CRIPTOGRAFIA_REPOUSO", "REDACAO_LGPD", "AUDITORIA_DOWNLOAD")
                ),
                entry(
                        InqueritoPolicialDigital.class,
                        "tb_inquerito_policial_digital",
                        EnumSet.of(DataClassificationCategory.DADOS_PESSOAIS, DataClassificationCategory.DADOS_JUDICIAIS, DataClassificationCategory.DADOS_SENSIVEIS),
                        "ART_7_II__ART_11_II_A__ART_23",
                        true,
                        true,
                        "RETENCAO_CRIMINAL_REFORCADA",
                        List.of("ABAC_CRIMINAL", "STEP_UP_FORTE", "RLS_POR_ORGAO_COMPETENTE", "AUDITORIA_ACESSO_SENSIVEL")
                ),
                entry(
                        CidadaoProcessoNacionalProjection.class,
                        "tb_cidadao_processo_nacional_projection",
                        EnumSet.of(DataClassificationCategory.DADOS_PESSOAIS, DataClassificationCategory.DADOS_JUDICIAIS),
                        "ART_7_III__ART_23",
                        true,
                        true,
                        "RETENCAO_PROJECAO_CIDADAO",
                        List.of("MINIMIZACAO_DE_DADOS", "RLS_POR_IDENTIDADE", "MASCARAMENTO_POR_SIGILO")
                ),
                entry(
                        SigiloAccessRequest.class,
                        "tb_sigilo_access_request",
                        EnumSet.of(DataClassificationCategory.DADOS_PESSOAIS, DataClassificationCategory.DADOS_JUDICIAIS, DataClassificationCategory.METADADOS_OPERACIONAIS),
                        "ART_7_II__ART_23",
                        true,
                        true,
                        "RETENCAO_AUDITORIA_SIGILO",
                        List.of("ABAC_SIGILO", "AUDITORIA_APROVACAO", "HASH_DE_SEGREDO", "EXPIRACAO_CONTROLADA")
                ),
                entry(
                        SigiloProcessoProofChallenge.class,
                        "tb_sigilo_processo_proof_challenge",
                        EnumSet.of(DataClassificationCategory.DADOS_PESSOAIS, DataClassificationCategory.DADOS_JUDICIAIS, DataClassificationCategory.METADADOS_OPERACIONAIS),
                        "ART_7_II__ART_23",
                        true,
                        true,
                        "RETENCAO_PROVA_DE_ACESSO",
                        List.of("CHALLENGE_AUDITAVEL", "EXPIRACAO_CURTA", "MASCARAMENTO_DE_PAYLOAD", "LOG_IMUTAVEL")
                ),
                entry(
                        DjePublicacao.class,
                        "pjb_dje_publicacao",
                        EnumSet.of(DataClassificationCategory.DADOS_JUDICIAIS, DataClassificationCategory.METADADOS_OPERACIONAIS),
                        "ART_7_II__ART_23",
                        false,
                        false,
                        "RETENCAO_PUBLICACAO_OFICIAL",
                        List.of("ASSINATURA_AUDITAVEL", "TRILHA_DE_DISPATCH", "RECONCILIACAO_DE_PRAZO")
                )
        );
        this.byEntityClassName = indexByEntityClassName(entries);
        this.byTableName = indexByTableName(entries);
    }

    public List<DataClassificationEntry> snapshot() {
        return entries;
    }

    public Optional<DataClassificationEntry> findByEntityClassName(String entityClassName) {
        if (entityClassName == null || entityClassName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byEntityClassName.get(entityClassName));
    }

    public Optional<DataClassificationEntry> findByTableName(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byTableName.get(tableName));
    }

    public DataClassificationEntry requireByEntityClass(Class<?> entityClass) {
        Objects.requireNonNull(entityClass, "entityClass");
        return findByEntityClassName(entityClass.getName())
                .orElseThrow(() -> new IllegalArgumentException("Entidade sem classificação LGPD catalogada: " + entityClass.getName()));
    }

    private Map<String, DataClassificationEntry> indexByEntityClassName(List<DataClassificationEntry> entries) {
        LinkedHashMap<String, DataClassificationEntry> out = new LinkedHashMap<>();
        for (DataClassificationEntry entry : entries) {
            out.put(entry.entityClassName(), entry);
        }
        return Collections.unmodifiableMap(out);
    }

    private Map<String, DataClassificationEntry> indexByTableName(List<DataClassificationEntry> entries) {
        LinkedHashMap<String, DataClassificationEntry> out = new LinkedHashMap<>();
        for (DataClassificationEntry entry : entries) {
            out.put(entry.tableName(), entry);
        }
        return Collections.unmodifiableMap(out);
    }

    private DataClassificationEntry entry(Class<?> entityClass,
                                          String tableName,
                                          EnumSet<DataClassificationCategory> categories,
                                          String legalBasisProfile,
                                          boolean judicialSecrecyAware,
                                          boolean rlsRecommended,
                                          String retentionProfile,
                                          List<String> accessControls) {
        return new DataClassificationEntry(
                entityClass.getName(),
                tableName,
                categories,
                legalBasisProfile,
                judicialSecrecyAware,
                rlsRecommended,
                retentionProfile,
                accessControls
        );
    }
}
