package com.tcc.pjb.backend.model.entity.ui;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@PjbDataOwnership(module = PjbModuleId.IDENTIDADE_SEGURANCA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_usuario_calculo_experience_pref")
public class UsuarioCalculoExperiencePreference {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "principal_key", nullable = false, length = 190)
  private String principalKey;

  @Column(name = "equipe_ativa_id")
  private Long equipeAtivaId;

  @Column(name = "domain_code", length = 80)
  private String domainCode;

  @Column(name = "experience_mode", nullable = false, length = 40)
  private String experienceMode;

  @Column(name = "scope_type", nullable = false, length = 30)
  private String scopeType;

  @Column(name = "ramo_direito", length = 80)
  private String ramoDireito;

  @Column(name = "classe_processual", length = 120)
  private String classeProcessual;

  @Column(name = "tipo_causa", length = 120)
  private String tipoCausa;

  @Column(name = "perfil_equipe", length = 80)
  private String perfilEquipe;

  @Column(name = "tribunal", length = 80)
  private String tribunal;

  @Column(name = "sistema_origem", length = 80)
  private String sistemaOrigem;

  @Column(name = "institutional_policy", nullable = false)
  private boolean institutionalPolicy;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected UsuarioCalculoExperiencePreference() {
  }

  public UsuarioCalculoExperiencePreference(String principalKey, Long equipeAtivaId, String domainCode, String experienceMode, String scopeType, String ramoDireito, String classeProcessual, String tipoCausa, String perfilEquipe, String tribunal, String sistemaOrigem, boolean institutionalPolicy) {
    this.principalKey = principalKey;
    this.equipeAtivaId = equipeAtivaId;
    this.domainCode = domainCode;
    this.experienceMode = experienceMode;
    this.scopeType = scopeType;
    this.ramoDireito = ramoDireito;
    this.classeProcessual = classeProcessual;
    this.tipoCausa = tipoCausa;
    this.perfilEquipe = perfilEquipe;
    this.tribunal = tribunal;
    this.sistemaOrigem = sistemaOrigem;
    this.institutionalPolicy = institutionalPolicy;
    this.updatedAt = Instant.now();
  }

  public Long getId() { return id; }
  public String getPrincipalKey() { return principalKey; }
  public void setPrincipalKey(String principalKey) { this.principalKey = principalKey; }
  public Long getEquipeAtivaId() { return equipeAtivaId; }
  public void setEquipeAtivaId(Long equipeAtivaId) { this.equipeAtivaId = equipeAtivaId; }
  public String getDomainCode() { return domainCode; }
  public void setDomainCode(String domainCode) { this.domainCode = domainCode; }
  public String getExperienceMode() { return experienceMode; }
  public void setExperienceMode(String experienceMode) { this.experienceMode = experienceMode; }
  public String getScopeType() { return scopeType; }
  public void setScopeType(String scopeType) { this.scopeType = scopeType; }
  public String getRamoDireito() { return ramoDireito; }
  public void setRamoDireito(String ramoDireito) { this.ramoDireito = ramoDireito; }
  public String getClasseProcessual() { return classeProcessual; }
  public void setClasseProcessual(String classeProcessual) { this.classeProcessual = classeProcessual; }
  public String getTipoCausa() { return tipoCausa; }
  public void setTipoCausa(String tipoCausa) { this.tipoCausa = tipoCausa; }
  public String getPerfilEquipe() { return perfilEquipe; }
  public void setPerfilEquipe(String perfilEquipe) { this.perfilEquipe = perfilEquipe; }
  public String getTribunal() { return tribunal; }
  public void setTribunal(String tribunal) { this.tribunal = tribunal; }
  public String getSistemaOrigem() { return sistemaOrigem; }
  public void setSistemaOrigem(String sistemaOrigem) { this.sistemaOrigem = sistemaOrigem; }
  public boolean isInstitutionalPolicy() { return institutionalPolicy; }
  public void setInstitutionalPolicy(boolean institutionalPolicy) { this.institutionalPolicy = institutionalPolicy; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
