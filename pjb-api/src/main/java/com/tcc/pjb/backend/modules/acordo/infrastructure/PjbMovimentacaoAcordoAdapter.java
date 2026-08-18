package com.tcc.pjb.backend.modules.acordo.infrastructure;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.acordo.api.MovimentacaoAcordoCommand;
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
    public void registrarSalaAberta(MovimentacaoAcordoCommand command) {
        registrar(command, "SALA_ACORDO_ABERTA");
    }

    @Override
    public void registrarTermoEnviadoHomologacao(MovimentacaoAcordoCommand command) {
        registrar(command, "ACORDO_TERMO_ENVIADO_HOMOLOGACAO");
    }

    @Override
    public void registrarHomologacao(MovimentacaoAcordoCommand command) {
        registrar(command, "ACORDO_HOMOLOGADO");
    }

    @Override
    public void registrarRejeicaoHomologacao(MovimentacaoAcordoCommand command) {
        registrar(command, "ACORDO_REJEITADO");
    }

    @Override
    public void registrarEncerramentoSemAcordo(MovimentacaoAcordoCommand command) {
        registrar(command, "ACORDO_ENCERRADO_SEM_COMPOSICAO");
    }

    private void registrar(MovimentacaoAcordoCommand command, String fallbackTipo) {
        if (command == null || command.processoId() == null) {
            throw new IllegalArgumentException("Comando de movimentacao de acordo invalido");
        }
        Processo processo = processoRepository.findById(command.processoId())
                .orElseThrow(() -> new IllegalArgumentException("Processo nao encontrado"));
        Usuario usuario = command.operadorId() == null ? null : usuarioRepository.findById(command.operadorId()).orElse(null);
        String tipo = nonBlank(command.tipo(), fallbackTipo);
        String origem = nonBlank(command.origem(), "ACORDO_PROCESSUAL");
        movimentacaoRepository.save(MovimentacaoProcessual.builder()
                .processo(processo)
                .ator(usuario)
                .descricao(limit(origem + " " + tipo + ": " + nonBlank(command.descricao(), "Movimentacao de sala de acordo processual."), 3000))
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
