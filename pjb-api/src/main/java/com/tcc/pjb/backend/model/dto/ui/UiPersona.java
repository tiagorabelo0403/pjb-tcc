package com.tcc.pjb.backend.model.dto.ui;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

public enum UiPersona {
  SERVIDOR,
  MAGISTRATURA,
  MINISTERIO_PUBLICO,
  DEFENSORIA,
  ADVOCACIA,
  PROCURADORIA,
  POLICIA,
  AUXILIAR_JUSTICA,
  CIDADAO,
  ADMIN,
  OUTRO;

  public static UiPersona fromTipoUsuario(TipoUsuario t) {
    if (t == null) return OUTRO;
    if (t.isAdmin()) return ADMIN;
    if (t.isServidorJudiciario()) return SERVIDOR;
    if (t.isMagistratura()) return MAGISTRATURA;
    if (t.isMinisterioPublico()) return MINISTERIO_PUBLICO;
    if (t.isDefensoriaPublica()) return DEFENSORIA;
    if (t.isProcuradoria()) return PROCURADORIA;
    if (t == TipoUsuario.DELEGADO_POLICIA || t == TipoUsuario.AGENTE_POLICIAL) return POLICIA;
    if (t.isAuxiliarJustica()) return AUXILIAR_JUSTICA;
    if (t.isAdvocacia() || t == TipoUsuario.OAB_PRESIDENTE_SECCIONAL) return ADVOCACIA;
    if (t == TipoUsuario.CIDADAO) return CIDADAO;
    return OUTRO;
  }
}
