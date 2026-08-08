package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import com.tcc.pjb.backend.model.entity.enums.EnteFederativo;
import com.tcc.pjb.backend.model.entity.enums.PapelEquipe;
import com.tcc.pjb.backend.model.entity.enums.SituacaoConta;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@PjbDataOwnership(module = PjbModuleId.IDENTIDADE_SEGURANCA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String email;
    private String cpf;
    private String oab;

    @Column(name = "oab_normalizada")
    private String oabNormalizada;

    @Column(name = "oab_uf")
    private String oabUf;

    @Column(name = "oab_sufixo")
    private String oabSufixo;

    @Enumerated(EnumType.STRING)
    private TipoUsuario tipoUsuario;

    private String perfil;

    @Column(name = "senha")
    private String senha;
    private String uf;
    private String comarca;
    private Boolean ativo;

    @Enumerated(EnumType.STRING)
    @Column(name = "ente_federativo")
    private EnteFederativo enteFederativo;

    @Column(name = "identidade_juridica_id")
    private UUID identidadeJuridicaId;

    @Column(name = "registro_profissional")
    private String registroProfissional;

    @Column(name = "especialidades_raw", length = 2000)
    private String especialidadesRaw;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao_conta", nullable = false, length = 30)
    private SituacaoConta situacaoConta = SituacaoConta.ATIVA;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public String getUsername() { return email != null && !email.isBlank() ? email : cpf; }
    public void setEmail(String email) { this.email = email; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getOab() { return oab; }
    public void setOab(String oab) { this.oab = oab; }

    public String getOabNumero() {
        if (oabNormalizada != null && !oabNormalizada.isBlank()) {
            String digits = oabNormalizada.replaceAll("\\D+", "");
            return digits.isBlank() ? oabNormalizada : digits;
        }
        if (oab != null && !oab.isBlank()) {
            String digits = oab.replaceAll("\\D+", "");
            return digits.isBlank() ? oab : digits;
        }
        return null;
    }

    public void setOabNumero(String oabNumero) {
        if (oabNumero == null || oabNumero.isBlank()) {
            this.oab = null;
            return;
        }
        String numero = oabNumero.trim();
        this.oab = (oabUf == null || oabUf.isBlank()) ? numero : numero + "/" + oabUf.trim().toUpperCase();
        if (this.oabNormalizada == null || this.oabNormalizada.isBlank()) {
            this.oabNormalizada = numero.replaceAll("\\s+", "").toUpperCase();
        }
    }

    public String getOabNormalizada() { return oabNormalizada; }
    public void setOabNormalizada(String oabNormalizada) { this.oabNormalizada = oabNormalizada; }

    public String getOabUf() { return oabUf; }
    public void setOabUf(String oabUf) { this.oabUf = oabUf; }

    public String getOabSufixo() { return oabSufixo; }
    public void setOabSufixo(String oabSufixo) { this.oabSufixo = oabSufixo; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public EnteFederativo getEnteFederativo() { return enteFederativo; }
    public void setEnteFederativo(EnteFederativo enteFederativo) { this.enteFederativo = enteFederativo; }

    public UUID getIdentidadeJuridicaId() { return identidadeJuridicaId; }
    public void setIdentidadeJuridicaId(UUID identidadeJuridicaId) { this.identidadeJuridicaId = identidadeJuridicaId; }

    public boolean atuaNoMunicipio() {
        return enteFederativo == EnteFederativo.MUNICIPIO;
    }

    public boolean atuaNoEstado() {
        return enteFederativo == EnteFederativo.ESTADO;
    }

    public boolean atuaNaUniao() {
        return enteFederativo == EnteFederativo.UNIAO;
    }

    public String getNomeCompleto() {
        return nome;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nome = nomeCompleto;
    }


    public TipoUsuario getTipoUsuario() { return tipoUsuario; }
    public void setTipoUsuario(TipoUsuario tipoUsuario) { this.tipoUsuario = tipoUsuario; }

    public String getPerfil() { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }

    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }

    public String getComarca() { return comarca; }
    public void setComarca(String comarca) { this.comarca = comarca; }

    public Boolean getAtivo() { return ativo; }
    public boolean isAtivo() { return Boolean.TRUE.equals(ativo); }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }

    public String getRegistroProfissional() { return registroProfissional; }
    public void setRegistroProfissional(String registroProfissional) { this.registroProfissional = registroProfissional; }

    public SituacaoConta getSituacaoConta() { return situacaoConta; }
    public void setSituacaoConta(SituacaoConta situacaoConta) { this.situacaoConta = situacaoConta; }

    public List<String> getEspecialidades() {
        if (especialidadesRaw == null || especialidadesRaw.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String item : especialidadesRaw.split("[,;|]")) {
            if (item != null) {
                String normalized = item.trim();
                if (!normalized.isBlank() && out.stream().noneMatch(v -> v.equalsIgnoreCase(normalized))) {
                    out.add(normalized);
                }
            }
        }
        return List.copyOf(out);
    }

    public void setEspecialidades(List<String> especialidades) {
        if (especialidades == null || especialidades.isEmpty()) {
            this.especialidadesRaw = null;
            return;
        }
        List<String> normalized = new ArrayList<>();
        for (String item : especialidades) {
            if (item == null) {
                continue;
            }
            String trimmed = item.trim();
            if (!trimmed.isBlank() && normalized.stream().noneMatch(v -> v.equalsIgnoreCase(trimmed))) {
                normalized.add(trimmed);
            }
        }
        this.especialidadesRaw = normalized.isEmpty() ? null : String.join(", ", normalized);
    }

    public void setEspecialidadesRaw(String especialidadesRaw) {
        this.especialidadesRaw = especialidadesRaw;
    }

    public String getEspecialidadesRaw() {
        return especialidadesRaw;
    }

    public boolean possuiEspecialidade(String especialidade) {
        if (especialidade == null || especialidade.isBlank()) {
            return false;
        }
        String alvo = especialidade.trim().toUpperCase(Locale.ROOT);
        return getEspecialidades().stream().map(v -> v.toUpperCase(Locale.ROOT)).anyMatch(alvo::equals);
    }

    public boolean isAdvogado() {
        return tipoUsuario == TipoUsuario.ADVOGADO || (oabNormalizada != null && !oabNormalizada.isBlank()) || (oab != null && !oab.isBlank());
    }

    public boolean isMagistrado() {
        return tipoUsuario != null && tipoUsuario.isMagistratura();
    }

    public boolean isMinisterioPublico() {
        return tipoUsuario != null && tipoUsuario.isMinisterioPublico();
    }

    public boolean isDefensoriaPublica() {
        return tipoUsuario != null && tipoUsuario.isDefensoriaPublica();
    }

    public boolean isAdminForum() {
        return tipoUsuario == TipoUsuario.ADMINISTRADOR || tipoUsuario == TipoUsuario.SERVIDOR_FORUM;
    }

    public boolean isServidorJudiciario() {
        return tipoUsuario != null && tipoUsuario.isServidorJudiciario();
    }

    public boolean isPerito() {
        return tipoUsuario != null && tipoUsuario.isPerito();
    }

    public boolean isAssessor() {
        return tipoUsuario != null && tipoUsuario.isAssessor();
    }

    public boolean isServidor() {
        return tipoUsuario == TipoUsuario.SERVIDOR || tipoUsuario == TipoUsuario.SERVIDOR_FORUM;
    }

    public PapelEquipe getPapelEquipe() {
        PapelEquipe parsed = PapelEquipe.fromString(perfil);
        if (parsed != null) {
            return parsed;
        }
        if (isMagistrado()) return PapelEquipe.JUIZ_GABINETE;
        if (isAdvogado()) return PapelEquipe.ADVOGADO_SENIOR;
        if (isServidorJudiciario()) return PapelEquipe.SERVIDOR;
        return PapelEquipe.CIDADAO;
    }

    public boolean isAtivoESemanticoValido() {
        return isAtivo() && email != null && !email.isBlank() && nome != null && !nome.isBlank();
    }

    public void syncPerfilETipoUsuario() {
        if ((perfil == null || perfil.isBlank()) && tipoUsuario != null) {
            perfil = tipoUsuario.name();
        }
        if (tipoUsuario == null && perfil != null && !perfil.isBlank()) {
            tipoUsuario = TipoUsuario.fromString(perfil);
        }
    }

    public static UsuarioBuilder builder() {
        return new UsuarioBuilder();
    }

    public static final class UsuarioBuilder {
        private final Usuario target = new Usuario();
        public UsuarioBuilder id(Long id) { target.id = id; return this; }
        public UsuarioBuilder nome(String nome) { target.nome = nome; return this; }
        public UsuarioBuilder email(String email) { target.email = email; return this; }
        public UsuarioBuilder cpf(String cpf) { target.cpf = cpf; return this; }
        public UsuarioBuilder oab(String oab) { target.oab = oab; return this; }
        public UsuarioBuilder oabNormalizada(String oabNormalizada) { target.oabNormalizada = oabNormalizada; return this; }
        public UsuarioBuilder oabUf(String oabUf) { target.oabUf = oabUf; return this; }
        public UsuarioBuilder oabSufixo(String oabSufixo) { target.oabSufixo = oabSufixo; return this; }
        public UsuarioBuilder tipoUsuario(TipoUsuario tipoUsuario) { target.tipoUsuario = tipoUsuario; return this; }
        public UsuarioBuilder perfil(String perfil) { target.perfil = perfil; return this; }
        public UsuarioBuilder senha(String senha) { target.senha = senha; return this; }
        public UsuarioBuilder uf(String uf) { target.uf = uf; return this; }
        public UsuarioBuilder comarca(String comarca) { target.comarca = comarca; return this; }
        public UsuarioBuilder ativo(Boolean ativo) { target.ativo = ativo; return this; }
        public UsuarioBuilder enteFederativo(EnteFederativo enteFederativo) { target.enteFederativo = enteFederativo; return this; }
        public UsuarioBuilder identidadeJuridicaId(UUID identidadeJuridicaId) { target.identidadeJuridicaId = identidadeJuridicaId; return this; }
        public UsuarioBuilder registroProfissional(String registroProfissional) { target.registroProfissional = registroProfissional; return this; }
        public UsuarioBuilder situacaoConta(SituacaoConta situacaoConta) { target.situacaoConta = situacaoConta; return this; }
        public UsuarioBuilder especialidadesRaw(String especialidadesRaw) { target.especialidadesRaw = especialidadesRaw; return this; }
        public UsuarioBuilder especialidades(java.util.Collection<String> especialidades) { target.setEspecialidades(especialidades == null ? java.util.List.of() : new java.util.ArrayList<>(especialidades)); return this; }
        public Usuario build() { return target; }
    }


}
