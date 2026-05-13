package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.util.Objects;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorTipo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

public record RecursalCaseContext(
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
        boolean remessaNecessaria,
        boolean requisicaoPublicaPagamento) {

    public RecursalCaseContext(
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

    public RecursalCaseContext(
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

    public RecursalCaseContext {
        Objects.requireNonNull(tipoJustica, "tipoJustica");
        Objects.requireNonNull(ramo, "ramo");
        Objects.requireNonNull(rito, "rito");
        Objects.requireNonNull(fase, "fase");
        tribunalOrigem = tribunalOrigem == null ? RecursalTribunal.from(tipoJustica, null) : tribunalOrigem;
        tribunalDetalhadoOrigem = tribunalDetalhadoOrigem == null ? RecursalTribunalDetalhado.fromFamily(tribunalOrigem) : tribunalDetalhadoOrigem;
        instanciaAtual = instanciaAtual == null ? InstanceLevel.FIRST_INSTANCE : instanciaAtual;
        orgaoProlator = orgaoProlator == null ? OrgaoJulgadorTipo.MONOCRATICO : orgaoProlator;
        classFamily = classFamily == null ? RecursalClassClassifier.classify(classeProcessual, rito, ramo) : classFamily;
        requisicaoPublicaPagamento = requisicaoPublicaPagamento && fazendaPublicaOuMp;
    }

    public RecursalAuthority autoridadeAtual() {
        return switch (orgaoProlator) {
            case MONOCRATICO -> RecursalAuthority.JUIZO_SINGULAR;
            case RELATOR -> RecursalAuthority.RELATOR;
            case CAMARA -> RecursalAuthority.CAMARA;
            case TURMA -> RecursalAuthority.TURMA;
            case SECAO -> RecursalAuthority.SECAO;
            case ORGAO_ESPECIAL -> RecursalAuthority.ORGAO_ESPECIAL;
            case PLENARIO -> tribunalOrigem == RecursalTribunal.STJ ? RecursalAuthority.CORTE_ESPECIAL : RecursalAuthority.PLENARIO;
            case COLEGIADO -> tribunalOrigem == RecursalTribunal.STJ ? RecursalAuthority.SECAO : RecursalAuthority.TURMA;
        };
    }

    public boolean exigePreparoRecursal() {
        return !remessaNecessaria;
    }

    public boolean exigeContrarrazoesObrigatorias() {
        return !remessaNecessaria;
    }

    public boolean demandaRequisicaoPublicaPagamento() {
        return requisicaoPublicaPagamento;
    }
}
