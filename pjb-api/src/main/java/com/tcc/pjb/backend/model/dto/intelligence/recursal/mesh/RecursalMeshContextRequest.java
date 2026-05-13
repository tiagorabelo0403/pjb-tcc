package com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalClassFamily;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalDetalhado;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorTipo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

public record RecursalMeshContextRequest(
        @Positive Long processoId,
        @Size(max = 50) String numeroProcesso,
        @NotNull TipoJustica tipoJustica,
        @NotNull RamoDireito ramo,
        @NotNull RitoProcessual rito,
        @NotNull FaseProcessual fase,
        @NotBlank @Size(max = 160) String classeProcessual,
        @NotNull RecursalClassFamily classFamily,
        @NotNull RecursalTribunal tribunalOrigem,
        @NotNull RecursalTribunalDetalhado tribunalDetalhadoOrigem,
        @NotNull InstanceLevel instanciaAtual,
        @NotNull OrgaoJulgadorTipo orgaoProlator,
        boolean decisaoMonocratica,
        boolean acordaoColegiado,
        boolean fazendaPublicaOuMp,
        boolean justicaGratuitaOuIsencaoLegal,
        boolean materiaFederalInfraconstitucional,
        boolean materiaConstitucional,
        boolean tempestivo,
        boolean remessaNecessaria,
        boolean requisicaoPublicaPagamento) {

    public RecursalMeshContextRequest(
            Long processoId,
            String numeroProcesso,
            TipoJustica tipoJustica,
            RamoDireito ramo,
            RitoProcessual rito,
            FaseProcessual fase,
            String classeProcessual,
            RecursalClassFamily classFamily,
            RecursalTribunal tribunalOrigem,
            RecursalTribunalDetalhado tribunalDetalhadoOrigem,
            InstanceLevel instanciaAtual,
            OrgaoJulgadorTipo orgaoProlator,
            boolean decisaoMonocratica,
            boolean acordaoColegiado,
            boolean fazendaPublicaOuMp,
            boolean justicaGratuitaOuIsencaoLegal,
            boolean materiaFederalInfraconstitucional,
            boolean materiaConstitucional,
            boolean tempestivo,
            boolean remessaNecessaria) {
        this(
                processoId,
                numeroProcesso,
                tipoJustica,
                ramo,
                rito,
                fase,
                classeProcessual,
                classFamily,
                tribunalOrigem,
                tribunalDetalhadoOrigem,
                instanciaAtual,
                orgaoProlator,
                decisaoMonocratica,
                acordaoColegiado,
                fazendaPublicaOuMp,
                justicaGratuitaOuIsencaoLegal,
                materiaFederalInfraconstitucional,
                materiaConstitucional,
                tempestivo,
                remessaNecessaria,
                false
        );
    }

    public RecursalMeshContextRequest(
            Long processoId,
            String numeroProcesso,
            TipoJustica tipoJustica,
            RamoDireito ramo,
            RitoProcessual rito,
            FaseProcessual fase,
            String classeProcessual,
            RecursalClassFamily classFamily,
            RecursalTribunal tribunalOrigem,
            RecursalTribunalDetalhado tribunalDetalhadoOrigem,
            InstanceLevel instanciaAtual,
            OrgaoJulgadorTipo orgaoProlator,
            boolean decisaoMonocratica,
            boolean acordaoColegiado,
            boolean fazendaPublicaOuMp,
            boolean justicaGratuitaOuIsencaoLegal,
            boolean materiaFederalInfraconstitucional,
            boolean materiaConstitucional,
            boolean tempestivo) {
        this(
                processoId,
                numeroProcesso,
                tipoJustica,
                ramo,
                rito,
                fase,
                classeProcessual,
                classFamily,
                tribunalOrigem,
                tribunalDetalhadoOrigem,
                instanciaAtual,
                orgaoProlator,
                decisaoMonocratica,
                acordaoColegiado,
                fazendaPublicaOuMp,
                justicaGratuitaOuIsencaoLegal,
                materiaFederalInfraconstitucional,
                materiaConstitucional,
                tempestivo,
                false,
                false
        );
    }
}
