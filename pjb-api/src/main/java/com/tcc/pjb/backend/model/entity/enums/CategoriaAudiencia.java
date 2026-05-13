package com.tcc.pjb.backend.model.entity.enums;

import java.util.EnumSet;
import java.util.Set;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;

public enum CategoriaAudiencia {

    
    CONSENSUAL(
            "Consensual",
            "Audiências voltadas à autocomposição",
            EnumSet.of(
                    RamoDireito.CIVIL,
                    RamoDireito.TRABALHISTA,
                    RamoDireito.ADMINISTRATIVO
            ),
            EnumSet.of(
                    FaseProcessual.CONHECIMENTO,
                    FaseProcessual.EXECUCAO
            ),
            EnumSet.of(
                    OrgaoJulgadorTipo.MONOCRATICO
            ),
            false,
            true,
            true,
            NivelSegurancaInstitucional.PADRAO,
            true,
            true,
            true
    ),

    
    INSTRUTORIA(
            "Instrutória",
            "Produção de provas orais, técnicas ou periciais",
            EnumSet.of(
                    RamoDireito.CIVIL,
                    RamoDireito.PENAL,
                    RamoDireito.TRABALHISTA,
                    RamoDireito.MILITAR
            ),
            
            
            EnumSet.of(
                    FaseProcessual.CONHECIMENTO
            ),
            EnumSet.of(
                    OrgaoJulgadorTipo.MONOCRATICO
            ),
            false,
            false,
            false,
            NivelSegurancaInstitucional.REFORCADO,
            true,
            true,
            true
    ),

    
    PROCESSUAL(
            "Processual",
            "Organização, saneamento e condução do processo",
            EnumSet.allOf(RamoDireito.class),
            EnumSet.of(
                    FaseProcessual.CONHECIMENTO,
                    FaseProcessual.RECURSAL
            ),
            EnumSet.of(
                    OrgaoJulgadorTipo.MONOCRATICO,
                    OrgaoJulgadorTipo.COLEGIADO
            ),
            false,
            false,
            false,
            NivelSegurancaInstitucional.REFORCADO,
            true,
            true,
            false
    ),

    
    ESPECIAL(
            "Especial",
            "Audiências de impacto institucional relevante",
            EnumSet.of(
                    RamoDireito.PENAL,
                    RamoDireito.CONSTITUCIONAL,
                    RamoDireito.ELEITORAL,
                    RamoDireito.MILITAR
            ),
            
            EnumSet.of(
                    FaseProcessual.CONHECIMENTO,
                    FaseProcessual.RECURSAL
            ),
            EnumSet.of(
                    OrgaoJulgadorTipo.MONOCRATICO,
                    OrgaoJulgadorTipo.COLEGIADO
            ),
            true,
            false,
            false,
            NivelSegurancaInstitucional.ALTO,
            false,
            true,
            true
    ),

    
    CONSTITUCIONAL(
            "Constitucional",
            "Audiências relacionadas a matérias constitucionais e controle concentrado",
            EnumSet.of(
                    RamoDireito.CONSTITUCIONAL
            ),
            
            
            EnumSet.of(
                    FaseProcessual.RECURSAL
            ),
            EnumSet.of(
                    OrgaoJulgadorTipo.PLENARIO
            ),
            true,
            false,
            false,
            NivelSegurancaInstitucional.MAXIMO_CONSTITUCIONAL,
            false,
            true,
            true
    ),

    
    MODERNA(
            "Moderna",
            "Audiências digitais, híbridas ou assíncronas",
            EnumSet.allOf(RamoDireito.class),
            EnumSet.allOf(FaseProcessual.class),
            EnumSet.of(
                    OrgaoJulgadorTipo.MONOCRATICO,
                    OrgaoJulgadorTipo.COLEGIADO
            ),
            false,
            true,
            true,
            NivelSegurancaInstitucional.REFORCADO,
            true,
            true,
            false
    );

    

    private final String rotulo;
    private final String descricao;
    private final Set<RamoDireito> ramosPermitidos;
    private final Set<FaseProcessual> fasesPermitidas;
    private final Set<OrgaoJulgadorTipo> orgaosPermitidos;

    private final boolean existeNoSTF;
    private final boolean admiteIA;
    private final boolean admitePublicidade;

    private final NivelSegurancaInstitucional nivelSeguranca;
    private final boolean exigeAta;
    private final boolean admiteGravacao;
    private final boolean riscoNulidade;

    CategoriaAudiencia(
            String rotulo,
            String descricao,
            Set<RamoDireito> ramosPermitidos,
            Set<FaseProcessual> fasesPermitidas,
            Set<OrgaoJulgadorTipo> orgaosPermitidos,
            boolean existeNoSTF,
            boolean admiteIA,
            boolean admitePublicidade,
            NivelSegurancaInstitucional nivelSeguranca,
            boolean exigeAta,
            boolean admiteGravacao,
            boolean riscoNulidade
    ) {
        this.rotulo = rotulo;
        this.descricao = descricao;
        this.ramosPermitidos = Set.copyOf(ramosPermitidos);
        this.fasesPermitidas = Set.copyOf(fasesPermitidas);
        this.orgaosPermitidos = Set.copyOf(orgaosPermitidos);
        this.existeNoSTF = existeNoSTF;
        this.admiteIA = admiteIA;
        this.admitePublicidade = admitePublicidade;
        this.nivelSeguranca = nivelSeguranca;
        this.exigeAta = exigeAta;
        this.admiteGravacao = admiteGravacao;
        this.riscoNulidade = riscoNulidade;
    }

    

    public boolean compativelCom(RamoDireito ramo, FaseProcessual fase, OrgaoJulgadorTipo orgao) {
        return ramo != null && fase != null && orgao != null
                && ramosPermitidos.contains(ramo)
                && fasesPermitidas.contains(fase)
                && orgaosPermitidos.contains(orgao);
    }

    public boolean exigeBlindagemMaxima() {
        return nivelSeguranca == NivelSegurancaInstitucional.MAXIMO_CONSTITUCIONAL;
    }

    public boolean geraRiscoDeNulidade() {
        return riscoNulidade;
    }

    

    public String getRotulo() {
        return rotulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public Set<RamoDireito> getRamosPermitidos() {
        return ramosPermitidos;
    }

    public Set<FaseProcessual> getFasesPermitidas() {
        return fasesPermitidas;
    }

    public Set<OrgaoJulgadorTipo> getOrgaosPermitidos() {
        return orgaosPermitidos;
    }

    public boolean isExisteNoSTF() {
        return existeNoSTF;
    }

    public boolean isAdmiteIA() {
        return admiteIA;
    }

    public boolean isAdmitePublicidade() {
        return admitePublicidade;
    }

    public NivelSegurancaInstitucional getNivelSeguranca() {
        return nivelSeguranca;
    }

    public boolean isExigeAta() {
        return exigeAta;
    }

    public boolean isAdmiteGravacao() {
        return admiteGravacao;
    }
}
