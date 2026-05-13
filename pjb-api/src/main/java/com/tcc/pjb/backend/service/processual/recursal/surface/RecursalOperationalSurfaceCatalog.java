package com.tcc.pjb.backend.service.processual.recursal.surface;

import com.tcc.pjb.backend.model.dto.processual.recursal.surface.RecursalOperationalSurfaceGapView;
import java.util.List;
import java.util.Set;

public final class RecursalOperationalSurfaceCatalog {

    private static final RecursalOperationalSurfaceGapView GAP_CONTRACTS = new RecursalOperationalSurfaceGapView(
            "SPECIALIZED_CONTRACTS_AND_ITS",
            "CRITICA",
            "Fechar contracts e ITs por surface especializada",
            "As surfaces recursais agora estão segmentadas por eixo, mas ainda faltam provider contracts e integração executável dedicados para blindar o boundary HTTP de advogado, institucional, documental e inteligência."
    );

    private static final RecursalOperationalSurfaceGapView GAP_DOCUMENTAL = new RecursalOperationalSurfaceGapView(
            "DOCUMENT_VIEWER_ASSINATURA_AUTENTICIDADE",
            "ALTA",
            "Aprofundar viewer, autenticidade e assinatura",
            "O eixo documental recursal agora expõe suite soberana de visualizador, autenticidade e evidência de assinatura, mas ainda falta endurecer validação externa federada, política fina de sigilo por artefato e fechamento global do compile fora do recursal."
    );

    private static final RecursalOperationalSurfaceGapView GAP_MOBILE = new RecursalOperationalSurfaceGapView(
            "MOBILE_PUSH_GOVERNANCE",
            "ALTA",
            "Endurecer avisos móveis sem scheduler paralelo",
            "A governança recursal de mobile, ciência, preferências finas por perfil/canal, política federada e endurecimento móvel soberano agora existe como suite HTTP própria, restando seguir fechando o compile global do pjb-api fora do recursal."
    );

    private static final RecursalOperationalSurfaceGapView GAP_GLOBAL_COMPILE = new RecursalOperationalSurfaceGapView(
            "GLOBAL_COMPILE_RECOVERY",
            "ALTA",
            "Continuar fechamento do compile global do pjb-api",
            "A rodada preserva o recursal organizado e mais explícito em HTTP, mas a recuperação global de compile do pjb-api ainda permanece aberta fora do escopo local destas surfaces."
    );

    public static final RecursalOperationalSurfaceAxisDefinition ATTORNEY = new RecursalOperationalSurfaceAxisDefinition(
            "SURFACE_ADVOGADO_RECURSAL",
            "Advogado, partes e peticionamento",
            "/surfaces/attorney",
            Set.of(
                    "PAINEL_ADVOGADO_RECURSAL_COMPLETO",
                    "PAINEL_RECURSAL_PARTES_REPRESENTANTES",
                    "AUTOS_DIGITAIS_RECURSAIS_DETALHADOS",
                    "HABILITACAO_ASSOCIACAO_RECURSAL_GOVERNADA",
                    "ESCRITORIO_ASSISTENTES_SUBSTABELECIMENTO_RECURSAL",
                    "CERTIDOES_EXTERNAS_RECURSAIS",
                    "REUSO_INTELIGENTE_PETICIONAMENTO_RECURSAL",
                    "PETICIONAMENTO_LOTE_ASSINATURA_RECURSAL",
                    "MATRIZ_NACIONAL_PETICIONAMENTO_RECURSAL"
            ),
            List.of(GAP_CONTRACTS)
    );

    public static final RecursalOperationalSurfaceAxisDefinition INSTITUTIONAL = new RecursalOperationalSurfaceAxisDefinition(
            "SURFACE_INSTITUCIONAL_RECURSAL",
            "Institucional, caixas e secretaria",
            "/surfaces/institutional",
            Set.of(
                    "PAINEL_EXTERNO_OPERACIONAL_RECURSAL",
                    "ORGANIZACAO_INSTITUCIONAL_RECURSAL",
                    "CAIXAS_HISTORICO_INSTITUCIONAL_RECURSAL",
                    "SECRETARIA_MULTIGRAU_REFORCADA",
                    "MATRIZ_CAPACIDADES_SECRETARIA_MULTIGRAU"
            ),
            List.of(GAP_CONTRACTS)
    );

    public static final RecursalOperationalSurfaceAxisDefinition DOCUMENTAL = new RecursalOperationalSurfaceAxisDefinition(
            "SURFACE_DOCUMENTAL_RECURSAL",
            "Autos digitais, certidões e colaboração documental",
            "/surfaces/documental",
            Set.of(
                    "COLABORACAO_MULTIMIDIA_DOCUMENTAL_RECURSAL",
                    "AUTOS_DIGITAIS_RECURSAIS_DETALHADOS",
                    "CERTIDOES_EXTERNAS_RECURSAIS",
                    "COMPETENCIA_E_DISTRIBUICAO_RECURSAL_GUIADA",
                    "WIZARD_DISTRIBUICAO_ASSISTIDA_IA"
            ),
            List.of(GAP_CONTRACTS, GAP_DOCUMENTAL)
    );

    public static final RecursalOperationalSurfaceAxisDefinition INTELLIGENCE = new RecursalOperationalSurfaceAxisDefinition(
            "SURFACE_INTELIGENCIA_RECURSAL",
            "Observabilidade, indexação e avisos",
            "/surfaces/intelligence",
            Set.of(
                    "OBSERVABILIDADE_INDEXACAO_INTELIGENTE_RECURSAL",
                    "ALERTAS_PRAZO_NOTIFICACOES_RECURSAIS",
                    "ESCALONAMENTO_ALERTAS_POR_PERFIL",
                    "ALERTA_VERMELHO_MULTICANAL_E_VOTOS_VIVOS",
                    "POS_JULGAMENTO_RECURSAL_ESCALONADO"
            ),
            List.of(GAP_CONTRACTS, GAP_MOBILE)
    );

    private static final List<RecursalOperationalSurfaceAxisDefinition> ALL = List.of(
            ATTORNEY,
            INSTITUTIONAL,
            DOCUMENTAL,
            INTELLIGENCE
    );

    private static final List<RecursalOperationalSurfaceGapView> AGGREGATED_GAPS = List.of(
            GAP_CONTRACTS,
            GAP_DOCUMENTAL,
            GAP_MOBILE,
            GAP_GLOBAL_COMPILE
    );

    private RecursalOperationalSurfaceCatalog() {
    }

    public static List<RecursalOperationalSurfaceAxisDefinition> all() {
        return ALL;
    }

    public static List<RecursalOperationalSurfaceGapView> aggregatedGaps(boolean poderRecorrerBloqueado) {
        if (poderRecorrerBloqueado) {
            return List.of(GAP_CONTRACTS, GAP_DOCUMENTAL, GAP_GLOBAL_COMPILE);
        }
        return AGGREGATED_GAPS;
    }
}
