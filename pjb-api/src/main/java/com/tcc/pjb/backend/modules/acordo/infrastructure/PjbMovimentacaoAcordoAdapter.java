package com.tcc.pjb.backend.modules.acordo.infrastructure;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.acordo.api.MovimentacaoAcordoPort;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class PjbMovimentacaoAcordoAdapter implements MovimentacaoAcordoPort {

    private final ProcessoRepository processoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MovimentacaoProcessualRepository movimentacaoRepository;

    public PjbMovimentacaoAcordoAdapter(ProcessoRepository processoRepository,
                                        UsuarioRepository usuarioRepository,
                                        MovimentacaoProcessualRepository movimentacaoRepository) {
        this.processoRepository = processoRepository;
        this.usuarioRepository = usuarioRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    @Override
    public void registrarHomologacao(Long processoId, Long magistradoId, String descricao) {
        registrar(processoId, magistradoId, "ACORDO_HOMOLOGADO", descricao);
    }

    @Override
    public void registrarEncerramentoSemAcordo(Long processoId, Long usuarioId, String descricao) {
        registrar(processoId, usuarioId, "ACORDO_ENCERRADO_SEM_COMPOSICAO", descricao);
    }

    private void registrar(Long processoId, Long usuarioId, String tipo, String descricao) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo nao encontrado"));
        Usuario usuario = usuarioId == null ? null : usuarioRepository.findById(usuarioId).orElse(null);
        movimentacaoRepository.save(MovimentacaoProcessual.builder()
                .processo(processo)
                .ator(usuario)
                .descricao(limit(tipo + ": " + nonBlank(descricao, "Movimentacao de sala de acordo processual."), 3000))
                .dataMovimentacao(Instant.now())
                .build());
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String limit(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
