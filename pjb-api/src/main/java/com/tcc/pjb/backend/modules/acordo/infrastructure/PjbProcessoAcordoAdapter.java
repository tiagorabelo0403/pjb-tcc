package com.tcc.pjb.backend.modules.acordo.infrastructure;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.modules.acordo.api.ProcessoAcordoContexto;
import com.tcc.pjb.backend.modules.acordo.api.ProcessoAcordoPort;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PjbProcessoAcordoAdapter implements ProcessoAcordoPort {

    private final ProcessoRepository processoRepository;
    private final MovimentacaoProcessualRepository movimentacaoRepository;

    public PjbProcessoAcordoAdapter(ProcessoRepository processoRepository,
                                    MovimentacaoProcessualRepository movimentacaoRepository) {
        this.processoRepository = processoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    @Override
    public boolean existeProcesso(Long processoId) {
        return processoId != null && processoRepository.existsById(processoId);
    }

    @Override
    public ProcessoAcordoContexto obterContextoProcessual(Long processoId) {
        Processo processo = processoRepository.findContextoCompletoById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo nao encontrado"));
        FaseProcessual fase = processo.getFaseAtual();
        String resumo = texto(processo.getJanelaAcordoResumo(), processo.getResultadoFinal(), processo.getObjetoProcessual());
        boolean antesContestacao = fase == FaseProcessual.AUTUACAO || fase == FaseProcessual.DISTRIBUICAO || fase == FaseProcessual.CITACAO;
        boolean antesAudiencia = containsAny(resumo, "AUDIENCIA", "CONCILIACAO", "MEDIACAO");
        boolean aposContestacao = fase == FaseProcessual.RESPOSTA || fase == FaseProcessual.SANEAMENTO || fase == FaseProcessual.INSTRUTORIA;
        boolean propostaFormal = containsAny(resumo, "PROPOSTA FORMAL", "PROPOSTA DE ACORDO", "CONTRAPROPOSTA");
        boolean aposPericia = fase == FaseProcessual.PERICIA_TECNICA || containsAny(resumo, "LAUDO", "PERICIA");
        boolean antesSentenca = fase == FaseProcessual.CONHECIMENTO || fase == FaseProcessual.SANEAMENTO || fase == FaseProcessual.INSTRUTORIA || fase == FaseProcessual.PERICIA_TECNICA;
        boolean faseRecursal = fase != null && fase.isRecursal();
        boolean execucao = fase != null && fase.isExecutionLike();
        boolean mutirao = containsAny(resumo, "MUTIRAO");
        boolean requerimento = containsAny(resumo, "REQUERIMENTO DE PARTE", "REQUERIMENTO");
        boolean determinacao = containsAny(resumo, "DETERMINACAO JUDICIAL", "DETERMINACAO");
        boolean cejusc = containsAny(resumo, "CEJUSC");
        boolean permiteAcordo = antesContestacao
                || antesAudiencia
                || (aposContestacao && propostaFormal)
                || aposPericia
                || antesSentenca
                || faseRecursal
                || execucao
                || mutirao
                || requerimento
                || determinacao
                || cejusc;
        return new ProcessoAcordoContexto(
                processoId,
                fase != null ? fase.name() : null,
                processo.isSigiloso(),
                permiteAcordo,
                processo.getClasseProcessual(),
                processo.getJurisdicao() != null ? processo.getJurisdicao().getId() : null,
                null,
                antesContestacao,
                antesAudiencia,
                aposContestacao,
                propostaFormal,
                aposPericia,
                antesSentenca,
                faseRecursal,
                execucao,
                mutirao,
                requerimento,
                determinacao,
                cejusc,
                processo.getPotencialAcordoScore(),
                processo.getJanelaAcordoResumo()
        );
    }

    @Override
    public boolean processoEstaEmSegredo(Long processoId) {
        return processoRepository.findById(processoId).map(Processo::isSigiloso).orElse(false);
    }

    @Override
    public boolean processoPermiteAcordo(Long processoId) {
        if (processoId == null) {
            return false;
        }
        return obterContextoProcessual(processoId).permiteAcordo();
    }

    @Override
    public void registrarMovimentacaoAcordo(Long processoId, String tipo, String descricao) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo nao encontrado"));
        movimentacaoRepository.save(MovimentacaoProcessual.builder()
                .processo(processo)
                .descricao(limit(nonBlank(tipo, "ACORDO") + ": " + nonBlank(descricao, "Movimentacao de acordo processual."), 3000))
                .dataMovimentacao(Instant.now())
                .build());
    }

    private boolean containsAny(String source, String... tokens) {
        if (source == null || source.isBlank()) {
            return false;
        }
        String normalized = source.toUpperCase(Locale.ROOT);
        for (String token : tokens) {
            if (normalized.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String texto(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                if (!builder.isEmpty()) {
                    builder.append(' ');
                }
                builder.append(value.trim());
            }
        }
        return builder.toString();
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String limit(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
