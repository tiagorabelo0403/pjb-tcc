package com.tcc.pjb.backend.core.forum.routing;

public enum JudicialOrganKind {
  TJ,
  TRF,
  TRT,
  TRE,
  TJM,
  STM,
  STJ,
  STF,
  TST,
  TSE,
  UNKNOWN;

  public static final JudicialOrganKind VARA = UNKNOWN;
  public static final JudicialOrganKind TURMA = TJ;
  public static final JudicialOrganKind CAMARA = TJ;
  public static final JudicialOrganKind GABINETE = UNKNOWN;
  public static final JudicialOrganKind NUCLEO = UNKNOWN;
  public static final JudicialOrganKind SECRETARIA = UNKNOWN;
  public static final JudicialOrganKind COLEGIADO = UNKNOWN;
}
