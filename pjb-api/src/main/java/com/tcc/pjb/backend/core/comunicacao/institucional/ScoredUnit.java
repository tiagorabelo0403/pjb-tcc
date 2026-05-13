package com.tcc.pjb.backend.core.comunicacao.institucional;

import java.util.List;

record ScoredUnit(com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional unit,
                  int score,
                  boolean eligible,
                  List<String> reasons) {
}
