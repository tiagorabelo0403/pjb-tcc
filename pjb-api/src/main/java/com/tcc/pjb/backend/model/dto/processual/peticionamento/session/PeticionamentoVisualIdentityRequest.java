package com.tcc.pjb.backend.model.dto.processual.peticionamento.session;

import jakarta.validation.constraints.Size;

public class PeticionamentoVisualIdentityRequest {

    @Size(max = 600)
    private String nomeExibicao;
    @Size(max = 600)
    private String nomeInstituicao;
    @Size(max = 512)
    private String brasaoOuLogomarcaUri;
    @Size(max = 1500)
    private String cabecalhoLivre;
    @Size(max = 1500)
    private String rodapeLivre;
    @Size(max = 16)
    private String paletaPrimaria;
    @Size(max = 16)
    private String paletaSecundaria;
    private Boolean exibirRegistroProfissional;
    private Boolean exibirBrasaoOuLogomarca;

    public String getNomeExibicao() {
        return nomeExibicao;
    }

    public void setNomeExibicao(String nomeExibicao) {
        this.nomeExibicao = nomeExibicao;
    }

    public String getNomeInstituicao() {
        return nomeInstituicao;
    }

    public void setNomeInstituicao(String nomeInstituicao) {
        this.nomeInstituicao = nomeInstituicao;
    }

    public String getBrasaoOuLogomarcaUri() {
        return brasaoOuLogomarcaUri;
    }

    public void setBrasaoOuLogomarcaUri(String brasaoOuLogomarcaUri) {
        this.brasaoOuLogomarcaUri = brasaoOuLogomarcaUri;
    }

    public String getCabecalhoLivre() {
        return cabecalhoLivre;
    }

    public void setCabecalhoLivre(String cabecalhoLivre) {
        this.cabecalhoLivre = cabecalhoLivre;
    }

    public String getRodapeLivre() {
        return rodapeLivre;
    }

    public void setRodapeLivre(String rodapeLivre) {
        this.rodapeLivre = rodapeLivre;
    }

    public String getPaletaPrimaria() {
        return paletaPrimaria;
    }

    public void setPaletaPrimaria(String paletaPrimaria) {
        this.paletaPrimaria = paletaPrimaria;
    }

    public String getPaletaSecundaria() {
        return paletaSecundaria;
    }

    public void setPaletaSecundaria(String paletaSecundaria) {
        this.paletaSecundaria = paletaSecundaria;
    }

    public Boolean getExibirRegistroProfissional() {
        return exibirRegistroProfissional;
    }

    public void setExibirRegistroProfissional(Boolean exibirRegistroProfissional) {
        this.exibirRegistroProfissional = exibirRegistroProfissional;
    }

    public Boolean getExibirBrasaoOuLogomarca() {
        return exibirBrasaoOuLogomarca;
    }

    public void setExibirBrasaoOuLogomarca(Boolean exibirBrasaoOuLogomarca) {
        this.exibirBrasaoOuLogomarca = exibirBrasaoOuLogomarca;
    }
}
