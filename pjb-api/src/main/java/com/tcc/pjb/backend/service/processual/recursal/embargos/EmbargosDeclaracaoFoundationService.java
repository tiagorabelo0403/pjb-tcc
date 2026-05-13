package com.tcc.pjb.backend.service.processual.recursal.embargos;

import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDeclaracao;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDeclaracaoContradicao;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDeclaracaoErroMaterial;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDeclaracaoGround;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDeclaracaoObscuridade;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDeclaracaoOmissao;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.EmbargosDeclaracaoBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalFormalSectionLabels;
import com.tcc.pjb.backend.model.dto.processual.recursal.embargos.EmbargosDeclaracaoFoundationResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.embargos.EmbargosDeclaracaoGroundView;
import com.tcc.pjb.backend.model.dto.processual.recursal.embargos.EmbargosDeclaracaoRequest;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class EmbargosDeclaracaoFoundationService {

    private final Map<String, EmbargosDeclaracaoGround> catalog;

    public EmbargosDeclaracaoFoundationService() {
        LinkedHashMap<String, EmbargosDeclaracaoGround> mutable = new LinkedHashMap<>();
        register(mutable, new EmbargosDeclaracaoOmissao("OMISSAO", true));
        register(mutable, new EmbargosDeclaracaoContradicao("CONTRADICAO", true));
        register(mutable, new EmbargosDeclaracaoObscuridade("OBSCURIDADE"));
        register(mutable, new EmbargosDeclaracaoErroMaterial("ERRO_MATERIAL", false));
        this.catalog = Map.copyOf(mutable);
    }

    public EmbargosDeclaracaoFoundationResponse describe() {
        return responseFrom(new EmbargosDeclaracao(
                new LinkedHashSet<>(catalog.values()),
                false,
                false,
                true
        ), null);
    }

    public EmbargosDeclaracaoFoundationResponse preview(EmbargosDeclaracaoRequest request) {
        Set<EmbargosDeclaracaoGround> grounds = request.fundamentos().stream()
                .map(this::resolveGround)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        EmbargosDeclaracao embargos = new EmbargosDeclaracao(
                grounds,
                request.efeitosInfringentesPretendidos(),
                request.contraDecisaoMonocratica(),
                request.interrompePrazoRecursalPrincipal()
        );
        return responseFrom(embargos, request.observacoes());
    }

    private EmbargosDeclaracaoFoundationResponse responseFrom(EmbargosDeclaracao embargos, String observacoes) {
        EmbargosDeclaracaoBlueprint blueprint = new EmbargosDeclaracaoBlueprint(
                5,
                true,
                embargos.interrompePrazoRecursalPrincipal(),
                embargos.grounds().stream().map(EmbargosDeclaracaoGround::formalName).collect(Collectors.toCollection(LinkedHashSet::new)),
                true
        );
        List<EmbargosDeclaracaoGroundView> detailed = embargos.grounds().stream()
                .map(ground -> new EmbargosDeclaracaoGroundView(
                        ground.code(),
                        ground.formalName(),
                        ground.admiteContraditorioPrevio(),
                        ground.admiteEfeitoModificativo()
                ))
                .toList();
        return new EmbargosDeclaracaoFoundationResponse(
                blueprint.prazoDiasUteis(),
                blueprint.cabivelContraQualquerDecisao(),
                blueprint.interrompePrazoRecursalPrincipal(),
                blueprint.efeitoInfringenteExigeFundamentoApto(),
                blueprint.fundamentosCabiveis(),
                detailed,
                embargos.requiresCounterReasons(),
                embargos.potentiallyRequiresPreparo(),
                embargos.requiresCollegiateMerit(),
                List.of(
                        RecursalFormalSectionLabels.PETICAO_INTERPOSICAO,
                        RecursalFormalSectionLabels.FUNDAMENTOS_EMBARGOS,
                        RecursalFormalSectionLabels.PEDIDO_EFEITO_INFRINGENTE
                ),
                observacoes
        );
    }

    private EmbargosDeclaracaoGround resolveGround(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("fundamento de embargos não pode ser nulo");
        }
        String normalized = raw.trim().toUpperCase(java.util.Locale.ROOT);
        EmbargosDeclaracaoGround ground = catalog.get(normalized);
        if (ground == null) {
            throw new IllegalArgumentException("fundamento de embargos não reconhecido: " + raw);
        }
        return ground;
    }

    private static void register(Map<String, EmbargosDeclaracaoGround> target, EmbargosDeclaracaoGround ground) {
        target.put(ground.code().toUpperCase(java.util.Locale.ROOT), ground);
    }
}
