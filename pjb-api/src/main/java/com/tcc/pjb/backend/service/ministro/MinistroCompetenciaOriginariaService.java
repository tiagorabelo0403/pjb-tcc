package com.tcc.pjb.backend.service.ministro;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

@Service
public class MinistroCompetenciaOriginariaService {

    private static final Map<String, CompetenciaOriginariaView> CATALOGO = Map.ofEntries(
            Map.entry("ADI", new CompetenciaOriginariaView("ADI", "Ação Direta de Inconstitucionalidade",
                    RitoProcessual.ESPECIAL_ACAO_DIRETA_INCONSTITUCIONALIDADE, "Controle concentrado abstrato de constitucionalidade.", List.of("Petição inicial", "Informações", "Manifestação da AGU/PGR", "Julgamento plenário"))),
            Map.entry("ADC", new CompetenciaOriginariaView("ADC", "Ação Declaratória de Constitucionalidade",
                    RitoProcessual.ESPECIAL_ACAO_DECLARATORIA_CONSTITUCIONALIDADE, "Controle concentrado para afirmação de constitucionalidade.", List.of("Petição inicial", "Informações", "Manifestação da AGU/PGR", "Julgamento plenário"))),
            Map.entry("ADPF", new CompetenciaOriginariaView("ADPF", "Arguição de Descumprimento de Preceito Fundamental",
                    RitoProcessual.ESPECIAL_ARGUICAO_DESCUMPRIMENTO_PRECEITO_FUNDAMENTAL, "Proteção de preceitos fundamentais em hipóteses subsidiárias.", List.of("Petição inicial", "Admissibilidade", "Manifestação de autoridades", "Julgamento plenário"))),
            Map.entry("HC_ORIGINARIO", new CompetenciaOriginariaView("HC_ORIGINARIO", "Habeas Corpus Originário",
                    RitoProcessual.ESPECIAL_HABEAS_CORPUS, "Controle imediato de liberdade de locomoção em competência originária.", List.of("Distribuição", "Informações da autoridade coatora", "Parecer ministerial", "Julgamento"))),
            Map.entry("MS_MINISTRO_ESTADO", new CompetenciaOriginariaView("MS_MINISTRO_ESTADO", "Mandado de Segurança contra ato de Ministro de Estado",
                    RitoProcessual.ESPECIAL_MANDADO_SEGURANCA, "Tutela mandamental em competência originária.", List.of("Petição inicial", "Liminar", "Informações", "Parecer ministerial", "Julgamento"))),
            Map.entry("RCL", new CompetenciaOriginariaView("RCL", "Reclamação",
                    RitoProcessual.PENAL_RECLAMACAO_CRIMINAL, "Preservação de competência e garantia de autoridade de decisões.", List.of("Distribuição", "Medida urgente", "Informações", "Julgamento"))),
            Map.entry("AO", new CompetenciaOriginariaView("AO", "Ação Originária",
                    RitoProcessual.COMUM_ORDINARIO, "Ações originárias gerais dos tribunais superiores.", List.of("Distribuição", "Citação", "Contestação", "Instrução", "Julgamento")))
    );

    public List<CompetenciaOriginariaView> listarCatalogo() {
        return CATALOGO.values().stream().sorted((a, b) -> a.sigla().compareToIgnoreCase(b.sigla())).toList();
    }

    public CompetenciaOriginariaView sugerir(String siglaOuClasse) {
        String key = normalize(siglaOuClasse);
        if (CATALOGO.containsKey(key)) {
            return CATALOGO.get(key);
        }
        if (key.contains("ADPF")) {
            return CATALOGO.get("ADPF");
        }
        if (key.contains("ADI")) {
            return CATALOGO.get("ADI");
        }
        if (key.contains("ADC")) {
            return CATALOGO.get("ADC");
        }
        if (key.contains("HABEAS") || key.equals("HC")) {
            return CATALOGO.get("HC_ORIGINARIO");
        }
        if (key.contains("MANDADO") && key.contains("SEGURANCA")) {
            return CATALOGO.get("MS_MINISTRO_ESTADO");
        }
        if (key.contains("RECLAMACAO")) {
            return CATALOGO.get("RCL");
        }
        return CATALOGO.get("AO");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]+", "_");
    }

    public record CompetenciaOriginariaView(
            String sigla,
            String titulo,
            RitoProcessual ritoProcessual,
            String finalidade,
            List<String> etapasNucleares
    ) {
    }
}
