package com.tcc.pjb.backend.service.institutional.architecture;

import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalArchitectureResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalFederatedShardBlueprintService {

    private static final List<AdminInstitutionalArchitectureResponse.ShardCluster> CLUSTERS = List.of(
            new AdminInstitutionalArchitectureResponse.ShardCluster(
                    "NORTE",
                    "Cluster Norte",
                    List.of(),
                    List.of("AC", "AM", "AP", "PA", "RO", "RR", "TO"),
                    "roteamento_por_uf_para_malha_norte"
            ),
            new AdminInstitutionalArchitectureResponse.ShardCluster(
                    "NORDESTE",
                    "Cluster Nordeste",
                    List.of(),
                    List.of("AL", "BA", "CE", "MA", "PB", "PE", "PI", "RN", "SE"),
                    "roteamento_por_uf_para_malha_nordeste"
            ),
            new AdminInstitutionalArchitectureResponse.ShardCluster(
                    "CENTRO_OESTE",
                    "Cluster Centro-Oeste",
                    List.of(),
                    List.of("DF", "GO", "MT", "MS"),
                    "roteamento_por_uf_para_malha_centro_oeste"
            ),
            new AdminInstitutionalArchitectureResponse.ShardCluster(
                    "SUDESTE",
                    "Cluster Sudeste",
                    List.of(),
                    List.of("ES", "MG", "RJ", "SP"),
                    "roteamento_por_uf_para_malha_sudeste"
            ),
            new AdminInstitutionalArchitectureResponse.ShardCluster(
                    "SUL",
                    "Cluster Sul",
                    List.of(),
                    List.of("PR", "RS", "SC"),
                    "roteamento_por_uf_para_malha_sul"
            ),
            new AdminInstitutionalArchitectureResponse.ShardCluster(
                    "FEDERAL_SUPERIOR",
                    "Cluster Federal e Cortes Superiores",
                    List.of("STF", "STJ", "TST", "TSE", "STM", "CNJ", "TRF", "TNU", "CSJT"),
                    List.of(),
                    "roteamento_por_prefixo_de_tribunal_ou_servico_nacional"
            )
    );

    public List<AdminInstitutionalArchitectureResponse.ShardCluster> clusters() {
        return CLUSTERS;
    }

    public AdminInstitutionalArchitectureResponse.ClusterResolution resolve(String tribunalCodigo, String uf) {
        String normalizedTribunal = normalize(tribunalCodigo);
        String normalizedUf = normalize(uf);
        ArrayList<String> reasons = new ArrayList<>();
        for (AdminInstitutionalArchitectureResponse.ShardCluster cluster : CLUSTERS) {
            if (matchesTribunalPrefix(cluster, normalizedTribunal)) {
                reasons.add("prefixo_tribunal=" + normalizedTribunal);
                reasons.add("roteamento_federado_nacional");
                return new AdminInstitutionalArchitectureResponse.ClusterResolution(
                        cluster.code(),
                        cluster.label(),
                        metadataKey(cluster.code(), normalizedTribunal, normalizedUf),
                        true,
                        List.copyOf(reasons)
                );
            }
        }
        for (AdminInstitutionalArchitectureResponse.ShardCluster cluster : CLUSTERS) {
            if (cluster.ufs().contains(normalizedUf)) {
                reasons.add("uf=" + normalizedUf);
                reasons.add("roteamento_jurisdicional_regional");
                return new AdminInstitutionalArchitectureResponse.ClusterResolution(
                        cluster.code(),
                        cluster.label(),
                        metadataKey(cluster.code(), normalizedTribunal, normalizedUf),
                        true,
                        List.copyOf(reasons)
                );
            }
        }
        reasons.add("fallback_cluster_federal_superior");
        return new AdminInstitutionalArchitectureResponse.ClusterResolution(
                "FEDERAL_SUPERIOR",
                "Cluster Federal e Cortes Superiores",
                metadataKey("FEDERAL_SUPERIOR", normalizedTribunal, normalizedUf),
                true,
                List.copyOf(reasons)
        );
    }

    public Map<String, Object> healthSnapshot(Map<String, Object> federacaoHealth) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("clustersDeclarados", CLUSTERS.size());
        out.put("metadataStore", "POSTGRESQL_GLOBAL_COM_REGISTRO_DE_TOPOLOGIA_LOCALIZADOR_DE_PROCESSO_E_VINCULOS_DE_COOPERACAO");
        out.put("federacao", federacaoHealth == null ? Map.of() : federacaoHealth);
        return out;
    }

    private boolean matchesTribunalPrefix(AdminInstitutionalArchitectureResponse.ShardCluster cluster, String tribunalCodigo) {
        if (tribunalCodigo == null || tribunalCodigo.isBlank()) {
            return false;
        }
        return cluster.tribunalPrefixes().stream().anyMatch(tribunalCodigo::startsWith);
    }

    private String metadataKey(String clusterCode, String tribunalCodigo, String uf) {
        return clusterCode + ':' + blankToDash(tribunalCodigo) + ':' + blankToDash(uf);
    }

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }
}
