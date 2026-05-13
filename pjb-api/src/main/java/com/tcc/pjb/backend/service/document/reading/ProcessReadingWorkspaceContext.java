package com.tcc.pjb.backend.service.document.reading;

import com.tcc.pjb.backend.model.entity.EventoProcessual;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoPagina;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import java.util.List;
import java.util.Map;
import java.util.UUID;

record ProcessReadingWorkspaceContext(Processo processo,
                                      List<DocumentoProcessual> documentos,
                                      List<DocumentoPagina> paginas,
                                      List<DocumentoPagina> navigationPages,
                                      List<MovimentacaoProcessual> movimentacoes,
                                      List<EventoProcessual> eventos,
                                      long totalDocumentos,
                                      long totalPaginas,
                                      long paginasComTexto,
                                      Map<UUID, ProcessReadingPageCounter> documentStats) {
}
