package com.tcc.pjb.backend.core.processual.routing;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;

@Component
public class RelationalConstraintMeshResolver {

    public RelationalConstraintMeshProfile resolve(NationalProcessRoutingService.RoutingCommand command,
                                                   String linkageMode,
                                                   String preventionMode,
                                                   String dependencyMode,
                                                   TerritorialRoutingProfile territorial) {
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();
        String attachmentMode;
        String targetDeskProfile;
        String registryBucket;
        String linkageStrength;
        String triageBucket;

        if (command.redistribuicaoImpedimento()) {
            attachmentMode = "REENCAMINHAR_COM_BLOQUEIO_ORGAO";
            targetDeskProfile = "MESA_REDISTRIBUICAO_IMPEDIMENTO";
            registryBucket = "IMPEDIMENTO_SUSPEICAO";
            linkageStrength = "ESTRITA";
            triageBucket = "GATE_IMPEDIMENTO";
            warnings.add("Redistribuição exige exclusão do órgão anterior e trilha de auditoria integral.");
            reviewChecklist.add("Bloquear sorteio para o órgão/relator impedido antes da nova distribuição.");
        } else if (notBlank(command.preventionReference())) {
            attachmentMode = "DISTRIBUIR_POR_PREVENCAO";
            targetDeskProfile = command.grau() == GrauJurisdicao.PRIMEIRO_GRAU ? "MESA_PREVENCAO_SERVENTIA" : "GABINETE_RELATOR_PREVENTO";
            registryBucket = "PREVENCAO_REFERENCIADA";
            linkageStrength = "ESTRITA";
            triageBucket = "GATE_PREVENTION";
            fundamentos.add("A prevenção referenciada prevalece sobre o sorteio livre enquanto não houver saneamento contrário.");
            reviewChecklist.add("Conferir identidade objetiva/subjetiva para ratificar a prevenção referenciada.");
        } else if (command.dependenciaDeclarada()) {
            attachmentMode = "ANEXAR_POR_DEPENDENCIA";
            targetDeskProfile = command.grau() == GrauJurisdicao.PRIMEIRO_GRAU ? "MESA_DEPENDENCIA_SERVENTIA" : "SECRETARIA_COLEGIADA_DEPENDENCIA";
            registryBucket = "DEPENDENCIA_PROCESSUAL";
            linkageStrength = "ALTA";
            triageBucket = "GATE_DEPENDENCIA";
            fundamentos.add("Processos dependentes devem compartilhar trilha de triagem, prevenção e controle de autos vinculados.");
        } else if (command.conexaoDeclarada() && command.continenciaDeclarada()) {
            attachmentMode = "REUNIR_E_REDISPONIBILIZAR_ACERVO";
            targetDeskProfile = "MESA_RELACIONAL_COMPLEXA";
            registryBucket = "CONEXAO_CONTINENCIA";
            linkageStrength = "ALTA";
            triageBucket = "GATE_RELACIONAL_COMPLEXO";
            warnings.add("Conexão e continência simultâneas exigem deliberação de secretaria/gabinete antes da consolidação do juízo prevento.");
            reviewChecklist.add("Identificar ação continente, ação contida e processo líder para o acervo reunido.");
        } else if (command.conexaoDeclarada()) {
            attachmentMode = "REUNIR_POR_CONEXAO";
            targetDeskProfile = "MESA_CONEXAO";
            registryBucket = "CONEXAO";
            linkageStrength = "MODERADA";
            triageBucket = "GATE_CONEXAO";
            fundamentos.add("Conexão orienta agrupamento probatório e prevenção para evitar decisões incongruentes.");
        } else if (command.continenciaDeclarada()) {
            attachmentMode = "REUNIR_POR_CONTINENCIA";
            targetDeskProfile = "MESA_CONTINENCIA";
            registryBucket = "CONTINENCIA";
            linkageStrength = "ALTA";
            triageBucket = "GATE_CONTINENCIA";
            fundamentos.add("Continência desloca a ação contida para o processo continente e impõe revisão da competência derivada.");
        } else {
            attachmentMode = "AUTONOMO";
            targetDeskProfile = "MESA_AUTONOMA";
            registryBucket = "AUTONOMA";
            linkageStrength = "BAIXA";
            triageBucket = "GATE_AUTONOMO";
        }

        if ((command.conexaoDeclarada() || command.continenciaDeclarada() || command.dependenciaDeclarada())
                && isBlank(command.processoReferencia()) && isBlank(command.preventionReference())) {
            warnings.add("Malha relacional sem processo raiz expresso; risco de vínculo implícito incorreto.");
            reviewChecklist.add("Preencher processo raiz ou prevenção expressa para travar o bucket relacional correto.");
        }
        if (command.grau() == GrauJurisdicao.SEGUNDO_GRAU || command.grau() == GrauJurisdicao.SUPERIOR || command.grau() == GrauJurisdicao.CONSTITUCIONAL) {
            fundamentos.add("Em instância colegiada, o bucket relacional deve respeitar relatoria, órgão fracionário e gabinete prevento.");
        }
        if (command.plantaoJudicial() || command.pedidoLiminar()) {
            warnings.add("Urgência ativa: a malha relacional deve ser processada em desk prioritário sem romper a prevenção material.");
        }
        if (command.segredoSolicitado()) {
            reviewChecklist.add("Aplicar visibilidade mínima ao compartilhar autos de referência, anexos e chaves de prevenção.");
        }
        if (territorial != null && territorial.territorialLabel() != null) {
            fundamentos.add("Registry territorial vinculado à malha relacional: " + territorial.territorialLabel() + '.');
        }
        if (!"AUTONOMA".equals(linkageMode)) {
            fundamentos.add("Modo relacional ativo: " + linkageMode + '.');
        }
        if (preventionMode != null && !preventionMode.isBlank()) {
            fundamentos.add("Prevenção operacional: " + preventionMode + '.');
        }
        if (dependencyMode != null && !dependencyMode.isBlank()) {
            fundamentos.add("Dependência operacional: " + dependencyMode + '.');
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("attachmentMode", attachmentMode);
        metadata.put("targetDeskProfile", targetDeskProfile);
        metadata.put("registryBucket", registryBucket);
        metadata.put("linkageStrength", linkageStrength);
        metadata.put("triageBucket", triageBucket);
        metadata.put("territorialToken", territorial != null ? territorial.territoryToken() : null);
        metadata.put("targetReference", firstNonBlank(command.preventionReference(), command.processoReferencia()));
        metadata.put("strictRegistryKey", buildRegistryKey(command, territorial, registryBucket));
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

        return new RelationalConstraintMeshProfile(
                attachmentMode,
                targetDeskProfile,
                registryBucket,
                linkageStrength,
                triageBucket,
                List.copyOf(warnings),
                List.copyOf(fundamentos),
                List.copyOf(reviewChecklist),
                metadata
        );
    }

    private String buildRegistryKey(NationalProcessRoutingService.RoutingCommand command,
                                    TerritorialRoutingProfile territorial,
                                    String registryBucket) {
        return String.join("|",
                firstNonBlank(registryBucket, "AUTONOMA"),
                firstNonBlank(command.preventionReference(), command.processoReferencia(), "SEM_REFERENCIA"),
                territorial == null ? "SEM_TERRITORIO" : firstNonBlank(territorial.territoryToken(), "SEM_TERRITORIO"),
                command.grau() == null ? "SEM_GRAU" : command.grau().name());
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (notBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean notBlank(String value) {
        return !isBlank(value);
    }
}
