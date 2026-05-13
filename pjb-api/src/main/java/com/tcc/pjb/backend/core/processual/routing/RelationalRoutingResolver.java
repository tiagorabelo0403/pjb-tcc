package com.tcc.pjb.backend.core.processual.routing;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;

@Component
public class RelationalRoutingResolver {

    private final RelationalConstraintMeshResolver constraintMeshResolver;
    private final PreventionConstraintResolver preventionConstraintResolver;

    public RelationalRoutingResolver(RelationalConstraintMeshResolver constraintMeshResolver,
                                     PreventionConstraintResolver preventionConstraintResolver) {
        this.constraintMeshResolver = Objects.requireNonNull(constraintMeshResolver);
        this.preventionConstraintResolver = Objects.requireNonNull(preventionConstraintResolver);
    }

    public RelationalRoutingProfile resolve(NationalProcessRoutingService.RoutingCommand command,
                                            TipoJustica tipoJustica,
                                            TerritorialRoutingProfile territorial) {
        Objects.requireNonNull(command, "command");
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();

        String reference = firstNonBlank(command.preventionReference(), command.processoReferencia());
        String linkageMode;
        String preventionMode;
        String dependencyMode;
        String distributionOverride;
        String deskSuffix;
        boolean strictPrevention = false;

        if (command.redistribuicaoImpedimento()) {
            linkageMode = "REDISTRIBUICAO_IMPEDIMENTO";
            preventionMode = "AFASTAMENTO_ORGAO_PREVENTO";
            dependencyMode = "NOVO_SORTEIO_CONTROLADO";
            distributionOverride = "REDISTRIBUICAO_IMPEDIMENTO";
            deskSuffix = "REDISTRIBUICAO";
            strictPrevention = true;
            warnings.add("Redistribuição por impedimento informada; vedar manutenção no mesmo acervo prevento.");
            reviewChecklist.add("Confirmar impedimento/suspeição, trilha de auditoria e bloqueio do órgão de origem.");
        } else if (notBlank(command.preventionReference())) {
            linkageMode = "PREVENCAO_REFERENCIADA";
            preventionMode = "PREVENCAO_ESTRITA:" + command.preventionReference().trim();
            dependencyMode = command.grau() == GrauJurisdicao.PRIMEIRO_GRAU ? "MESMA_UNIDADE" : "MESMO_RELATOR";
            distributionOverride = "DEPENDENCIA_PREVENCAO";
            deskSuffix = "PREVENTO";
            strictPrevention = true;
            warnings.add("Referência de prevenção declarada; sorteio livre deve permanecer bloqueado até conferência da unidade preventa.");
            reviewChecklist.add("Conferir identidade de partes, pedido, causa de pedir e órgão prevento.");
        } else if (notBlank(command.processoReferencia()) && command.dependenciaDeclarada()) {
            linkageMode = "DEPENDENCIA_PROCESSUAL";
            preventionMode = "DEPENDENCIA_REFERENCIADA:" + command.processoReferencia().trim();
            dependencyMode = command.grau() == GrauJurisdicao.PRIMEIRO_GRAU ? "MESMA_SERVENTIA" : "MESMO_COLEGIADO";
            distributionOverride = "DEPENDENCIA_PROCESSUAL";
            deskSuffix = "DEPENDENCIA";
            warnings.add("Dependência processual declarada; validar distribuição por anexação e não por sorteio autônomo.");
            reviewChecklist.add("Confirmar vinculação entre processo principal e incidente/dependente.");
        } else if (command.conexaoDeclarada() && command.continenciaDeclarada()) {
            linkageMode = "CONEXAO_CONTINENCIA";
            preventionMode = "PREVENCAO_POR_RELACAO";
            dependencyMode = "MALHA_RELACIONAL_COMPLEXA";
            distributionOverride = "DISTRIBUICAO_RELACIONAL";
            deskSuffix = "RELACIONAL";
            warnings.add("Conexão e continência declaradas simultaneamente; triagem deve validar atração do juízo prevento.");
            reviewChecklist.add("Checar prevenção, risco de decisões conflitantes e necessidade de reunião processual.");
        } else if (command.conexaoDeclarada()) {
            linkageMode = "CONEXAO_DECLARADA";
            preventionMode = territorial.preventionMode();
            dependencyMode = "AGRUPAMENTO_POR_CONEXAO";
            distributionOverride = "DISTRIBUICAO_RELACIONAL";
            deskSuffix = "CONEXAO";
            warnings.add("Conexão declarada; revisar prevenção e acervo já distribuído.");
            reviewChecklist.add("Validar identidade parcial de causa de pedir, pedido ou prova compartilhada.");
        } else if (command.continenciaDeclarada()) {
            linkageMode = "CONTINENCIA_DECLARADA";
            preventionMode = "PREVENCAO_POR_CONTINENCIA";
            dependencyMode = "REUNIAO_POR_CONTINENCIA";
            distributionOverride = "DISTRIBUICAO_RELACIONAL";
            deskSuffix = "CONTINENCIA";
            strictPrevention = true;
            warnings.add("Continência declarada; risco de distribuição em vara distinta exige revisão humana.");
            reviewChecklist.add("Conferir abrangência subjetiva/objetiva entre ação continente e contida.");
        } else {
            linkageMode = "AUTONOMA";
            preventionMode = territorial.preventionMode();
            dependencyMode = "SEM_VINCULO";
            distributionOverride = null;
            deskSuffix = "AUTONOMA";
        }

        if ((command.conexaoDeclarada() || command.continenciaDeclarada() || command.dependenciaDeclarada())
                && isBlank(command.processoReferencia()) && isBlank(command.preventionReference())) {
            warnings.add("Sinal relacional sem processo de referência explícito; identificar autos de origem antes da distribuição final.");
            reviewChecklist.add("Informar número CNJ/processo de referência para prevenir vínculo implícito indevido.");
        }

        if ((command.grau() == GrauJurisdicao.SEGUNDO_GRAU || command.grau() == GrauJurisdicao.SUPERIOR || command.grau() == GrauJurisdicao.CONSTITUCIONAL)
                && !"AUTONOMA".equals(linkageMode)) {
            strictPrevention = true;
            fundamentos.add("Distribuição em instância colegiada com relação processual ativa; prevenção deve ser validada em gabinete/relatoria.");
            reviewChecklist.add("Conferir relator prevento, órgão fracionário prevento e acervo vinculado.");
        }

        if (command.segredoSolicitado()) {
            fundamentos.add("Sigilo solicitado impacta anexação relacional, compartilhamento de prevenção e visibilidade de autos referenciados.");
        }
        if (command.plantaoJudicial() || command.pedidoLiminar()) {
            fundamentos.add("Urgência declarada pode deslocar a relação processual para desk imediato sem afastar prevenção.");
        }
        if (tipoJustica != null) {
            fundamentos.add("Malha relacional calibrada para a justiça " + tipoJustica.name() + '.');
        }
        if (territorial.territorialLabel() != null) {
            fundamentos.add("Âncora territorial relacional: " + territorial.territorialLabel());
        }

        PreventionConstraintProfile binding = preventionConstraintResolver.resolve(command, territorial);
        reference = binding.effectiveReference(reference);
        strictPrevention = strictPrevention || binding.strictLock();
        warnings.addAll(binding.warnings());
        fundamentos.addAll(binding.fundamentos());
        reviewChecklist.addAll(binding.reviewChecklist());

        RelationalConstraintMeshProfile mesh = constraintMeshResolver.resolve(command, linkageMode, preventionMode, dependencyMode, territorial);
        warnings.addAll(mesh.warnings());
        fundamentos.addAll(mesh.fundamentos());
        reviewChecklist.addAll(mesh.reviewChecklist());

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("processoReferencia", command.processoReferencia());
        metadata.put("preventionReference", command.preventionReference());
        metadata.put("dependenciaDeclarada", command.dependenciaDeclarada());
        metadata.put("conexaoDeclarada", command.conexaoDeclarada());
        metadata.put("continenciaDeclarada", command.continenciaDeclarada());
        metadata.put("redistribuicaoImpedimento", command.redistribuicaoImpedimento());
        metadata.put("justica", tipoJustica != null ? tipoJustica.name() : null);
        metadata.put("grau", command.grau() != null ? command.grau().name() : null);
        metadata.put("territorialMode", territorial.mode());
        metadata.put("binding", binding.toMap());
        metadata.put("constraintMesh", mesh.toMap());

        return new RelationalRoutingProfile(
                linkageMode,
                preventionMode,
                dependencyMode,
                reference,
                distributionOverride,
                deskSuffix,
                mesh.attachmentMode(),
                mesh.targetDeskProfile(),
                mesh.registryBucket(),
                mesh.linkageStrength(),
                mesh.triageBucket(),
                strictPrevention,
                List.copyOf(warnings),
                List.copyOf(fundamentos),
                List.copyOf(reviewChecklist),
                metadata
        );
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
